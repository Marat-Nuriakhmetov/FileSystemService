package com.fileservice.config;

import com.fileservice.controller.FileServiceController;
import com.fileservice.service.FileCreateService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.fileservice.config.Constants.ENV_VARIABLE_FILE_OPERATION_SERVICE_ROOT_DIR;
import static com.fileservice.config.Constants.BEAN_NAME_ROOT_DIRECTORY;

/**
 * Guice module configuration for the File Service application.
 * This module provides dependency injection configuration for file system operations,
 * including the root directory path and service bindings.
 *
 * <p>The module handles:
 * <ul>
 *     <li>Service bindings for file operations</li>
 *     <li>Root directory path configuration and validation</li>
 *     <li>Environment-based configuration management</li>
 * </ul>
 *
 * <p>Configuration is primarily handled through environment variables:
 * <pre>
 *
 * // Linux/Mac:
 * export FILE_OPERATION_SERVICE_ROOT_DIR=/path/to/root
 *
 * // Windows:
 * set FILE_OPERATION_SERVICE_ROOT_DIR=C:\path\to\root
 *
 * // Docker:
 * ENV FILE_OPERATION_SERVICE_ROOT_DIR=/app/data
 * </pre>
 *
 * <p>Usage example:
 * <pre>
 * Injector injector = Guice.createInjector(new AppModule());
 * FileCreateService fileService = injector.getInstance(FileCreateService.class);
 * </pre>
 *
 * <p>Security note:
 * The root directory serves as a security boundary for all file operations.
 * Operations are restricted to this directory and its subdirectories to prevent
 * unauthorized access to other parts of the file system.
 *
 * @see com.google.inject.AbstractModule
 */
public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
       bind(FileServiceController.class);
    }

    /**
     * Provides the root directory Path for file system operations.
     * The root directory is configured through the 'root.dir' environment variable
     * and serves as the base directory for all file operations, ensuring security
     * by containing operations within this directory.
     *
     * <p>The method performs the following validations:
     * <ul>
     *     <li>Checks if the environment variable 'root.dir' is set and not empty</li>
     *     <li>Verifies that the specified directory exists</li>
     *     <li>Confirms that the path points to a directory and not a file</li>
     * </ul>
     *
     * <p>Usage example:
     * <pre>
     * // Set environment variable
     * export FILE_OPERATION_SERVICE_ROOT_DIR=/path/to/root
     *
     * // The method will provide a validated Path object
     * Path rootDir = provideRootDirectory();
     * </pre>
     *
     * @return a {@link Path} object representing the validated root directory
     * @throws IllegalArgumentException if:
     *         <ul>
     *             <li>The environment variable 'root.dir' is not set or is empty</li>
     *             <li>The specified path does not exist</li>
     *             <li>The specified path is not a directory</li>
     *         </ul>
     * @throws SecurityException if the application lacks permission to access the directory
     * @throws InvalidPathException if the environment variable contains an invalid path
     *
     * @see Path
     * @see Files
     */
    @Provides
    @Named(BEAN_NAME_ROOT_DIRECTORY)
    public Path provideRootDirectory() {
        String rootDirectory = System.getenv(ENV_VARIABLE_FILE_OPERATION_SERVICE_ROOT_DIR);

        if (rootDirectory == null || rootDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("Root directory cannot be null or empty");
        }

        Path normalizedRoot = Paths.get(rootDirectory).normalize();
        if (!Files.exists(normalizedRoot) || !Files.isDirectory(normalizedRoot)) {
            throw new InvalidPathException(rootDirectory, "Root directory must exist and be a directory");
        }
        return normalizedRoot;
    }
}