package com.hookflow.hookflow.publisher.persistence.adapter.postgres;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.publisher.exception.IdempotencyConflictException;
import com.hookflow.hookflow.publisher.persistence.exception.EventPersistenceException;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresEventStoreAdapterTest {

    @Mock
    private EventRecordMapper eventRecordMapper;

    private PostgresEventStoreAdapter adapter;
    private final UUID eventId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new PostgresEventStoreAdapter(eventRecordMapper);
    }

    @Test
    void testSaveSuccess() {
        EventRecord record = EventRecord.builder()
                .id(eventId)
                .tenantId(tenantId)
                .idempotencyKey("idem_123")
                .requestId("req_123")
                .eventType("order.created")
                .destination("https://example.com/webhook")
                .payload(Map.of("key", "val"))
                .tags(List.of("tag1"))
                .status(EventStatus.PENDING_PUBLISH)
                .occurredAt(Instant.now())
                .build();

        when(eventRecordMapper.insert(any(EventRecord.class))).thenReturn(1);

        EventRecord result = adapter.save(record);

        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(eventRecordMapper).insert(record);
    }

    @Test
    void testSaveNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> adapter.save(null));
    }

    @Test
    void testSaveDuplicateKeyThrowsIdempotencyConflictException() {
        EventRecord record = EventRecord.builder()
                .id(eventId)
                .tenantId(tenantId)
                .idempotencyKey("duplicate_key")
                .requestId("req_123")
                .eventType("order.created")
                .build();

        when(eventRecordMapper.insert(any())).thenThrow(new DuplicateKeyException("Duplicate key violation"));

        IdempotencyConflictException ex = assertThrows(IdempotencyConflictException.class, () -> adapter.save(record));
        assertEquals("duplicate_key", ex.getIdempotencyKey());
    }

    @Test
    void testSaveMapperErrorThrowsEventPersistenceException() {
        EventRecord record = EventRecord.builder()
                .id(eventId)
                .requestId("req_123")
                .eventType("order.created")
                .build();

        when(eventRecordMapper.insert(any())).thenThrow(new RuntimeException("DB down"));

        assertThrows(EventPersistenceException.class, () -> adapter.save(record));
    }

    @Test
    void testUpdateStatusSuccess() {
        when(eventRecordMapper.updateStatus(eq(eventId), eq(EventStatus.PUBLISHED_TO_QUEUE), any(Instant.class))).thenReturn(1);

        adapter.updateStatus(eventId, EventStatus.PUBLISHED_TO_QUEUE);

        verify(eventRecordMapper).updateStatus(eq(eventId), eq(EventStatus.PUBLISHED_TO_QUEUE), any(Instant.class));
    }

    @Test
    void testUpdateStatusNullParamsThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> adapter.updateStatus(null, EventStatus.PUBLISH_FAILED));
        assertThrows(IllegalArgumentException.class, () -> adapter.updateStatus(eventId, null));
    }

    @Test
    void testExistsByTenantAndIdempotencyKey() {
        when(eventRecordMapper.existsByTenantIdAndIdempotencyKey(tenantId, "test_key")).thenReturn(true);

        boolean exists = adapter.existsByTenantAndIdempotencyKey(tenantId, "test_key");
        assertTrue(exists);
        verify(eventRecordMapper).existsByTenantIdAndIdempotencyKey(tenantId, "test_key");
    }

    @Test
    void testFindPublishFailed() {
        EventRecord record = EventRecord.builder().id(eventId).status(EventStatus.PUBLISH_FAILED).build();
        when(eventRecordMapper.findPublishFailedBatch(10)).thenReturn(List.of(record));

        List<EventRecord> results = adapter.findPublishFailed(10);
        assertEquals(1, results.size());
        assertEquals(eventId, results.get(0).getId());
    }
}
