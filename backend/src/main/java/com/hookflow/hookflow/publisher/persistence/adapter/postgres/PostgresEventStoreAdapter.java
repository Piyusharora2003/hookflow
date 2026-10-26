package com.hookflow.hookflow.publisher.persistence.adapter.postgres;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.publisher.persistence.exception.EventPersistenceException;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;
import com.hookflow.hookflow.publisher.persistence.port.EventStorePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * PostgreSQL adapter for {@link EventStorePort} using MyBatis.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "database.type", havingValue = "postgres")
public class PostgresEventStoreAdapter implements EventStorePort {

    private final EventRecordMapper eventRecordMapper;

    public PostgresEventStoreAdapter(EventRecordMapper eventRecordMapper) {
        this.eventRecordMapper = eventRecordMapper;
    }

    @Override
    public EventRecord save(EventRecord eventRecord) {
        if (eventRecord == null) {
            throw new IllegalArgumentException("EventRecord must not be null");
        }

        if (eventRecord.getCreatedAt() == null) {
            eventRecord.setCreatedAt(Instant.now());
        }

        try {
            log.debug("Persisting event record to PostgreSQL [requestId={}, eventType={}]",
                    eventRecord.getRequestId(), eventRecord.getEventType());
            eventRecordMapper.insert(eventRecord);
            log.debug("Successfully persisted event record [id={}, requestId={}]",
                    eventRecord.getId(), eventRecord.getRequestId());
            return eventRecord;
        } catch (Exception e) {
            log.error("Failed to persist event record [requestId={}, eventType={}]: {}",
                    eventRecord.getRequestId(), eventRecord.getEventType(), e.getMessage(), e);
            throw new EventPersistenceException("Failed to persist event record: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(Long id, EventStatus status) {
        if (id == null) {
            throw new IllegalArgumentException("Event id must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("EventStatus must not be null");
        }

        try {
            log.debug("Updating event record status in PostgreSQL [id={}, status={}]", id, status);
            int rows = eventRecordMapper.updateStatus(id, status);
            if (rows == 0) {
                log.warn("No event record found to update status [id={}, status={}]", id, status);
            } else {
                log.debug("Successfully updated event record status [id={}, status={}]", id, status);
            }
        } catch (Exception e) {
            log.error("Failed to update event record status [id={}, status={}]: {}", id, status, e.getMessage(), e);
            throw new EventPersistenceException("Failed to update event status: " + e.getMessage(), e);
        }
    }
}
