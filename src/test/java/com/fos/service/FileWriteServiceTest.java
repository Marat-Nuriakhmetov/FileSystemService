package com.fos.service;

import com.fos.service.lock.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FileWriteServiceTest {

    @Mock
    private DistributedLockService lockService;

    @Mock
    private DistributedLockService.DistributedLock distributedLock;

    private FileWriteService fileWriteService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(lockService.acquireLock(anyString())).thenReturn(distributedLock);
        doNothing().when(distributedLock).close();

        fileWriteService = new FileWriteService(tempDir, lockService);
    }

    // Basic Operation Tests
    @Test
    void append_BasicWrite_Success() throws IOException, DistributedLockService.LockAcquisitionException {
        // Given
        Path file = tempDir.resolve("test.txt");
        String content = "Hello, World!";

        // When
        boolean result = fileWriteService.append(file.toString(), content);

        // Then
        assertTrue(result);
        assertEquals(content, Files.readString(file));
        verify(lockService).acquireLock("file:" + file);
    }

    @Test
    void append_MultipleWrites_Success() throws IOException, DistributedLockService.LockAcquisitionException {
        // Given
        Path file = tempDir.resolve("test.txt");
        String content1 = "Line 1\n";
        String content2 = "Line 2\n";

        // When
        fileWriteService.append(file.toString(), content1);
        fileWriteService.append(file.toString(), content2);

        // Then
        assertEquals(content1 + content2, Files.readString(file));
        verify(lockService, times(2)).acquireLock(anyString());
    }

    // Parameter Validation Tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void append_InvalidPath_ThrowsException(String invalidPath) {
        assertThrows(IllegalArgumentException.class,
                () -> fileWriteService.append(invalidPath, "content"));
    }

    @Test
    void append_NullData_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> fileWriteService.append("test.txt", null));
    }

    // Security Tests
    @Test
    void append_PathOutsideRoot_ThrowsException() {
        // Given
        Path outsidePath = tempDir.getParent().resolve("outside.txt");

        // When & Then
        assertThrows(SecurityException.class,
                () -> fileWriteService.append(outsidePath.toString(), "content"));
    }

    // Lock Handling Tests
    @Test
    void append_LockAcquisitionFails_ThrowsException() throws Exception {
        // Given
        when(lockService.acquireLock(anyString()))
                .thenThrow(new DistributedLockService.LockAcquisitionException("Lock failed"));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> fileWriteService.append("test.txt", "content"));
    }

    @Test
    void append_EnsuresLockIsReleased() throws IOException {
        // Given
        Path file = tempDir.resolve("test.txt");

        // When
        fileWriteService.append(file.toString(), "content");

        // Then
        verify(distributedLock).close();
    }

    // Concurrent Operation Tests
    @Test
    void append_ConcurrentWrites_Success() throws Exception {
        // Given
        Path file = tempDir.resolve("concurrent.txt");
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        try {
            // When
            List<CompletableFuture<Void>> futures = IntStream.range(0, numThreads)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        try {
                            fileWriteService.append(file.toString(),
                                    String.format("Thread-%d\n", i));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, executor))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Then
            List<String> lines = Files.readAllLines(file);
            assertEquals(numThreads, lines.size());
            assertTrue(lines.stream().allMatch(line -> line.matches("Thread-\\d+")));
            verify(lockService, times(numThreads)).acquireLock(anyString());

        } finally {
            executor.shutdown();
        }
    }

    // Edge Cases
    @Test
    void append_ToNonexistentDirectory_ThrowsException() throws IOException {
        // When & Then
        assertThrows(NoSuchFileException.class,
                () -> fileWriteService.append("nonexistent/test.txt", "content"));
    }

    // Performance Tests
    @Test
    void append_LargeNumberOfWrites_Success() throws Exception {
        // Given
        Path file = tempDir.resolve("performance.txt");
        int numWrites = 1000;

        // When
        List<CompletableFuture<Void>> futures = IntStream.range(0, numWrites)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        fileWriteService.append(file.toString(), "Line " + i + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Then
        List<String> lines = Files.readAllLines(file);
        assertEquals(numWrites, lines.size());
        verify(lockService, times(numWrites)).acquireLock(anyString());
    }

    // Error Recovery Tests
    @Test
    void append_WhenIOExceptionOccurs_LockIsReleased() throws Exception {
        // Given
        Path file = tempDir.resolve("error.txt");
        doThrow(new IOException("Test error"))
                .when(distributedLock).close();

        // When & Then
        assertThrows(IOException.class,
                () -> fileWriteService.append(file.toString(), "content"));
        verify(distributedLock).close();
    }
}