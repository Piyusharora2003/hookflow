package com.hookflow.hookflow.publisher.service;

import com.hookflow.hookflow.common.queue.QueueAdapter;
import com.hookflow.hookflow.common.queue.QueueMessage;
import com.hookflow.hookflow.common.queue.QueuePublishException;
import com.hookflow.hookflow.filter.RequestIdFilter;
import com.hookflow.hookflow.publisher.dto.PublishEventRequest;
import com.hookflow.hookflow.publisher.dto.PublishEventResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class EventPublisherServiceImpl implements EventPublisherService {

    private final QueueAdapter queueAdapter;
    private final String queueName;

    public EventPublisherServiceImpl(
            QueueAdapter queueAdapter,
            @Value("${hookflow.queue.name:hookflow-events}") String queueName
    ) {
        this.queueAdapter = queueAdapter;
        this.queueName = queueName;
    }

    @Override
    @Retryable(
            retryFor = QueuePublishException.class,
            maxAttemptsExpression = "${hookflow.queue.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${hookflow.queue.retry.initial-delay-ms:500}",
                    multiplierExpression = "${hookflow.queue.retry.multiplier:2.0}",
                    maxDelayExpression = "${hookflow.queue.retry.max-delay-ms:5000}",
                    random = true
            )
    )
    public PublishEventResponse publishEvent(
            PublishEventRequest request,
            String idempotencyKey
    ) {
        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);

        QueueMessage<PublishEventRequest> message = QueueMessage.<PublishEventRequest>builder()
                .requestId(requestId)
                .messageId(UUID.randomUUID().toString())
                .eventType(request.eventType())
                .timestamp(Instant.now())
                .body(request)
                .build();

        log.debug("Publishing event to queue [requestId={}, queueName={}]", requestId, queueName);
        queueAdapter.publish(message, queueName);

        return new PublishEventResponse(
                requestId,
                null,
                request.eventType(),
                request.destination(),
                "ACCEPTED",
                Instant.now(),
                request.occurredAt(),
                request.tags()
        );
    }
}
