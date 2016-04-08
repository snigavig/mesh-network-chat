package com.example.chatroom.model;

import java.io.Serializable;

public class TechnicalMessage implements Serializable {
    private final String message;
    private String data;
    private long timestamp;

    public TechnicalMessage(String message) {
        this.message = message;
    }

    public TechnicalMessage(Object object) {
        TechnicalMessage incomingMessage = (TechnicalMessage) object;
        this.message = incomingMessage.getMessage();
        this.data = incomingMessage.getData();
    }

    public String getMessage() {
        return message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
