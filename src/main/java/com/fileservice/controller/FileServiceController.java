package com.fileservice.controller;

import com.fileservice.dto.EntryType;
import com.fileservice.dto.FileInfo;
import com.fileservice.service.DirectoryListService;
import com.fileservice.service.FileCopyService;
import com.fileservice.service.FileCreateService;
import com.fileservice.service.FileDeleteService;
import com.fileservice.service.FileGetInfoService;
import com.fileservice.service.FileMoveService;
import com.fileservice.service.FileReadService;
import com.fileservice.service.FileWriteService;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService;
import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@JsonRpcService
@AllArgsConstructor
@NoArgsConstructor(force = true) // TODO improve DI
public class FileServiceController {

    @Inject
    private final FileGetInfoService fileGetInfoService;
    @Inject
    private final DirectoryListService directoryListService;
    @Inject
    private final FileCreateService fileCreateService;
    @Inject
    private final FileDeleteService fileDeleteService;
    @Inject
    private final FileMoveService fileMoveService;
    @Inject
    private final FileCopyService fileCopyService;
    @Inject
    private final FileWriteService fileWriteService;
    @Inject
    private final FileReadService fileReadService;

    @JsonRpcMethod
    public FileInfo getFileInfo(@JsonRpcParam("path") String path) throws IOException {
        return fileGetInfoService.getFileInfo(path);
    }

    @JsonRpcMethod
    public List<FileInfo> listDirectory(@JsonRpcParam("path") String path) throws IOException {
        return directoryListService.listDirectory(path);
    }

    @JsonRpcMethod
    public boolean create(
            @JsonRpcParam("path") String path,
            @JsonRpcParam("type") EntryType entryType
    ) throws IOException {
        switch (entryType) {
            case FILE -> fileCreateService.createFile(path);
            case DIRECTORY -> fileCreateService.createDirectory(path);
        }
        return true;
    }

    @JsonRpcMethod
    public boolean delete(
            @JsonRpcParam("path") String path,
            @JsonRpcParam("recursive") boolean recursive
    ) throws IOException {
        return fileDeleteService.delete(path, recursive);
    }

    @JsonRpcMethod
    public boolean move(
            @JsonRpcParam("sourcePath") String sourcePath,
            @JsonRpcParam("targetPath") String targetPath
    ) throws IOException {
        return fileMoveService.move(sourcePath, targetPath);
    }

    @JsonRpcMethod
    public boolean copy(
            @JsonRpcParam("sourcePath") String sourcePath,
            @JsonRpcParam("targetPath") String targetPath
    ) throws IOException {
        return fileCopyService.copy(sourcePath, targetPath);
    }

    @JsonRpcMethod
    public boolean append(
            @JsonRpcParam("path") String path,
            @JsonRpcParam("data") String data
    ) throws IOException {
        return fileWriteService.append(path, data);
    }

    @JsonRpcMethod
    public String read(
            @JsonRpcParam("path") String path,
            @JsonRpcParam("offset") long offset,
            @JsonRpcParam("length") int length
    ) throws FileNotFoundException {
        return fileReadService.read(path, offset, length);
    }

}
