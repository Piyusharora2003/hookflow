package com.hookflow.hookflow.common.exception;

import com.hookflow.hookflow.common.queue.QueuePublishException;
import com.hookflow.hookflow.filter.RequestIdFilter;
import com.hookflow.hookflow.publisher.exception.IdempotencyConflictException;
import com.hookflow.hookflow.publisher.persistence.exception.EventPersistenceException;
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

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflictException(IdempotencyConflictException ex) {
        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);
        log.warn("Idempotency conflict [requestId={}, key={}]: {}", requestId, ex.getIdempotencyKey(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                requestId,
                "IDEMPOTENCY_KEY_CONFLICT",
                ex.getMessage(),
                Instant.now(),
                HttpStatus.CONFLICT.value()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(EventPersistenceException.class)
    public ResponseEntity<ErrorResponse> handleEventPersistenceException(EventPersistenceException ex) {
        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);
        log.error("Event persistence failed [requestId={}]: {}", requestId, ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                requestId,
                "EVENT_PERSISTENCE_FAILED",
                "Failed to persist event record. Please retry shortly.",
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

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