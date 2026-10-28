package com.hookflow.hookflow.publisher.service;

import com.hookflow.hookflow.publisher.dto.PublishEventRequest;
import com.hookflow.hookflow.publisher.dto.PublishEventResponse;

import java.util.UUID;

public interface EventPublisherService {

    PublishEventResponse publishEvent(
            UUID tenantId,
            PublishEventRequest request,
            String idempotencyKey
    );

    default PublishEventResponse publishEvent(
            PublishEventRequest request,
            String idempotencyKey
    ) {
        return publishEvent(null, request, idempotencyKey);
    }
}
