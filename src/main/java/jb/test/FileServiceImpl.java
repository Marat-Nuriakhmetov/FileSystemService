package jb.test;

import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService;

// Service implementation
@JsonRpcService
public class FileServiceImpl implements FileService {

    @Override
    @JsonRpcMethod
    public String getFileInfo(@JsonRpcParam("path") String path) {
        return "File info for: " + path;
    }

    @Override
    @JsonRpcMethod
    public boolean createFile(String path) {
        return true;
    }
}