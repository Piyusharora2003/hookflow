package com.hookflow.hookflow.publisher.constant;

public final class PublisherConstants {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String X_REQUEST_ID_HEADER = "X-Request-Id";
    private PublisherConstants() {
        // Prevent instantiation
    }
}
