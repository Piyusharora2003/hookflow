package com.hookflow.hookflow.publisher.service;

import com.hookflow.hookflow.publisher.dto.PublishEventRequest;
import com.hookflow.hookflow.publisher.dto.PublishEventResponse;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherServiceImpl implements EventPublisherService {

    @Override
    public PublishEventResponse publishEvent(
            PublishEventRequest request,
            String idempotencyKey
    ) {
        // Structure only - logic to be implemented (requestId accessible via MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY) if needed)
        return null;
    }
}
