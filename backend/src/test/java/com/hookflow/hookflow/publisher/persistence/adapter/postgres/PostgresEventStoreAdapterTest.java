package com.hookflow.hookflow.publisher.persistence.adapter.postgres;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.publisher.persistence.exception.EventPersistenceException;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresEventStoreAdapterTest {

    @Mock
    private EventRecordMapper eventRecordMapper;

    private PostgresEventStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostgresEventStoreAdapter(eventRecordMapper);
    }

    @Test
    void testSaveSuccess() {
        EventRecord record = EventRecord.builder()
                .requestId("req_123")
                .eventType("order.created")
                .destination("https://example.com/webhook")
                .payload(Map.of("key", "val"))
                .tags(List.of("tag1"))
                .status(EventStatus.PUBLISHED)
                .occurredAt(Instant.now())
                .build();

        doAnswer(invocation -> {
            EventRecord arg = invocation.getArgument(0);
            arg.setId(1L);
            return 1;
        }).when(eventRecordMapper).insert(any(EventRecord.class));

        EventRecord result = adapter.save(record);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertNotNull(result.getCreatedAt());
        verify(eventRecordMapper).insert(record);
    }

    @Test
    void testSaveNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> adapter.save(null));
    }

    @Test
    void testSaveMapperErrorThrowsEventPersistenceException() {
        EventRecord record = EventRecord.builder()
                .requestId("req_123")
                .eventType("order.created")
                .build();

        doThrow(new RuntimeException("DB down")).when(eventRecordMapper).insert(any());

        assertThrows(EventPersistenceException.class, () -> adapter.save(record));
    }

    @Test
    void testUpdateStatusSuccess() {
        when(eventRecordMapper.updateStatus(1L, EventStatus.PUBLISH_FAILED)).thenReturn(1);

        adapter.updateStatus(1L, EventStatus.PUBLISH_FAILED);

        verify(eventRecordMapper).updateStatus(1L, EventStatus.PUBLISH_FAILED);
    }

    @Test
    void testUpdateStatusNullParamsThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> adapter.updateStatus(null, EventStatus.PUBLISH_FAILED));
        assertThrows(IllegalArgumentException.class, () -> adapter.updateStatus(1L, null));
    }

    @Test
    void testUpdateStatusMapperErrorThrowsEventPersistenceException() {
        doThrow(new RuntimeException("DB down")).when(eventRecordMapper).updateStatus(1L, EventStatus.PUBLISH_FAILED);

        assertThrows(EventPersistenceException.class, () -> adapter.updateStatus(1L, EventStatus.PUBLISH_FAILED));
    }
}
