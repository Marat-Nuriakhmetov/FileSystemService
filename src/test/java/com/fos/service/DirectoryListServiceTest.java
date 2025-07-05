package com.fos.service;

import com.fos.dto.FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DirectoryListServiceTest {

    @Mock
    private FileGetInfoService fileGetInfoService;

    private DirectoryListService directoryListService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        directoryListService = new DirectoryListService(fileGetInfoService, tempDir);
    }

    // Constructor Tests
    @Test
    void constructor_WithNullRootDirectory_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DirectoryListService(fileGetInfoService, null));
    }

    // Basic Listing Tests
    @Test
    void listDirectory_EmptyDirectory_ReturnsEmptyList() throws IOException {
        // Given
        Path emptyDir = Files.createDirectory(tempDir.resolve("emptyDir"));

        // When
        List<FileInfo> result = directoryListService.listDirectory(emptyDir.toString());

        // Then
        assertTrue(result.isEmpty());
        verify(fileGetInfoService, never()).getFileInfo(anyString());
    }

    @Test
    void listDirectory_WithFiles_ReturnsFileList() throws IOException {
        // Given
        Path dir = Files.createDirectory(tempDir.resolve("dir"));
        Path file1 = Files.createFile(dir.resolve("file1.txt"));
        Path file2 = Files.createFile(dir.resolve("file2.txt"));

        FileInfo fileInfo1 = createMockFileInfo("file1.txt");
        FileInfo fileInfo2 = createMockFileInfo("file2.txt");

        when(fileGetInfoService.getFileInfo(file1.toString())).thenReturn(fileInfo1);
        when(fileGetInfoService.getFileInfo(file2.toString())).thenReturn(fileInfo2);

        // When
        List<FileInfo> result = directoryListService.listDirectory(dir.toString());

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(fileInfo1));
        assertTrue(result.contains(fileInfo2));
    }

    // Error Cases
    @Test
    void listDirectory_NonexistentDirectory_ThrowsException() {
        // Given
        Path nonexistentDir = tempDir.resolve("nonexistent");

        // When & Then
        assertThrows(NoSuchFileException.class,
                () -> directoryListService.listDirectory(nonexistentDir.toString()));
    }

    @Test
    void listDirectory_NotADirectory_ThrowsException() throws IOException {
        // Given
        Path file = Files.createFile(tempDir.resolve("file.txt"));

        // When & Then
        assertThrows(NotDirectoryException.class,
                () -> directoryListService.listDirectory(file.toString()));
    }

    @Test
    void listDirectory_OutsideRootDirectory_ThrowsException() {
        // Given
        Path outsidePath = tempDir.getParent().resolve("outside");

        // When & Then
        assertThrows(SecurityException.class,
                () -> directoryListService.listDirectory(outsidePath.toString()));
    }

    // Special Cases
    @Test
    void listDirectory_WithFailedFileInfo_SkipsFailedEntries() throws IOException {
        // Given
        Path dir = Files.createDirectory(tempDir.resolve("dir"));
        Path file1 = Files.createFile(dir.resolve("file1.txt"));
        Path file2 = Files.createFile(dir.resolve("file2.txt"));

        FileInfo fileInfo1 = createMockFileInfo("file1.txt");
        when(fileGetInfoService.getFileInfo(file1.toString())).thenReturn(fileInfo1);
        when(fileGetInfoService.getFileInfo(file2.toString())).thenThrow(new IOException());

        // When
        List<FileInfo> result = directoryListService.listDirectory(dir.toString());

        // Then
        assertEquals(1, result.size());
        assertTrue(result.contains(fileInfo1));
    }

    @Test
    void listDirectory_WithSubdirectories_ListsAllEntries() throws IOException {
        // Given
        Path dir = Files.createDirectory(tempDir.resolve("dir"));
        Path subdir = Files.createDirectory(dir.resolve("subdir"));
        Path file = Files.createFile(dir.resolve("file.txt"));

        FileInfo dirInfo = createMockFileInfo("subdir");
        FileInfo fileInfo = createMockFileInfo("file.txt");

        when(fileGetInfoService.getFileInfo(subdir.toString())).thenReturn(dirInfo);
        when(fileGetInfoService.getFileInfo(file.toString())).thenReturn(fileInfo);

        // When
        List<FileInfo> result = directoryListService.listDirectory(dir.toString());

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(dirInfo));
        assertTrue(result.contains(fileInfo));
    }

    // isReadableDirectory Tests
    @Test
    void isReadableDirectory_ValidDirectory_ReturnsTrue() throws IOException {
        // Given
        Path dir = Files.createDirectory(tempDir.resolve("readable"));

        // When & Then
        assertTrue(directoryListService.isReadableDirectory(dir.toString()));
    }

    @Test
    void isReadableDirectory_NotADirectory_ReturnsFalse() throws IOException {
        // Given
        Path file = Files.createFile(tempDir.resolve("file.txt"));

        // When & Then
        assertFalse(directoryListService.isReadableDirectory(file.toString()));
    }

    @Test
    void isReadableDirectory_NonexistentPath_ReturnsFalse() {
        // Given
        Path nonexistent = tempDir.resolve("nonexistent");

        // When & Then
        assertFalse(directoryListService.isReadableDirectory(nonexistent.toString()));
    }

    // Helper Methods
    private FileInfo createMockFileInfo(String name) {
        return new FileInfo(
                name,
                tempDir.resolve(name).toString(),
                0L
        );
    }
}