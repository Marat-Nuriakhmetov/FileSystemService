package com.fileservice.health;

/**
 * Interface for health indicators.
 */
interface HealthIndicator {
    String getName();
    HealthIndicatorResult check();
}