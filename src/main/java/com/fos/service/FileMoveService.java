package com.fos.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;

import static com.fos.config.Constants.BEAN_NAME_ROOT_DIR;

/**
 * Service for moving files and directories within the file system.
 * Ensures safe movement of files with proper validation and error handling.
 */
@Singleton
public class FileMoveService  extends BaseFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileMoveService.class);

    @Inject
    public FileMoveService(@Named(BEAN_NAME_ROOT_DIR) Path rootDirectory) {
        super(rootDirectory);
    }

    /**
     * Moves a file or directory from source path to target path.
     *
     * @param sourcePath the source path of the file or directory to move
     * @param targetPath the target path where the file or directory should be moved
     * @return true if the move operation was successful
     * @throws IllegalArgumentException if either path is null or empty
     * @throws SecurityException if either path is outside the root directory
     * @throws NoSuchFileException if the source file doesn't exist
     * @throws FileAlreadyExistsException if the target file already exists
     * @throws IOException if an I/O error occurs
     */
    public boolean move(String sourcePath, String targetPath) throws IOException {
        Path source = validateAndNormalizeFullPath(sourcePath);
        Path target = validateAndNormalizeFullPath(targetPath);
        if (source.equals(target)) {
            throw new IllegalArgumentException("Source and target paths cannot be the same");
        }

        // Additional validations
        if (!Files.exists(source)) {
            LOGGER.warn("Source does not exist: {}", sourcePath);
            throw new NoSuchFileException("Source does not exist: " + sourcePath);
        }

        if (Files.exists(target)) {
            LOGGER.warn("Target already exists: {}", targetPath);
            throw new FileAlreadyExistsException("Target already exists: " + targetPath);
        }

        // Ensure parent directory of target exists
        Path targetParent = target.getParent();
        if (targetParent != null && !Files.exists(targetParent)) {
            LOGGER.warn("Target parent directory does not exist: {}", targetParent);
            throw new NoSuchFileException("Target parent directory does not exist: " + targetParent);
        }

        // Prevent moving a directory into itself or its subdirectories
        if (Files.isDirectory(source) && target.startsWith(source)) {
            LOGGER.warn("Cannot move directory into itself: {} -> {}", sourcePath, targetPath);
            throw new IOException("Cannot move directory into itself");
        }

        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            LOGGER.info("Successfully moved: {} -> {}",
                    sourcePath, targetPath);
            return true;
        } catch (AtomicMoveNotSupportedException e) {
            // Fallback to non-atomic move
            LOGGER.warn("Atomic move not supported, falling back to regular move");
            Files.move(source, target);
            LOGGER.warn("Successfully moved (non-atomic): {} -> {}",
                    sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to move: {} -> {}", sourcePath, targetPath, e);
            throw new IOException("Failed to move file: " + e.getMessage(), e);
        }
    }

}