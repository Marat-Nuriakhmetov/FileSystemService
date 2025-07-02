package com.fileservice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileWriteServiceTest {

    private FileWriteService fileWriteService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        fileWriteService = new FileWriteService(tempDir);
    }

    @AfterEach
    void tearDown() {
        fileWriteService.shutdown();
    }

    // Constructor Tests
    @Test
    void constructor_WithNullRootDirectory_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileWriteService(null));
    }

    // Basic Write Tests
    @Test
    void append_BasicWrite_Success() throws IOException {
        // Given
        Path file = tempDir.resolve("test.txt");
        String data = "Hello, World!";

        // When
        boolean result = fileWriteService.append(file.toString(), data);

        // Then
        assertTrue(result);
        assertEquals(data, Files.readString(file));
    }

    @Test
    void append_MultipleWrites_Success() throws IOException {
        // Given
        Path file = tempDir.resolve("test.txt");
        String data1 = "Line 1\n";
        String data2 = "Line 2\n";

        // When
        fileWriteService.append(file.toString(), data1);
        fileWriteService.append(file.toString(), data2);

        // Then
        String content = Files.readString(file);
        assertEquals(data1 + data2, content);
    }

    // Concurrent Write Tests
    @Test
    void append_ConcurrentWrites_Success() throws Exception {
        // Given
        Path file = tempDir.resolve("concurrent.txt");
        int numThreads = 10;
        int writesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        try {
            // When
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < numThreads; i++) {
                final int threadNum = i;
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < writesPerThread; j++) {
                        fileWriteService.append(file.toString(),
                                String.format("Thread-%d-Write-%d\n", threadNum, j));
                    }
                    return null;
                }));
            }

            // Wait for all writes to complete
            for (Future<Void> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }

            // Then
            List<String> lines = Files.readAllLines(file);
            assertEquals(numThreads * writesPerThread, lines.size());
            assertTrue(lines.stream().allMatch(line ->
                    line.matches("Thread-\\d+-Write-\\d+")));

        } finally {
            executor.shutdownNow();
        }
    }

    // Error Cases
    @ParameterizedTest
    @NullAndEmptySource
    void append_NullOrEmptyPath_ThrowsException(String invalidPath) {
        assertThrows(IllegalArgumentException.class,
                () -> fileWriteService.append(invalidPath, "data"));
    }

    @Test
    void append_NullData_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> fileWriteService.append(tempDir.resolve("test.txt").toString(), null));
    }

    @Test
    void append_PathOutsideRoot_ThrowsException() {
        // Given
        Path outsidePath = tempDir.getParent().resolve("outside.txt");

        // When & Then
        assertThrows(SecurityException.class,
                () -> fileWriteService.append(outsidePath.toString(), "data"));
    }

    // Lock Cleanup Tests
    // @Test
    void lockCleanup_RemovesExpiredLocks() throws Exception {
        // Given
        Path file = tempDir.resolve("cleanup.txt");
        fileWriteService.append(file.toString(), "initial\n");

        // When
        Thread.sleep(TimeUnit.MINUTES.toMillis(2)); // Wait for lock to expire

        // Then
        assertTrue(fileWriteService.append(file.toString(), "after cleanup\n"));
    }
    void lockCleanup_ExpiresOldLocks() throws Exception {
        // Given
        Path file = tempDir.resolve("cleanup.txt");
        fileWriteService.append(file.toString(), "initial\n");

        // When
        Thread.sleep(TimeUnit.MINUTES.toMillis(2)); // Wait for lock to expire

        // Then
        assertTrue(fileWriteService.append(file.toString(), "after cleanup\n"));
    }

    // Performance Tests
    @Test
    void append_LargeNumberOfWrites_Success() throws Exception {
        // Given
        Path file = tempDir.resolve("performance.txt");
        int numWrites = 1000;

        // When
        List<CompletableFuture<Void>> futures = IntStream.range(0, numWrites)
                .mapToObj(i -> CompletableFuture.runAsync(() ->
                {
                    try {
                        fileWriteService.append(file.toString(), "Line " + i + "\n");
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }))
                .collect(Collectors.toList());

        // Then
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);

        List<String> lines = Files.readAllLines(file);
        assertEquals(numWrites, lines.size());
    }

    // Special Cases
    @Test
    void append_ToDirectory_ThrowsException() throws IOException {
        // Given
        Path dir = Files.createDirectory(tempDir.resolve("dir"));

        // When & Then
        assertThrows(IOException.class,
                () -> fileWriteService.append(dir.toString(), "data"));
    }

    @Test
    @Disabled("Not implemented yet")
    void append_ToReadOnlyFile_ThrowsException() throws IOException {
        // Given
        Path file = Files.createFile(tempDir.resolve("readonly.txt"));
        file.toFile().setReadOnly();

        // When & Then
        assertThrows(IOException.class,
                () -> fileWriteService.append(file.toString(), "data"));
    }
}