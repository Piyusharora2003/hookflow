package com.hookflow.hookflow.common.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueMessage<T> {
    private String requestId;
    private String messageId;
    private String eventType;
    private Instant timestamp;
    private T body;
    private String receiptHandle;
}
