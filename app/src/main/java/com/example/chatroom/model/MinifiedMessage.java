package com.example.chatroom.model;

import java.io.Serializable;

public class MinifiedMessage implements Serializable {

    public final String uuid;
    public final long created;
    public final String text;

    public MinifiedMessage(Message message) {
        this.uuid = message.getUuid();
        this.created = message.getCreated();
        this.text = message.getText();
    }

    public MinifiedMessage(Object object) {
        MinifiedMessage incomingMessage = (MinifiedMessage) object;
        this.uuid = incomingMessage.uuid;
        this.created = incomingMessage.created;
        this.text = incomingMessage.text;
    }
}
