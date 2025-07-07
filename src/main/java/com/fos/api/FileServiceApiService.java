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
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * JSON-RPC 2.0 API controller for distributed file system operations.
 * 
 * <p>This service provides secure file operations within a configurable root directory,
 * with distributed locking for concurrent append operations across multiple service instances.
 * All file paths are relative to the configured root directory for security isolation.
 *
 * <p><strong>Supported Operations:</strong>
 * <ul>
 *     <li><strong>Information:</strong> Retrieve file/directory metadata (name, path, size)</li>
 *     <li><strong>Listing:</strong> List directory contents with detailed information</li>
 *     <li><strong>Creation:</strong> Create empty files and directories</li>
 *     <li><strong>Deletion:</strong> Delete files and directories (with recursive option)</li>
 *     <li><strong>Movement:</strong> Move and copy files/directories</li>
 *     <li><strong>Data Operations:</strong> Atomic append and partial read operations</li>
 * </ul>
 *
 * <p><strong>Concurrency:</strong> Only append operations are synchronized using Redis-based
 * distributed locking to ensure atomic writes across service instances. Other operations
 * are not synchronized for optimal performance.
 *
 * <p><strong>Example JSON-RPC Requests:</strong>
 * <pre>{@code
 * // Create a file
 * {
 *   "jsonrpc": "2.0",
 *   "method": "create",
 *   "params": ["documents/test.txt", "FILE"],
 *   "id": 1
 * }
 * 
 * // List directory contents
 * {
 *   "jsonrpc": "2.0",
 *   "method": "listDirectory",
 *   "params": ["documents"],
 *   "id": 2
 * }
 * 
 * // Append data with distributed locking
 * {
 *   "jsonrpc": "2.0",
 *   "method": "append",
 *   "params": ["logs/app.log", "[INFO] Application started\n"],
 *   "id": 3
 * }
 * }</pre>
 *
 * <p><strong>Error Handling:</strong>
 * All methods return standard JSON-RPC 2.0 error responses for failures:
 * <ul>
 *     <li><strong>-32602:</strong> Invalid params (path outside root, invalid parameters)</li>
 *     <li><strong>-32603:</strong> Internal error (I/O failures, lock acquisition failures)</li>
 * </ul>
 *
 * <p><strong>Best Practices:</strong>
 * <ul>
 *     <li>Use forward slashes (/) as path separators on all platforms</li>
 *     <li>Avoid absolute paths - all paths are relative to root directory</li>
 *     <li>Check file existence with {@code getFileInfo} before operations</li>
 *     <li>Use {@code listDirectory} to verify directory structure</li>
 *     <li>For large files, use {@code read} with appropriate offset/length chunks</li>
 *     <li>Append operations are automatically thread-safe across instances</li>
 * </ul>
 *
 * <p><strong>Performance Considerations:</strong>
 * <ul>
 *     <li>Append operations have higher latency due to distributed locking</li>
 * </ul>

 * @see JsonRpcService
 * @see FileInfo
 * @see EntryType
 */
@Singleton
@JsonRpcService
public class FileServiceApiService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileServiceApiService.class);

    private final FileGetInfoService fileGetInfoService;
    private final DirectoryListService directoryListService;
    private final FileCreateService fileCreateService;
    private final FileDeleteService fileDeleteService;
    private final FileMoveService fileMoveService;
    private final FileCopyService fileCopyService;
    private final FileWriteService fileWriteService;
    private final FileReadService fileReadService;

    /**
     * Constructs the FileServiceApiService with all required dependencies.
     * 
     * <p>This constructor is called by the Guice dependency injection framework
     * to wire all service dependencies. All services are injected as singletons
     * to ensure consistent behavior across the application.
     *
     * @param fileGetInfoService service for retrieving file/directory information
     * @param directoryListService service for listing directory contents
     * @param fileCreateService service for creating files and directories
     * @param fileDeleteService service for deleting files and directories
     * @param fileMoveService service for moving files and directories
     * @param fileCopyService service for copying files and directories
     * @param fileWriteService service for writing/appending to files (with distributed locking)
     * @param fileReadService service for reading files with offset/length support
     */
    @Inject
    public FileServiceApiService(FileGetInfoService fileGetInfoService, DirectoryListService directoryListService, FileCreateService fileCreateService, FileDeleteService fileDeleteService, FileMoveService fileMoveService, FileCopyService fileCopyService, FileWriteService fileWriteService, FileReadService fileReadService) {
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
     * <p>Returns comprehensive metadata including name, path, size, and type.
     * Path validation ensures the target is within the configured root directory.
     * 
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     * // JSON-RPC Request
     * {
     *   "jsonrpc": "2.0",
     *   "method": "getFileInfo",
     *   "params": ["documents/report.pdf"],
     *   "id": 1
     * }
     * 
     * // JSON-RPC Response
     * {
     *   "jsonrpc": "2.0",
     *   "result": {
     *     "name": "report.pdf",
     *     "path": "documents/report.pdf",
     *     "size": 1048576,
     *     "type": "FILE"
     *   },
     *   "id": 1
     * }
     * }</pre>
     *
     * @param path the relative path to the file or directory (e.g., "documents/file.txt")
     * @return FileInfo object containing name, path, size, and type information
     * @throws IOException if the file doesn't exist, path is invalid, or I/O error occurs
     * @throws IllegalArgumentException if path is outside root directory
     */
    @JsonRpcMethod
    public FileInfo getFileInfo(@JsonRpcParam("path") String path) throws IOException {
        LOGGER.trace("Getting file info for: {}", path);
        return fileGetInfoService.getFileInfo(path);
    }

    /**
     * Lists the contents of a directory.
     * 
     * <p>Returns detailed information for all files and subdirectories within
     * the specified directory. Results are sorted alphabetically by name.
     *
     * @param path the relative directory path to list (e.g., "documents" or "." for root)
     * @return list of FileInfo objects for all directory contents, empty list if directory is empty
     * @throws IOException if directory doesn't exist, is not a directory, or I/O error occurs
     * @throws IllegalArgumentException if path is outside root directory
     */
    @JsonRpcMethod
    public List<FileInfo> listDirectory(@JsonRpcParam("path") String path) throws IOException {
        LOGGER.trace("Listing directory: {}", path);
        return directoryListService.listDirectory(path);
    }

    /**
     * Creates a new empty file or directory.
     * 
     * <p>Creates the specified file or directory at the given path. Parent directories
     * must exist before creating files or subdirectories. Files are created empty.
     *
     * @param path the relative path where to create the entry (e.g., "documents/new-file.txt")
     * @param entryType the type of entry to create: FILE or DIRECTORY
     * @return true if creation was successful, false if entry already exists
     * @throws IOException if parent directory doesn't exist or I/O error occurs
     * @throws IllegalArgumentException if path is outside root directory or invalid
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
     * <p>For files: Deletes the file immediately.
     * For directories: If recursive=false, only deletes empty directories.
     * If recursive=true, deletes directory and all contents recursively.
     *
     * @param path the relative path to delete (e.g., "documents/old-file.txt")
     * @param recursive if true, recursively deletes directories and their contents;
     *                  if false, only deletes empty directories
     * @return true if deletion was successful, false if file/directory doesn't exist
     * @throws IOException if directory is not empty (when recursive=false) or I/O error occurs
     * @throws IllegalArgumentException if path is outside root directory
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
     * <p>Atomically moves the source to the target location. If target exists,
     * it will be overwritten. Parent directory of target must exist.
     *
     * @param sourcePath the relative source path (e.g., "documents/old-name.txt")
     * @param targetPath the relative target path (e.g., "archive/new-name.txt")
     * @return true if move was successful
     * @throws IOException if source doesn't exist, target parent doesn't exist, or I/O error occurs
     * @throws IllegalArgumentException if either path is outside root directory
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
     * <p>Creates a copy of the source at the target location. For directories,
     * recursively copies all contents. If target exists, it will be overwritten.
     *
     * @param sourcePath the relative source path (e.g., "documents/original.txt")
     * @param targetPath the relative target path (e.g., "backup/copy.txt")
     * @return true if copy was successful
     * @throws IOException if source doesn't exist, target parent doesn't exist, or I/O error occurs
     * @throws IllegalArgumentException if either path is outside root directory
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
     * Appends data to a file with distributed locking.
     * 
     * <p><strong>Thread-Safe:</strong> This operation uses Redis-based distributed locking
     * to ensure atomic appends across multiple service instances. Concurrent append
     * operations are serialized to prevent data corruption.
     * 
     * <p>Data is appended to the end of the file. If file doesn't exist, it will be created.
     * Lock timeout is configured to prevent deadlocks (default: 30 seconds).
     * 
     * <p><strong>Concurrency Guarantee:</strong> If two clients simultaneously append "ABC" and "123"
     * to the same file, the result will be either "ABC123" or "123ABC", never interleaved.
     * 
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     * // JSON-RPC Request
     * {
     *   "jsonrpc": "2.0",
     *   "method": "append",
     *   "params": ["logs/app.log", "[2024-01-15 10:30:00] INFO: User logged in\n"],
     *   "id": 1
     * }
     * }</pre>
     *
     * @param path the relative path to the file (e.g., "logs/application.log")
     * @param data the string data to append to the file (UTF-8 encoded)
     * @return true if append was successful
     * @throws IOException if file cannot be created/written or I/O error occurs
     * @throws IllegalArgumentException if path is outside root directory
     * @throws RuntimeException if distributed lock cannot be acquired within timeout
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
     * <p>Performs partial file reading starting from the specified byte offset.
     * Useful for reading large files in chunks or accessing specific file sections.
     * This operation is optimized for concurrent access and does not require locking.
     * 
     * <p>If offset + length exceeds file size, reads until end of file.
     * Returns empty string if offset is beyond file end.
     * 
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     * // Read first 1024 bytes of a file
     * {
     *   "jsonrpc": "2.0",
     *   "method": "read",
     *   "params": ["documents/large-file.txt", 0, 1024],
     *   "id": 1
     * }
     * 
     * // Read next chunk starting from byte 1024
     * {
     *   "jsonrpc": "2.0",
     *   "method": "read",
     *   "params": ["documents/large-file.txt", 1024, 1024],
     *   "id": 2
     * }
     * }</pre>
     * 
     * <p><strong>Performance Note:</strong> For optimal performance when reading large files,
     * use chunk sizes between 1KB and 1MB depending on network conditions.
     *
     * @param path the relative path to the file (e.g., "documents/data.txt")
     * @param offset the starting byte position (0-based, must be >= 0)
     * @param length the maximum number of bytes to read (must be > 0, recommended: 1KB-1MB)
     * @return the read data as a UTF-8 string, empty string if nothing to read
     * @throws IOException if file doesn't exist, is a directory, or I/O error occurs
     * @throws IllegalArgumentException if path is outside root directory, offset < 0, or length <= 0
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