package com.fileservice.service;

import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Singleton
public class FileDeleteService {
    public boolean delete(String path, boolean recursive) throws IOException {
        // TODO recursive?
        return Files.deleteIfExists(Paths.get(path));
    }
}
