package it.uniupo.descriptors;

import java.io.Serializable;

/**
 * A class to represent a message with additional info.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.20
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 6919971531219311210L;

    private String body;
    private long sendTime;

    public Message(String body, long sendTime) {
        this.body = body;
        this.sendTime = sendTime;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }
}
