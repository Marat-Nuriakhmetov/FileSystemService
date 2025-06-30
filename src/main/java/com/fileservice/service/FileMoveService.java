package com.fileservice.service;

import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Singleton
public class FileMoveService {
    public boolean move(String sourcePath, String targetPath) throws IOException {
        // TODO exception handling
        Files.move(Paths.get(sourcePath), Paths.get(targetPath));
        return true;
    }

}
