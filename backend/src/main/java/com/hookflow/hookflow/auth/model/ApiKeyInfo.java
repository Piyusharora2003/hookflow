package com.hookflow.hookflow.auth.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Representation of an authenticated API key scoped to an application.
 */
public record ApiKeyInfo(
        UUID id,
        UUID applicationId,
        String status,
        Instant expiresAt,
        Set<String> allowedDestinations
) {
    public static final ApiKeyInfo NOT_FOUND = new ApiKeyInfo(
            null,
            null,
            "NOT_FOUND",
            null,
            Collections.emptySet()
    );

    public boolean isNotFound() {
        return "NOT_FOUND".equalsIgnoreCase(status);
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status)
                && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    public boolean isDestinationAllowed(String destinationAlias) {
        if (allowedDestinations == null || allowedDestinations.isEmpty()) {
            return true;
        }
        return allowedDestinations.contains(destinationAlias.toLowerCase());
    }
}
