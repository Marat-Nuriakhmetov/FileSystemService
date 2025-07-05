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
 * Servlet that handles health check requests for the application.
 * Provides a REST endpoint that returns the current health status of the system
 * in JSON format. The health check includes various system components and
 * returns appropriate HTTP status codes based on the overall health status.
 *
 * <p>Endpoint: GET /health
 * <p>Response format:
 * <pre>
 * {
 *   "status": "UP|DOWN",
 *   "timestamp": "2023-01-01T12:00:00.000Z",
 *   "details": {
 *     "component1": { ... },
 *     "component2": { ... }
 *   }
 * }
 * </pre>
 *
 * <p>HTTP Status Codes:
 * <ul>
 *   <li>200 OK - System is healthy</li>
 *   <li>503 Service Unavailable - System is unhealthy</li>
 *   <li>500 Internal Server Error - Health check failed</li>
 * </ul>
 *
 * @see HealthCheckService
 * @see HealthStatus
 */
public class HealthCheckServlet extends HttpServlet {

    /**
     * Logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckServlet.class);

    /**
     * Service responsible for performing health checks.
     */
    private final HealthCheckService healthCheckService;

    /**
     * JSON Object mapper for serializing health status.
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new HealthCheckServlet with the specified health check service.
     *
     * @param healthCheckService the service to use for health checks
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
     * Handles GET requests to the health check endpoint.
     * Performs a health check and returns the results as JSON.
     *
     * <p>The response includes:
     * <ul>
     *   <li>Current system status (UP/DOWN)</li>
     *   <li>Timestamp of the check</li>
     *   <li>Detailed status of individual components</li>
     * </ul>
     *
     * @param request the HTTP servlet request
     * @param response the HTTP servlet response
     * @throws IOException if an I/O error occurs while writing the response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        LOGGER.debug("Processing health check request");

        try {
            // Perform health check
            HealthStatus status = healthCheckService.checkHealth();
            LOGGER.debug("Health check completed with status: {}", status.getStatus());

            // Configure response
            configureResponse(response);

            // Set HTTP status code based on health status
            response.setStatus(determineHttpStatus(status));

            // Write response
            String jsonResponse = objectMapper.writeValueAsString(status);
            response.getWriter().write(jsonResponse);

        } catch (Exception e) {
            LOGGER.error("Health check failed", e);
            handleError(response);
        }
    }

    /**
     * Configures the HTTP response headers.
     *
     * @param response the HTTP servlet response to configure
     */
    private void configureResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    /**
     * Determines the appropriate HTTP status code based on the health status.
     *
     * @param status the health status
     * @return the HTTP status code to use
     */
    private int determineHttpStatus(HealthStatus status) {
        return status.getStatus() == HealthStatus.Status.UP ?
                HttpServletResponse.SC_OK :
                HttpServletResponse.SC_SERVICE_UNAVAILABLE;
    }

    /**
     * Handles errors that occur during health check processing.
     *
     * @param response the HTTP servlet response
     * @throws IOException if an I/O error occurs while writing the error response
     */
    private void handleError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Health check failed\"}");
    }
}