package com.hookflow.hookflow.publisher.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PublishEventRequest(
        String eventType,
        String destination,
        Map<String, Object> payload,
        List<String> tags,
        Instant occurredAt
) {
}
