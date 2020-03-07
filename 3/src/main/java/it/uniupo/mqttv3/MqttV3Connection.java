package it.uniupo.mqttv3;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.UUID;

public class MqttV3Connection {

    private String hostURI;
    private String clientID;
    private MqttConnectOptions conOpts = new MqttConnectOptions();
    private boolean automaticReconnect = false;

    public MqttV3Connection(String hostURI) {
        this.hostURI = hostURI;

        this.clientID = "mqtt-client-" + UUID.randomUUID().toString();
    }

    public String getHostURI() {
        return hostURI;
    }

    public String getClientID() {
        return clientID;
    }

    public MqttConnectOptions getConOpts() {
        return conOpts;
    }

    public boolean isAutomaticReconnectEnabled() {
        return this.automaticReconnect;
    }

    public void setMaxInflight(int maxInflight) {
        conOpts.setMaxInflight(maxInflight);
    }

}
