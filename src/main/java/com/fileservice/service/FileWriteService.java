package com.fileservice.service;

import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class FileWriteService {

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public boolean append(String path, String data) throws IOException {
        // TODO exception handling
        Object lock = locks.computeIfAbsent(path, k -> new Object());
        synchronized (lock) {
            Files.writeString(
                    Paths.get(path),
                    data,
                    StandardOpenOption.APPEND
            );
        }
        // Optional: Remove lock if file is not being used
        locks.remove(path, lock);

        return true;
    }
}
