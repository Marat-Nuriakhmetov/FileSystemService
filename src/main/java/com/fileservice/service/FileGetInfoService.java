package com.fileservice.service;

import com.fileservice.dto.FileInfo;
import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

@Singleton
public class FileGetInfoService {

    public FileInfo getFileInfo(String path) throws IOException {
        // TODO exception handling
        Path file = Paths.get(path);
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        return new FileInfo(
                file.getFileName().toString(),
                path,
                attrs.size()
        );
    }
}
