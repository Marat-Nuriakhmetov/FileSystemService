package com.fos.integ;

import com.fos.dto.FileInfo;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "INTEG_TEST", matches = "ENABLED")
public class FileOperationServiceNonAppendIntegrationTest extends BaseFileOperationServiceIntegrationTest {

    public FileOperationServiceNonAppendIntegrationTest() throws MalformedURLException {

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

}
