package com.hookflow.hookflow.publisher.service;

import com.hookflow.hookflow.common.queue.QueueAdapter;
import com.hookflow.hookflow.common.queue.QueueMessage;
import com.hookflow.hookflow.filter.RequestIdFilter;
import com.hookflow.hookflow.publisher.dto.PublishEventRequest;
import com.hookflow.hookflow.publisher.dto.PublishEventResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceImplTest {

    @Mock
    private QueueAdapter queueAdapter;

    private EventPublisherServiceImpl eventPublisherService;
    private final String queueName = "test-events-queue";

    @BeforeEach
    void setUp() {
        eventPublisherService = new EventPublisherServiceImpl(queueAdapter, queueName);
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

        PublishEventResponse response = eventPublisherService.publishEvent(request, "idempotency_key_1");

        assertNotNull(response);
        assertEquals("req_abc123", response.eventId());
        assertEquals("ACCEPTED", response.status());
        assertEquals("order.completed", response.eventType());
        assertEquals("https://webhook.site/endpoint", response.destination());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueueMessage<PublishEventRequest>> captor = ArgumentCaptor.forClass(QueueMessage.class);
        verify(queueAdapter).publish(captor.capture(), eq(queueName));

        QueueMessage<PublishEventRequest> capturedMessage = captor.getValue();
        assertEquals("req_abc123", capturedMessage.getRequestId());
        assertEquals("order.completed", capturedMessage.getEventType());
        assertEquals(request, capturedMessage.getBody());
    }
}
