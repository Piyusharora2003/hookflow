package com.hookflow.hookflow.publisher.controller;

import com.hookflow.hookflow.auth.filter.ApiKeyAuthenticationFilter;
import com.hookflow.hookflow.auth.model.ApiKeyInfo;
import com.hookflow.hookflow.publisher.constant.PublisherConstants;
import com.hookflow.hookflow.publisher.dto.PublishEventRequest;
import com.hookflow.hookflow.publisher.dto.PublishEventResponse;
import com.hookflow.hookflow.publisher.service.EventPublisherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/events")
public class EventPublisherController {

    private final EventPublisherService eventPublisherService;

    public EventPublisherController(EventPublisherService eventPublisherService) {
        this.eventPublisherService = eventPublisherService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PublishEventResponse> publishEvent(
            @RequestBody PublishEventRequest request,
            @RequestHeader(value = PublisherConstants.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestAttribute(value = ApiKeyAuthenticationFilter.AUTH_API_KEY_INFO_ATTR, required = false) ApiKeyInfo apiKeyInfo
    ) {
        UUID tenantId = (apiKeyInfo != null) ? apiKeyInfo.applicationId() : null;

        log.info("Received event publish request [tenantId={}, eventType={}, destination={}, idempotencyKey={}]",
                tenantId, request.eventType(), request.destination(), idempotencyKey);

        PublishEventResponse response = eventPublisherService.publishEvent(tenantId, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
