package com.fileservice.util;

import com.fileservice.dto.FileInfo;

import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;

/**
 * Utility class for generating random FileInfo objects for testing purposes.
 */
public class FileInfoGenerator {
    private static final Random random = new Random();
    private static final String[] FILE_EXTENSIONS = {".txt", ".pdf", ".doc", ".jpg", ".png"};
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024; // 1GB

    /**
     * Generates a random FileInfo object.
     *
     * @return randomly generated FileInfo
     */
    public static FileInfo generateRandom() {
        return generateRandom(null);
    }

    /**
     * Generates a random FileInfo object with the specified base path.
     *
     * @param basePath the base path for the generated FileInfo
     * @return randomly generated FileInfo
     */
    public static FileInfo generateRandom(Path basePath) {
        String name = generateRandomName();
        String path = basePath != null
                ? basePath.resolve(name).toString()
                : "/tmp/" + name;

        return new FileInfo(
                name,                           // name
                path,                          // path
                generateRandomSize()          // size
        );
    }

    /**
     * Generates a random file name with extension.
     *
     * @return random file name
     */
    private static String generateRandomName() {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = random.nextBoolean()
                ? FILE_EXTENSIONS[random.nextInt(FILE_EXTENSIONS.length)]
                : "";
        return uuid + extension;
    }

    /**
     * Generates a random file size.
     *
     * @return random file size
     */
    private static long generateRandomSize() {
        return Math.abs(random.nextLong()) % MAX_FILE_SIZE;
    }

    /**
     * Generates a random timestamp within the last year.
     *
     * @return random timestamp
     */
    private static long generateRandomTimestamp() {
        long now = System.currentTimeMillis();
        long oneYearAgo = now - (365L * 24 * 60 * 60 * 1000);
        return oneYearAgo + (long) (random.nextDouble() * (now - oneYearAgo));
    }

    /**
     * Builder class for creating FileInfo objects with specific attributes.
     */
    public static class Builder {
        private String name;
        private String path;
        private long size;
        private boolean isDirectory;
        private boolean isRegularFile;
        private boolean isSymbolicLink;
        private long lastModifiedTime;
        private long creationTime;
        private long lastAccessTime;

        public Builder() {
            // Initialize with random values
            FileInfo random = generateRandom();
            this.name = random.getName();
            this.path = random.getPath();
            this.size = random.getSize();
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPath(String path) {
            this.path = path;
            return this;
        }

        public Builder withSize(long size) {
            this.size = size;
            return this;
        }

        public Builder withDirectory(boolean isDirectory) {
            this.isDirectory = isDirectory;
            return this;
        }

        public Builder withRegularFile(boolean isRegularFile) {
            this.isRegularFile = isRegularFile;
            return this;
        }

        public Builder withSymbolicLink(boolean isSymbolicLink) {
            this.isSymbolicLink = isSymbolicLink;
            return this;
        }

        public Builder withLastModifiedTime(long lastModifiedTime) {
            this.lastModifiedTime = lastModifiedTime;
            return this;
        }

        public Builder withCreationTime(long creationTime) {
            this.creationTime = creationTime;
            return this;
        }

        public Builder withLastAccessTime(long lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
            return this;
        }

        public FileInfo build() {
            return new FileInfo(
                    name,
                    path,
                    size
            );
        }
    }
}