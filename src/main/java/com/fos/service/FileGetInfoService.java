package com.fos.service;

import com.fos.dto.FileInfo;
import com.fos.util.PathUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import static com.fos.config.Constants.BEAN_NAME_ROOT_DIRECTORY;

/**
 * Service for retrieving file information from the file system.
 * Provides detailed information about files and directories.
 */
@Singleton
public class FileGetInfoService extends BaseFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileGetInfoService.class);

    @Inject
    public FileGetInfoService(@Named(BEAN_NAME_ROOT_DIRECTORY) Path rootDirectory) {
        super(rootDirectory);
    }

    /**
     * Retrieves detailed information about a file or directory.
     *
     * @param path the path to get information about
     * @return FileInfo object containing file details
     * @throws IllegalArgumentException if the path is null or empty
     * @throws SecurityException        if the path is outside the root directory
     * @throws NoSuchFileException      if the file does not exist
     * @throws IOException              if an I/O error occurs
     */
    public FileInfo getFileInfo(String path) throws IOException {

        Path file = validateAndNormalizeFullPath(path);

        if (!Files.exists(file)) {
            LOGGER.warn("File not found: {}", path);
            throw new NoSuchFileException("File not found: " + path);
        }

        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            FileInfo fileInfo = FileInfo
                    .builder()
                    .name(file.getFileName().toString())
                    .path(PathUtils.getRelativePath(rootDirectory, file))
                    .size(attrs.size())
                    .build();

            LOGGER.trace("Retrieved file info for: {}", path);
            return fileInfo;

        } catch (IOException e) {
            LOGGER.error("Failed to read file attributes: {}", path, e);
            throw new IOException("Failed to read file attributes: " + path, e);
        }
    }

}