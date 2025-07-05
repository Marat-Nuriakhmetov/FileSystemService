package com.fos.config;

import com.fos.api.FileServiceApiService;
import com.fos.health.HealthCheckService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.fos.config.Constants.*;

/**
 * Guice module configuration for the File Operation Service (FOS).
 * Provides dependency injection configuration for file system operations,
 * Redis connection management, and service bindings.
 *
 * <p>Configuration Priority:
 * 1. Command line arguments
 * 2. Environment variables
 * 3. JVM system properties
 *
 * <p>Required Configurations:
 * <ul>
 *     <li>Root Directory: Base directory for all file operations</li>
 *     <li>Redis Connection: Host, port, and authentication details</li>
 * </ul>
 *
 * <p>Configuration Methods (the values as examples):
 * <pre>
 * 1. Command Line Arguments:
 *    java -jar app.jar /path/to/root redis-host 6379 password
 *
 * 2. Environment Variables:
 *    export FOS_ROOT_DIR=/path/to/root
 *    export REDIS_HOST=localhost
 *    export REDIS_PORT=6379
 *    export REDIS_PASSWORD=secret
 *
 * 3. JVM Arguments:
 *    -Dfos.root.dir=/path/to/root
 *    -Dfos.redis.host=localhost
 *    -Dfos.redis.port=6379
 *    -Dfos.redis.password=secret
 * </pre>
 *
 * <p>Security Notes:
 * <ul>
 *     <li>Root directory serves as a security boundary</li>
 *     <li>Redis connections are password protected</li>
 *     <li>All paths are normalized to prevent directory traversal</li>
 * </ul>
 *
 * @see FileServiceApiService
 * @see HealthCheckService
 * @see JedisPool
 */
@Singleton
public class AppModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppModule.class);

    // Redis connection constants
    private static final int REDIS_TIMEOUT = 2000; // milliseconds
    private static final int REDIS_MAX_TOTAL = 100;
    private static final int REDIS_MAX_IDLE = 20;
    private static final int REDIS_MIN_IDLE = 10;

    private final String[] programArgs;

    /**
     * Constructs a new AppModule with command line arguments.
     *
     * @param programArgs command line arguments array containing configuration values
     */
    public AppModule(String[] programArgs) {
        this.programArgs = programArgs != null ? programArgs : new String[0];
    }

    /**
     * Configures the Guice bindings for the application.
     * Binds all necessary services and components.
     */
    @Override
    protected void configure() {
        bind(FileServiceApiService.class);
        bind(HealthCheckService.class);
    }

    /**
     * Provides the root directory Path for file system operations.
     * Attempts to load the root directory path in the following order:
     * 1. Command line argument
     * 2. Environment variable
     * 3. JVM system property
     *
     * @return validated and normalized root directory Path
     * @throws IllegalArgumentException if root directory is not configured or invalid
     * @throws InvalidPathException if the path is invalid or inaccessible
     */
    @Provides
    @Named(BEAN_NAME_ROOT_DIR)
    @Singleton
    public Path provideRootDirectory() {
        String rootDirectory = resolveRootDirectory();
        validateRootDirectory(rootDirectory);

        Path normalizedRoot = Paths.get(rootDirectory).normalize();
        validateRootPath(normalizedRoot);

        LOGGER.info("Initialized root directory: {}", normalizedRoot);
        return normalizedRoot;
    }

    /**
     * Provides a configured JedisPool for Redis operations.
     * Configures connection pooling and attempts to establish Redis connection
     * using provided credentials.
     *
     * @return configured JedisPool instance
     * @throws IllegalArgumentException if Redis configuration is invalid
     */
    @Provides
    @Singleton
    public JedisPool provideJedisPool() {
        JedisPoolConfig poolConfig = createJedisPoolConfig();
        RedisConfig redisConfig = resolveRedisConfig();

        LOGGER.info("Initializing Redis connection pool to {}:{}",
                redisConfig.host, redisConfig.port);

        return new JedisPool(
                poolConfig,
                redisConfig.host,
                redisConfig.port,
                REDIS_TIMEOUT,
                redisConfig.password
        );
    }

    // Private helper methods

    private String resolveRootDirectory() {
        String rootDir = null;

        // Try command line args
        if (programArgs.length > 0) {
            rootDir = programArgs[0];
            LOGGER.debug("Root directory from command line: {}", rootDir);
        }

        // Try environment variable
        if (rootDir == null) {
            rootDir = System.getenv(ENV_VAR_ROOT_DIR);
            LOGGER.debug("Root directory from environment: {}", rootDir);
        }

        // Try JVM property
        if (rootDir == null) {
            rootDir = System.getProperty(VM_ARG_ROOT_DIR);
            LOGGER.debug("Root directory from JVM property: {}", rootDir);
        }

        return rootDir;
    }

    private void validateRootDirectory(String rootDirectory) {
        if (rootDirectory == null || rootDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Root directory must be configured via command line, " +
                            "environment variable, or JVM property"
            );
        }
    }

    private void validateRootPath(Path normalizedRoot) {
        if (!Files.exists(normalizedRoot) || !Files.isDirectory(normalizedRoot)) {
            throw new InvalidPathException(
                    normalizedRoot.toString(),
                    "Root directory must exist and be a directory"
            );
        }
    }

    private JedisPoolConfig createJedisPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(REDIS_MAX_TOTAL);
        poolConfig.setMaxIdle(REDIS_MAX_IDLE);
        poolConfig.setMinIdle(REDIS_MIN_IDLE);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        return poolConfig;
    }

    private RedisConfig resolveRedisConfig() {
        RedisConfig config = new RedisConfig();

        // Try command line args
        if (programArgs.length > 3) {
            config.host = programArgs[1];
            config.port = Integer.parseInt(programArgs[2]);
            config.password = programArgs[3];
            return config;
        }

        // Try environment variables
        config.host = System.getenv(ENV_VAR_REDIS_HOST);
        String portStr = System.getenv(ENV_VAR_REDIS_PORT);
        config.password = System.getenv(ENV_VAR_REDIS_PASSWORD);

        // Fall back to JVM properties
        if (config.host == null) {
            config.host = System.getProperty(VM_ARG_REDIS_HOST);
            portStr = System.getProperty(VM_ARG_REDIS_PORT);
            config.password = System.getProperty(VM_ARG_REDIS_PASSWORD);
        }

        validateRedisConfig(config, portStr);
        return config;
    }

    private void validateRedisConfig(RedisConfig config, String portStr) {
        if (config.host == null || config.host.trim().isEmpty()) {
            throw new IllegalArgumentException("Redis host must be configured");
        }

        if (portStr == null || !portStr.matches("\\d+")) {
            throw new IllegalArgumentException("Valid Redis port must be configured");
        }

        config.port = Integer.parseInt(portStr);

        if (config.password == null || config.password.trim().isEmpty()) {
            throw new IllegalArgumentException("Redis password must be configured");
        }
    }

    /**
     * Internal class to hold Redis configuration parameters.
     */
    private static class RedisConfig {
        String host;
        int port;
        String password;
    }
}