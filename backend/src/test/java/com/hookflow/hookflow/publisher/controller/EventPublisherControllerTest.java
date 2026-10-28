package com.hookflow.hookflow.publisher.controller;

import com.hookflow.hookflow.auth.filter.ApiKeyAuthenticationFilter;
import com.hookflow.hookflow.auth.model.ApiKeyInfo;
import com.hookflow.hookflow.common.exception.GlobalExceptionHandler;
import com.hookflow.hookflow.publisher.constant.PublisherConstants;
import com.hookflow.hookflow.publisher.dto.PublishEventRequest;
import com.hookflow.hookflow.publisher.dto.PublishEventResponse;
import com.hookflow.hookflow.publisher.exception.IdempotencyConflictException;
import com.hookflow.hookflow.publisher.service.EventPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventPublisherControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventPublisherService eventPublisherService;

    @InjectMocks
    private EventPublisherController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testPublishEventSuccess() throws Exception {
        PublishEventRequest request = new PublishEventRequest(
                "order.created",
                "https://webhook.site/endpoint",
                Map.of("key", "value"),
                List.of("tag1"),
                Instant.now()
        );

        ApiKeyInfo apiKeyInfo = new ApiKeyInfo(
                UUID.randomUUID(),
                tenantId,
                "ACTIVE",
                null,
                Collections.emptySet()
        );

        UUID eventId = UUID.randomUUID();
        PublishEventResponse response = new PublishEventResponse(
                eventId.toString(),
                null,
                "order.created",
                "https://webhook.site/endpoint",
                "PUBLISHED_TO_QUEUE",
                Instant.now(),
                request.occurredAt(),
                request.tags()
        );

        when(eventPublisherService.publishEvent(eq(tenantId), any(PublishEventRequest.class), eq("key_123")))
                .thenReturn(response);

        mockMvc.perform(post("/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header(PublisherConstants.IDEMPOTENCY_KEY_HEADER, "key_123")
                        .requestAttr(ApiKeyAuthenticationFilter.AUTH_API_KEY_INFO_ATTR, apiKeyInfo))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED_TO_QUEUE"))
                .andExpect(jsonPath("$.eventType").value("order.created"));

        verify(eventPublisherService).publishEvent(eq(tenantId), any(PublishEventRequest.class), eq("key_123"));
    }

    @Test
    void testPublishEventDuplicateKeyReturns409Conflict() throws Exception {
        PublishEventRequest request = new PublishEventRequest(
                "order.created",
                "https://webhook.site/endpoint",
                Map.of("key", "value"),
                List.of("tag1"),
                Instant.now()
        );

        ApiKeyInfo apiKeyInfo = new ApiKeyInfo(
                UUID.randomUUID(),
                tenantId,
                "ACTIVE",
                null,
                Collections.emptySet()
        );

        when(eventPublisherService.publishEvent(eq(tenantId), any(PublishEventRequest.class), eq("dup_key")))
                .thenThrow(new IdempotencyConflictException("dup_key"));

        mockMvc.perform(post("/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header(PublisherConstants.IDEMPOTENCY_KEY_HEADER, "dup_key")
                        .requestAttr(ApiKeyAuthenticationFilter.AUTH_API_KEY_INFO_ATTR, apiKeyInfo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_CONFLICT"));
    }
}

