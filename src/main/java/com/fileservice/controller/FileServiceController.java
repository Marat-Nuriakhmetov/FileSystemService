package com.fileservice.controller;

import com.fileservice.dto.EntryType;
import com.fileservice.dto.FileInfo;
import com.fileservice.service.DirectoryListService;
import com.fileservice.service.FileCopyService;
import com.fileservice.service.FileCreateService;
import com.fileservice.service.FileDeleteService;
import com.fileservice.service.FileGetInfoService;
import com.fileservice.service.FileMoveService;
import com.fileservice.service.FileReadService;
import com.fileservice.service.FileWriteService;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * JSON-RPC controller for file system operations.
 * Provides a REST-like interface for various file operations through JSON-RPC 2.0 protocol.
 *
 * <p>Supported operations:
 * <ul>
 *     <li>File/Directory information retrieval</li>
 *     <li>Directory listing</li>
 *     <li>File/Directory creation</li>
 *     <li>File/Directory deletion</li>
 *     <li>File moving and copying</li>
 *     <li>File reading and writing</li>
 * </ul>
 *
 * <p>All methods are exposed as JSON-RPC endpoints and can be called using
 * standard JSON-RPC 2.0 protocol requests.
 *
 * @see JsonRpcService
 */
@Singleton
@JsonRpcService
public class FileServiceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileServiceController.class);

    private final FileGetInfoService fileGetInfoService;
    private final DirectoryListService directoryListService;
    private final FileCreateService fileCreateService;
    private final FileDeleteService fileDeleteService;
    private final FileMoveService fileMoveService;
    private final FileCopyService fileCopyService;
    private final FileWriteService fileWriteService;
    private final FileReadService fileReadService;

    @Inject
    public FileServiceController(FileGetInfoService fileGetInfoService, DirectoryListService directoryListService, FileCreateService fileCreateService, FileDeleteService fileDeleteService, FileMoveService fileMoveService, FileCopyService fileCopyService, FileWriteService fileWriteService, FileReadService fileReadService) {
        this.fileGetInfoService = fileGetInfoService;
        this.directoryListService = directoryListService;
        this.fileCreateService = fileCreateService;
        this.fileDeleteService = fileDeleteService;
        this.fileMoveService = fileMoveService;
        this.fileCopyService = fileCopyService;
        this.fileWriteService = fileWriteService;
        this.fileReadService = fileReadService;
    }

    /**
     * Retrieves detailed information about a file or directory.
     *
     * @param path the path to the file or directory
     * @return FileInfo object containing detailed information
     * @throws IOException if an I/O error occurs
     */
    @JsonRpcMethod
    public FileInfo getFileInfo(@JsonRpcParam("path") String path) throws IOException {
        LOGGER.trace("Getting file info for: {}", path);
        return fileGetInfoService.getFileInfo(path);
    }

    /**
     * Lists the contents of a directory.
     *
     * @param path the directory path to list
     * @return list of FileInfo objects for directory contents
     * @throws IOException if an I/O error occurs
     */
    @JsonRpcMethod
    public List<FileInfo> listDirectory(@JsonRpcParam("path") String path) throws IOException {
        LOGGER.trace("Listing directory: {}", path);
        return directoryListService.listDirectory(path);
    }

    /**
     * Creates a new file or directory.
     *
     * @param path      the path where to create the entry
     * @param entryType the type of entry to create (FILE or DIRECTORY)
     * @return true if creation was successful
     * @throws IOException if an I/O error occurs
     */
    @JsonRpcMethod
    public boolean create(
            @JsonRpcParam("path") String path,
            @JsonRpcParam("type") EntryType entryType
    ) throws IOException {
        LOGGER.trace("Creating {}: {}", entryType, path);
        return switch (entryType) {
            case FILE -> fileCreateService.createFile(path);
            case DIRECTORY -> fileCreateService.createDirectory(path);
        };
    }

    /**
     * Deletes a file or directory.
     *
     * @param path      the path to delete
     * @param recursive if true, recursively deletes directories and their contents
     * @return true if deletion was successful
     * @throws IOException if an I/O error occurs
     */
    @JsonRpcMethod
    public boolean delete(
            @JsonRpcParam("path") String path,
            @JsonRpcParam("recursive") boolean recursive
    ) throws IOException {
        LOGGER.trace("Deleting path: {} (recursive: {})",
                new Object[]{path, recursive});
        return fileDeleteService.delete(path, recursive);
    }

    /**
     * Moves a file or directory to a new location.
     *
     * @param sourcePath the source path
     * @param targetPath the target path
     * @return true if move was successful
     * @throws IOException if an I/O error occurs
     */
    @JsonRpcMethod
    public boolean move(
            @JsonRpcParam("sourcePath") String sourcePath,
            @JsonRpcParam("targetPath") String targetPath
    ) throws IOException {
        LOGGER.trace("Moving: {} -> {}", sourcePath, targetPath);
        return fileMoveService.move(sourcePath, targetPath);
    }

    /**
     * Copies a file or directory to a new location.
     *
     * @param sourcePath the source path
     * @param targetPath the target path
     * @return true if copy was successful
     * @throws IOException if an I/O error occurs
     */
    @JsonRpcMethod
    public boolean copy(
            @JsonRpcParam("sourcePath") String sourcePath,
            @JsonRpcParam("targetPath") String targetPath
    ) throws IOException {
        LOGGER.trace("Copying: {} -> {}", sourcePath, targetPath);
        return fileCopyService.copy(sourcePath, targetPath);
    }

    /**
     * Appends data to a file.
     *
     * @param path the path to the file
     * @param data the data to append
     * @return true if append was successful
     * @throws IOException if an I/O error occurs
     */
    @JsonRpcMethod
    public boolean append(
            @JsonRpcParam("path") String path,
            @JsonRpcParam("data") String data
    ) throws IOException {
        LOGGER.trace("Appending to file: {}", path);
        return fileWriteService.append(path, data);
    }

    /**
     * Reads data from a file at specified offset and length.
     *
     * @param path   the path to the file
     * @param offset the starting position
     * @param length the number of bytes to read
     * @return the read data as a string
     * @throws IOException if an I/O error occurs
     */
    @JsonRpcMethod
    public String read(
            @JsonRpcParam("path") String path,
            @JsonRpcParam("offset") long offset,
            @JsonRpcParam("length") int length
    ) throws IOException {
        LOGGER.trace("Reading file: {} (offset: {}, length: {})", path, offset, length);
        return fileReadService.read(path, offset, length);
    }
}