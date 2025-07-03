package com.fileservice.health;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Health indicator for disk space.
 */
class DiskSpaceHealthIndicator implements HealthIndicator {
    private final Path path;
    private static final long MIN_DISK_SPACE = 10 * 1024 * 1024; // 10MB

    public DiskSpaceHealthIndicator(Path path) {
        this.path = path;
    }

    @Override
    public String getName() {
        return "diskSpace";
    }

    @Override
    public HealthIndicatorResult check() {
        try {
            FileStore store = Files.getFileStore(path);
            long freeSpace = store.getUsableSpace();
            boolean healthy = freeSpace > MIN_DISK_SPACE;

            return new HealthIndicatorResult(healthy,
                    Map.of(
                            "free", freeSpace,
                            "threshold", MIN_DISK_SPACE
                    ));
        } catch (IOException e) {
            return new HealthIndicatorResult(false, "Unable to check disk space");
        }
    }
}