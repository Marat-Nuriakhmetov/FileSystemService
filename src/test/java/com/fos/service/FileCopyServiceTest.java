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

import static org.junit.jupiter.api.Assertions.*;

class FileCopyServiceTest {

    private FileCopyService fileCopyService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        fileCopyService = new FileCopyService(tempDir);
    }

    @Test
    void copy_ValidFile_Success() throws IOException {
        // Given
        Path sourceFile = createSourceFile("test.txt", "test content");
        Path targetFile = tempDir.resolve("target.txt");

        // When
        boolean result = fileCopyService.copy(
                sourceFile.toString(),
                targetFile.toString()
        );

        // Then
        assertTrue(result);
        assertTrue(Files.exists(targetFile));
        assertArrayEquals(
                Files.readAllBytes(sourceFile),
                Files.readAllBytes(targetFile)
        );
    }

    @Test
    void copy_EmptyFile_Success() throws IOException {
        // Given
        Path sourceFile = createSourceFile("empty.txt", "");
        Path targetFile = tempDir.resolve("target.txt");

        // When
        boolean result = fileCopyService.copy(
                sourceFile.toString(),
                targetFile.toString()
        );

        // Then
        assertTrue(result);
        assertTrue(Files.exists(targetFile));
        assertEquals(0L, Files.size(targetFile));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void copy_NullOrEmptySourcePath_ThrowsIllegalArgumentException(String sourcePath) {
        // Given
        String targetPath = tempDir.resolve("target.txt").toString();

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileCopyService.copy(sourcePath, targetPath)
        );
        assertEquals("Path cannot be null or empty", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void copy_NullOrEmptyTargetPath_ThrowsIllegalArgumentException(String targetPath) {
        // Given
        Path sourceFile = tempDir.resolve("source.txt");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileCopyService.copy(sourceFile.toString(), targetPath)
        );
        assertEquals("Path cannot be null or empty", exception.getMessage());
    }

    @Test
    void copy_NonexistentSourceFile_ThrowsNoSuchFileException() {
        // Given
        Path nonexistentSource = tempDir.resolve("nonexistent.txt");
        Path targetFile = tempDir.resolve("target.txt");

        // When & Then
        NoSuchFileException exception = assertThrows(
                NoSuchFileException.class,
                () -> fileCopyService.copy(
                        nonexistentSource.toString(),
                        targetFile.toString()
                )
        );
        assertTrue(exception.getMessage().contains("Source file does not exist"));
    }

    @Test
    void copy_SourceIsDirectory_ThrowsIOException() throws IOException {
        // Given
        Path sourceDir = Files.createDirectory(tempDir.resolve("sourcedir"));
        Path targetFile = tempDir.resolve("target.txt");

        // When & Then
        IOException exception = assertThrows(
                IOException.class,
                () -> fileCopyService.copy(
                        sourceDir.toString(),
                        targetFile.toString()
                )
        );
        assertTrue(exception.getMessage().contains("Source is a directory"));
    }

    @Test
    void copy_TargetFileExists_ThrowsFileAlreadyExistsException() throws IOException {
        // Given
        Path sourceFile = createSourceFile("source.txt", "source content");
        Path targetFile = createSourceFile("target.txt", "existing content");

        // When & Then
        assertThrows(
                FileAlreadyExistsException.class,
                () -> fileCopyService.copy(
                        sourceFile.toString(),
                        targetFile.toString()
                )
        );
    }

    @Test
    void copy_InvalidTargetPath_ThrowsIOException() throws IOException {
        // Given
        Path sourceFile = createSourceFile("source.txt", "test content");
        Path invalidTarget = tempDir.resolve("nonexistent").resolve("target.txt");

        // When & Then
        assertThrows(
                IOException.class,
                () -> fileCopyService.copy(
                        sourceFile.toString(),
                        invalidTarget.toString()
                )
        );
    }

    // Helper method to create test files
    private Path createSourceFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.write(file, content.getBytes());
        return file;
    }
}