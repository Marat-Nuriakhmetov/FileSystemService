package com.fos.integ;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fos.dto.FileInfo;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "INTEG_TEST", matches = "ENABLED")
public class FileOperationServiceIntegrationTest {

    private final String fosServerBaseUrl = System.getenv("FOS_SERVER_BASE_URL");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JSONRPC2Session session = createSession();

    public FileOperationServiceIntegrationTest() throws MalformedURLException {
    }

    @Test
    void testOperationsExceptAppendData_HappyPath() throws IOException, JSONRPC2SessionException {

        // delete file if previous tests didn't delete it
        callServer("delete", Arrays.asList("test.txt", true));
        callServer("delete", Arrays.asList("test", true));

        // Create folder
        boolean result = callServerAndVCastnResult("create", Arrays.asList("test", "DIRECTORY"), Boolean.class);
        assertTrue(result);

        // Create file
        result = callServerAndVCastnResult("create", Arrays.asList("test/test.txt", "FILE"), Boolean.class);
        assertTrue(result);

        // get info
        JSONObject fileInfoResponse = callServerAndVCastnResult("getFileInfo", Arrays.asList("test/test.txt"), JSONObject.class);
        String fileInfoAsString = JSONObject.toJSONString(fileInfoResponse);
        FileInfo fileInfo = objectMapper.readValue(fileInfoAsString, FileInfo.class);
        assertEquals("test/test.txt", fileInfo.getPath());
        assertEquals("test.txt", fileInfo.getName());
        assertEquals(0, fileInfo.getSize());

        // read the file
        String content = callServerAndVCastnResult("read", Arrays.asList("test/test.txt", 0, 10000), String.class);
        assertEquals("", content);

        // create nested folder
        result = callServerAndVCastnResult("create", Arrays.asList("test/nested", "DIRECTORY"), Boolean.class);
        assertTrue(result);

        // list the dir
        JSONArray fileInfosResponse = callServerAndVCastnResult("listDirectory", List.of("test"), JSONArray.class);

        fileInfoAsString = JSONObject.toJSONString((Map<String, ? extends Object>) fileInfosResponse.get(0));
        fileInfo = objectMapper.readValue(fileInfoAsString, FileInfo.class);
        assertEquals("test/test.txt", fileInfo.getPath());

        fileInfoAsString = JSONObject.toJSONString((Map<String, ? extends Object>) fileInfosResponse.get(1));
        fileInfo = objectMapper.readValue(fileInfoAsString, FileInfo.class);
        assertEquals("test/nested", fileInfo.getPath());

        // delete file
        result = callServerAndVCastnResult("delete", Arrays.asList("test/test.txt", true), Boolean.class);
        assertTrue(result);

        // delete folder
        result = callServerAndVCastnResult("delete", Arrays.asList("test", true), Boolean.class);
        assertTrue(result);

        // list the root dir
        JSONArray list = callServerAndVCastnResult("listDirectory", List.of("."), JSONArray.class);
        assertEquals("[]", JSONArray.toJSONString(list));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "INTEG_TEST_TEST_ALL_OPERATION", matches = "ENABLED")
    void testAllOperationHappyPath() throws IOException, JSONRPC2SessionException {

        // delete file if previous tests didn't delete it
        callServer("delete", Arrays.asList("test.txt", true));
        callServer("delete", Arrays.asList("test", true));

        // Create folder
        boolean result = callServerAndVCastnResult("create", Arrays.asList("test", "DIRECTORY"), Boolean.class);
        assertTrue(result);

        // Create file
        result = callServerAndVCastnResult("create", Arrays.asList("test/test.txt", "FILE"), Boolean.class);
        assertTrue(result);

        // get info
        JSONObject fileInfoResponse = callServerAndVCastnResult("getFileInfo", Arrays.asList("test/test.txt"), JSONObject.class);
        String fileInfoAsString = JSONObject.toJSONString(fileInfoResponse);
        FileInfo fileInfo = objectMapper.readValue(fileInfoAsString, FileInfo.class);
        assertEquals("test/test.txt", fileInfo.getPath());
        assertEquals("test.txt", fileInfo.getName());
        assertEquals(0, fileInfo.getSize());

        /*
        // write to file
        result = callServerAndVCastnResult("append", Arrays.asList("test/test.txt", "Hello"), Boolean.class);
        assertTrue(result);

        // read the file
        String content = callServerAndVCastnResult("read", Arrays.asList("test/test.txt", 0, 10000), String.class);
        assertEquals("Hello", content);

        // append to file
        result = callServerAndVCastnResult("append", Arrays.asList("test/test.txt", " world!"), Boolean.class);
        assertTrue(result);

        // read the entire file
        content = callServerAndVCastnResult("read", Arrays.asList("test/test.txt", 0, 10000), String.class);
        assertEquals("Hello world!", content);

        // read the file partially
        content = callServerAndVCastnResult("read", Arrays.asList("test/test.txt", 6, 5), String.class);
        assertEquals("world", content);

        // create nested folder
        result = callServerAndVCastnResult("create", Arrays.asList("test/nested", "DIRECTORY"), Boolean.class);
        assertTrue(result);

        // list the dir
        JSONArray fileInfosResponse = callServerAndVCastnResult("listDirectory", List.of("test"), JSONArray.class);

        fileInfoAsString = JSONObject.toJSONString((Map<String, ? extends Object>) fileInfosResponse.get(0));
        fileInfo = objectMapper.readValue(fileInfoAsString, FileInfo.class);
        assertEquals("test/test.txt", fileInfo.getPath());

        fileInfoAsString = JSONObject.toJSONString((Map<String, ? extends Object>) fileInfosResponse.get(1));
        fileInfo = objectMapper.readValue(fileInfoAsString, FileInfo.class);
        assertEquals("test/nested", fileInfo.getPath());

        // delete file
        result = callServerAndVCastnResult("delete", Arrays.asList("test/test.txt", true), Boolean.class);
        assertTrue(result);

        // delete folder
        result = callServerAndVCastnResult("delete", Arrays.asList("test", true), Boolean.class);
        assertTrue(result);

        // list the root dir
        JSONArray list = callServerAndVCastnResult("listDirectory", List.of("."), JSONArray.class);
        assertEquals("[]", JSONArray.toJSONString(list));
        */

    }

    private <T> T callServerAndVCastnResult(String method, List<Object> positionalParams, Class<T> clazz) throws JSONRPC2SessionException {
        Object result = callServer(method, positionalParams);
        assertInstanceOf(clazz, result);
        return clazz.cast(result);
    }

    private Object callServer(String method, List<Object> positionalParams) throws JSONRPC2SessionException {
        int requestID = 0;
        JSONRPC2Request request = new JSONRPC2Request(method, positionalParams, requestID);
        // Send request
        JSONRPC2Response response = session.send(request);;
        return response.getResult();
    }

    private JSONRPC2Session createSession() throws MalformedURLException {
        // Creating a new session to a JSON-RPC 2.0 web service at a specified URL

        // The JSON-RPC 2.0 server URL
        URL serverURL = new URL(fosServerBaseUrl);;

        // Create new JSON-RPC 2.0 client session

        // Once the client session object is created, you can use to send a series
        // of JSON-RPC 2.0 requests and notifications to it.

        return new JSONRPC2Session(serverURL);
    }

}
