package com.fileservice.health;

import lombok.Getter;

/**
 * Result class for health indicators.
 */
@Getter
class HealthIndicatorResult {
    private final boolean healthy;
    private final Object details;

    public HealthIndicatorResult(boolean healthy, Object details) {
        this.healthy = healthy;
        this.details = details;
    }
}
