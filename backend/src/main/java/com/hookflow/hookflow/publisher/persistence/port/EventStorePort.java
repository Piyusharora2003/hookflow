package com.hookflow.hookflow.publisher.persistence.port;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventStorePort {

    EventRecord save(EventRecord eventRecord);

    void updateStatus(UUID id, EventStatus status);

    boolean existsByTenantAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    Optional<EventRecord> findById(UUID id);

    List<EventRecord> findPublishFailed(int limit);
}
