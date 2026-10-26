package com.hookflow.hookflow.common.exception;

import java.time.Instant;

public record ErrorResponse(
        String requestId,
        String error,
        String message,
        Instant timestamp,
        int status
) {
}
