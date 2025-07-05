package com.fos.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;

import static com.fos.config.Constants.BEAN_NAME_ROOT_DIRECTORY;

/**
 * Service for copying files within a designated root directory.
 * Provides secure file copying operations with validation and error handling.
 * This service ensures that all file operations are contained within the root directory
 * and handles various edge cases and error conditions.
 *
 * <p>Features:
 * <ul>
 *     <li>Secure file copying within root directory</li>
 *     <li>Path validation and normalization</li>
 *     <li>Comprehensive error handling</li>
 *     <li>Atomic copy operations where possible</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * FileCopyService service = new FileCopyService(rootPath);
 * try {
 *     service.copy("/root/source.txt", "/root/target.txt");
 * } catch (IOException e) {
 *     // Handle copy failure
 * }
 * </pre>
 *
 * <p>Thread safety:
 * This service is thread-safe and can be used concurrently.
 * File system operations are atomic where supported by the underlying system.
 *
 * @since 1.0
 */
@Singleton
public class FileCopyService  extends BaseFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileCopyService.class);

    /**
     * Constructs a new FileCopyService with the specified root directory.
     *
     * @param rootDirectory the root directory path for all file operations
     * @throws IllegalArgumentException if rootDirectory is null or empty
     */
    @Inject
    public FileCopyService(@Named(BEAN_NAME_ROOT_DIRECTORY) Path rootDirectory) {
        super(rootDirectory);
    }

    /**
     * Copies a file from the source path to the target path.
     * Both paths must be within the root directory.
     *
     * <p>The operation will fail if:
     * <ul>
     *     <li>Source file doesn't exist</li>
     *     <li>Source is a directory</li>
     *     <li>Target file already exists</li>
     *     <li>Either path is outside the root directory</li>
     *     <li>Insufficient permissions</li>
     * </ul>
     *
     * @param sourcePath the path of the file to copy
     * @param targetPath the path where the file should be copied to
     * @return true if the copy operation was successful
     * @throws IllegalArgumentException if either path is null or empty
     * @throws NoSuchFileException if the source file doesn't exist
     * @throws FileAlreadyExistsException if the target file already exists
     * @throws SecurityException if either path is outside the root directory
     * @throws IOException if an I/O error occurs during copying
     */
    public boolean copy(String sourcePath, String targetPath) throws IOException {

        Path source = validateAndNormalizeFullPath(sourcePath);
        Path target = validateAndNormalizeFullPath(targetPath);
        if (source.equals(target)) {
            throw new IllegalArgumentException("Source and target paths cannot be the same");
        }

        try {
            if (!Files.exists(source)) {
                LOGGER.warn("Source file does not exist: {}", sourcePath);
                throw new NoSuchFileException("Source file does not exist: " + sourcePath);
            }

            if (Files.isDirectory(source)) {
                LOGGER.warn(  "Source is a directory: {}", sourcePath);
                throw new IOException("Source is a directory: " + sourcePath);
            }

            if (Files.exists(target)) {
                LOGGER.warn("Target file already exists: {}", targetPath);
                throw new FileAlreadyExistsException("Target file already exists: " + targetPath);
            }

            // Ensure parent directory exists
            Path targetParent = target.getParent();
            if (targetParent != null && !Files.exists(targetParent)) {
                LOGGER.warn( "Target parent directory does not exist: {}", targetParent);
                throw new NoSuchFileException("Target parent directory does not exist: " + targetParent);
            }

            Files.copy(source, target);
            LOGGER.info( "Successfully copied file: {} -> {}",
                    sourcePath, targetPath);

            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to copy file: {} -> {}", sourcePath, targetPath, e);
            throw e;
        }
    }

}