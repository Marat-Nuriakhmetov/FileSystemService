package com.fileservice.service;

import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Singleton
public class FileCopyService {
    public boolean copy(String sourcePath, String targetPath) throws IOException {
        // TODO exception handling
        Files.copy(Paths.get(sourcePath), Paths.get(targetPath));
        return true;
    }
}
