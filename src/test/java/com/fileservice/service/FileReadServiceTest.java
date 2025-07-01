package com.fileservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileReadServiceTest {

    private FileReadService fileReadService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileReadService = new FileReadService(tempDir);
    }

    // Constructor Tests
    @Test
    void constructor_WithNullRootDirectory_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileReadService(null));
    }


    // Basic Read Tests
    @Test
    void read_EntireFile_Success() throws IOException {
        // Given
        String content = "Hello, World!";
        Path file = createFile("test.txt", content);

        // When
        String result = fileReadService.read(file.toString(), 0, content.length());

        // Then
        assertEquals(content, result);
    }

    @Test
    void read_PartialContent_Success() throws IOException {
        // Given
        String content = "Hello, World!";
        Path file = createFile("test.txt", content);

        // When
        String result = fileReadService.read(file.toString(), 7, 5);

        // Then
        assertEquals("World", result);
    }

    // Parameter Validation Tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void read_InvalidPath_ThrowsException(String invalidPath) {
        assertThrows(IllegalArgumentException.class,
                () -> fileReadService.read(invalidPath, 0, 10));
    }

    @Test
    void read_NegativeOffset_ThrowsException() throws IOException {
        // Given
        Path file = createFile("test.txt", "content");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> fileReadService.read(file.toString(), -1, 10));
    }

    @Test
    void read_NegativeLength_ThrowsException() throws IOException {
        // Given
        Path file = createFile("test.txt", "content");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> fileReadService.read(file.toString(), 0, -1));
    }

    @Test
    void read_LengthExceedsMaximum_ThrowsException() throws IOException {
        // Given
        Path file = createFile("test.txt", "content");
        int tooLarge = 1024 * 1024 + 1;

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> fileReadService.read(file.toString(), 0, tooLarge));
    }

    // Security Tests
    @Test
    void read_PathOutsideRoot_ThrowsException() throws IOException {
        // Given
        Path outsidePath = tempDir.getParent().resolve("outside.txt");
        Files.writeString(outsidePath, "content");

        // When & Then
        assertThrows(SecurityException.class,
                () -> fileReadService.read(outsidePath.toString(), 0, 10));
    }

    // Edge Cases
    @Test
    void read_EmptyFile_ReturnsEmptyString() throws IOException {
        // Given
        Path file = createFile("empty.txt", "");

        // When
        String result = fileReadService.read(file.toString(), 0, 10);

        // Then
        assertEquals("", result);
    }

    @Test
    void read_OffsetBeyondFileSize_ThrowsException() throws IOException {
        // Given
        Path file = createFile("test.txt", "content");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> fileReadService.read(file.toString(), 100, 10));
    }

    @Test
    void read_NonexistentFile_ThrowsException() {
        // Given
        Path nonexistentFile = tempDir.resolve("nonexistent.txt");

        // When & Then
        assertThrows(IOException.class,
                () -> fileReadService.read(nonexistentFile.toString(), 0, 10));
    }

    // Special Cases
    @Test
    void read_Directory_ThrowsException() throws IOException {
        // Given
        Path directory = Files.createDirectory(tempDir.resolve("testDir"));

        // When & Then
        assertThrows(IOException.class,
                () -> fileReadService.read(directory.toString(), 0, 10));
    }

    // isReadable Tests
    @Test
    void isReadable_ExistingFile_ReturnsTrue() throws IOException {
        // Given
        Path file = createFile("test.txt", "content");

        // When & Then
        assertTrue(fileReadService.isReadable(file.toString()));
    }

    @Test
    void isReadable_NonexistentFile_ReturnsFalse() {
        // Given
        Path nonexistentFile = tempDir.resolve("nonexistent.txt");

        // When & Then
        assertFalse(fileReadService.isReadable(nonexistentFile.toString()));
    }

    @Test
    void isReadable_Directory_ReturnsFalse() throws IOException {
        // Given
        Path directory = Files.createDirectory(tempDir.resolve("testDir"));

        // When & Then
        assertFalse(fileReadService.isReadable(directory.toString()));
    }

    // Helper Methods
    private Path createFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void read_LargeFile_Success() throws IOException {
        // Given
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("Line ").append(i).append("\n");
        }
        Path file = createFile("large.txt", largeContent.toString());

        // When
        String result = fileReadService.read(file.toString(), 100, 1000);

        // Then
        assertEquals(largeContent.substring(100, 1100), result);
    }

    @Test
    void read_MultipleReadsFromSameFile_Success() throws IOException {
        // Given
        String content = "Hello, World!";
        Path file = createFile("test.txt", content);

        // When & Then
        assertEquals("Hello", fileReadService.read(file.toString(), 0, 5));
        assertEquals("World", fileReadService.read(file.toString(), 7, 5));
        assertEquals("!", fileReadService.read(file.toString(), 12, 1));
    }
}