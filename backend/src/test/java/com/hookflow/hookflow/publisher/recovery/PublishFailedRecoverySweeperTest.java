package com.hookflow.hookflow.publisher.recovery;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.common.queue.QueueAdapter;
import com.hookflow.hookflow.common.queue.QueuePublishException;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;
import com.hookflow.hookflow.publisher.persistence.port.EventStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishFailedRecoverySweeperTest {

    @Mock
    private EventStorePort eventStorePort;

    @Mock
    private QueueAdapter queueAdapter;

    private PublishFailedRecoverySweeper sweeper;
    private final String queueName = "test-queue";

    @BeforeEach
    void setUp() {
        sweeper = new PublishFailedRecoverySweeper(eventStorePort, queueAdapter, queueName, 50);
    }

    @Test
    void testRecoverPublishFailedEventsSuccess() {
        UUID eventId = UUID.randomUUID();
        EventRecord event = EventRecord.builder()
                .id(eventId)
                .requestId("req_rec_1")
                .eventType("payment.succeeded")
                .destination("https://example.com/callback")
                .payload(Map.of("amount", 50))
                .tags(List.of("test"))
                .status(EventStatus.PUBLISH_FAILED)
                .occurredAt(Instant.now())
                .build();

        when(eventStorePort.findPublishFailed(50)).thenReturn(List.of(event));

        sweeper.recoverPublishFailedEvents();

        verify(queueAdapter).publish(any(), eq(queueName));
        verify(eventStorePort).updateStatus(eventId, EventStatus.PUBLISHED_TO_QUEUE);
    }

    @Test
    void testRecoverPublishFailedEventsWhenQueueFailsDoesNotUpdateStatus() {
        UUID eventId = UUID.randomUUID();
        EventRecord event = EventRecord.builder()
                .id(eventId)
                .requestId("req_rec_2")
                .eventType("payment.succeeded")
                .destination("https://example.com/callback")
                .payload(Map.of("amount", 50))
                .status(EventStatus.PUBLISH_FAILED)
                .occurredAt(Instant.now())
                .build();

        when(eventStorePort.findPublishFailed(50)).thenReturn(List.of(event));
        doThrow(new QueuePublishException("Queue still down")).when(queueAdapter).publish(any(), eq(queueName));

        sweeper.recoverPublishFailedEvents();

        verify(queueAdapter).publish(any(), eq(queueName));
        verify(eventStorePort, never()).updateStatus(eventId, EventStatus.PUBLISHED_TO_QUEUE);
    }

    @Test
    void testRecoverPublishFailedEventsEmptyList() {
        when(eventStorePort.findPublishFailed(50)).thenReturn(Collections.emptyList());

        sweeper.recoverPublishFailedEvents();

        verify(queueAdapter, never()).publish(any(), any());
    }
}

