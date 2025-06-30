package com.fileservice.service;

import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Singleton
public class FileCreateService {

    public boolean createFile(String path) throws IOException {
        // TODO add try catch for false
        Files.createFile(Paths.get(path));
        return true;
    }

    public boolean createDirectory(String path) throws IOException {
        // TODO add try catch for false
        Files.createDirectory(Paths.get(path));
        return true;
    }
}
