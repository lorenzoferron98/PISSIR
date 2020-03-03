package it.uniupo.mqttv3;

public class MqttV3Subscribe {

    private String topic;
    private int qos = 0;
    private boolean verbose = false;

    public MqttV3Subscribe(String topic, int qos) {
        this.topic = topic;
        this.qos = qos;
    }

    public String getTopic() {
        return topic;
    }

    public int getQos() {
        return qos;
    }

    public boolean isVerbose() {
        return verbose;
    }
}
