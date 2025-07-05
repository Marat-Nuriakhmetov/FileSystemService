package com.fos.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.fos.config.Constants.BEAN_NAME_ROOT_DIRECTORY;

/**
 * Service for creating files and directories.
 * Provides methods to create single files and directories in the file system.
 */
@Singleton
public class FileCreateService extends BaseFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileCreateService.class);

    @Inject
    public FileCreateService(@Named(BEAN_NAME_ROOT_DIRECTORY) Path rootDirectory) {
        super(rootDirectory);
    }

    /**
     * Creates a new empty file at the specified path.
     *
     * @param path the path where the file should be created
     * @return true if file was created successfully
     * @throws IllegalArgumentException if the path is null or empty
     * @throws FileAlreadyExistsException if a file already exists at the specified path
     * @throws IOException if an I/O error occurs or the parent directory doesn't exist
     */
    public boolean createFile(String path) throws IOException {

        try {
            Path fullPath = validateAndNormalizeFullPath(path);
            validateParentDirectory(fullPath);
            validatePathWithinRootFolder(fullPath);

            Files.createFile(fullPath);
            LOGGER.trace("File created successfully at: {}", path);
            return true;
        } catch (FileAlreadyExistsException e) {
            LOGGER.warn("File already exists at: {}", path);
            throw e;
        } catch (IOException e) {
            LOGGER.error("Failed to create file at: {}", path, e);
            throw e;
        }
    }

    /**
     * Creates a new directory at the specified path.
     *
     * @param path the path where the directory should be created
     * @return true if directory was created successfully
     * @throws IllegalArgumentException if the path is null or empty
     * @throws FileAlreadyExistsException if a directory already exists at the specified path
     * @throws IOException if an I/O error occurs or the parent directory doesn't exist
     */
    public boolean createDirectory(String path) throws IOException {

        try {
            Path dirFullPath = validateAndNormalizeFullPath(path);
            validateParentDirectory(dirFullPath);

            Files.createDirectory(dirFullPath);
            LOGGER.trace("Directory created successfully at: {}", path);
            return true;
        } catch (FileAlreadyExistsException e) {
            LOGGER.warn("Directory already exists at: {}", path);
            throw e;
        } catch (IOException e) {
            LOGGER.error("Failed to create directory at: {}", path, e);
            throw e;
        }
    }

}