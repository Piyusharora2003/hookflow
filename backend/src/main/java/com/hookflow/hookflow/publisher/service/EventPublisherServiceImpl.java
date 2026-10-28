package com.hookflow.hookflow.publisher.service;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.common.queue.QueueAdapter;
import com.hookflow.hookflow.common.queue.QueueMessage;
import com.hookflow.hookflow.common.queue.QueuePublishException;
import com.hookflow.hookflow.filter.RequestIdFilter;
import com.hookflow.hookflow.publisher.dto.PublishEventRequest;
import com.hookflow.hookflow.publisher.dto.PublishEventResponse;
import com.hookflow.hookflow.publisher.exception.IdempotencyConflictException;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;
import com.hookflow.hookflow.publisher.persistence.port.EventStorePort;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class EventPublisherServiceImpl implements EventPublisherService {

    private final QueueAdapter queueAdapter;
    private final EventStorePort eventStorePort;
    private final String queueName;
    private final RetryTemplate retryTemplate;

    @Autowired
    public EventPublisherServiceImpl(
            QueueAdapter queueAdapter,
            EventStorePort eventStorePort,
            @Value("${hookflow.queue.name:hookflow-events}") String queueName,
            @Value("${hookflow.queue.retry.max-attempts:3}") int maxAttempts,
            @Value("${hookflow.queue.retry.initial-delay-ms:500}") long initialDelayMs,
            @Value("${hookflow.queue.retry.multiplier:2.0}") double multiplier,
            @Value("${hookflow.queue.retry.max-delay-ms:5000}") long maxDelayMs
    ) {
        this.queueAdapter = queueAdapter;
        this.eventStorePort = eventStorePort;
        this.queueName = queueName;
        this.retryTemplate = buildRetryTemplate(maxAttempts, initialDelayMs, multiplier, maxDelayMs);
    }

    public EventPublisherServiceImpl(
            QueueAdapter queueAdapter,
            EventStorePort eventStorePort,
            String queueName
    ) {
        this(queueAdapter, eventStorePort, queueName, 3, 500, 2.0, 5000);
    }

    private static RetryTemplate buildRetryTemplate(
            int maxAttempts,
            long initialDelayMs,
            double multiplier,
            long maxDelayMs
    ) {
        return RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .exponentialBackoff(initialDelayMs, multiplier, maxDelayMs)
                .retryOn(QueuePublishException.class)
                .build();
    }

    @Override
    public PublishEventResponse publishEvent(
            UUID tenantId,
            PublishEventRequest request,
            String idempotencyKey
    ) {
        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);

        if (idempotencyKey != null && tenantId != null) {
            if (eventStorePort.existsByTenantAndIdempotencyKey(tenantId, idempotencyKey)) {
                log.warn("Idempotency conflict for tenantId={} and key={}", tenantId, idempotencyKey);
                throw new IdempotencyConflictException(idempotencyKey);
            }
        }

        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        EventRecord eventRecord = EventRecord.builder()
                .id(eventId)
                .tenantId(tenantId)
                .idempotencyKey(idempotencyKey)
                .requestId(requestId)
                .eventType(request.eventType())
                .destination(request.destination())
                .payload(request.payload())
                .tags(request.tags())
                .status(EventStatus.PENDING_PUBLISH)
                .createdAt(now)
                .updatedAt(now)
                .occurredAt(request.occurredAt() != null ? request.occurredAt() : now)
                .build();

        log.debug("Persisting event before queue publish [id={}, requestId={}, eventType={}]",
                eventId, requestId, request.eventType());
        eventStorePort.save(eventRecord);

        QueueMessage<PublishEventRequest> message = QueueMessage.<PublishEventRequest>builder()
                .requestId(requestId)
                .messageId(eventId.toString())
                .eventType(request.eventType())
                .timestamp(now)
                .body(request)
                .build();

        log.debug("Publishing event to queue [id={}, requestId={}, queueName={}]", eventId, requestId, queueName);
        boolean publishSucceeded = false;
        try {
            retryTemplate.execute(context -> {
                queueAdapter.publish(message, queueName);
                return null;
            });
            publishSucceeded = true;
            eventStorePort.updateStatus(eventRecord.getId(), EventStatus.PUBLISHED_TO_QUEUE);
        } catch (Exception ex) {
            log.error("Queue publishing failed after retries for event [id={}, requestId={}]. Updating status to PUBLISH_FAILED",
                    eventRecord.getId(), requestId, ex);
            try {
                if (eventRecord.getId() != null) {
                    eventStorePort.updateStatus(eventRecord.getId(), EventStatus.PUBLISH_FAILED);
                }
            } catch (Exception updateEx) {
                log.error("Failed to update event status to PUBLISH_FAILED in database [id={}, requestId={}]: {}",
                        eventRecord.getId(), requestId, updateEx.getMessage(), updateEx);
            }
        }

        String finalStatus = publishSucceeded
                ? EventStatus.PUBLISHED_TO_QUEUE.name()
                : EventStatus.PUBLISH_FAILED.name();

        return new PublishEventResponse(
                eventRecord.getId().toString(),
                null,
                request.eventType(),
                request.destination(),
                finalStatus,
                now,
                eventRecord.getOccurredAt(),
                request.tags()
        );
    }
}
