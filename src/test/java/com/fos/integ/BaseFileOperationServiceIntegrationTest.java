package com.fos.integ;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public abstract class BaseFileOperationServiceIntegrationTest {

    final String fosServerBaseUrl = System.getenv("FOS_SERVER_BASE_URL");

    final ObjectMapper objectMapper = new ObjectMapper();

    private final JSONRPC2Session session = createSession();

    protected BaseFileOperationServiceIntegrationTest() throws MalformedURLException {
    }

    <T> T callServerAndVCastnResult(String method, List<Object> positionalParams, Class<T> clazz) throws JSONRPC2SessionException {
        Object result = callServer(method, positionalParams);
        assertInstanceOf(clazz, result);
        return clazz.cast(result);
    }

    Object callServer(String method, List<Object> positionalParams) throws JSONRPC2SessionException {
        int requestID = 0;
        JSONRPC2Request request = new JSONRPC2Request(method, positionalParams, requestID);
        // Send request
        JSONRPC2Response response = session.send(request);;
        return response.getResult();
    }

    private JSONRPC2Session createSession() throws MalformedURLException {
        // Creating a new session to a JSON-RPC 2.0 web service at a specified URL

        // The JSON-RPC 2.0 server URL
        URL serverURL = new URL(fosServerBaseUrl + "/fos");;

        // Create new JSON-RPC 2.0 client session

        // Once the client session object is created, you can use to send a series
        // of JSON-RPC 2.0 requests and notifications to it.

        return new JSONRPC2Session(serverURL);
    }
}
