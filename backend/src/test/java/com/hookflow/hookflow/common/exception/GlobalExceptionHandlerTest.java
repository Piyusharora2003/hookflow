package com.hookflow.hookflow.common.exception;

import com.hookflow.hookflow.common.queue.QueuePublishException;
import com.hookflow.hookflow.filter.RequestIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        MDC.put(RequestIdFilter.MDC_REQUEST_ID_KEY, "req_err_789");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void testHandleQueuePublishException() {
        QueuePublishException exception = new QueuePublishException("Queue is down");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleQueuePublishException(exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("req_err_789", response.getBody().requestId());
        assertEquals("QUEUE_PUBLISH_FAILED", response.getBody().error());
        assertEquals(503, response.getBody().status());
    }

    @Test
    void testHandleEventPersistenceException() {
        com.hookflow.hookflow.publisher.persistence.exception.EventPersistenceException exception =
                new com.hookflow.hookflow.publisher.persistence.exception.EventPersistenceException("DB connection error");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleEventPersistenceException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("req_err_789", response.getBody().requestId());
        assertEquals("EVENT_PERSISTENCE_FAILED", response.getBody().error());
        assertEquals(500, response.getBody().status());
    }
}
