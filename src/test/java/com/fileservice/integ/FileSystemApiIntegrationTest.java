package com.fileservice.integ;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileservice.dto.FileInfo;
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

@EnabledIfEnvironmentVariable(named = "INTEG_TEST", matches = "ON")
public class FileSystemApiIntegrationTest {

    private final String fosServerBaseUrl = "http://localhost:8080/fos";
    //     private final String fosServerBaseUrl = System.getenv("FOS_SERVER_BASE_URL");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testAllOperationHappyPath() throws IOException {

        // delete file if previous tests didn't delete it
        callServer("delete", Arrays.asList("test.txt", true));
        callServer("delete", Arrays.asList("test", true));

        // Create folder
        Object result = callServer("create", Arrays.asList("test", "DIRECTORY"));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);

        // Create file
        result = callServer("create", Arrays.asList("test/test.txt", "FILE"));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);

        // get info
        Object fileInfoResponse = callServer("getFileInfo", Arrays.asList("test/test.txt"));
        assertInstanceOf(JSONObject.class, fileInfoResponse);
        String fileInfoAsString = JSONObject.toJSONString((Map<String, ? extends Object>) fileInfoResponse);
        FileInfo fileInfo = objectMapper.readValue(fileInfoAsString, FileInfo.class);
        assertEquals("test/test.txt", fileInfo.getPath());
        assertEquals("test.txt", fileInfo.getName());
        assertEquals(0, fileInfo.getSize());

        // write to file
        result = callServer("append", Arrays.asList("test/test.txt", "Hello"));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);

        // read the file
        Object content = callServer("read", Arrays.asList("test/test.txt", 0, 10000));
        assertInstanceOf(String.class, content);
        assertEquals("Hello", content);

        // append to file
        result = callServer("append", Arrays.asList("test/test.txt", " world!"));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);

        // read the entire file
        content = callServer("read", Arrays.asList("test/test.txt", 0, 10000));
        assertInstanceOf(String.class, content);
        assertEquals("Hello world!", content);

        // read the file partially
        content = callServer("read", Arrays.asList("test/test.txt", 6, 5));
        assertInstanceOf(String.class, content);
        assertEquals("world", content);

        // create nested folder
        result = callServer("create", Arrays.asList("test/nested", "DIRECTORY"));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);

        // list the dir
        Object fileInfosResponse = callServer("listDirectory", List.of("test"));
        String fileInfosAsString = JSONArray.toJSONString((List<? extends FileInfo>) fileInfosResponse);
        assertEquals("[{\"path\":\"test\\/test.txt\",\"size\":12,\"name\":\"test.txt\"},{\"path\":\"test\\/nested\",\"size\":64,\"name\":\"nested\"}]", fileInfosAsString);

        // delete file
        result = callServer("delete", Arrays.asList("test/test.txt", true));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);

        // delete folder
        result = callServer("delete", Arrays.asList("test", true));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);
    }

    // TODO change to map
    private Object callServer(String method, List<Object> positionalParams) {
        // Creating a new session to a JSON-RPC 2.0 web service at a specified URL

        // The JSON-RPC 2.0 server URL
        URL serverURL = null;

        try {
            serverURL = new URL(fosServerBaseUrl);
        } catch (MalformedURLException e) {
            // handle exception...
        }

        // Create new JSON-RPC 2.0 client session
        JSONRPC2Session mySession = new JSONRPC2Session(serverURL);
        //https://software.dzhuvinov.com/json-rpc-2.0-client.html
        // mySession.getOptions().setRequestContentType("application/json+rpc");

        // Once the client session object is created, you can use to send a series
        // of JSON-RPC 2.0 requests and notifications to it.

        // Sending an example "getServerTime" request:

        // Construct new request
        int requestID = 0;
        JSONRPC2Request request = new JSONRPC2Request(method, positionalParams, requestID);

        // Send request
        JSONRPC2Response response = null;

        try {
            response = mySession.send(request);
        } catch (JSONRPC2SessionException e) {
            System.err.println(e.getMessage());
            // TODO handle exception...
        }

        // Print response result / error

        //if (response.indicatesSuccess())
        //    System.out.println(response.getResult());
        //else
        //    System.out.println(response.getError().getMessage());

        return response.getResult();
    }

}
