package com.fos.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonRpcServlet}.
 * Tests JSON-RPC request handling, error conditions, and response formatting.
 */
@ExtendWith(MockitoExtension.class)
class JsonRpcServletTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private TestService testService;

    private JsonRpcServlet servlet;
    private StringWriter responseWriter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        servlet = new JsonRpcServlet(testService);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        objectMapper = new ObjectMapper();
    }

    @Test
    void doPost_ValidRequest_ReturnsSuccessResponse() throws IOException {
        // Given
        String jsonRpcRequest = createJsonRpcRequest("testMethod", "param1");
        setupRequestReader(jsonRpcRequest);

        // When
        servlet.doPost(request, response);

        // Then
        verify(response).setContentType("application/json+rpc");
        verify(response).setCharacterEncoding("UTF-8");

        String responseContent = responseWriter.toString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertEquals("2.0", responseJson.get("jsonrpc").asText());
        assertNotNull(responseJson.get("id"));
    }

    @Test
    void doPost_InvalidJson_ReturnsErrorResponse() throws IOException {
        // Given
        String invalidJson = "{ invalid json }";
        setupRequestReader(invalidJson);

        // When
        servlet.doPost(request, response);

        // Then
        String responseContent = responseWriter.toString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue(responseJson.has("error"));
        assertEquals(-32700, responseJson.get("error").get("code").asInt());
    }

    @Test
    void doPost_MissingMethod_ReturnsErrorResponse() throws IOException {
        // Given
        ObjectNode requestJson = objectMapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("id", 1);
        setupRequestReader(requestJson.toString());

        // When
        servlet.doPost(request, response);

        // Then
        String responseContent = responseWriter.toString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue(responseJson.has("error"));
        assertEquals(-32600, responseJson.get("error").get("code").asInt());
    }

    @Test
    void doPost_IOExceptionInResponse_PropagatesException() throws IOException {
        // Given
        String jsonRpcRequest = createJsonRpcRequest("testMethod", "param1");
        setupRequestReader(jsonRpcRequest);
        when(response.getWriter()).thenThrow(new IOException("Test exception"));

        // When & Then
        assertThrows(IOException.class, () -> servlet.doPost(request, response));
    }

    @Test
    void doPost_BatchRequest_HandlesCorrectly() throws IOException {
        // Given
        String batchRequest = "[" +
                createJsonRpcRequest("method1", "param1") + "," +
                createJsonRpcRequest("method2", "param2") +
                "]";
        setupRequestReader(batchRequest);

        // When
        servlet.doPost(request, response);

        // Then
        String responseContent = responseWriter.toString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue(responseJson.isArray());
    }

    @Test
    void doPost_NotificationRequest_HandlesCorrectly() throws IOException {
        // Given
        ObjectNode requestJson = objectMapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "testMethod");
        setupRequestReader(requestJson.toString());

        // When
        servlet.doPost(request, response);

        // Then
        assertEquals("", responseWriter.toString());
    }

    // Helper Methods

    private void setupRequestReader(String content) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(content));
        when(request.getReader()).thenReturn(reader);
    }

    private String createJsonRpcRequest(String method, String param) {
        return String.format(
                "{\"jsonrpc\":\"2.0\",\"method\":\"%s\",\"params\":[\"%s\"],\"id\":1}",
                method, param
        );
    }

    // Test Service Interface
    interface TestService {
        String testMethod(String param);
    }
}