package com.fileservice.integ;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO add missing tests and refactor
public class FileSystemApiIntegrationTest {

    @Test
    void test() throws IOException {
        // Create file
        Object result = callServer("create", Arrays.asList("test.txt", "FILE"));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);

        // get info
        Object fileInfoResponse = callServer("getFileInfo", Arrays.asList("test.txt"));
        assertInstanceOf(JSONObject.class, fileInfoResponse);
        String fileInfo = JSONObject.toJSONString((Map<String, ? extends Object>) fileInfoResponse);
        assertEquals("{\"path\":\"test.txt\",\"size\":0,\"name\":\"test.txt\"}", fileInfo);

        // write to file
        result = callServer("append", Arrays.asList("test.txt", "Hello world!"));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);

        // delete file
        result = callServer("delete", Arrays.asList("test.txt", true));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);
    }

    @Test
    void testConcurrentAppends() throws InterruptedException {

        // Create file
        Object result = callServer("create", Arrays.asList("test.txt", "FILE"));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);

        // generate a word of length > 8192 - buffer size in java.nio.file.Files
        String word =  String.join("", Collections.nCopies(100, "1234567890"));

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 10000; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    Object result = callServer("append", Arrays.asList("test.txt", word));
                    assertInstanceOf(Boolean.class, result);
                    assertTrue((Boolean) result);
                }
            });
        }

        executorService.shutdown();

        executorService.awaitTermination(1, TimeUnit.MINUTES);

        // read file
        Object text = callServer("read", Arrays.asList("test.txt", 0, 1000 * word.length()));
        assertInstanceOf(String.class, text);
        assertEquals(1000 * word.length(), ((String) text).length());
        assertEquals(String.join("", Collections.nCopies(1000, word)), ((String) text));

        // delete file
        result = callServer("delete", Arrays.asList("test.txt", true));
        assertInstanceOf(Boolean.class, result);
        assertTrue((Boolean) result);

    }

    // TODO change to map
    private Object callServer(String method, List<Object> positionalParams) {
        // Creating a new session to a JSON-RPC 2.0 web service at a specified URL

        // The JSON-RPC 2.0 server URL
        URL serverURL = null;

        try {
            serverURL = new URL("http://localhost:8080/fos");
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
