package com.hookflow.hookflow.publisher.persistence.model;

import com.hookflow.hookflow.common.model.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRecord {

    private Long id;
    private String requestId;
    private String eventType;
    private String destination;
    private Map<String, Object> payload;
    private List<String> tags;
    private EventStatus status;
    private Instant createdAt;
    private Instant occurredAt;
}
