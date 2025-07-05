package com.fos.service;

import com.fos.dto.FileInfo;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.fos.config.Constants.BEAN_NAME_ROOT_DIRECTORY;

/**
 * Service for listing directory contents with detailed file information.
 * Provides safe directory traversal within a configured root directory.
 */
@Singleton
public class DirectoryListService extends BaseFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryListService.class);
    private final FileGetInfoService fileGetInfoService;

    @Inject
    public DirectoryListService(
            FileGetInfoService fileGetInfoService,
            @Named(BEAN_NAME_ROOT_DIRECTORY) Path rootDirectory) {
        super(rootDirectory);
        this.fileGetInfoService = fileGetInfoService;
    }

    /**
     * Lists the contents of a directory with detailed file information.
     *
     * @param path the directory path to list
     * @return list of FileInfo objects for directory contents
     * @throws IllegalArgumentException if the path is null or empty
     * @throws SecurityException if the path is outside the root directory
     * @throws NotDirectoryException if the path is not a directory
     * @throws IOException if an I/O error occurs
     */
    public List<FileInfo> listDirectory(String path) throws IOException {
        validatePath(path);

        Path dirPath = validateAndNormalizeFullPath(path);

        if (!Files.exists(dirPath)) {
            LOGGER.warn("Directory does not exist: {}", path);
            throw new NoSuchFileException("Directory does not exist: " + path);
        }

        if (!Files.isDirectory(dirPath)) {
            LOGGER.warn("Path is not a directory: {}", path);
            throw new NotDirectoryException("Path is not a directory: " + path);
        }

        if (!Files.isReadable(dirPath)) {
            LOGGER.warn("Directory is not readable: {}", path);
            throw new AccessDeniedException("Directory is not readable: " + path);
        }

        try (Stream<Path> listing = Files.list(dirPath)) {
            List<FileInfo> results = listing
                    .map(p -> {
                        try {
                            return fileGetInfoService.getFileInfo(p.toString());
                        } catch (IOException e) {
                            LOGGER.warn("Failed to get info for: {}", p, e);
                            return null;
                        }
                    })
                    .filter(info -> info != null)
                    .collect(Collectors.toList());

            LOGGER.trace("Listed {} entries in directory: {}", results.size(), path);

            return Collections.unmodifiableList(results);

        } catch (IOException e) {
            LOGGER.error("Error listing directory: {}", path, e);
            throw new IOException(
                    "Error listing directory: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a path exists and is a readable directory.
     *
     * @param path the path to check
     * @return true if the path is a readable directory
     * @throws IllegalArgumentException if the path is null or empty
     * @throws SecurityException if the path is outside the root directory
     */
    public boolean isReadableDirectory(String path) {
        validatePath(path);
        Path normalizedPath = validateAndNormalizeFullPath(path);

        return Files.exists(normalizedPath) &&
                Files.isDirectory(normalizedPath) &&
                Files.isReadable(normalizedPath);
    }
}