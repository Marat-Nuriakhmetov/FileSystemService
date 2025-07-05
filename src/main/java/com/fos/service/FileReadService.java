package com.fos.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.fos.config.Constants.BEAN_NAME_ROOT_DIRECTORY;

/**
 * Service for reading file contents with offset and length support.
 * Provides safe file reading within a configured root directory.
 */
@Singleton
public class FileReadService  extends BaseFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileReadService.class);
    private static final int MAX_READ_LENGTH = 1024 * 1024; // 1MB maximum read size

    @Inject
    public FileReadService(@Named(BEAN_NAME_ROOT_DIRECTORY) Path rootDirectory) {
        super(rootDirectory);
    }

    /**
     * Reads a portion of a file starting at the specified offset.
     *
     * @param path   the path to the file to read
     * @param offset the position to start reading from
     * @param length the number of bytes to read
     * @return the read content as a String
     * @throws IllegalArgumentException if path is null/empty, offset/length are negative, or offset is beyond file size
     * @throws SecurityException if the path is outside the root directory
     * @throws IOException if an I/O error occurs
     */
    public String read(String path, long offset, int length) throws IOException {
        validateParameters(path, offset, length);

        Path fillNormalizedPath = validateAndNormalizeFullPath(path);

        try {
            // Verify file exists and is readable
            if (!Files.exists(fillNormalizedPath)) {
                throw new IOException("File does not exist: " + path);
            }

            if (!Files.isRegularFile(fillNormalizedPath)) {
                throw new IOException("Not a regular file: " + path);
            }

            if (!Files.isReadable(fillNormalizedPath)) {
                throw new IOException("File is not readable: " + path);
            }

            return readFileContent(fillNormalizedPath, offset, length);

        } catch (IOException e) {
            LOGGER.error("Error reading file: {}", path, e);
            throw new IOException("Error reading file: " + e.getMessage(), e);
        }
    }

    private void validateParameters(String path, long offset, int length) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }

        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative");
        }

        if (length > MAX_READ_LENGTH) {
            throw new IllegalArgumentException("Requested length exceeds maximum allowed (" + MAX_READ_LENGTH + " bytes)");
        }
    }

    private String readFileContent(Path path, long offset, int length) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long fileLength = file.length();

            if (offset > fileLength) {
                throw new IllegalArgumentException("Offset beyond file size");
            }

            // Adjust length if it would read beyond EOF
            length = (int) Math.min(length, fileLength - offset);

            if (length == 0) {
                return "";
            }

            file.seek(offset);
            byte[] buffer = new byte[length];
            int bytesRead = file.read(buffer);

            if (bytesRead == -1) {
                return "";
            }

            LOGGER.trace("Successfully read {} bytes from {} at offset {}",
                    bytesRead, path, offset);

            return new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
        }
    }

    /**
     * Checks if a file exists and is readable.
     *
     * @param path the path to check
     * @return true if the file exists and is readable
     * @throws IllegalArgumentException if the path is null or empty
     * @throws SecurityException if the path is outside the root directory
     */
    public boolean isReadable(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        Path normalizedPath = Paths.get(path).normalize();
        if (!normalizedPath.startsWith(rootDirectory)) {
            throw new SecurityException("Path must be within root directory");
        }

        return Files.exists(normalizedPath) &&
                Files.isRegularFile(normalizedPath) &&
                Files.isReadable(normalizedPath);
    }
}