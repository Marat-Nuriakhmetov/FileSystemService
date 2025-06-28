package jb.test;

import jb.test.dto.FileInfo;

// Service interface
@SuppressWarnings("unused")
public interface FileService {
    FileInfo getFileInfo(String path);
    boolean createFile(String path);
}