package com.fileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object (DTO) representing file system entry information.
 * This class encapsulates various attributes of a file, directory, or symbolic link
 * in the file system, providing a comprehensive view of the entry's properties.
 *
 * <p>Properties include:
 * <ul>
 *     <li>Basic information (name, path, size)</li>
 *     <li>Type indicators (file, directory, symbolic link)</li>
 *     <li>Timestamps (creation, modification, access)</li>
 * </ul>
 *
 * <p>Note: All fields are final to ensure immutability after construction.
 * The class uses Lombok annotations to generate boilerplate code.
 *
 * @see lombok.Data
 * @see lombok.AllArgsConstructor
 */
@Data
@AllArgsConstructor
@Builder
public class FileInfo {

    /**
     * The name of the file or directory.
     * This is typically the last component of the path.
     */
    private final String name;

    /**
     * The complete path to the file or directory.
     * This represents the absolute or relative path depending on how it was created.
     */
    private final String path;

    /**
     * The size of the file in bytes.
     * For directories, this might not represent the total content size.
     */
    private final long size;

    /**
     * Indicates whether this entry is a directory.
     * True if the entry is a directory, false otherwise.
     */
    private final boolean isDirectory;

    /**
     * Indicates whether this entry is a regular file.
     * True if the entry is a regular file, false for directories or special files.
     */
    private final boolean isRegularFile;

    /**
     * Indicates whether this entry is a symbolic link.
     * True if the entry is a symbolic link, false otherwise.
     */
    private final boolean isSymbolicLink;

    /**
     * The last modification time in milliseconds since the epoch.
     * Represents when the file's content or metadata was last changed.
     */
    private final long lastModifiedTime;

    /**
     * The creation time in milliseconds since the epoch.
     * Represents when the file was created in the file system.
     */
    private final long creationTime;

    /**
     * The last access time in milliseconds since the epoch.
     * Represents when the file was last read or executed.
     */
    private final long lastAccessTime;
}
