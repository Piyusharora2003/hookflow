package com.hookflow.hookflow.common.model;

public enum EventStatus {
    PENDING_PUBLISH,
    PUBLISHED_TO_QUEUE,
    PUBLISH_FAILED,
    IN_PROGRESS,
    ACKNOWLEDGED,
    FAILED
}