package com.fos.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fos.dto.HealthStatus;
import com.fos.health.HealthCheckService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HealthCheckServlet}.
 * Tests various scenarios including successful health checks,
 * system failures, and error conditions.
 */
@ExtendWith(MockitoExtension.class)
class HealthCheckServletTest {

    @Mock
    private HealthCheckService healthCheckService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private StringWriter responseWriter;
    private HealthCheckServlet servlet;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        servlet = new HealthCheckServlet(healthCheckService);
        objectMapper = new ObjectMapper();
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void doGet_WhenSystemHealthy_ReturnsOkStatus() throws IOException {
        // Given
        HealthStatus healthStatus = createHealthStatus(HealthStatus.Status.UP);
        when(healthCheckService.checkHealth(anyString())).thenReturn(healthStatus);

        // When
        servlet.doGet(request, response);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verifyResponseHeaders(response);

        JsonNode jsonResponse = objectMapper.readTree(responseWriter.toString());
        assertEquals("UP", jsonResponse.get("status").asText());
        assertNotNull(jsonResponse.get("timestamp"));
        assertNotNull(jsonResponse.get("details"));
    }

    @Test
    void doGet_WhenSystemUnhealthy_ReturnsServiceUnavailable() throws IOException {
        // Given
        HealthStatus healthStatus = createHealthStatus(HealthStatus.Status.DOWN);
        when(healthCheckService.checkHealth(anyString())).thenReturn(healthStatus);

        // When
        servlet.doGet(request, response);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verifyResponseHeaders(response);

        JsonNode jsonResponse = objectMapper.readTree(responseWriter.toString());
        assertEquals("DOWN", jsonResponse.get("status").asText());
    }

    @Test
    void doGet_WhenHealthCheckThrowsException_ReturnsInternalServerError()
            throws IOException {
        // Given
        when(healthCheckService.checkHealth(anyString()))
                .thenThrow(new RuntimeException("Health check failed"));

        // When
        servlet.doGet(request, response);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verifyResponseHeaders(response);

        JsonNode jsonResponse = objectMapper.readTree(responseWriter.toString());
        assertEquals("ERROR", jsonResponse.get("status").asText());
        assertEquals("Health check failed", jsonResponse.get("message").asText());
        assertTrue(jsonResponse.get("requestId").asText().startsWith("HC-"));
    }

    @Test
    void doGet_ResponseIncludesRequestId() throws IOException {
        // Given
        HealthStatus healthStatus = createHealthStatus(HealthStatus.Status.UP);
        when(healthCheckService.checkHealth(anyString())).thenReturn(healthStatus);

        // When
        servlet.doGet(request, response);

        // Then
        String responseContent = responseWriter.toString();
        assertTrue(responseContent.contains("HC-1234567890"));
    }

    @Test
    void doGet_SetsCorrectResponseHeaders() throws IOException {
        // Given
        when(healthCheckService.checkHealth(anyString()))
                .thenReturn(createHealthStatus(HealthStatus.Status.UP));

        // When
        servlet.doGet(request, response);

        // Then
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        verify(response).setHeader("Cache-Control",
                "no-cache, no-store, must-revalidate");
        verify(response).setHeader("Pragma", "no-cache");
        verify(response).setHeader("Expires", "0");
    }

    // Helper Methods

    private HealthStatus createHealthStatus(HealthStatus.Status status) {
        Map<String, Object> details = new HashMap<>();
        details.put("memory", Map.of(
                "status", "UP",
                "free", "512MB",
                "total", "1024MB"
        ));

        return new HealthStatus(
                status,
                details,
                "HC-1234567890"
        );
    }

    private void verifyResponseHeaders(HttpServletResponse response) {
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        verify(response).setHeader("Cache-Control",
                "no-cache, no-store, must-revalidate");
    }
}