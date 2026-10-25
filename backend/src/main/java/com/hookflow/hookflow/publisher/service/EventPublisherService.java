package com.hookflow.hookflow.publisher.service;

import com.hookflow.hookflow.publisher.dto.PublishEventRequest;
import com.hookflow.hookflow.publisher.dto.PublishEventResponse;

public interface EventPublisherService {

    PublishEventResponse publishEvent(
            PublishEventRequest request,
            String idempotencyKey
    );
}
