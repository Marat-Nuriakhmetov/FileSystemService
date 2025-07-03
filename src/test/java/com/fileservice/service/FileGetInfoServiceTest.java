package com.fileservice.service;

import com.fileservice.dto.FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for FileGetInfoService.
 */
class FileGetInfoServiceTest {

    @TempDir
    private Path tempDir;

    private FileGetInfoService fileGetInfoService;

    @BeforeEach
    void setUp() {
        fileGetInfoService = new FileGetInfoService(tempDir);
    }

    @Nested
    class BasicFileTests {
        @Test
        void getFileInfo_RegularFile_ReturnsCorrectInfo() throws IOException {
            // Given
            String content = "test content";
            Path file = createFile("test.txt", content);
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);

            // When
            FileInfo info = fileGetInfoService.getFileInfo(file.toString());

            // Then
            assertEquals("test.txt", info.getName());
            assertEquals("test.txt", info.getPath());
            assertEquals(content.length(), info.getSize());
        }

        @Test
        void getFileInfo_Directory_ReturnsCorrectInfo() throws IOException {
            // Given
            Path dir = Files.createDirectory(tempDir.resolve("testDir"));
            BasicFileAttributes attrs = Files.readAttributes(dir, BasicFileAttributes.class);

            // When
            FileInfo info = fileGetInfoService.getFileInfo(dir.toString());

            // Then
            assertEquals("testDir", info.getName());
            assertEquals("testDir", info.getPath());
        }
    }

    @Nested
    class SymbolicLinkTests {
        @Test
        void getFileInfo_SymbolicLink_ReturnsCorrectInfo() throws IOException {
            // Given
            Path target = createFile("target.txt", "content");
            Path link = tempDir.resolve("link");
            Files.createSymbolicLink(link, target);

            // When
            FileInfo info = fileGetInfoService.getFileInfo(link.toString());

            // Then
            assertEquals("link", info.getName());
            assertEquals("link", info.getPath());
            // TODO fix isSymbolicLink
            // assertTrue(info.isSymbolicLink());
        }
    }

    @Nested
    class ErrorCases {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void getFileInfo_InvalidPath_ThrowsException(String invalidPath) {
            assertThrows(IllegalArgumentException.class,
                    () -> fileGetInfoService.getFileInfo(invalidPath));
        }

        @Test
        void getFileInfo_NonexistentFile_ThrowsException() {
            // Given
            Path nonexistent = tempDir.resolve("nonexistent.txt");

            // When & Then
            assertThrows(NoSuchFileException.class,
                    () -> fileGetInfoService.getFileInfo(nonexistent.toString()));
        }

        @Test
        void getFileInfo_PathOutsideRoot_ThrowsException() {
            // Given
            Path outsidePath = tempDir.getParent().resolve("outside.txt");

            // When & Then
            assertThrows(SecurityException.class,
                    () -> fileGetInfoService.getFileInfo(outsidePath.toString()));
        }
    }

    @Nested
    class PathHandlingTests {
        @Test
        void getFileInfo_NestedPath_ReturnsCorrectRelativePath() throws IOException {
            // Given
            Path dir = Files.createDirectory(tempDir.resolve("parent"));
            Path file = createFile(dir.resolve("child.txt"), "content");

            // When
            FileInfo info = fileGetInfoService.getFileInfo(file.toString());

            // Then
            assertEquals("parent/child.txt", info.getPath().replace('\\', '/'));
        }

        @Test
        void getFileInfo_NormalizedPath_HandlesCorrectly() throws IOException {
            // Given
            Path dir = Files.createDirectory(tempDir.resolve("parent"));
            Path file = createFile(dir.resolve("child.txt"), "content");
            String unnormalizedPath = dir.toString() + "/../parent/child.txt";

            // When
            FileInfo info = fileGetInfoService.getFileInfo(unnormalizedPath);

            // Then
            assertEquals("parent/child.txt", info.getPath().replace('\\', '/'));
        }
    }

    // Helper methods
    private Path createFile(String fileName, String content) throws IOException {
        return createFile(tempDir.resolve(fileName), content);
    }

    private Path createFile(Path path, String content) throws IOException {
        Files.write(path, content.getBytes());
        return path;
    }
}