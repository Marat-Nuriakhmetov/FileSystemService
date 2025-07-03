package com.fileservice.service.lock;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing distributed locks using Redis.
 * Provides thread-safe and distributed locking mechanisms with automatic lock extension
 * and proper cleanup.
 */
@Singleton
public class DistributedLockService {
    private static final Logger LOGGER = Logger.getLogger(DistributedLockService.class.getName());

    private static final String LOCK_PREFIX = "lock:";
    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration LOCK_EXTENSION_INTERVAL = Duration.ofSeconds(10);
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
        private final String lockKey;
        private final String lockValue;
        private final Thread extensionThread;
        private volatile boolean valid = true;

        private DistributedLock(String lockKey, String lockValue) {
            this.lockKey = lockKey;
            this.lockValue = lockValue;
            this.extensionThread = createExtensionThread();
            this.extensionThread.start();
        }

        private Thread createExtensionThread() {
            Thread thread = new Thread(() -> {
                while (valid) {
                    try {
                        Thread.sleep(LOCK_EXTENSION_INTERVAL.toMillis());
                        if (valid) {
                            extendLock();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            thread.setDaemon(true);
            thread.setName("Lock-Extension-" + lockKey);
            return thread;
        }

        private void extendLock() {
            try (Jedis jedis = jedisPool.getResource()) {
                String currentValue = jedis.get(lockKey);
                if (lockValue.equals(currentValue)) {
                    jedis.expire(lockKey, DEFAULT_LOCK_TIMEOUT.getSeconds());
                    LOGGER.fine("Extended lock: " + lockKey);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to extend lock: " + lockKey, e);
            }
        }

        @Override
        public void close() {
            valid = false;
            extensionThread.interrupt();
            releaseLock();
        }

        private void releaseLock() {
            try (Jedis jedis = jedisPool.getResource()) {
                String currentValue = jedis.get(lockKey);
                if (lockValue.equals(currentValue)) {
                    jedis.del(lockKey);
                    LOGGER.fine("Released lock: " + lockKey);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to release lock: " + lockKey, e);
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
    public DistributedLock acquireLock(String resourceId, Duration timeout)
            throws LockAcquisitionException {
        String lockKey = LOCK_PREFIX + resourceId;
        String lockValue = generateLockValue();

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                if (tryAcquireLock(lockKey, lockValue, timeout)) {
                    LOGGER.fine("Acquired lock: " + lockKey);
                    return new DistributedLock(lockKey, lockValue);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                        "Failed to acquire lock: " + lockKey + ", attempt " + attempt, e);
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

    private boolean tryAcquireLock(String lockKey, String lockValue, Duration timeout) {
        try (Jedis jedis = jedisPool.getResource()) {
            SetParams params = new SetParams()
                    .nx()
                    .ex(timeout.getSeconds());

            String result = jedis.set(lockKey, lockValue, params);
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