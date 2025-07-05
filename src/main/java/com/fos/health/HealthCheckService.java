package com.fos.health;

import com.fos.dto.HealthStatus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fos.config.Constants.BEAN_NAME_ROOT_DIR;

/**
 * Service for performing various health checks.
 */
@Singleton
public class HealthCheckService {
    private final Path rootDirectory;
    private final List<HealthIndicator> healthIndicators;

    @Inject
    public HealthCheckService(@Named(BEAN_NAME_ROOT_DIR) Path rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.healthIndicators = new ArrayList<>();
        initializeHealthIndicators();
    }

    private void initializeHealthIndicators() {
        // Add various health indicators
        healthIndicators.add(new DiskSpaceHealthIndicator(rootDirectory));
        // Add more indicators as needed
    }

    public HealthStatus checkHealth(String requestId) {
        boolean isHealthy = true;
        Map<String, Object> details = new HashMap<>();

        for (HealthIndicator indicator : healthIndicators) {
            try {
                HealthIndicatorResult result = indicator.check();
                details.put(indicator.getName(), result.getDetails());
                isHealthy &= result.isHealthy();
            } catch (Exception e) {
                details.put(indicator.getName(), new HealthIndicatorResult(false, "Check failed: " + e.getMessage()));
                isHealthy = false;
            }
        }

        HealthStatus.Status status = isHealthy ? HealthStatus.Status.UP : HealthStatus.Status.DOWN;

        return new HealthStatus(status, details, requestId);
    }
}
