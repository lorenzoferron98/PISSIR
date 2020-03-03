package it.uniupo;

import java.util.ArrayList;
import java.util.List;

/**
 * Data result.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.20
 */
public class Data {

    private String topic;
    private int qos;
    private long sendMsg;
    private long rcvMsg;
    private long elapsedTime;
    private List<Long> rtts = new ArrayList<>(0);

    public Data() {
        // Empty body...
    }

    public Data(String topic, long sendMsg, long rcvMsg, long elapsedTime) {
        this.topic = topic;
        this.sendMsg = sendMsg;
        this.rcvMsg = rcvMsg;
        this.elapsedTime = elapsedTime;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getSendMsg() {
        return sendMsg;
    }

    public void setSendMsg(long sendMsg) {
        this.sendMsg = sendMsg;
    }

    public long getRcvMsg() {
        return rcvMsg;
    }

    public void setRcvMsg(long rcvMsg) {
        this.rcvMsg = rcvMsg;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public List<Long> getRtts() {
        return rtts;
    }

    public double getAverageRTT() {
        return rtts.stream().mapToLong(l -> l).average().orElse(0);
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

}
