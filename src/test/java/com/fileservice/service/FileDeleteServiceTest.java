package com.fileservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDeleteServiceTest {

    private FileDeleteService fileDeleteService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        fileDeleteService = new FileDeleteService(tempDir);
    }

    // Constructor Tests
    @Test
    void constructor_WithNullRootDirectory_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileDeleteService(null));
    }

    // Delete File Tests
    @Test
    void delete_ExistingFile_Success() throws IOException {
        // Given
        Path file = createFile("test.txt");

        // When
        boolean result = fileDeleteService.delete(file.toString(), false);

        // Then
        assertTrue(result);
        assertFalse(Files.exists(file));
    }

    @Test
    void delete_NonexistentFile_ReturnsFalse() throws IOException {
        // Given
        Path nonexistentFile = tempDir.resolve("nonexistent.txt");

        // When
        boolean result = fileDeleteService.delete(nonexistentFile.toString(), false);

        // Then
        assertFalse(result);
    }

    // Delete Directory Tests
    @Test
    void delete_EmptyDirectory_Success() throws IOException {
        // Given
        Path dir = createDirectory("emptyDir");

        // When
        boolean result = fileDeleteService.delete(dir.toString(), false);

        // Then
        assertTrue(result);
        assertFalse(Files.exists(dir));
    }

    @Test
    void delete_NonEmptyDirectoryWithoutRecursive_ThrowsException() throws IOException {
        // Given
        Path dir = createDirectory("nonEmptyDir");
        createFile(dir.resolve("file.txt"));

        // When & Then
        assertThrows(DirectoryNotEmptyException.class,
                () -> fileDeleteService.delete(dir.toString(), false));
        assertTrue(Files.exists(dir));
    }

    @Test
    void delete_NonEmptyDirectoryWithRecursive_Success() throws IOException {
        // Given
        Path dir = createDirectory("nonEmptyDir");
        createFile(dir.resolve("file1.txt"));
        Path subDir = createDirectory(dir.resolve("subDir"));
        createFile(subDir.resolve("file2.txt"));

        // When
        boolean result = fileDeleteService.delete(dir.toString(), true);

        // Then
        assertTrue(result);
        assertFalse(Files.exists(dir));
    }

    // Path Validation Tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void delete_InvalidPath_ThrowsException(String invalidPath) {
        assertThrows(IllegalArgumentException.class,
                () -> fileDeleteService.delete(invalidPath, false));
    }

    @Test
    void delete_PathOutsideRoot_ThrowsException() throws IOException {
        // Given
        Path outsidePath = tempDir.getParent().resolve("outside.txt");

        // When & Then
        assertThrows(SecurityException.class,
                () -> fileDeleteService.delete(outsidePath.toString(), false));
    }

    // Exists and IsDirectory Tests
    @Test
    void exists_ExistingFile_ReturnsTrue() throws IOException {
        // Given
        Path file = createFile("test.txt");

        // When & Then
        assertTrue(fileDeleteService.exists(file.toString()));
    }

    @Test
    void exists_NonexistentFile_ReturnsFalse() {
        // Given
        Path nonexistentFile = tempDir.resolve("nonexistent.txt");

        // When & Then
        assertFalse(fileDeleteService.exists(nonexistentFile.toString()));
    }

    @Test
    void isDirectory_Directory_ReturnsTrue() throws IOException {
        // Given
        Path dir = createDirectory("testDir");

        // When & Then
        assertTrue(fileDeleteService.isDirectory(dir.toString()));
    }

    @Test
    void isDirectory_File_ReturnsFalse() throws IOException {
        // Given
        Path file = createFile("test.txt");

        // When & Then
        assertFalse(fileDeleteService.isDirectory(file.toString()));
    }

    // Complex Scenario Tests
    @Test
    void delete_ComplexDirectoryStructure_Success() throws IOException {
        // Given
        Path rootDir = createDirectory("complexDir");
        createFile(rootDir.resolve("file1.txt"));
        Path subDir1 = createDirectory(rootDir.resolve("subDir1"));
        createFile(subDir1.resolve("file2.txt"));
        Path subDir2 = createDirectory(rootDir.resolve("subDir2"));
        createFile(subDir2.resolve("file3.txt"));
        createDirectory(subDir2.resolve("emptyDir"));

        // When
        boolean result = fileDeleteService.delete(rootDir.toString(), true);

        // Then
        assertTrue(result);
        assertFalse(Files.exists(rootDir));
    }

    // Helper Methods
    private Path createFile(String fileName) throws IOException {
        return createFile(tempDir.resolve(fileName));
    }

    private Path createFile(Path path) throws IOException {
        Files.createFile(path);
        return path;
    }

    private Path createDirectory(String dirName) throws IOException {
        return createDirectory(tempDir.resolve(dirName));
    }

    private Path createDirectory(Path path) throws IOException {
        Files.createDirectory(path);
        return path;
    }

    @Test
    void delete_SymbolicLink_Success() throws IOException {
        // Given
        Path target = createFile("target.txt");
        Path link = tempDir.resolve("link");
        Files.createSymbolicLink(link, target);

        // When
        boolean result = fileDeleteService.delete(link.toString(), false);

        // Then
        assertTrue(result);
        assertFalse(Files.exists(link));
        assertTrue(Files.exists(target)); // Target should still exist
    }

    @Test
    void delete_ReadOnlyFile_Success() throws IOException {
        // Given
        Path file = createFile("readonly.txt");
        file.toFile().setReadOnly();

        // When
        boolean result = fileDeleteService.delete(file.toString(), false);

        // Then
        assertTrue(result);
        assertFalse(Files.exists(file));
    }
}