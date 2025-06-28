package jb.test;

import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService;
import jb.test.dto.FileInfo;

// Service implementation
@JsonRpcService
public class FileServiceImpl implements FileService {

    @Override
    @JsonRpcMethod
    public FileInfo getFileInfo(@JsonRpcParam("path") String path) {
        return new FileInfo("test", "path", 1234);
    }

    @Override
    @JsonRpcMethod
    public boolean createFile(String path) {
        return true;
    }

}