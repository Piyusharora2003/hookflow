package com.hookflow.hookflow.publisher.exception;

public class IdempotencyConflictException extends RuntimeException {

    private final String idempotencyKey;

    public IdempotencyConflictException(String idempotencyKey) {
        super("An event with idempotency key '" + idempotencyKey + "' already exists for this tenant");
        this.idempotencyKey = idempotencyKey;
    }

    public IdempotencyConflictException(String idempotencyKey, String message) {
        super(message);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}

