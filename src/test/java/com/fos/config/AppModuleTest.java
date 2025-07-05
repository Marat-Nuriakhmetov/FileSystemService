package com.fos.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import static com.fos.config.Constants.BEAN_NAME_ROOT_DIR;
import static com.fos.config.Constants.ENV_VAR_REDIS_HOST;
import static com.fos.config.Constants.ENV_VAR_REDIS_PASSWORD;
import static com.fos.config.Constants.ENV_VAR_REDIS_PORT;
import static com.fos.config.Constants.ENV_VAR_ROOT_DIR;
import static com.fos.config.Constants.VM_ARG_ROOT_DIR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppModuleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppModuleTest.class);

    @TempDir
    private Path tempDir;

    // Root Directory Tests
    @Test
    void provideRootDirectory_WithCommandLineArgs_Success() {
        // Given
        String[] args = {tempDir.toString()};
        AppModule module = new AppModule(args);

        // When
        Injector injector = Guice.createInjector(module);
        Path rootDir = injector.getInstance(Key.get(Path.class, Names.named(BEAN_NAME_ROOT_DIR)));

        // Then
        assertEquals(tempDir, rootDir);
    }

    @Test
    @SetEnvironmentVariable(
            key = ENV_VAR_ROOT_DIR, value = "."
    )
    void provideRootDirectory_WithEnvironmentVariable_Success() throws Exception {
        // Given
        AppModule module = new AppModule(new String[]{});

        // When
        Injector injector = Guice.createInjector(module);
        Path rootDir = injector.getInstance(Key.get(Path.class, Names.named(BEAN_NAME_ROOT_DIR)));

        // Then
        assertEquals("", rootDir.toString());
    }

    @Test
    void provideRootDirectory_WithSystemProperty_Success() {
        // Given
        System.setProperty(VM_ARG_ROOT_DIR, tempDir.toString());
        AppModule module = new AppModule(new String[]{});

        // When
        Injector injector = Guice.createInjector(module);
        Path rootDir = injector.getInstance(Key.get(Path.class, Names.named(BEAN_NAME_ROOT_DIR)));

        // Then
        assertEquals(tempDir, rootDir);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void provideRootDirectory_WithInvalidPath_ThrowsException(String invalidPath) {
        // Given
        AppModule module = new AppModule(new String[]{invalidPath});

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> module.provideRootDirectory());
    }

    @Test
    void provideRootDirectory_WithNonexistentDirectory_ThrowsException() {
        // Given
        String nonexistentPath = tempDir.resolve("nonexistent").toString();
        AppModule module = new AppModule(new String[]{nonexistentPath});

        // When & Then
        assertThrows(InvalidPathException.class,
                () -> module.provideRootDirectory());
    }

    // Redis Configuration Tests

    @Test
    void provideJedisPool_WithCommandLineArgs_Success() {
        // Given
        String[] args = {
                tempDir.toString(),
                "localhost",
                "6379",
                "password"
        };
        AppModule module = new AppModule(args);

        // When
        Injector injector = Guice.createInjector(module);
        JedisPool jedisPool = injector.getInstance(JedisPool.class);

        // Then
        assertNotNull(jedisPool);
    }

    @Test
    @SetEnvironmentVariable.SetEnvironmentVariables({
            @SetEnvironmentVariable(key = ENV_VAR_REDIS_HOST, value = "localhost"),
            @SetEnvironmentVariable(key = ENV_VAR_REDIS_PORT, value = "6379"),
            @SetEnvironmentVariable(key = ENV_VAR_REDIS_PASSWORD, value = "password")
    })
    void provideJedisPool_WithEnvironmentVariables_Success() throws Exception {
        // Given
        AppModule module = new AppModule(new String[]{});

        // When
        Injector injector = Guice.createInjector(module);
        JedisPool jedisPool = injector.getInstance(JedisPool.class);

        // Then
        assertNotNull(jedisPool);
    }

    @Test
    void provideJedisPool_WithInvalidPort_ThrowsException() {
        // Given
        String[] args = {
                tempDir.toString(),
                "localhost",
                "invalid",
                "password"
        };
        AppModule module = new AppModule(args);

        // When & Then

        assertThrows(IllegalArgumentException.class,
                () -> module.provideJedisPool());
    }

    @Test
    void provideJedisPool_WithMissingPassword_ThrowsException() {
        // Given
        String[] args = {
                tempDir.toString(),
                "localhost",
                "6379"
        };
        AppModule module = new AppModule(args);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> module.provideJedisPool());
    }

    // Configuration Priority Tests

    @Test
    void configuration_PriorityOrder_Success() throws Exception {
        // TODO
    }
}