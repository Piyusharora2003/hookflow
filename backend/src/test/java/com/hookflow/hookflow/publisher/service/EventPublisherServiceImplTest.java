package com.hookflow.hookflow.publisher.service;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.common.queue.QueueAdapter;
import com.hookflow.hookflow.common.queue.QueueMessage;
import com.hookflow.hookflow.common.queue.QueuePublishException;
import com.hookflow.hookflow.filter.RequestIdFilter;
import com.hookflow.hookflow.publisher.dto.PublishEventRequest;
import com.hookflow.hookflow.publisher.dto.PublishEventResponse;
import com.hookflow.hookflow.publisher.exception.IdempotencyConflictException;
import com.hookflow.hookflow.publisher.persistence.exception.EventPersistenceException;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;
import com.hookflow.hookflow.publisher.persistence.port.EventStorePort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceImplTest {

    @Mock
    private QueueAdapter queueAdapter;

    @Mock
    private EventStorePort eventStorePort;

    private EventPublisherServiceImpl eventPublisherService;
    private final String queueName = "test-events-queue";
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        eventPublisherService = new EventPublisherServiceImpl(
                queueAdapter,
                eventStorePort,
                queueName,
                2,
                10,
                1.5,
                50
        );
        MDC.put(RequestIdFilter.MDC_REQUEST_ID_KEY, "req_abc123");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void testPublishEventSuccess() {
        PublishEventRequest request = new PublishEventRequest(
                "order.completed",
                "https://webhook.site/endpoint",
                Map.of("amount", 99.99),
                List.of("production", "v1"),
                Instant.now()
        );

        doAnswer(invocation -> invocation.getArgument(0))
                .when(eventStorePort).save(any(EventRecord.class));

        PublishEventResponse response = eventPublisherService.publishEvent(tenantId, request, "idempotency_key_1");

        assertNotNull(response);
        assertNotNull(response.eventId());
        assertEquals(EventStatus.PUBLISHED_TO_QUEUE.name(), response.status());
        assertEquals("order.completed", response.eventType());
        assertEquals("https://webhook.site/endpoint", response.destination());

        // Verify event was saved to DB with PENDING_PUBLISH before queue publishing
        ArgumentCaptor<EventRecord> recordCaptor = ArgumentCaptor.forClass(EventRecord.class);
        verify(eventStorePort).save(recordCaptor.capture());
        EventRecord capturedRecord = recordCaptor.getValue();
        assertEquals(tenantId, capturedRecord.getTenantId());
        assertEquals("idempotency_key_1", capturedRecord.getIdempotencyKey());
        assertEquals("req_abc123", capturedRecord.getRequestId());
        assertEquals("order.completed", capturedRecord.getEventType());
        assertEquals("https://webhook.site/endpoint", capturedRecord.getDestination());
        assertEquals(EventStatus.PENDING_PUBLISH, capturedRecord.getStatus());
        assertEquals(request.payload(), capturedRecord.getPayload());
        assertEquals(request.tags(), capturedRecord.getTags());

        // Verify queueAdapter.publish was called
        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueueMessage<PublishEventRequest>> queueCaptor = ArgumentCaptor.forClass(QueueMessage.class);
        verify(queueAdapter).publish(queueCaptor.capture(), eq(queueName));

        QueueMessage<PublishEventRequest> capturedMessage = queueCaptor.getValue();
        assertEquals("req_abc123", capturedMessage.getRequestId());
        assertEquals(capturedRecord.getId().toString(), capturedMessage.getMessageId());
        assertEquals("order.completed", capturedMessage.getEventType());
        assertEquals(request, capturedMessage.getBody());

        // Verify updateStatus was called on success to set PUBLISHED_TO_QUEUE
        verify(eventStorePort).updateStatus(capturedRecord.getId(), EventStatus.PUBLISHED_TO_QUEUE);
    }

    @Test
    void testPublishEventWhenQueuePublishFailsUpdatesStatusToPublishFailedAndReturnsResponse() {
        PublishEventRequest request = new PublishEventRequest(
                "order.completed",
                "https://webhook.site/endpoint",
                Map.of("amount", 99.99),
                List.of("production"),
                Instant.now()
        );

        doAnswer(invocation -> invocation.getArgument(0))
                .when(eventStorePort).save(any(EventRecord.class));

        doThrow(new QueuePublishException("SQS down"))
                .when(queueAdapter).publish(any(), eq(queueName));

        PublishEventResponse response = eventPublisherService.publishEvent(tenantId, request, "idempotency_key_fail");

        assertNotNull(response);
        assertNotNull(response.eventId());
        assertEquals(EventStatus.PUBLISH_FAILED.name(), response.status());

        // Verify DB record was saved initially with PENDING_PUBLISH
        verify(eventStorePort).save(any(EventRecord.class));
        // Verify retry occurred (maxAttempts = 2)
        verify(queueAdapter, times(2)).publish(any(), eq(queueName));
        // Verify DB record status was updated to PUBLISH_FAILED with the saved ID
        ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(eventStorePort).updateStatus(idCaptor.capture(), eq(EventStatus.PUBLISH_FAILED));
        assertEquals(response.eventId(), idCaptor.getValue().toString());
    }

    @Test
    void testPublishEventDuplicateIdempotencyKeyThrowsConflictException() {
        PublishEventRequest request = new PublishEventRequest(
                "order.completed",
                "https://webhook.site/endpoint",
                Map.of("amount", 99.99),
                List.of("production"),
                Instant.now()
        );

        when(eventStorePort.existsByTenantAndIdempotencyKey(tenantId, "duplicate_key")).thenReturn(true);

        IdempotencyConflictException thrown = assertThrows(IdempotencyConflictException.class, () ->
                eventPublisherService.publishEvent(tenantId, request, "duplicate_key")
        );

        assertNotNull(thrown);
        assertEquals("duplicate_key", thrown.getIdempotencyKey());
        verify(eventStorePort, never()).save(any());
        verify(queueAdapter, never()).publish(any(), any());
    }

    @Test
    void testPublishEventWhenDbSaveFailsDoesNotPublishToQueue() {
        PublishEventRequest request = new PublishEventRequest(
                "order.completed",
                "https://webhook.site/endpoint",
                Map.of("amount", 99.99),
                List.of("production"),
                Instant.now()
        );

        doThrow(new EventPersistenceException("Database connection error"))
                .when(eventStorePort).save(any(EventRecord.class));

        EventPersistenceException thrown = assertThrows(EventPersistenceException.class, () ->
                eventPublisherService.publishEvent(tenantId, request, "idempotency_key_fail")
        );

        assertNotNull(thrown);
        assertEquals("Database connection error", thrown.getMessage());
        // Verify queue publish was never called
        verify(queueAdapter, never()).publish(any(), any());
        verify(eventStorePort, never()).updateStatus(any(), any());
    }
}
