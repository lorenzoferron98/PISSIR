package it.uniupo.mqttv3;

import it.uniupo.Data;
import it.uniupo.descriptors.Message;
import it.uniupo.util.ByteUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class MqttV3Executor implements MqttCallback {

    MqttV3Connection v3ConnectionParameters;
    MqttV3Publish v3PublishParameters;
    MqttV3Subscribe v3SubscriptionParameters;
    MqttAsyncClient v3Client;
    private long timeout;
    private Data data = new Data();
    private Mode mode;

    /**
     * Initialises the MQTTv3 Executor
     *
     * @param qos
     * @param topic
     * @param payload
     * @param timeout
     * @param hostURI
     */
    public MqttV3Executor(int qos, String topic, String payload, int timeout, String hostURI) {
        data.setTopic(topic);
        data.setQos(qos);
        data.setMode(mode = payload == null ? Mode.SUB : Mode.PUB);
        this.timeout = TimeUnit.SECONDS.toMillis(timeout);
        this.v3ConnectionParameters = new MqttV3Connection(hostURI);
        if (payload != null) {
            this.v3ConnectionParameters.setMaxInflight(3000000);
            this.v3PublishParameters = new MqttV3Publish(payload, qos, topic);
        } else
            this.v3SubscriptionParameters = new MqttV3Subscribe(topic, qos);
    }

    @Override
    public void connectionLost(Throwable cause) {
        if (!v3ConnectionParameters.isAutomaticReconnectEnabled())
            closeClientAndExit();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        data.setRcvMsg(data.getRcvMsg() + 1);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // NO-OP
    }

    public void execute(CyclicBarrier gate) {
        try {
            // Create Client
            this.v3Client = new MqttAsyncClient(this.v3ConnectionParameters.getHostURI(),
                    this.v3ConnectionParameters.getClientID(), new MemoryPersistence());
            this.v3Client.setCallback(this);

            // Connect to Server
            IMqttToken connectToken = v3Client.connect(v3ConnectionParameters.getConOpts());

            // Execute action based on mode
            if (mode == Mode.PUB) {
                // Publish a message
                long msgCount = 0;
                connectToken.waitForCompletion();
                gate.await();
                long start = System.currentTimeMillis();
                long end = start + timeout;

                for (; System.currentTimeMillis() <= end; msgCount++) {
                    Message message = new Message(this.v3PublishParameters.getPayload(), System.currentTimeMillis());
                    publishMessage(ByteUtils.objToByte(message), this.v3PublishParameters.getQos(),
                            this.v3PublishParameters.isRetain(), this.v3PublishParameters.getTopic());
                }
                data.setSendMsg(msgCount);
                data.setElapsedTime(System.currentTimeMillis() - start);
            } else {
                connectToken.waitForCompletion();
                // Subscribe to a topic
                IMqttToken subToken = this.v3Client.subscribe(v3SubscriptionParameters.getTopic(),
                        v3SubscriptionParameters.getQos());
                gate.await();
                long start = System.currentTimeMillis();

                for (long end = start + timeout; System.currentTimeMillis() <= end;) ;
                data.setElapsedTime(System.currentTimeMillis() - start);
            }

            // Close the client
            disconnectClient();
            closeClientAndExit();
        } catch (MqttException | IOException | InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    private void closeClientAndExit() {
        // Close the client
        try {
            this.v3Client.close();
            //mainThread.join();
        } catch (MqttException e) {
            // End the Application
            System.exit(1);
        }
    }

    private void disconnectClient() throws MqttException {
        // Disconnect
        IMqttToken disconnectToken = v3Client.disconnect(timeout * 2);
        disconnectToken.waitForCompletion();
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
        IMqttDeliveryToken deliveryToken = v3Client.publish(topic, v3Message);
        deliveryToken.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                try {
                    data.getRtts().add(System.currentTimeMillis() - ((Message) ByteUtils.byteToObj(v3Message.getPayload())).getSendTime());
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                // NO-OP
            }
        });
        deliveryToken.waitForCompletion();
    }

    public Data getResults() {
        return data;
    }
}
