package com.fos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileMoveServiceTest {

    private FileMoveService fileMoveService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileMoveService = new FileMoveService(tempDir);
    }

    // Constructor Tests
    @Test
    void constructor_WithNullRootDirectory_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileMoveService(null));
    }

    // Basic Move Tests
    @Test
    void move_ValidFile_Success() throws IOException {
        // Given
        Path source = createFile("source.txt", "test content");
        Path target = tempDir.resolve("target.txt");

        // When
        boolean result = fileMoveService.move(source.toString(), target.toString());

        // Then
        assertTrue(result);
        assertFalse(Files.exists(source));
        assertTrue(Files.exists(target));
        assertEquals("test content", Files.readString(target));
    }

    @Test
    void move_EmptyDirectory_Success() throws IOException {
        // Given
        Path sourceDir = Files.createDirectory(tempDir.resolve("sourceDir"));
        Path targetDir = tempDir.resolve("targetDir");

        // When
        boolean result = fileMoveService.move(sourceDir.toString(), targetDir.toString());

        // Then
        assertTrue(result);
        assertFalse(Files.exists(sourceDir));
        assertTrue(Files.exists(targetDir));
        assertTrue(Files.isDirectory(targetDir));
    }

    // Error Cases
    @Test
    void move_NonexistentSource_ThrowsException() {
        // Given
        Path source = tempDir.resolve("nonexistent.txt");
        Path target = tempDir.resolve("target.txt");

        // When & Then
        assertThrows(NoSuchFileException.class,
                () -> fileMoveService.move(source.toString(), target.toString()));
    }

    @Test
    void move_ExistingTarget_ThrowsException() throws IOException {
        // Given
        Path source = createFile("source.txt");
        Path target = createFile("target.txt");

        // When & Then
        assertThrows(FileAlreadyExistsException.class,
                () -> fileMoveService.move(source.toString(), target.toString()));
    }

    @Test
    void move_NonexistentTargetParent_ThrowsException() throws IOException {
        // Given
        Path source = createFile("source.txt");
        Path target = tempDir.resolve("nonexistent").resolve("target.txt");

        // When & Then
        assertThrows(NoSuchFileException.class,
                () -> fileMoveService.move(source.toString(), target.toString()));
    }

    // Directory Tests
    @Test
    void move_DirectoryIntoItself_ThrowsException() throws IOException {
        // Given
        Path sourceDir = Files.createDirectory(tempDir.resolve("sourceDir"));
        Path targetDir = sourceDir.resolve("subdir");

        // When & Then
        assertThrows(IOException.class,
                () -> fileMoveService.move(sourceDir.toString(), targetDir.toString()));
    }

    @Test
    void move_DirectoryWithContent_Success() throws IOException {
        // Given
        Path sourceDir = Files.createDirectory(tempDir.resolve("sourceDir"));
        createFile(sourceDir.resolve("file1.txt"));
        createFile(sourceDir.resolve("file2.txt"));
        Files.createDirectory(sourceDir.resolve("subdir"));
        Path targetDir = tempDir.resolve("targetDir");

        // When
        boolean result = fileMoveService.move(sourceDir.toString(), targetDir.toString());

        // Then
        assertTrue(result);
        assertFalse(Files.exists(sourceDir));
        assertTrue(Files.exists(targetDir));
        assertTrue(Files.exists(targetDir.resolve("file1.txt")));
        assertTrue(Files.exists(targetDir.resolve("file2.txt")));
        assertTrue(Files.exists(targetDir.resolve("subdir")));
    }

    // Path Validation Tests
    @ParameterizedTest
    @NullAndEmptySource
    void move_NullOrEmptySource_ThrowsException(String sourcePath) {
        assertThrows(IllegalArgumentException.class,
                () -> fileMoveService.move(sourcePath, "target.txt"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void move_NullOrEmptyTarget_ThrowsException(String targetPath) throws IOException {
        Path source = createFile("source.txt");
        assertThrows(IllegalArgumentException.class,
                () -> fileMoveService.move(source.toString(), targetPath));
    }

    @Test
    void move_PathsOutsideRoot_ThrowsException() throws IOException {
        // Given
        Path outsidePath = tempDir.getParent().resolve("outside.txt");
        Path source = createFile("source.txt");

        // When & Then
        assertThrows(SecurityException.class,
                () -> fileMoveService.move(source.toString(), outsidePath.toString()));
        assertThrows(SecurityException.class,
                () -> fileMoveService.move(outsidePath.toString(), source.toString()));
    }

    // Special Cases
    @Test
    void move_SymbolicLink_Success() throws IOException {
        // Given
        Path source = createFile("source.txt");
        Path link = Files.createSymbolicLink(tempDir.resolve("link"), source);
        Path target = tempDir.resolve("target.txt");

        // When
        boolean result = fileMoveService.move(link.toString(), target.toString());

        // Then
        assertTrue(result);
        assertFalse(Files.exists(link));
        assertTrue(Files.exists(target));
        assertTrue(Files.exists(source)); // Original file should still exist
    }

    // Helper Methods
    private Path createFile(String fileName) throws IOException {
        return createFile(tempDir.resolve(fileName));
    }

    private Path createFile(Path path) throws IOException {
        return Files.createFile(path);
    }

    private Path createFile(String fileName, String content) throws IOException {
        Path file = createFile(fileName);
        Files.writeString(file, content);
        return file;
    }

    private Path createFile(Path path, String content) throws IOException {
        Path file = createFile(path);
        Files.writeString(file, content);
        return file;
    }
}