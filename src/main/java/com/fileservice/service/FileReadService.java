package com.fileservice.service;

import com.google.inject.Singleton;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

@Singleton
public class FileReadService {

    public String read(String path, long offset, int length) throws FileNotFoundException {
        // TODO exception handling
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }

        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative");
        }

        try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
            long fileLength = file.length();

            if (offset > fileLength) {
                throw new IllegalArgumentException("Offset beyond file size");
            }

            // Adjust length if it would read beyond EOF
            length = (int) Math.min(length, fileLength - offset);

            file.seek(offset);

            byte[] buffer = new byte[length];

            int bytesRead = file.read(buffer);

            if (bytesRead == -1) {
                return "";
            }
            return new String(buffer, 0, bytesRead);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + e.getMessage(), e);
        }

    }
}

