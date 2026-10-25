package com.hookflow.hookflow.common.queue;

public class QueuePublishException extends RuntimeException {

    public QueuePublishException(String message) {
        super(message);
    }

    public QueuePublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
