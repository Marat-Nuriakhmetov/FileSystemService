package com.fos;

import com.fos.config.AppModule;
import com.fos.api.FileServiceApiService;
import com.fos.api.HealthCheckServlet;
import com.fos.api.JsonRpcServlet;
import com.fos.health.HealthCheckService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Main application class for the File Operation Service.
 * This class initializes and starts the web server with JSON-RPC endpoints
 * for file system operations.
 *
 * <p>The application:
 * <ul>
 *     <li>Sets up dependency injection using Guice</li>
 *     <li>Configures and starts Jetty server</li>
 *     <li>Initializes JSON-RPC endpoints</li>
 *     <li>Handles server lifecycle</li>
 * </ul>
 *
 * <p>Configuration:
 * <ul>
 *     <li>Server port: 8080 (default)</li>
 *     <li>Context path: /</li>
 *     <li>RPC endpoint: /fos</li>
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
 * @see AppModule
 * @see FileServiceApiService
 * @see JsonRpcServlet
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final int DEFAULT_PORT = 8080;
    private static final String CONTEXT_PATH = "/";
    private static final String RPC_ENDPOINT = "/fos";
    private static final String HEALTH_CHECK_ENDPOINT = "/health";

    /**
     * Application entry point.
     * Initializes and starts the server with all required components.
     *
     * @param args command line arguments (not used currently)
     * @throws Exception if server initialization or startup fails
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting File Operation Service...");

        try {
            // Initialize dependency injection
            LOGGER.trace("Initializing Guice injector...");
            Injector injector = Guice.createInjector(new AppModule(args));

            // Get controller instance
            LOGGER.trace("Creating FileServiceController...");
            FileServiceApiService fileServiceApiService =
                    injector.getInstance(FileServiceApiService.class);

            // Get health check service instance
            HealthCheckService healthCheckService =
                    injector.getInstance(HealthCheckService.class);

            // Start server
            Server server = createAndConfigureServer(fileServiceApiService, healthCheckService);
            startServer(server);

        } catch (Exception e) {
            LOGGER.error("Failed to start application", e);
            throw e;
        }
    }

    static Server createAndConfigureServer(FileServiceApiService fileServiceApiService, HealthCheckService healthCheckService) {
        LOGGER.trace("Configuring Jetty server...");

        // Create server instance
        Server server = new Server(DEFAULT_PORT);

        // Configure context handler
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(CONTEXT_PATH);
        server.setHandler(context);

        // Configure error handler
        ErrorHandler errorHandler = createCustomErrorHandler();
        server.setErrorHandler(errorHandler);

        // Add File Operation Service API servlet
        JsonRpcServlet rpcServlet = new JsonRpcServlet(fileServiceApiService);
        context.addServlet(new ServletHolder(rpcServlet), RPC_ENDPOINT);

        // Add health check servlet
        HealthCheckServlet healthCheckServlet = new HealthCheckServlet(healthCheckService);
        context.addServlet(new ServletHolder(healthCheckServlet), HEALTH_CHECK_ENDPOINT);

        LOGGER.info("Server configured on port " + DEFAULT_PORT);
        return server;
    }

    /**
     * Creates a custom error handler for the server.
     * Handles various HTTP error scenarios with appropriate responses.
     *
     * @return configured ErrorHandler instance
     */
    private static ErrorHandler createCustomErrorHandler() {
        // TODO add error handling
        return new ErrorHandler();
    }

    /**
     * Starts the server and waits for it to complete.
     *
     * @param server the configured Server instance to start
     * @throws Exception if server fails to start or encounters an error
     */
    private static void startServer(Server server) throws Exception {
        try {
            LOGGER.info("Starting server...");
            server.start();
            LOGGER.info("Server started successfully");

            // Wait for server to complete
            server.join();

        } catch (Exception e) {
            LOGGER.error("Server failed to start", e);
            throw e;
        }
    }

    /**
     * Stops the server gracefully.
     * TODO call it from shutdown hooks
     *
     * @param server the Server instance to stop
     */
    static void stopServer(Server server) {
        try {
            if (server != null && server.isRunning()) {
                LOGGER.info("Stopping server...");
                server.stop();
                LOGGER.info("Server stopped successfully");
            }
        } catch (Exception e) {
            LOGGER.error("Error stopping server", e);
        }
    }
}