package com.hookflow.hookflow.publisher.persistence.exception;

public class EventPersistenceException extends RuntimeException {

    public EventPersistenceException(String message) {
        super(message);
    }

    public EventPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
