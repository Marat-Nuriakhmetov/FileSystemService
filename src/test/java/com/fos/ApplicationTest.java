package com.fos;

import com.fos.api.FileServiceApiService;
import com.fos.config.Constants;
import com.fos.dto.HealthStatus;
import com.fos.health.HealthCheckService;
import com.google.inject.ProvisionException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.fos.config.Constants.ENV_VAR_REDIS_HOST;
import static com.fos.config.Constants.ENV_VAR_REDIS_PASSWORD;
import static com.fos.config.Constants.ENV_VAR_REDIS_PORT;
import static com.fos.config.Constants.ENV_VAR_ROOT_DIR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationTest {

    @Mock
    private FileServiceApiService fileServiceApiService;

    @Mock
    private HealthCheckService healthCheckService;

    private Server server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Use random available port for testing
        server = new Server(0);
        ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    @Test
    void createAndConfigureServer_Success() throws Exception {
        // Given
        HealthStatus healthStatus = createHealthStatus();
        when(healthCheckService.checkHealth(anyString())).thenReturn(healthStatus);

        Server server = Application.createAndConfigureServer(
                fileServiceApiService,
                healthCheckService
        );

        try {
            // When
            server.start();
            port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();

            // Then
            assertTrue(server.isRunning());
            assertNotNull(server.getHandler());

            // Verify endpoints are accessible
            verifyEndpoint("/fos/nonExist", 404);
            verifyEndpoint("/health", 200);

        } finally {
            server.stop();
        }
    }

    @Test
    void server_HandlesInvalidRequests() throws Exception {
        // Given
        Server server = Application.createAndConfigureServer(
                fileServiceApiService,
                healthCheckService
        );

        try {
            // When
            server.start();
            port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();

            // Then
            verifyEndpoint("/invalid", 404);

        } finally {
            server.stop();
        }
    }

    @Test
    void server_HandlesRootPath() throws Exception {
        // Given
        Server server = Application.createAndConfigureServer(
                fileServiceApiService,
                healthCheckService
        );

        try {
            // When
            server.start();
            port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();

            // Then
            verifyEndpoint("/", 404); // or whatever your expected behavior is

        } finally {
            server.stop();
        }
    }

    @Test
    @SetEnvironmentVariable.SetEnvironmentVariables({
            @SetEnvironmentVariable(key = ENV_VAR_ROOT_DIR, value = "."),
            @SetEnvironmentVariable(key = ENV_VAR_REDIS_HOST, value = "localhost"),
            @SetEnvironmentVariable(key = ENV_VAR_REDIS_PORT, value = "6379"),
            @SetEnvironmentVariable(key = ENV_VAR_REDIS_PASSWORD, value = "password")
    })
    void main_WithValidConfiguration_StartsServer() throws Exception {

        // When
        Thread serverThread = new Thread(() -> {
            try {
                Application.main(new String[]{});
            } catch (Exception e) {
                fail("Server failed to start: " + e.getMessage());
            }
        });
        serverThread.start();

        // Give the server some time to start
        Thread.sleep(2000);

        // Then
        // Verify server is running by making a request
        URL healthUrl = new URL("http://localhost:8080/health");
        HttpURLConnection conn = (HttpURLConnection) healthUrl.openConnection();
        assertEquals(200, conn.getResponseCode());
    }

    @Test
    void main_WithInvalidConfiguration_FailsGracefully() {
        // Given
        System.clearProperty(Constants.VM_ARG_ROOT_DIR);

        // When & Then
        assertThrows(ProvisionException.class,
                () -> Application.main(new String[]{}));
    }

    @Test
    void server_HandlesShutdownGracefully() throws Exception {
        // Given
        Server server = Application.createAndConfigureServer(
                fileServiceApiService,
                healthCheckService
        );
        server.start();

        // When
        Application.stopServer(server);

        // Then
        assertFalse(server.isRunning());
    }

    // Helper Methods
    private void verifyEndpoint(String path, int expectedStatus) throws IOException {
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        assertEquals(expectedStatus, conn.getResponseCode());
    }

    @Test
    void server_HandlesMultipleRequests() throws Exception {
        // Given
        Server server = Application.createAndConfigureServer(
                fileServiceApiService,
                healthCheckService
        );

        try {
            // When
            server.start();
            port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();

            // Then
            // Simulate multiple concurrent requests
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        verifyEndpoint("/health", 200);
                    } catch (IOException e) {
                        fail("Request failed: " + e.getMessage());
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

        } finally {
            server.stop();
        }
    }

    @Test
    void server_HandlesLargeRequests() throws Exception {
        // Given
        Server server = Application.createAndConfigureServer(
                fileServiceApiService,
                healthCheckService
        );

        try {
            // When
            server.start();
            port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();

            // Then
            URL url = new URL("http://localhost:" + port + "/fos");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Create large request payload
            StringBuilder largePayload = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largePayload.append("test data ");
            }

            // Send request
            conn.getOutputStream().write(largePayload.toString().getBytes());

            // Verify response
            assertEquals(200, conn.getResponseCode());

        } finally {
            server.stop();
        }
    }

    private HealthStatus createHealthStatus() {
        Map<String, Object> details = new HashMap<>();
        details.put("memory", Map.of(
                "status", "UP",
                "free", "512MB",
                "total", "1024MB"
        ));

        return new HealthStatus(
                HealthStatus.Status.UP,
                details,
                "HC-1234567890"
        );
    }
}