package rpc.json;

import com.fasterxml.jackson.databind.node.TextNode;
import com.rabbitmq.tools.jsonrpc.DefaultJsonRpcMapper;
import com.rabbitmq.tools.jsonrpc.JsonRpcMapper;
import com.rabbitmq.tools.jsonrpc.ProcedureDescription;
import com.rabbitmq.tools.jsonrpc.ServiceDescription;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JSON-RPC it.uniupo.Server class.
 * {@link com.rabbitmq.tools.jsonrpc.JsonRpcServer} delegates JSON parsing and generating to
 * a {@link JsonRpcMapper}.
 *
 * @see JsonRpcClient
 * @see JsonRpcMapper
 * @see com.rabbitmq.tools.jsonrpc.JacksonJsonRpcMapper
 */
public class JsonRpcServer {

    private static volatile boolean keepRunning = true;
    // To allow a graceful disconnect.
    final Thread mainThread = Thread.currentThread();
    private final JsonRpcMapper mapper;
    private final String serverUri;
    private final String listenTopic;
    /**
     * Holds the JSON-RPC service description for this client.
     */
    private ServiceDescription serviceDescription;
    /**
     * The instance backing this server.
     */
    private Object interfaceInstance;
    private MqttAsyncClient mqttClient;

    public JsonRpcServer(String serverUri, String sendToTopic, Class<?> interfaceClass, Object interfaceInstance) {
        this(serverUri, sendToTopic, interfaceClass, interfaceInstance, new DefaultJsonRpcMapper());
    }

    /**
     * Construct a server that talks to the outside world using the
     * given URI.
     *
     * @param serverUri         connection properties
     * @param listenTopic       MQTT topic to use
     * @param interfaceClass    Java interface that this server is exposing to the world
     * @param interfaceInstance Java instance (of interfaceClass) that is being exposed
     * @param mapper            JSON mapper
     */
    public JsonRpcServer(String serverUri, String listenTopic, Class<?> interfaceClass, Object interfaceInstance, JsonRpcMapper mapper) {
        this.serverUri = serverUri;
        this.mapper = mapper;
        this.listenTopic = listenTopic;
        initMqttClient();
        init(interfaceClass, interfaceInstance);
    }

    private void init(Class<?> interfaceClass, Object interfaceInstance) {
        /**
         * The interface this server implements.
         */
        this.interfaceInstance = interfaceInstance;
        this.serviceDescription = new ServiceDescription(interfaceClass);
    }

    private void initMqttClient() {
        String clientID = "mqtt-service-" + UUID.randomUUID().toString();

        try {
            this.mqttClient = new MqttAsyncClient(serverUri, clientID, new MemoryPersistence());
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("Connection to broker messaging lost! " + cause.getMessage());
                    keepRunning = false;
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Object id;
                    String method;
                    String replyToTopic = null;
                    Object[] params;
                    String response;

                    try {
                        JsonRpcMapper.JsonRpcRequest request = mapper.parse(new String(message.getPayload()), serviceDescription);
                        if (request == null) {
                            response = errorResponse(null, 400, "Bad Request", null);
                        } else {
                            params = request.getParameters();
                            replyToTopic = ((TextNode) params[params.length - 1]).textValue();
                            params = ArrayUtils.remove(params, params.length - 1);
                            if (!ServiceDescription.JSON_RPC_VERSION.equals(request.getVersion())) {
                                response = errorResponse(null, 505, "JSONRPC version not supported", null);
                            } else {
                                id = request.getId();
                                method = request.getMethod();
                                if (request.isSystemDescribe()) response = resultResponse(id, serviceDescription);
                                else if (request.isSystem())
                                    response = errorResponse(id, 403, "System methods forbidden", null);
                                else {
                                    Object result;
                                    try {
                                        Method matchingMethod = matchingMethod(method, params);
                                        result = matchingMethod.invoke(interfaceInstance, params);
                                        response = resultResponse(id, result);
                                    } catch (Throwable t) {
                                        response = errorResponse(id, 500, "Internal it.uniupo.Server Error", t);
                                    }
                                }
                            }
                        }
                    } catch (ClassCastException cce) {
                        // Bogus request!
                        response = errorResponse(null, 400, "Bad Request", null);
                    }

                    publishMessage(response.getBytes(), 1, false, replyToTopic);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // NO-OP
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> keepRunning = false));
        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Public API - main server loop. Call this to begin processing
     * requests.
     */
    public void mainloop() throws MqttException {
        IMqttToken connectToken = mqttClient.connect();
        connectToken.waitForCompletion();
        IMqttToken subToken = this.mqttClient.subscribe(listenTopic, 1);
        subToken.waitForCompletion();
        if (!subToken.isComplete() || subToken.getException() != null) {
            System.err.println("Error subscribing: " + subToken.getException());
            System.exit(1);
        }
        if (subToken.getGrantedQos()[0] != 1) {
            System.err.println("Expected Qos level 1 but got Qos level: " + subToken.getGrantedQos()[0]);
            System.exit(1);
        }

        while (keepRunning) ;
        disconnectClient();
        closeClientAndExit();
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
        mqttClient.publish(topic, v3Message);
    }

    /**
     * Construct and encode a JSON-RPC success response for the
     * request ID given, using the result value passed in.
     */
    private String resultResponse(Object id, Object result) {
        return response(id, "result", result);
    }

    /**
     * Retrieves the best matching method for the given method name and parameters.
     * <p>
     * Subclasses may override this if they have specialised
     * dispatching requirements, so long as they continue to honour
     * their ServiceDescription.
     */
    public Method matchingMethod(String methodName, Object[] params) {
        ProcedureDescription proc = serviceDescription.getProcedure(methodName, params.length);
        return proc.internal_getMethod();
    }

    /**
     * Construct and encode a JSON-RPC error response for the request
     * ID given, using the code, message, and possible
     * (JSON-encodable) argument passed in.
     */
    private String errorResponse(Object id, int code, String message, Object errorArg) {
        Map<String, Object> err = new HashMap<>();
        err.put("name", "JSONRPCError");
        err.put("code", code);
        err.put("message", message);
        err.put("error", errorArg);
        return response(id, "error", err);
    }

    /**
     * Private API - used by errorResponse and resultResponse.
     */
    private String response(Object id, String label, Object value) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("version", ServiceDescription.JSON_RPC_VERSION);
        if (id != null) {
            resp.put("id", id);
        }
        resp.put(label, value);
        return mapper.write(resp);
    }


    /**
     * Public API - gets the service description record that this
     * service built from interfaceClass at construction time.
     */
    public ServiceDescription getServiceDescription() {
        return serviceDescription;
    }

    public void disconnectClient() throws MqttException {
        // Disconnect
        mqttClient.disconnect();
    }

    public void closeClientAndExit() {
        // Close the client
        try {
            this.mqttClient.close();
            mainThread.join();
        } catch (MqttException | InterruptedException e) {
            // End the Application
            System.exit(1);
        }
    }
}
