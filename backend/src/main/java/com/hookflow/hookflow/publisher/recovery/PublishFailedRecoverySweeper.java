package com.hookflow.hookflow.publisher.recovery;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.common.queue.QueueAdapter;
import com.hookflow.hookflow.common.queue.QueueMessage;
import com.hookflow.hookflow.publisher.dto.PublishEventRequest;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;
import com.hookflow.hookflow.publisher.persistence.port.EventStorePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "hookflow.publisher.recovery.enabled", havingValue = "true", matchIfMissing = true)
public class PublishFailedRecoverySweeper {

    private final EventStorePort eventStorePort;
    private final QueueAdapter queueAdapter;
    private final String queueName;
    private final int batchSize;

    public PublishFailedRecoverySweeper(
            EventStorePort eventStorePort,
            QueueAdapter queueAdapter,
            @Value("${hookflow.queue.name:hookflow-events}") String queueName,
            @Value("${hookflow.publisher.recovery.batch-size:50}") int batchSize
    ) {
        this.eventStorePort = eventStorePort;
        this.queueAdapter = queueAdapter;
        this.queueName = queueName;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${hookflow.publisher.recovery.interval-ms:30000}")
    public void recoverPublishFailedEvents() {
        log.debug("Scanning for PUBLISH_FAILED events to recover...");
        List<EventRecord> failedEvents = eventStorePort.findPublishFailed(batchSize);
        if (failedEvents.isEmpty()) {
            return;
        }

        log.info("Found {} PUBLISH_FAILED events for recovery", failedEvents.size());
        for (EventRecord event : failedEvents) {
            try {
                PublishEventRequest request = new PublishEventRequest(
                        event.getEventType(),
                        event.getDestination(),
                        event.getPayload(),
                        event.getTags(),
                        event.getOccurredAt()
                );

                QueueMessage<PublishEventRequest> message = QueueMessage.<PublishEventRequest>builder()
                        .requestId(event.getRequestId())
                        .messageId(event.getId().toString())
                        .eventType(event.getEventType())
                        .timestamp(Instant.now())
                        .body(request)
                        .build();

                queueAdapter.publish(message, queueName);
                eventStorePort.updateStatus(event.getId(), EventStatus.PUBLISHED_TO_QUEUE);
                log.info("Successfully recovered and published event [id={}, requestId={}]",
                        event.getId(), event.getRequestId());
            } catch (Exception e) {
                log.warn("Failed to republish event during recovery [id={}, requestId={}]: {}",
                        event.getId(), event.getRequestId(), e.getMessage());
            }
        }
    }
}

