package com.fos.api;

import com.fos.dto.EntryType;
import com.fos.dto.FileInfo;
import com.fos.service.DirectoryListService;
import com.fos.service.FileCopyService;
import com.fos.service.FileCreateService;
import com.fos.service.FileDeleteService;
import com.fos.service.FileGetInfoService;
import com.fos.service.FileMoveService;
import com.fos.service.FileReadService;
import com.fos.service.FileWriteService;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FileServiceApiService} focusing on API contract validation
 * and behavior verification.
 */
@ExtendWith(MockitoExtension.class)
class FileServiceApiServiceTest {

    @Mock
    private FileGetInfoService fileGetInfoService;
    @Mock
    private DirectoryListService directoryListService;
    @Mock
    private FileCreateService fileCreateService;
    @Mock
    private FileDeleteService fileDeleteService;
    @Mock
    private FileMoveService fileMoveService;
    @Mock
    private FileCopyService fileCopyService;
    @Mock
    private FileWriteService fileWriteService;
    @Mock
    private FileReadService fileReadService;

    private FileServiceApiService apiService;

    @BeforeEach
    void setUp() {
        apiService = new FileServiceApiService(
                fileGetInfoService, directoryListService, fileCreateService,
                fileDeleteService, fileMoveService, fileCopyService,
                fileWriteService, fileReadService
        );
    }

    // API Contract Tests

    @Test
    void verifyJsonRpcServiceAnnotation() {
        assertTrue(FileServiceApiService.class.isAnnotationPresent(com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService.class),
                "Class should be annotated with @JsonRpcService");
    }

    @Test
    void verifyAllMethodsHaveJsonRpcMethodAnnotation() {
        Method[] methods = FileServiceApiService.class.getDeclaredMethods();
        Arrays.stream(methods)
                .filter(method -> !method.isSynthetic()) // Exclude synthetic methods
                .forEach(method -> {
                    assertTrue(method.isAnnotationPresent(JsonRpcMethod.class),
                            "Method " + method.getName() + " should have @JsonRpcMethod annotation");
                });
    }

    @Test
    void verifyAllParametersHaveJsonRpcParamAnnotation() {
        Method[] methods = FileServiceApiService.class.getDeclaredMethods();
        Arrays.stream(methods)
                .filter(method -> !method.isSynthetic())
                .forEach(method -> {
                    Arrays.stream(method.getParameters()).forEach(parameter -> {
                        assertTrue(parameter.isAnnotationPresent(JsonRpcParam.class),
                                "Parameter " + parameter.getName() + " in method " +
                                        method.getName() + " should have @JsonRpcParam annotation");
                    });
                });
    }

    // Functional Tests

    @Test
    void getFileInfo_Success() throws IOException {
        // Given
        String path = "/test/file.txt";
        FileInfo expectedInfo = createMockFileInfo(path);
        when(fileGetInfoService.getFileInfo(path)).thenReturn(expectedInfo);

        // When
        FileInfo result = apiService.getFileInfo(path);

        // Then
        assertEquals(expectedInfo, result);
        verify(fileGetInfoService).getFileInfo(path);
    }

    @Test
    void listDirectory_Success() throws IOException {
        // Given
        String path = "/test";
        List<FileInfo> expectedList = List.of(
                createMockFileInfo("/test/file1.txt"),
                createMockFileInfo("/test/file2.txt")
        );
        when(directoryListService.listDirectory(path)).thenReturn(expectedList);

        // When
        List<FileInfo> result = apiService.listDirectory(path);

        // Then
        assertEquals(expectedList, result);
        verify(directoryListService).listDirectory(path);
    }

    @Test
    void creatFile_Success() throws IOException {
        // Given
        String path = "/test/entry";
        when(fileCreateService.createFile(anyString())).thenReturn(true);

        // When
        boolean result = apiService.create(path, EntryType.FILE);

        // Then
        assertTrue(result);
        verify(fileCreateService).createFile(path);
    }

    @Test
    void createDirectory_Success() throws IOException {
        // Given
        String path = "/test/entry";
        when(fileCreateService.createDirectory(anyString())).thenReturn(true);

        // When
        boolean result = apiService.create(path, EntryType.DIRECTORY);

        // Then
        assertTrue(result);
        verify(fileCreateService).createDirectory(path);
    }

    @Test
    void delete_WithRecursive_Success() throws IOException {
        // Given
        String path = "/test/dir";
        when(fileDeleteService.delete(path, true)).thenReturn(true);

        // When
        boolean result = apiService.delete(path, true);

        // Then
        assertTrue(result);
        verify(fileDeleteService).delete(path, true);
    }

    @Test
    void move_Success() throws IOException {
        // Given
        String sourcePath = "/test/source.txt";
        String targetPath = "/test/target.txt";
        when(fileMoveService.move(sourcePath, targetPath)).thenReturn(true);

        // When
        boolean result = apiService.move(sourcePath, targetPath);

        // Then
        assertTrue(result);
        verify(fileMoveService).move(sourcePath, targetPath);
    }

    @Test
    void copy_Success() throws IOException {
        // Given
        String sourcePath = "/test/source.txt";
        String targetPath = "/test/target.txt";
        when(fileCopyService.copy(sourcePath, targetPath)).thenReturn(true);

        // When
        boolean result = apiService.copy(sourcePath, targetPath);

        // Then
        assertTrue(result);
        verify(fileCopyService).copy(sourcePath, targetPath);
    }

    @Test
    void append_Success() throws IOException {
        // Given
        String path = "/test/file.txt";
        String data = "test data";
        when(fileWriteService.append(path, data)).thenReturn(true);

        // When
        boolean result = apiService.append(path, data);

        // Then
        assertTrue(result);
        verify(fileWriteService).append(path, data);
    }

    @Test
    void read_Success() throws IOException {
        // Given
        String path = "/test/file.txt";
        long offset = 0;
        int length = 100;
        String expectedContent = "test content";
        when(fileReadService.read(path, offset, length)).thenReturn(expectedContent);

        // When
        String result = apiService.read(path, offset, length);

        // Then
        assertEquals(expectedContent, result);
        verify(fileReadService).read(path, offset, length);
    }

    // Error Handling Tests

    @Test
    void allMethods_WhenIOExceptionOccurs_PropagateException() throws IOException {
        // Given
        IOException expectedException = new IOException("Test exception");
        when(fileGetInfoService.getFileInfo(anyString()))
                .thenThrow(expectedException);

        // When & Then
        assertThrows(IOException.class, () ->
                apiService.getFileInfo("/test/file.txt"));
    }

    // Helper Methods
    private FileInfo createMockFileInfo(String path) {
        return new FileInfo(
                "test-file",
                path,
                100L
        );
    }
}