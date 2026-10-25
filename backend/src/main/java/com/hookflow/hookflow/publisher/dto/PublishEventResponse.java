package com.hookflow.hookflow.publisher.dto;

import java.time.Instant;
import java.util.List;

public record PublishEventResponse(
        String eventId,
        String deliveryId,
        String eventType,
        String destination,
        String status,
        Instant receivedAt,
        Instant occurredAt,
        List<String> tags
) {
}
