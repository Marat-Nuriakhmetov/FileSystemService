package com.fos.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstract base class for file system operations providing common functionality
 * and security measures for file handling services.
 *
 * <p>This class implements core security and validation features including:
 * <ul>
 *     <li>Root directory containment</li>
 *     <li>Path validation and normalization</li>
 *     <li>Parent directory verification</li>
 *     <li>Common file system checks</li>
 * </ul>
 *
 * <p>Security features:
 * <ul>
 *     <li>Prevents directory traversal attacks</li>
 *     <li>Ensures operations remain within root directory</li>
 *     <li>Validates all file paths before operations</li>
 *     <li>Normalizes paths to prevent manipulation</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * public class CustomFileService extends BaseFileService {
 *     public CustomFileService(Path rootDirectory) {
 *         super(rootDirectory);
 *     }
 *
 *     public void customOperation(String path) {
 *         Path validatedPath = validateAndNormalizePath(path);
 *         // Perform operation
 *     }
 * }
 * </pre>
 *
 * <p>Thread safety:
 * This class is thread-safe. The root directory is immutable after construction,
 * and all validation methods are stateless.
 */
public abstract class BaseFileService {

    protected final Path rootDirectory;

    protected BaseFileService(Path rootDirectory) {
        if (rootDirectory == null) {
            throw new IllegalArgumentException("Root path cannot be null");
        }
        this.rootDirectory = rootDirectory;
    }

    /**
     * Validates and normalizes the path, ensuring it's within the root directory.
     *
     * @param path the path to validate and normalize
     * @return the normalized path
     * @throws SecurityException if the path is outside the root directory
     */
    protected Path validateAndNormalizeFullPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        Path fullPath = rootDirectory.resolve(Paths.get(path)).normalize();
        if (!fullPath.startsWith(rootDirectory)) {
            throw new SecurityException("Path must be within root directory");
        }
        return fullPath;
    }

    /**
     * Validates both source and target paths.
     *
     * @param sourcePath the source path to validate
     * @param targetPath the target path to validate
     * @throws IllegalArgumentException if either path is null or empty
     * @throws SecurityException if either path is outside the root directory
     */
    protected void validatePaths(String sourcePath, String targetPath) {
        // Validate source path
        Path normalizedSource = validateAndNormalizeFullPath(sourcePath);

        // Validate target path
        Path normalizedTarget = validateAndNormalizeFullPath(targetPath);

        // Prevent identical source and target
        if (normalizedSource.equals(normalizedTarget)) {
            throw new IllegalArgumentException("Source and target paths cannot be the same");
        }
    }

    /**
     * Validates that the parent directory exists.
     *
     * @param path the path to validate
     * @throws IOException if the parent directory doesn't exist
     */
    protected void validateParentDirectory(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            throw new IOException("Parent directory does not exist: " + parent);
        }
    }

    /**
     * Validates that the path is within the root directory
     *
     * @param path the path to validate
     * @throws IOException if the parent directory doesn't exist
     */
    protected void validatePathWithinRootFolder(Path path) throws IOException {
        if (!path.startsWith(rootDirectory)) {
            throw new SecurityException("Path is not within the root directory: " + path);
        }
    }

    /**
     * Validates the provided path.
     *
     * @param path the path to validate
     * @throws IllegalArgumentException if the path is null or empty
     * @throws SecurityException if the path is outside the root directory
     */
    protected void validatePath(String path) {
        validateAndNormalizeFullPath(path);
    }

    /**
     * Checks if a path is a directory.
     *
     * @param path the path to check
     * @return true if the path is a directory, false otherwise
     * @throws IllegalArgumentException if the path is null or empty
     * @throws SecurityException if the path is outside the root directory
     */
    protected boolean isDirectory(String path) {
        validatePath(path);
        return Files.isDirectory(Paths.get(path));
    }

    /**
     * Checks if a file exists at the specified path.
     *
     * @param path the path to check
     * @return true if the file exists, false otherwise
     * @throws IllegalArgumentException if the path is null or empty
     * @throws SecurityException if the path is outside the root directory
     */
    protected boolean exists(String path) {
        return Files.exists(validateAndNormalizeFullPath(path));
    }

}
