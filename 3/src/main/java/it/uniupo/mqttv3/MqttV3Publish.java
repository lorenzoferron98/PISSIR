package it.uniupo.mqttv3;

public class MqttV3Publish {

    private String payload;
    private int qos;
    private String topic;
    private boolean retain = false;

    public MqttV3Publish(String payload, int qos, String topic) {
        this.payload = payload;
        this.qos = qos;
        this.topic = topic;
    }

    public byte[] getPayloadInBytes() {
        return payload.getBytes();
    }

    public String getPayload() {
        return payload;
    }

    public int getQos() {
        return qos;
    }


    public String getTopic() {
        return topic;
    }

    public boolean isRetain() {
        return retain;
    }
}
