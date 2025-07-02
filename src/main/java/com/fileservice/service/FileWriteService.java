package com.fileservice.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.fileservice.config.Constants.BEAN_NAME_ROOT_DIRECTORY;

/**
 * Service for concurrent file writing operations with memory leak prevention.
 * Uses file system locks for synchronization and includes automatic cleanup.
 */
@Singleton
public class FileWriteService  extends BaseFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileWriteService.class);
    private static final long LOCK_CLEANUP_INTERVAL = 5; // minutes
    private static final long LOCK_EXPIRATION = 1; // minutes

    private final ConcurrentHashMap<String, LockInfo> locks;
    private final ScheduledExecutorService cleanupExecutor;

    private static class LockInfo {
        final Object lock;
        volatile long lastAccessed;

        LockInfo() {
            this.lock = new Object();
            this.lastAccessed = System.currentTimeMillis();
        }

        void touch() {
            this.lastAccessed = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lastAccessed > TimeUnit.MINUTES.toMillis(LOCK_EXPIRATION);
        }
    }

    @Inject
    public FileWriteService(@Named(BEAN_NAME_ROOT_DIRECTORY) Path rootDirectory) {
        super(rootDirectory);
        this.locks = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LockCleanupThread");
            t.setDaemon(true);
            return t;
        });

        startCleanupTask();
    }

    /**
     * Appends data to a file with concurrent access protection.
     *
     * @param path the path to the file
     * @param data the data to append
     * @return true if the operation was successful
     * @throws IllegalArgumentException if path is null/empty or data is null
     * @throws SecurityException if the path is outside the root directory
     * @throws IOException if an I/O error occurs
     */
    public boolean append(String path, String data) throws IOException {
        validateParameters(path, data);
        Path normalizedFullPath = validateAndNormalizeFullPath(path);

        LockInfo lockInfo = locks.compute(normalizedFullPath.toString(), (k, v) ->
                v == null ? new LockInfo() : v
        );

        synchronized (lockInfo.lock) {
            try {
                lockInfo.touch();
                return appendWithLock(normalizedFullPath, data);
            } finally {
                // Don't remove the lock here - let the cleanup task handle it
                lockInfo.touch();
            }
        }
    }

    private void validateParameters(String path, String data) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
    }


    private boolean appendWithLock(Path path, String data) throws IOException {
        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
                StandardOpenOption.CREATE)) {

            // Use FileLock for system-level synchronization
            try (FileLock ignored = channel.lock()) {
                byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
                channel.write(java.nio.ByteBuffer.wrap(bytes));
                channel.force(true);
                return true;
            }
        } catch (IOException e) {
            LOGGER.error("Error appending to file: {}", path, e);
            throw new IOException("Failed to append to file: " + e.getMessage(), e);
        }
    }

    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredLocks,
                LOCK_CLEANUP_INTERVAL,
                LOCK_CLEANUP_INTERVAL,
                TimeUnit.MINUTES
        );
    }

    private void cleanupExpiredLocks() {
        locks.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired();
            if (expired) {
                LOGGER.trace("Removing expired lock for: {}", entry.getKey());
            }
            return expired;
        });
    }

    /**
     * Properly shutdown the service and cleanup resources.
     */
    public void shutdown() {
        // TODO add to App closing
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}