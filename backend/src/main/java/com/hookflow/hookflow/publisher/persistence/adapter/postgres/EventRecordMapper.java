package com.hookflow.hookflow.publisher.persistence.adapter.postgres;

import com.hookflow.hookflow.common.model.EventStatus;
import com.hookflow.hookflow.publisher.persistence.model.EventRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EventRecordMapper {

    int insert(EventRecord eventRecord);

    int updateStatus(@Param("id") Long id, @Param("status") EventStatus status);

    EventRecord findById(@Param("id") Long id);
}
