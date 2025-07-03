package com.fileservice.service.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        when(jedisPool.getResource()).thenReturn(jedis);
        lockService = new DistributedLockService(jedisPool);
    }

    @Test
    void acquireLock_Success() throws Exception {
        // Given
        String resourceId = "test-resource";
        when(jedis.set(anyString(), anyString(), any(SetParams.class)))
                .thenReturn("OK");

        // When
        try (DistributedLockService.DistributedLock lock =
                     lockService.acquireLock(resourceId)) {
            // Then
            assertNotNull(lock);
            verify(jedis).set(
                    eq(resourceId),
                    anyString(),
                    any(SetParams.class)
            );
        }
    }

    @Test
    void acquireLock_WhenLockExists_RetriesAndFails() throws Exception {
        // Given
        String resourceId = "test-resource";
        when(jedis.set(anyString(), anyString(), any(SetParams.class)))
                .thenReturn(null);

        // When & Then
        assertThrows(DistributedLockService.LockAcquisitionException.class,
                () -> lockService.acquireLock(resourceId));

        verify(jedis, times(3)).set(
                eq(resourceId),
                anyString(),
                any(SetParams.class)
        );
    }

    @Test
    void releaseLock_Success() throws Exception {
        // Given
        String resourceId = "test-resource";
        when(jedis.set(anyString(), anyString(), any(SetParams.class)))
                .thenReturn("OK");

        // When
        DistributedLockService.DistributedLock lock = lockService.acquireLock(resourceId);
        lock.close();

        // Then
        verify(jedis).del(eq(resourceId));
    }

    @Test
    void concurrentLockAttempts_OnlyOneSucceeds() throws Exception {
        // Given
        String resourceId = "test-resource";
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        when(jedis.set(anyString(), anyString(), any(SetParams.class)))
                .thenReturn("OK")
                .thenReturn(null);

        // When
        CompletableFuture<Void>[] futures = IntStream.range(0, numThreads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        startLatch.await();
                        lockService.acquireLock(resourceId);
                        successCount.incrementAndGet();
                    } catch (Exception ignored) {
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

        startLatch.countDown();
        CompletableFuture.allOf(futures).join();

        // Then
        assertEquals(1, successCount.get());
        executor.shutdown();
    }

    @Test
    void acquireLock_WhenJedisThrowsException_RetriesAndFails() {
        // Given
        String resourceId = "test-resource";
        when(jedis.set(anyString(), anyString(), any(SetParams.class)))
                .thenThrow(new RuntimeException("Redis error"));

        // When & Then
        assertThrows(DistributedLockService.LockAcquisitionException.class,
                () -> lockService.acquireLock(resourceId));

        verify(jedis, times(3)).set(
                eq(resourceId),
                anyString(),
                any(SetParams.class)
        );
    }

    @Test
    void acquireLock_WhenInterrupted_ThrowsException() {
        // Given
        String resourceId = "test-resource";
        when(jedis.set(anyString(), anyString(), any(SetParams.class)))
                .thenAnswer(invocation -> {
                    Thread.currentThread().interrupt();
                    return null;
                });

        // When & Then
        DistributedLockService.LockAcquisitionException exception =
                assertThrows(DistributedLockService.LockAcquisitionException.class,
                        () -> lockService.acquireLock(resourceId));

        assertTrue(exception.getCause() instanceof InterruptedException);
        assertTrue(Thread.interrupted()); // Clear interrupted status
    }
}