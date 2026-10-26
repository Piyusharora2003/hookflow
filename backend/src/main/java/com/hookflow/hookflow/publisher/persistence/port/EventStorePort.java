package com.hookflow.hookflow.publisher.persistence.port;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;

public interface EventStorePort {
    EventRecord save(EventRecord eventRecord);
    void updateStatus(Long id, EventStatus status);
}
