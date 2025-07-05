package com.fos.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.fos.config.Constants.BEAN_NAME_ROOT_DIRECTORY;

/**
 * Service for deleting files and directories from the file system.
 */
@Singleton
public class FileDeleteService  extends BaseFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeleteService.class);

    @Inject
    public FileDeleteService(@Named(BEAN_NAME_ROOT_DIRECTORY) Path rootDirectory) {
        super(rootDirectory);
    }

    /**
     * Deletes a file or directory at the specified path.
     *
     * @param path      the path to delete
     * @param recursive if true, recursively deletes directories and their contents
     * @return true if the file or directory was deleted, false if it didn't exist
     * @throws IllegalArgumentException if the path is null or empty
     * @throws SecurityException if the path is outside the root directory
     * @throws IOException if an I/O error occurs
     * @throws DirectoryNotEmptyException if trying to delete non-empty directory without recursive flag
     */
    public boolean delete(String path, boolean recursive) throws IOException {
        Path targetPath = validateAndNormalizeFullPath(path);

        if (!Files.exists(targetPath)) {
            LOGGER.trace("Path does not exist: {}", path);
            return false;
        }

        try {
            if (recursive && Files.isDirectory(targetPath)) {
                deleteDirectoryRecursively(targetPath);
                return true;
            } else {
                return Files.deleteIfExists(targetPath);
            }
        } catch (DirectoryNotEmptyException e) {
            LOGGER.warn("Cannot delete non-empty directory without recursive flag: {}", path);
            throw new DirectoryNotEmptyException("Directory is not empty: " + path);
        } catch (IOException e) {
            LOGGER.error("Failed to delete path: {}", path, e);
            throw e;
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     *
     * @param directory the directory to delete
     * @throws IOException if an I/O error occurs
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            // sort the list in reverse order,
            // so the directory itself comes after the including subdirectories and files
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            LOGGER.trace("Deleted: {}", path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

}