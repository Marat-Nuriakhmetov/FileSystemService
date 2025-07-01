package com.fileservice.dto;

/**
 * Enumeration representing the type of file system entries.
 * Provides type information and utility methods for file system operations.
 */
public enum EntryType {
    /**
     * Represents a regular file in the file system.
     * This includes text files, binary files, and other non-directory entries.
     */
    FILE,

    /**
     * Represents a directory in the file system.
     * Directories can contain other files and directories.
     */
    DIRECTORY;

    /**
     * Determines the EntryType based on whether the entry is a directory.
     *
     * @param isDirectory true if the entry is a directory, false otherwise
     * @return FILE if not a directory, DIRECTORY if it is
     */
    public static EntryType fromIsDirectory(boolean isDirectory) {
        return isDirectory ? DIRECTORY : FILE;
    }

    /**
     * Checks if this entry type represents a file.
     *
     * @return true if this type is FILE, false otherwise
     */
    public boolean isFile() {
        return this == FILE;
    }

    /**
     * Checks if this entry type represents a directory.
     *
     * @return true if this type is DIRECTORY, false otherwise
     */
    public boolean isDirectory() {
        return this == DIRECTORY;
    }

    /**
     * Returns a human-readable description of the entry type.
     *
     * @return "File" for FILE, "Directory" for DIRECTORY
     */
    public String getDescription() {
        return switch (this) {
            case FILE -> "File";
            case DIRECTORY -> "Directory";
        };
    }
}