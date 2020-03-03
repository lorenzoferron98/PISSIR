package rpc.over.mqtt.jsonrpc;

import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.tools.jsonrpc.JsonRpcException;
import com.rabbitmq.tools.jsonrpc.JsonRpcMapper;
import com.rabbitmq.tools.jsonrpc.ProcedureDescription;
import com.rabbitmq.tools.jsonrpc.ServiceDescription;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * <a href="https://www.jsonrpc.org/">JSON-RPC</a> is a lightweight
 * RPC mechanism using <a href="https://www.json.org/">JSON</a>
 * as a data language for request and reply messages. It is
 * rapidly becoming a standard in web development, where it is
 * used to make RPC requests over HTTP.
 */
public class JsonRpcClient implements InvocationHandler {

    private final static Random GENERATOR = new Random();

    private static volatile boolean keepRunning = true;
    private static volatile String replyStr;

    private final JsonRpcMapper mapper;
    private final String serverUri;
    private final String replyToTopic;
    private final String sendToTopic;
    /**
     * Holds the JSON-RPC service description for this client.
     */
    private ServiceDescription serviceDescription;
    private MqttAsyncClient mqttClient;
    private int actionTimeout = 5000;

    public JsonRpcClient(String serverUri, JsonRpcMapper mapper, String sendToTopic)
            throws JsonRpcException, TimeoutException, IOException, MqttException {
        this.serverUri = serverUri;
        this.mapper = mapper;
        this.sendToTopic = sendToTopic;
        this.replyToTopic = "/F3rr0N/rpcanswer/" + UUID.randomUUID().toString();
        initMqttClient();
        retrieveServiceDescription();
    }

    public JsonRpcClient(String serverUri, JsonRpcMapper mapper, String sendToTopic, int timeout)
            throws JsonRpcException, TimeoutException, IOException, MqttException {
        this(serverUri, mapper, sendToTopic);
        this.actionTimeout = timeout;
    }

    private void initMqttClient() {
        String clientID = "mqtt-client-" + UUID.randomUUID().toString();

        try {
            this.mqttClient = new MqttAsyncClient(serverUri, clientID, new MemoryPersistence());
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("Connection to broker messaging lost! " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    replyStr = new String(message.getPayload());
                    keepRunning = false;
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

            IMqttToken connectToken = mqttClient.connect();
            connectToken.waitForCompletion(actionTimeout);
            IMqttToken subToken = this.mqttClient.subscribe(replyToTopic, 1);
            subToken.waitForCompletion(actionTimeout);
            if (!subToken.isComplete() || subToken.getException() != null) {
                System.err.println("Error subscribing: " + subToken.getException());
                System.exit(1);
            }
            if (subToken.getGrantedQos()[0] != 1) {
                System.err.println("Expected Qos level 1 but got Qos level: " + subToken.getGrantedQos()[0]);
                System.exit(1);
            }
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Private API - parses a JSON-RPC reply object, checking it for exceptions.
     *
     * @return the result contained within the reply, if no exception is found
     * Throws JsonRpcException if the reply object contained an exception
     */
    private Object checkReply(JsonRpcMapper.JsonRpcResponse reply)
            throws JsonRpcException {
        if (reply.getError() != null) throw reply.getException();
        return reply.getResult();
    }

    /**
     * Public API - builds, encodes and sends a JSON-RPC request, and
     * waits for the response.
     *
     * @return the result contained within the reply, if no exception is found
     */
    public Object call(String method, Object[] params) throws MqttException, TimeoutException, IOException, JsonRpcException {
        Map<String, Object> request = new HashMap<>();
        request.put("id", "rpc" + GENERATOR.nextInt(Integer.MAX_VALUE));
        request.put("method", method);
        request.put("version", ServiceDescription.JSON_RPC_VERSION);
        params = (params == null) ? new Object[0] : params;
        params = ArrayUtils.add(params, replyToTopic);
        request.put("params", params);
        String requestStr = mapper.write(request);
        publishMessage(requestStr.getBytes(), 0, false, sendToTopic);
        params = ArrayUtils.remove(params, params.length - 1);

        keepRunning = true;
        long end = System.currentTimeMillis() + actionTimeout;
        while (keepRunning && System.currentTimeMillis() <= end) ;
        if (keepRunning) throw new TimeoutException("Exceeded action timeout: " + actionTimeout + "ms");
        try {
            Class<?> expectedType;
            if ("system.describe".equals(method) && params.length == 0) expectedType = Map.class;
            else {
                ProcedureDescription proc = serviceDescription.getProcedure(method, params.length);
                expectedType = proc.getReturnType();
            }
            JsonRpcMapper.JsonRpcResponse reply = mapper.parse(replyStr, expectedType);

            return checkReply(reply);
        } catch (ShutdownSignalException ex) {
            throw new IOException(ex.getMessage()); // wrap, re-throw
        }
    }


    /**
     * Simple helper function to publish a message.
     *
     * @param payload
     * @param qos
     * @param retain
     * @param topic
     * @throws MqttPersistenceException
     * @throws MqttException
     */
    private void publishMessage(byte[] payload, int qos, boolean retain, String topic)
            throws MqttPersistenceException, MqttException {
        MqttMessage v3Message = new MqttMessage(payload);
        v3Message.setQos(qos);
        v3Message.setRetained(retain);
        IMqttDeliveryToken deliveryToken = mqttClient.publish(topic, v3Message);
        deliveryToken.waitForCompletion(actionTimeout);
    }

    /**
     * Public API - implements InvocationHandler.invoke. This is
     * useful for constructing dynamic proxies for JSON-RPC
     * interfaces.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        return call(method.getName(), args);
    }

    /**
     * Public API - gets a dynamic proxy for a particular interface class.
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> klass)
            throws IllegalArgumentException {
        return (T) Proxy.newProxyInstance(klass.getClassLoader(),
                new Class[]{klass},
                this);
    }

    /**
     * Public API - gets the service description record that this
     * service loaded from the server itself at construction time.
     */
    public ServiceDescription getServiceDescription() {
        return serviceDescription;
    }

    /**
     * Private API - invokes the "system.describe" method on the
     * server, and parses and stores the resulting service description
     * in this object.
     * TODO: Avoid calling this from the constructor.
     *
     * @throws TimeoutException if a response is not received within the timeout specified, if any
     */
    private void retrieveServiceDescription() throws IOException, JsonRpcException, TimeoutException, MqttException {
        @SuppressWarnings("unchecked")
        Map<String, Object> rawServiceDescription = (Map<String, Object>) call("system.describe", null);
        serviceDescription = new ServiceDescription(rawServiceDescription);
    }

    public void disconnectClient() throws MqttException {
        // Disconnect
        IMqttToken disconnectToken = mqttClient.disconnect();
        disconnectToken.waitForCompletion(actionTimeout);
    }

    public void closeClientAndExit() {
        // Close the client
        try {
            this.mqttClient.close();
            //mainThread.join();
        } catch (MqttException e) {
            // End the Application
            System.exit(1);
        }
    }
}
