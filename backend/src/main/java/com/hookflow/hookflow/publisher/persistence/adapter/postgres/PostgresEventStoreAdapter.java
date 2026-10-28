package com.hookflow.hookflow.publisher.persistence.adapter.postgres;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.publisher.exception.IdempotencyConflictException;
import com.hookflow.hookflow.publisher.persistence.exception.EventPersistenceException;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;
import com.hookflow.hookflow.publisher.persistence.port.EventStorePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        if (eventRecord.getId() == null) {
            eventRecord.setId(UUID.randomUUID());
        }

        Instant now = Instant.now();
        if (eventRecord.getCreatedAt() == null) {
            eventRecord.setCreatedAt(now);
        }
        if (eventRecord.getUpdatedAt() == null) {
            eventRecord.setUpdatedAt(now);
        }

        try {
            log.debug("Persisting event record to PostgreSQL [id={}, requestId={}, eventType={}]",
                    eventRecord.getId(), eventRecord.getRequestId(), eventRecord.getEventType());
            eventRecordMapper.insert(eventRecord);
            log.debug("Successfully persisted event record [id={}, requestId={}]",
                    eventRecord.getId(), eventRecord.getRequestId());
            return eventRecord;
        } catch (DuplicateKeyException e) {
            log.warn("Idempotency conflict detected for tenantId={} and idempotencyKey={}",
                    eventRecord.getTenantId(), eventRecord.getIdempotencyKey());
            throw new IdempotencyConflictException(eventRecord.getIdempotencyKey());
        } catch (Exception e) {
            log.error("Failed to persist event record [id={}, requestId={}, eventType={}]: {}",
                    eventRecord.getId(), eventRecord.getRequestId(), eventRecord.getEventType(), e.getMessage(), e);
            throw new EventPersistenceException("Failed to persist event record: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(UUID id, EventStatus status) {
        if (id == null) {
            throw new IllegalArgumentException("Event id must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("EventStatus must not be null");
        }

        try {
            Instant updatedAt = Instant.now();
            log.debug("Updating event record status in PostgreSQL [id={}, status={}]", id, status);
            int rows = eventRecordMapper.updateStatus(id, status, updatedAt);
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

    @Override
    public boolean existsByTenantAndIdempotencyKey(UUID tenantId, String idempotencyKey) {
        if (tenantId == null || idempotencyKey == null) {
            return false;
        }
        try {
            return eventRecordMapper.existsByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to check idempotency key existence [tenantId={}, idempotencyKey={}]: {}",
                    tenantId, idempotencyKey, e.getMessage(), e);
            throw new EventPersistenceException("Failed to check idempotency key existence: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<EventRecord> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(eventRecordMapper.findById(id));
        } catch (Exception e) {
            log.error("Failed to find event by id [id={}]: {}", id, e.getMessage(), e);
            throw new EventPersistenceException("Failed to find event by id: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EventRecord> findPublishFailed(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        try {
            return eventRecordMapper.findPublishFailedBatch(limit);
        } catch (Exception e) {
            log.error("Failed to fetch PUBLISH_FAILED events: {}", e.getMessage(), e);
            throw new EventPersistenceException("Failed to fetch PUBLISH_FAILED events: " + e.getMessage(), e);
        }
    }
}
