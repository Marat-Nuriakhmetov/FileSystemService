package com.fos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fos.dto.HealthStatus;
import com.fos.health.HealthCheckService;
import com.google.inject.Inject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Health Check Endpoint for File Operation Service (FOS).
 *
 * <p>Provides a RESTful endpoint for monitoring system health, including file system
 * accessibility, resource availability, and overall service status. This servlet
 * is crucial for infrastructure monitoring and automated health checks in containerized
 * environments.</p>
 *
 * <h2>API Specification:</h2>
 * <pre>
 * GET /health
 *
 * Response Format:
 * {
 *   "status": "UP|DOWN",
 *   "timestamp": "2023-01-01T12:00:00.000Z",
 *   "details": {
 *     "fileSystem": {
 *       "status": "UP",
 *       "message": "Root directory accessible",
 *       "path": "/data"
 *     },
 *     "memory": {
 *       "status": "UP",
 *       "free": "512MB",
 *       "total": "1024MB"
 *     }
 *   }
 * }
 * </pre>
 *
 * <h2>HTTP Status Codes:</h2>
 * <ul>
 *   <li>{@code 200 OK} - System is healthy and operational</li>
 *   <li>{@code 503 Service Unavailable} - System is degraded or non-operational</li>
 *   <li>{@code 500 Internal Server Error} - Health check execution failed</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>
 * // Kubernetes liveness probe
 * livenessProbe:
 *   httpGet:
 *     path: /health
 *     port: 8080
 *   initialDelaySeconds: 10
 *   periodSeconds: 30
 *
 * // cURL request
 * curl -i http://localhost:8080/health
 * </pre>
 *
 * <h2>Implementation Notes:</h2>
 * <ul>
 *   <li>Thread-safe implementation suitable for concurrent access</li>
 *   <li>Non-caching responses for real-time health status</li>
 *   <li>Lightweight execution to minimize system impact</li>
 *   <li>Detailed logging for troubleshooting</li>
 * </ul>
 *
 * @see HealthCheckService For the underlying health check implementation
 * @see HealthStatus For the health status data structure
 * @version 1.0
 * @since 1.0
 */
public class HealthCheckServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckServlet.class);
    private final HealthCheckService healthCheckService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new HealthCheckServlet with dependency injection.
     *
     * @param healthCheckService Service responsible for performing health checks
     * @throws NullPointerException if healthCheckService is null
     */
    @Inject
    public HealthCheckServlet(HealthCheckService healthCheckService) {
        if (healthCheckService == null) {
            throw new NullPointerException("HealthCheckService cannot be null");
        }
        this.healthCheckService = healthCheckService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Processes GET requests to the health check endpoint.
     *
     * <p>Execution Flow:</p>
     * <ol>
     *   <li>Logs incoming request</li>
     *   <li>Performs system health check</li>
     *   <li>Sets appropriate response headers</li>
     *   <li>Returns health status in JSON format</li>
     * </ol>
     *
     * <p>Error Handling:</p>
     * <ul>
     *   <li>Catches and logs all exceptions</li>
     *   <li>Returns appropriate error response</li>
     *   <li>Maintains audit trail for troubleshooting</li>
     * </ul>
     *
     * @param request HTTP request object
     * @param response HTTP response object
     * @throws IOException if response writing fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String requestId = generateRequestId();
        LOGGER.debug("Processing health check request [{}]", requestId);

        try {
            HealthStatus status = healthCheckService.checkHealth(requestId);
            LOGGER.debug("Health check completed [{}] with status: {}",
                    requestId, status.getStatus());

            configureResponse(response);
            response.setStatus(determineHttpStatus(status));

            String jsonResponse = objectMapper.writeValueAsString(status);
            response.getWriter().write(jsonResponse);

        } catch (Exception e) {
            LOGGER.error("Health check failed [{}]", requestId, e);
            configureResponse(response);
            handleError(response, requestId);
        }
    }

    /**
     * Configures HTTP response headers for health check response.
     * Sets content type, character encoding, and cache control directives.
     *
     * @param response HTTP response to configure
     */
    private void configureResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    /**
     * Maps health status to appropriate HTTP status code.
     *
     * @param status Current health status
     * @return HTTP status code (200 for UP, 503 for DOWN)
     */
    private int determineHttpStatus(HealthStatus status) {
        return status.getStatus() == HealthStatus.Status.UP ?
                HttpServletResponse.SC_OK :
                HttpServletResponse.SC_SERVICE_UNAVAILABLE;
    }

    /**
     * Handles and formats error responses for failed health checks.
     *
     * @param response HTTP response for error
     * @param requestId Unique identifier for the request
     * @throws IOException if writing error response fails
     */
    private void handleError(HttpServletResponse response, String requestId)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String errorJson = String.format(
                "{\"status\":\"ERROR\",\"message\":\"Health check failed\",\"requestId\":\"%s\"}",
                requestId
        );
        response.getWriter().write(errorJson);
    }

    /**
     * Generates a unique identifier for request tracking.
     *
     * @return Unique request identifier
     */
    private String generateRequestId() {
        return String.format("HC-%d", System.currentTimeMillis());
    }
}