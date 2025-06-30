package com.fileservice.service;

import com.fileservice.dto.FileInfo;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class DirectoryListService {

    private final FileGetInfoService fileGetInfoService;

    @Inject
    public DirectoryListService(FileGetInfoService fileGetInfoService) {
        this.fileGetInfoService = fileGetInfoService;
    }

    public List<FileInfo> listDirectory(String path) throws IOException {
        // TODO improve exception handling
        return Files.list(Paths.get(path))
                .map(p -> {
                    try {
                        return fileGetInfoService.getFileInfo(p.toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

}
