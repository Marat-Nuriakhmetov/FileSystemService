package com.fileservice.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileReadServiceTest {

    private FileReadService fileReadService;
    @TempDir
    Path tempDir; // JUnit will create and clean up this temporary directory

    @BeforeEach
    void setUp() {
        fileReadService = new FileReadService();
    }

    @Test
    void shouldReadContentFromValidFile() throws IOException {
        // Arrange
        String content = "Hello, World!";
        File testFile = createTestFile(content);

        // Act
        String result = fileReadService.read(testFile.getPath(), 0, content.length());

        // Assert
        assertEquals(content, result);
    }

    @Test
    void shouldReadPartialContent() throws IOException {
        // Arrange
        String content = "Hello, World!";
        File testFile = createTestFile(content);

        // Act
        String result = fileReadService.read(testFile.getPath(), 7, 5);

        // Assert
        assertEquals("World", result);
    }

    @Test
    void shouldThrowExceptionWhenFileNotFound() {
        // Arrange
        String nonExistentPath = tempDir.resolve("non-existent.txt").toString();

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                fileReadService.read(nonExistentPath, 0, 10)
        );
    }

    @Test
    void shouldHandleEmptyFile() throws IOException {
        // Arrange
        File emptyFile = createTestFile("");

        // Act
        String result = fileReadService.read(emptyFile.getPath(), 0, 0);

        // Assert
        assertEquals("", result);
    }

    @Test
    void shouldThrowExceptionWhenOffsetBeyondFileSize() throws IOException {
        // Arrange
        String content = "Hello";
        File testFile = createTestFile(content);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                fileReadService.read(testFile.getPath(), 10, 1)
        );
    }

    @Test
    void shouldHandleZeroLengthRead() throws IOException {
        // Arrange
        String content = "Hello";
        File testFile = createTestFile(content);

        // Act
        String result = fileReadService.read(testFile.getPath(), 0, 0);

        // Assert
        assertEquals("", result);
    }

    @Test
    void shouldReadUpToEndOfFile() throws IOException {
        // Arrange
        String content = "Hello, World!";
        File testFile = createTestFile(content);

        // Act
        String result = fileReadService.read(testFile.getPath(), 7, 100);

        // Assert
        assertEquals("World!", result);
    }

    @Test
    void shouldHandleNegativeOffset() throws IOException {
        // Arrange
        String content = "Hello";
        File testFile = createTestFile(content);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                fileReadService.read(testFile.getPath(), -1, 5)
        );
    }

    @Test
    void shouldHandleNegativeLength() throws IOException {
        // Arrange
        String content = "Hello";
        File testFile = createTestFile(content);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                fileReadService.read(testFile.getPath(), 0, -1)
        );
    }

    // Helper method to create test file
    private File createTestFile(String content) throws IOException {
        File file = tempDir.resolve("test.txt").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }
}
