package com.fileservice.service.lock;

import com.fileservice.service.DirectoryListService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

/**
 * Service for managing distributed locks using Redis.
 * Provides thread-safe and distributed locking mechanisms with automatic lock extension
 * and proper cleanup.
 */
@Singleton
public class DistributedLockService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryListService.class);

    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final JedisPool jedisPool;

    @Inject
    public DistributedLockService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * Represents a distributed lock that can be used in a try-with-resources block.
     */
    public class DistributedLock implements AutoCloseable {
        private final String resourceId;

        private DistributedLock(String resourceId) {
            this.resourceId = resourceId;
        }

        @Override
        public void close() {
            releaseLock();
        }

        private void releaseLock() {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(resourceId);
                LOGGER.trace("Released lock: {}", resourceId);
            } catch (Exception e) {
                LOGGER.warn("Failed to release lock: {}",  resourceId, e);
            }
        }
    }

    /**
     * Acquires a distributed lock with the default timeout.
     *
     * @param resourceId the resource identifier to lock
     * @return a DistributedLock instance that must be closed after use
     * @throws LockAcquisitionException if the lock cannot be acquired
     */
    public DistributedLock acquireLock(String resourceId) throws LockAcquisitionException {
        return acquireLock(resourceId, DEFAULT_LOCK_TIMEOUT);
    }

    /**
     * Acquires a distributed lock with a specified timeout.
     *
     * @param resourceId the resource identifier to lock
     * @param timeout the lock timeout duration
     * @return a DistributedLock instance that must be closed after use
     * @throws LockAcquisitionException if the lock cannot be acquired
     */
    private DistributedLock acquireLock(String resourceId, Duration timeout)
            throws LockAcquisitionException {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                if (tryAcquireLock(resourceId, timeout)) {
                    LOGGER.trace("Acquired lock: {}", resourceId);
                    return new DistributedLock(resourceId);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to acquire lock: {}, attempt {}", resourceId, attempt, e);
            }

            if (attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LockAcquisitionException("Interrupted while waiting to retry", e);
                }
            }
        }

        throw new LockAcquisitionException(
                "Failed to acquire lock after " + MAX_RETRY_ATTEMPTS + " attempts");
    }

    private boolean tryAcquireLock(String resourceId, Duration timeout) {
        try (Jedis jedis = jedisPool.getResource()) {
            SetParams params = new SetParams()
                    .nx()
                    .ex(timeout.getSeconds());

            String result = jedis.set(resourceId, resourceId, params);
            return "OK".equals(result);
        }
    }

    private String generateLockValue() {
        try {
            return String.format("%s:%s:%d",
                    InetAddress.getLocalHost().getHostName(),
                    Thread.currentThread().getName(),
                    System.currentTimeMillis());
        } catch (UnknownHostException e) {
            return String.format("unknown:%s:%d",
                    Thread.currentThread().getName(),
                    System.currentTimeMillis());
        }
    }

    /**
     * Custom exception for lock acquisition failures.
     */
    public static class LockAcquisitionException extends Exception {
        public LockAcquisitionException(String message) {
            super(message);
        }

        public LockAcquisitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}