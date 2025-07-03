package com.fileservice.service;

import com.fileservice.service.lock.DistributedLockService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static com.fileservice.config.Constants.BEAN_NAME_ROOT_DIRECTORY;

/**
 * Service for concurrent file writing operations.
 * Uses distributed locks for synchronization.
 */
@Singleton
public class FileWriteService extends BaseFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileWriteService.class);

    private final DistributedLockService lockService;

    @Inject
    public FileWriteService(@Named(BEAN_NAME_ROOT_DIRECTORY) Path rootDirectory, DistributedLockService lockService) {
        super(rootDirectory);
        this.lockService = lockService;
    }

    /**
     * Appends data to a file with concurrent access protection.
     *
     * @param path the path to the file
     * @param data the data to append
     * @return true if the operation was successful
     * @throws IllegalArgumentException if path is null/empty or data is null
     * @throws SecurityException        if the path is outside the root directory
     * @throws IOException              if an I/O error occurs
     */
    public boolean append(String path, String data) throws IOException {
        validateParameters(path, data);
        Path normalizedFullPath = validateAndNormalizeFullPath(path);
        try (DistributedLockService.DistributedLock lock = lockService.acquireLock("file:" + path)) {
            // Perform file operation
            Files.writeString(normalizedFullPath, data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (DistributedLockService.LockAcquisitionException e) {
            throw new RuntimeException("Failed to acquire lock", e);
        }
    }

    private void validateParameters(String path, String data) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
    }
}