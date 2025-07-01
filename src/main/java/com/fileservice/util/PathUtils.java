package com.fileservice.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for path operations.
 */
public class PathUtils {

    /**
     * Returns a relative path by removing the root directory path.
     * For example, if root is "/home/user/root" and full path is "/home/user/root/folder/file.txt",
     * this method will return "folder/file.txt".
     *
     * @param rootPath the root directory path to remove
     * @param fullPath the full path from which to remove the root
     * @return the relative path, or the original path if it doesn't start with the root path
     * @throws IllegalArgumentException if either path is null
     */
    public static String getRelativePath(String rootPath, String fullPath) {
        if (rootPath == null || fullPath == null) {
            throw new IllegalArgumentException("Paths cannot be null");
        }

        Path root = Paths.get(rootPath).normalize();
        Path full = Paths.get(fullPath).normalize();

        if (full.startsWith(root)) {
            Path relative = root.relativize(full);
            return relative.toString();
        }

        return fullPath;
    }

    /**
     * Returns a relative path by removing the root directory path.
     *
     * @param rootPath the root directory path to remove
     * @param fullPath the full path from which to remove the root
     * @return the relative path, or the original path if it doesn't start with the root path
     * @throws IllegalArgumentException if either path is null
     */
    public static String getRelativePath(Path rootPath, Path fullPath) {
        if (rootPath == null || fullPath == null) {
            throw new IllegalArgumentException("Paths cannot be null");
        }

        Path normalizedRoot = rootPath.normalize();
        Path normalizedFull = fullPath.normalize();

        if (normalizedFull.startsWith(normalizedRoot)) {
            return normalizedRoot.relativize(normalizedFull).toString();
        }

        return fullPath.toString();
    }
}