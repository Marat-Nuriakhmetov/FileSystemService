package com.fileservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arteam.simplejsonrpc.server.JsonRpcServer;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * HTTP Servlet implementation for handling JSON-RPC 2.0 requests.
 * This servlet processes incoming JSON-RPC requests and delegates them to the appropriate service methods.
 *
 * <p>Features:
 * <ul>
 *     <li>JSON-RPC 2.0 protocol support</li>
 *     <li>UTF-8 character encoding</li>
 *     <li>Automatic request/response handling</li>
 *     <li>Service method delegation</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * // Create servlet with service
 * FileService fileService = new FileService();
 * JsonRpcServlet servlet = new JsonRpcServlet(fileService);
 *
 * // Register servlet in web container
 * context.addServlet(new ServletHolder(servlet), "/jsonrpc");
 * </pre>
 *
 * <p>Example JSON-RPC request:
 * <pre>
 * {
 *     "jsonrpc": "2.0",
 *     "method": "getFileInfo",
 *     "params": ["/path/to/file"],
 *     "id": 1
 * }
 * </pre>
 */
public class JsonRpcServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonRpcServlet.class);
    private static final String CONTENT_TYPE = "application/json+rpc";
    private static final String ENCODING = "UTF-8";

    /** The JSON-RPC server instance handling the protocol */
    private final JsonRpcServer jsonRpcServer;

    /** Jackson ObjectMapper for JSON processing */
    private final ObjectMapper mapper;

    /** The service instance that handles the actual business logic */
    private final Object service;

    /**
     * Constructs a new JsonRpcServlet with the specified service.
     *
     * @param service the service instance that will handle the RPC methods
     * @throws IllegalArgumentException if service is null
     */
    public JsonRpcServlet(Object service) {
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }
        this.jsonRpcServer = new JsonRpcServer();
        this.mapper = new ObjectMapper();
        this.service = service;
    }

    /**
     * Handles POST requests containing JSON-RPC calls.
     * Processes the request and returns the JSON-RPC response.
     *
     * <p>The method:
     * <ol>
     *     <li>Sets appropriate content type and encoding</li>
     *     <li>Reads the request body</li>
     *     <li>Processes the JSON-RPC request</li>
     *     <li>Writes the response</li>
     * </ol>
     *
     * @param req the HTTP servlet request
     * @param resp the HTTP servlet response
     * @throws IOException if an I/O error occurs while processing the request
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            // Set response headers
            resp.setContentType(CONTENT_TYPE);
            resp.setCharacterEncoding(ENCODING);

            // Read request body
            String textRequest = req.getReader().lines().collect(Collectors.joining());
            LOGGER.trace("Received JSON-RPC request: {}", textRequest);

            // Process request
            String result = jsonRpcServer.handle(textRequest, service);
            LOGGER.trace("Sending JSON-RPC response: {}", result);

            // Write response
            resp.getWriter().write(result);

        } catch (IOException e) {
            LOGGER.error("Error processing JSON-RPC request", e);
            throw e;
        }
    }

    /**
     * Returns the ObjectMapper instance used by this servlet.
     * Useful for customizing JSON serialization/deserialization.
     *
     * @return the ObjectMapper instance
     */
    public ObjectMapper getObjectMapper() {
        return mapper;
    }
}