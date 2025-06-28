package jb.test;

// Service interface
@SuppressWarnings("unused")
public interface FileService {
    String getFileInfo(String path);
    boolean createFile(String path);
}