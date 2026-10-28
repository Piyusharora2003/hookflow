package com.hookflow.hookflow.publisher.persistence.adapter.postgres;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
public interface EventRecordMapper {

    int insert(EventRecord eventRecord);

    int updateStatus(
            @Param("id") UUID id,
            @Param("status") EventStatus status,
            @Param("updatedAt") Instant updatedAt
    );

    EventRecord findById(@Param("id") UUID id);

    boolean existsByTenantIdAndIdempotencyKey(
            @Param("tenantId") UUID tenantId,
            @Param("idempotencyKey") String idempotencyKey
    );

    List<EventRecord> findPublishFailedBatch(@Param("limit") int limit);
}
