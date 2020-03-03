package it.uniupo.descriptors;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * A class to represent an executor into environment.
 * An executor can be a publisher or subscriber.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.20
 */
public class Executor {

    @SerializedName("size")
    @Expose(serialize = false)
    private int size;

    @SerializedName("qos")
    @Expose(serialize = false)
    private int qos;

    @SerializedName("topic")
    @Expose(serialize = false)
    private String topic = "/";

    @SerializedName("payload")
    @Expose(serialize = false)
    private String payload = null;

    /**
     * No args constructor for use in serialization
     */
    public Executor() {
        //Empty body...
    }

    public Executor(int size, int qos, String topic, String payload) {
        this.size = size;
        this.qos = qos;
        this.topic = topic;
        this.payload = payload;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

}
