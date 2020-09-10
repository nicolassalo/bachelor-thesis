package com.model;

public class ResponseMessage {

    String message;
    long timestamp;

    public ResponseMessage(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
}
