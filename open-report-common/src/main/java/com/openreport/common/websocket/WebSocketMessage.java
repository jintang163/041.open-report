package com.openreport.common.websocket;

import lombok.Data;

import java.io.Serializable;

@Data
public class WebSocketMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;

    private String topic;

    private Object payload;

    private Long timestamp;

    private String messageId;

    public WebSocketMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public WebSocketMessage(String type, String topic, Object payload) {
        this.type = type;
        this.topic = topic;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public static WebSocketMessage of(String type, String topic, Object payload) {
        return new WebSocketMessage(type, topic, payload);
    }
}
