package com.example.app.services;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StorageService {

    /**
     * Lists files and folders in a target directory path.
     */
    public JSONArray readDirectory(File dir) throws IOException {
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IOException("Target is not a valid directory or does not exist.");
        }
        JSONArray filesArray = new JSONArray();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                JSONObject item = new JSONObject();
                try {
                    item.put("name", file.getName());
                    item.put("isDirectory", file.isDirectory());
                    item.put("size", file.length());
                    item.put("lastModified", file.lastModified());
                } catch (Exception ignored) {}
                filesArray.put(item);
            }
        }
        return filesArray;
    }

    /**
     * Reads a file's raw content into a byte array.
     */
    public byte[] readFile(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Target is not a valid file or does not exist.");
        }
        byte[] fileBytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int totalRead = 0;
            while (totalRead < fileBytes.length) {
                int read = fis.read(fileBytes, totalRead, fileBytes.length - totalRead);
                if (read == -1) break;
                totalRead += read;
            }
        }
        return fileBytes;
    }

    /**
     * Creates a directory (supports recursive structures with .mkdirs()).
     */
    public boolean createDirectory(File dir, boolean recursive) {
        if (dir.exists()) return true;
        return recursive ? dir.mkdirs() : dir.mkdir();
    }

    /**
     * Writes binary payload contents to a target file destination.
     */
    public void createFile(File file, byte[] content) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs(); // Ensure path exists before writing file
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        }
    }

    /**
     * Deletes files or directories (supports recursive deletion).
     */
    public boolean deletePath(File target, boolean recursive) {
        if (!target.exists()) return true;
        if (recursive && target.isDirectory()) {
            File[] children = target.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deletePath(child, true)) {
                        return false;
                    }
                }
            }
        }
        return target.delete();
    }
}

