package com.hookflow.hookflow.common.exception;

import com.hookflow.hookflow.common.queue.QueuePublishException;
import com.hookflow.hookflow.filter.RequestIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QueuePublishException.class)
    public ResponseEntity<ErrorResponse> handleQueuePublishException(QueuePublishException ex) {
        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);
        log.error("Queue publish failed [requestId={}]: {}", requestId, ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                requestId,
                "QUEUE_PUBLISH_FAILED",
                "Failed to enqueue the event for delivery. Please retry shortly.",
                Instant.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
}
