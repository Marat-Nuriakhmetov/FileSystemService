package com.fileservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileCreateServiceTest {

    private FileCreateService fileCreateService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        fileCreateService = new FileCreateService(tempDir);
    }

    @Test
    void createFile_WithValidPath_Success() throws IOException {
        // Given
        Path filePath = tempDir.resolve("test.txt");

        // When
        boolean result = fileCreateService.createFile(filePath.toString());

        // Then
        assertTrue(result);
        assertTrue(Files.exists(filePath));
        assertTrue(Files.isRegularFile(filePath));
    }

    @Test
    void createFile_WithValidNestedPath_Success() throws IOException {
        // Given
        Path dirPath = tempDir.resolve("nested");
        Files.createDirectory(dirPath);
        Path filePath = dirPath.resolve("test.txt");

        // When
        boolean result = fileCreateService.createFile(filePath.toString());

        // Then
        assertTrue(result);
        assertTrue(Files.exists(filePath));
        assertTrue(Files.isRegularFile(filePath));
    }

    @Test
    void createFile_FileAlreadyExists_ThrowsException() throws IOException {
        // Given
        Path filePath = tempDir.resolve("existing.txt");
        Files.createFile(filePath);

        // When & Then
        assertThrows(FileAlreadyExistsException.class,
                () -> fileCreateService.createFile(filePath.toString())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void createFile_InvalidPath_ThrowsException(String path) {
        assertThrows(IllegalArgumentException.class,
                () -> fileCreateService.createFile(path)
        );
    }

    @Test
    void createFile_NonexistentParentDirectory_ThrowsException() {
        // Given
        Path filePath = tempDir.resolve("nonexistent").resolve("test.txt");

        // When & Then
        assertThrows(IOException.class,
                () -> fileCreateService.createFile(filePath.toString())
        );
    }

    @Test
    void createDirectory_WithValidPath_Success() throws IOException {
        // Given
        Path dirPath = tempDir.resolve("newDir");

        // When
        boolean result = fileCreateService.createDirectory(dirPath.toString());

        // Then
        assertTrue(result);
        assertTrue(Files.exists(dirPath));
        assertTrue(Files.isDirectory(dirPath));
    }

    @Test
    void createDirectory_WithValidNestedPath_Success() throws IOException {
        // Given
        Path parentDir = tempDir.resolve("parent");
        Files.createDirectory(parentDir);
        Path dirPath = parentDir.resolve("child");

        // When
        boolean result = fileCreateService.createDirectory(dirPath.toString());

        // Then
        assertTrue(result);
        assertTrue(Files.exists(dirPath));
        assertTrue(Files.isDirectory(dirPath));
    }

    @Test
    void createDirectory_DirectoryAlreadyExists_ThrowsException() throws IOException {
        // Given
        Path dirPath = tempDir.resolve("existingDir");
        Files.createDirectory(dirPath);

        // When & Then
        assertThrows(FileAlreadyExistsException.class,
                () -> fileCreateService.createDirectory(dirPath.toString())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void createDirectory_InvalidPath_ThrowsException(String path) {
        assertThrows(IllegalArgumentException.class,
                () -> fileCreateService.createDirectory(path)
        );
    }

    @Test
    void createDirectory_NonexistentParentDirectory_ThrowsException() {
        // Given
        Path dirPath = tempDir.resolve("nonexistent").resolve("newDir");

        // When & Then
        assertThrows(IOException.class,
                () -> fileCreateService.createDirectory(dirPath.toString())
        );
    }

    @Test
    void createDirectory_FileExistsAtPath_ThrowsException() throws IOException {
        // Given
        Path filePath = tempDir.resolve("file.txt");
        Files.createFile(filePath);

        // When & Then
        assertThrows(FileAlreadyExistsException.class,
                () -> fileCreateService.createDirectory(filePath.toString())
        );
    }

    @Test
    void createFile_OutsideRootDirectory_ThrowsException() throws IOException {
        // Given
        Path outsidePath = tempDir.getParent().resolve("outside.txt");

        // When & Then
        assertThrows(SecurityException.class,
                () -> fileCreateService.createFile(outsidePath.toString())
        );
    }

    @Test
    void createDirectory_OutsideRootDirectory_ThrowsException() throws IOException {
        // Given
        Path outsidePath = tempDir.getParent().resolve("outsideDir");

        // When & Then
        assertThrows(SecurityException.class,
                () -> fileCreateService.createDirectory(outsidePath.toString())
        );
    }

    @Test
    void constructor_WithNullRootDirectory_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileCreateService(null)
        );
    }
}