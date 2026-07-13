package com.example.app.services.maintenance;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.example.app.AppConfig;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.json.JSONObject;
import org.json.JSONArray;

public class ArcController {
    private static final String TAG = "ArcController";
    private static final ConcurrentHashMap<String, ReentrantLock> zipSourceLocks = new ConcurrentHashMap<>();

    public ArcController() {}

    /**
     * ◄ DYNAMIC STORAGE STRATEGY ROUTER
     * Resolves the execution root folder path mapping based on the active panel selections.
     */
    private File getStorageRoot(RequestContext request) {
        AppConfig config = request.getAppConfig();
        Context context = request.getAndroidContext();
        if (config != null && config.isPublicWorkspaceEnabled()) {
            return Environment.getExternalStorageDirectory(); // /storage/emulated/0
        } else {
            return context.getFilesDir(); // /data/user/0/com.example.app/files
        }
    }

    private File resolveSafeFile(RequestContext request, String relativePath) throws IOException {
        File root = getStorageRoot(request);
        if (relativePath == null || relativePath.isEmpty()) return root;
        
        File target = new File(root, relativePath);
        // Canonical protection guardrail layer to check for directory traversal path manipulation
        if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
            throw new SecurityException("Directory traversal validation escape attempt blocked cleanly.");
        }
        return target;
    }

    private void zipRecursiveHelper(File rootFolder, File sourceFile, ZipOutputStream zos) throws IOException {
        if (sourceFile.isDirectory()) {
            File[] files = sourceFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    zipRecursiveHelper(rootFolder, file, zos);
                }
            }
        } else {
            byte[] buffer = new byte[4096];
            String relativePath = rootFolder.toURI().relativize(sourceFile.toURI()).getPath();
            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                zos.putNextEntry(new ZipEntry(relativePath));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            }
        }
    }

    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code).contentType("application/json").body(errJson.toString()).build();
    }

    private String stripPathComponents(String path, int levelsToStrip) {
        if (levelsToStrip == 0 || path == null || path.isEmpty()) {
            return path;
        }
        String unifiedPath = path.replace('\\', '/');
        String[] segments = unifiedPath.split("/");
        if (segments.length <= levelsToStrip) {
            return null;
        }
        StringBuilder rebuiltPath = new StringBuilder();
        for (int i = levelsToStrip; i < segments.length; i++) {
            if (rebuiltPath.length() > 0) {
                rebuiltPath.append(File.separator);
            }
            rebuiltPath.append(segments[i]);
        }
        if (unifiedPath.endsWith("/") && rebuiltPath.length() > 0) {
            rebuiltPath.append(File.separator);
        }
        return rebuiltPath.toString();
    }

    @RequestMapping(path="/api/arc/unzip", method="POST")
    public ResponseContext unzipArchiveFile(RequestContext request) {
        ReentrantLock sourceLock = null;
        try {
            String jsonConfig = new String(request.getBody(), StandardCharsets.UTF_8);
            JSONObject unzipRequest = new JSONObject(jsonConfig);
            String zipPath = unzipRequest.getString("zipPath");
            String targetDirectoryPath = unzipRequest.getString("targetDirectory");
            int stripComponents = unzipRequest.optInt("stripComponents", 0);
            if (stripComponents < 0) stripComponents = 0;

            File zipFile = resolveSafeFile(request, zipPath);
            File targetDir = resolveSafeFile(request, targetDirectoryPath);

            if (!zipFile.exists() || !zipFile.isFile()) {
                return buildErrorResponse(404, "Source ZIP file archive resource not found on local disk.");
            }

            String sourceKey = zipFile.getCanonicalPath();
            sourceLock = zipSourceLocks.computeIfAbsent(sourceKey, k -> new ReentrantLock());
            sourceLock.lock();

            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry entry;
                byte[] buffer = new byte[4096];
                while ((entry = zis.getNextEntry()) != null) {
                    String strippedName = stripPathComponents(entry.getName(), stripComponents);
                    if (strippedName == null || strippedName.isEmpty()) {
                        zis.closeEntry();
                        continue;
                    }
                    
                    File newFile = resolveSafeFile(request, targetDirectoryPath + File.separator + strippedName);
                    if (!newFile.getCanonicalPath().startsWith(targetDir.getCanonicalPath())) {
                        throw new SecurityException("Zip Slip directory traversal attack attempt blocked: " + entry.getName());
                    }

                    if (entry.isDirectory()) {
                        if (!newFile.exists()) newFile.mkdirs();
                    } else {
                        File parent = newFile.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("message", "Archive successfully extracted onto native filesystem.");
            result.put("targetDirectory", targetDirectoryPath);
            result.put("componentsStripped", stripComponents);
            return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();

        } catch (SecurityException se) {
            return buildErrorResponse(403, "Directory traversal extraction safety boundary violation: " + se.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(500, "Native extraction extraction execution layer crash: " + e.getMessage());
        } finally {
            if (sourceLock != null && sourceLock.isHeldByCurrentThread()) {
                sourceLock.unlock();
            }
        }
    }
    @RequestMapping(path="/api/arc/zip", method="POST")
    public ResponseContext zipDirectoryOrFile(RequestContext request) {
        try {
            String jsonConfig = new String(request.getBody(), StandardCharsets.UTF_8);
            JSONObject zipRequest = new JSONObject(jsonConfig);
            String sourcePath = zipRequest.getString("sourcePath");
            String targetZipPath = zipRequest.getString("targetZipPath");

            File sourceFile = resolveSafeFile(request, sourcePath);
            File targetZipFile = resolveSafeFile(request, targetZipPath);

            if (!sourceFile.exists()) {
                return buildErrorResponse(404, "Source directory or file resource not found on local disk.");
            }

            File parentDir = targetZipFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(targetZipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                zipRecursiveHelper(sourceFile, sourceFile, zos);
            }

            if (targetZipPath.contains("Download")) {
                android.media.MediaScannerConnection.scanFile(
                    request.getAndroidContext(), 
                    new String[]{targetZipFile.getAbsolutePath()}, 
                    null, 
                    null
                );
            }

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("message", "Files compressed successfully into ZIP archive.");
            result.put("archiveSize", targetZipFile.length());
            return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();
        } catch (SecurityException se) {
            return buildErrorResponse(403, "Directory traversal compression boundary violation: " + se.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(500, "Native compression execution layer crash: " + e.getMessage());
        }
    }

    @RequestMapping(path="/api/arc/list", method="POST")
    public ResponseContext listArchiveContents(RequestContext request) {
        Log.d(TAG, "public ResponseContext listArchiveContents(RequestContext request)");
        ReentrantLock sourceLock = null;
        try {
            String jsonConfig = new String(request.getBody(), StandardCharsets.UTF_8);
            JSONObject listRequest = new JSONObject(jsonConfig);
            String zipPath = listRequest.getString("zipPath");
            int offset = listRequest.optInt("offset", 0);
            int limit = listRequest.optInt("limit", 100);
            String directoryPrefix = listRequest.optString("directoryPrefix", "");

            if (!directoryPrefix.isEmpty()) {
                directoryPrefix = directoryPrefix.replace('\\', '/');
                if (!directoryPrefix.endsWith("/")) {
                    directoryPrefix += "/";
                }
            }

            File zipFile = resolveSafeFile(request, zipPath);
            if (!zipFile.exists() || !zipFile.isFile()) {
                return buildErrorResponse(404, "Source ZIP file archive resource not found on local disk.");
            }

            String sourceKey = zipFile.getCanonicalPath();
            sourceLock = zipSourceLocks.computeIfAbsent(sourceKey, k -> new ReentrantLock());
            sourceLock.lock();

            JSONArray entriesArray = new JSONArray();
            int matchingIndex = 0;
            int itemsAdded = 0;
            int totalMatchingInZip = 0;

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName().replace('\\', '/');
                    if (!directoryPrefix.isEmpty() && !entryName.startsWith(directoryPrefix)) {
                        zis.closeEntry();
                        continue;
                    }
                    if (entryName.equals(directoryPrefix)) {
                        zis.closeEntry();
                        continue;
                    }

                    totalMatchingInZip++;

                    if (matchingIndex >= offset && itemsAdded < limit) {
                        JSONObject entryJson = new JSONObject();
                        entryJson.put("name", entry.getName());
                        entryJson.put("isDirectory", entry.isDirectory());
                        entryJson.put("size", entry.getSize());
                        entryJson.put("compressedSize", entry.getCompressedSize());
                        entryJson.put("crc", entry.getCrc());
                        entriesArray.put(entryJson);
                        itemsAdded++;
                    }
                    matchingIndex++;
                    zis.closeEntry();
                }
            }

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("zipPath", zipPath);
            result.put("directoryPrefix", directoryPrefix.isEmpty() ? null : directoryPrefix);
            result.put("offset", offset);
            result.put("limit", limit);
            result.put("count", itemsAdded);
            result.put("totalMatching", totalMatchingInZip);
            result.put("entries", entriesArray);
            return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();
        } catch (Exception e) {
            Log.d(TAG, "Native archive inspection layer crash: " + e.getMessage());
            return buildErrorResponse(500, "Native archive inspection layer crash: " + e.getMessage());
        } finally {
            finaliseListingLock(sourceLock);
        }
    }

    private void finaliseListingLock(ReentrantLock sourceLock) {
        if (sourceLock != null && sourceLock.isHeldByCurrentThread()) {
            sourceLock.unlock();
        }
    }

}
