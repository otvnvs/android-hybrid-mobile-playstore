package com.example.app.services.maintenance;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.example.app.MainActivity;
import com.example.app.AppConfig;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import com.example.app.services.StorageService;
import android.os.StatFs;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FsController {
    private static final String TAG = "FsController";
    private final StorageService storageService = new StorageService();

    public FsController() {}

    /**
     * Refactored Storage Router Engine baseline anchors.
     * Aligns the base workspace targets with the root device partitions.
     */
    private File getStorageRoot(RequestContext request) {
        AppConfig config = request.getAppConfig();
        Context context = request.getAndroidContext();
        
        if (config != null && config.isPublicWorkspaceEnabled()) {
            // Public Strategy Anchor: Standard system base partition point
            return Environment.getExternalStorageDirectory(); // /storage/emulated/0
        } else {
            // Private Sandbox Anchor: Standard isolated data cache base partition point
            return context.getFilesDir(); // /data/user/0/com.example.app/files
        }
    }

    private File resolveSafeFile(RequestContext request, String relativePath) throws IOException {
        File root = getStorageRoot(request);
        if (relativePath == null || relativePath.isEmpty()) {
            return root;
        }
        
        File target = new File(root, relativePath);
        // Canonical guardrail to verify lookups stay strictly inside active strategy boundaries
        if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
            throw new SecurityException("Directory traversal validation escape attempt blocked cleanly.");
        }
        return target;
    }

    @RequestMapping(path="/api/fs/webroot", method="GET")
    public ResponseContext getActiveWebRootProperties(RequestContext request) {
        Log.i(TAG, " -> REST API [GET]: Resolving relative web-root path modifiers.");
        try {
            AppConfig config = request.getAppConfig();
            String relativeWebRootPathModifier = "";

            if (config != null && config.isPublicWorkspaceEnabled()) {
                // Public Shared Documents Mode Strategy: Documents/[WorkspaceFolderName]/www
                relativeWebRootPathModifier = "Documents/" + config.getWorkspaceFolderName() + "/www";
            } else {
                // ◄ STRUCTURAL ALIGNMENT WIN: Explicitly return the "www" sub-folder token 
                // to perfectly mirror the native baseline partition layouts!
                relativeWebRootPathModifier = "www"; 
            }

            org.json.JSONObject result = new org.json.JSONObject();
            result.put("status", "success");
            result.put("web_root_path", relativeWebRootPathModifier);
            
            return ResponseContext.status(200)
                    .contentType("application/json")
                    .body(result.toString())
                    .build();
        } catch (Exception e) {
            return ResponseContext.status(500)
                    .body("{\"status\":\"error\",\"message\":\"Web root query layer crash: " + e.getMessage() + "\"}").build();
        }
    }


    @RequestMapping(path="/api/fs/locations", method="GET")
    public ResponseContext getStorageLocations(RequestContext request) {
        try {
            Context context = request.getAndroidContext();
            JSONObject locations = new JSONObject();
            File activeRoot = getStorageRoot(request);
            
            locations.put("external_storage_root", activeRoot.getAbsolutePath());
            
            if (context != null) {
                File sandboxDbDir = context.getDatabasePath("probe.db").getParentFile();
                if (sandboxDbDir != null) {
                    if (!sandboxDbDir.exists()) sandboxDbDir.mkdirs();
                    locations.put("sandbox_databases_root", sandboxDbDir.getAbsolutePath());
                } else {
                    locations.put("sandbox_databases_root", "unknown_path_error");
                }
                locations.put("package_name", context.getPackageName());
            } else {
                locations.put("sandbox_databases_root", "context_unavailable");
            }
            
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("locations", locations);
            return ResponseContext.status(200).contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(500, "Failed to resolve storage directories: " + e.getMessage());
        }
    }

    @RequestMapping(path="/api/fs/list", method="GET")
    public ResponseContext listDirectory(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path");
            File targetDir = resolveSafeFile(request, pathQuery);
            JSONArray contents = storageService.readDirectory(targetDir);
            
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("files", contents);
            return ResponseContext.status(200).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(400, "Failed listing directory: " + e.getMessage());
        }
    }
    @RequestMapping(path="/api/fs/read", method="GET")
    public ResponseContext readFileContent(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path");
            File targetFile = resolveSafeFile(request, pathQuery);
            byte[] fileData = storageService.readFile(targetFile);
            
            String mimeType = "application/octet-stream";
            if (targetFile.getName().endsWith(".txt")) mimeType = "text/plain";
            if (targetFile.getName().endsWith(".json")) mimeType = "application/json";
            
            return ResponseContext.status(200).contentType(mimeType).body(fileData).build();
        } catch (Exception e) {
            return buildErrorResponse(404, "Failed reading file: " + e.getMessage());
        }
    }

    @RequestMapping(path="/api/fs/mkdir", method="POST")
    public ResponseContext createDirectory(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path");
            String recursiveStr = request.getQueryParam("recursive");
            boolean recursive = "true".equalsIgnoreCase(recursiveStr);
            
            File targetDir = resolveSafeFile(request, pathQuery);
            boolean success = storageService.createDirectory(targetDir, recursive);
            
            JSONObject result = new JSONObject();
            result.put("status", success ? "success" : "error");
            result.put("message", success ? "Directory matched/created." : "Could not create directory structural layout.");
            return ResponseContext.status(success ? 200 : 500).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(400, "Directory processing failure: " + e.getMessage());
        }
    }

    @RequestMapping(path="/api/fs/write", method="POST")
    public ResponseContext createOrWriteFile(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path");
            File targetFile = resolveSafeFile(request, pathQuery);
            byte[] dataPayload = request.getBody();
            if (dataPayload == null) {
                dataPayload = new byte[0];
            }
            
            storageService.createFile(targetFile, dataPayload);
            JSONObject result = new JSONObject();
            result.put("status", "success");
            return ResponseContext.status(200).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(500, "File persist error: " + e.getMessage());
        }
    }

    @RequestMapping(path="/api/fs/delete", method="DELETE")
    public ResponseContext deleteFileSystemPath(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path");
            String recursiveStr = request.getQueryParam("recursive");
            boolean recursive = "true".equalsIgnoreCase(recursiveStr);
            
            File targetFile = resolveSafeFile(request, pathQuery);
            if (targetFile.getCanonicalPath().equals(getStorageRoot(request).getCanonicalPath())) {
                return buildErrorResponse(403, "Forbidden: Cannot delete the storage environment root context.");
            }
            
            boolean success = storageService.deletePath(targetFile, recursive);
            JSONObject result = new JSONObject();
            result.put("status", success ? "success" : "error");
            result.put("message", success ? "Deleted resource cleanly." : "Failed completely clearing resource targets.");
            return ResponseContext.status(success ? 200 : 500).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(400, "Resource cleaning failure: " + e.getMessage());
        }
    }

    @RequestMapping(path="/api/fs/diskspace", method="GET")
    public ResponseContext getDiskSpaceDiagnostics(RequestContext request) {
        try {
            JSONObject root = new JSONObject();
            Context context = request.getAndroidContext();
            
            JSONObject internalStorage = new JSONObject();
            File internalPath = Environment.getDataDirectory();
            StatFs internalStat = new StatFs(internalPath.getPath());
            
            long blockSizeInt = android.os.Build.VERSION.SDK_INT >= 18 ? internalStat.getBlockSizeLong() : internalStat.getBlockSize();
            long availableBlocksInt = android.os.Build.VERSION.SDK_INT >= 18 ? internalStat.getAvailableBlocksLong() : internalStat.getAvailableBlocks();
            long totalBlocksInt = android.os.Build.VERSION.SDK_INT >= 18 ? internalStat.getBlockCountLong() : internalStat.getBlockCount();
            
            internalStorage.put("partition_path", internalPath.getAbsolutePath());
            internalStorage.put("total_space_bytes", totalBlocksInt * blockSizeInt);
            internalStorage.put("available_space_bytes", availableBlocksInt * blockSizeInt);
            internalStorage.put("status", "success");
            root.put("internal_partition", internalStorage);
            
            JSONObject secondaryStorage = new JSONObject();
            boolean sdCardDetected = false;
            String sdCardPath = "unmounted";
            long sdTotalBytes = 0;
            long sdAvailBytes = 0;
            
            if (context != null) {
                File[] externalDirs = context.getExternalFilesDirs(null);
                if (externalDirs != null && externalDirs.length > 1 && externalDirs[1] != null) {
                    File sdFile = externalDirs[1];
                    String rawSdPath = sdFile.getAbsolutePath();
                    int androidIndex = rawSdPath.indexOf("/Android");
                    if (androidIndex != -1) {
                        File sdRoot = new File(rawSdPath.substring(0, androidIndex));
                        if (sdRoot.exists()) {
                            StatFs sdStat = new StatFs(sdRoot.getPath());
                            long blockSizeSd = android.os.Build.VERSION.SDK_INT >= 18 ? sdStat.getBlockSizeLong() : sdStat.getBlockSize();
                            long availableBlocksSd = android.os.Build.VERSION.SDK_INT >= 18 ? sdStat.getAvailableBlocksLong() : sdStat.getAvailableBlocks();
                            long totalBlocksSd = android.os.Build.VERSION.SDK_INT >= 18 ? sdStat.getBlockCountLong() : sdStat.getBlockCount();
                            
                            sdCardDetected = true;
                            sdCardPath = sdRoot.getAbsolutePath();
                            sdTotalBytes = totalBlocksSd * blockSizeSd;
                            sdAvailBytes = availableBlocksSd * blockSizeSd;
                        }
                    }
                }
            }
            
            secondaryStorage.put("removable_sdcard_mounted", sdCardDetected);
            secondaryStorage.put("partition_path", sdCardPath);
            secondaryStorage.put("total_space_bytes", sdTotalBytes);
            secondaryStorage.put("available_space_bytes", sdAvailBytes);
            root.put("secondary_partition", secondaryStorage);
            
            JSONObject cacheInfo = new JSONObject();
            if (context != null) {
                File cacheDir = context.getCacheDir();
                File codeCacheDir = android.os.Build.VERSION.SDK_INT >= 21 ? context.getCodeCacheDir() : null;
                long totalCacheUsage = 0;
                
                if (cacheDir != null && cacheDir.exists()) totalCacheUsage += calculateDirSizeHelper(cacheDir);
                if (codeCacheDir != null && codeCacheDir.exists()) totalCacheUsage += calculateDirSizeHelper(codeCacheDir);
                
                cacheInfo.put("sandbox_cache_path", cacheDir != null ? cacheDir.getAbsolutePath() : "unknown");
                cacheInfo.put("active_cache_usage_bytes", totalCacheUsage);
                cacheInfo.put("status", "success");
            } else {
                cacheInfo.put("status", "Context unavailable");
            }
            root.put("app_sandbox_cache", cacheInfo);
            
            return ResponseContext.status(200).contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .body(root.toString()).build();
        } catch (Exception e) {
            Log.e(TAG, "Disk space analytics engine execution crash", e);
            return buildErrorResponse(500, "Disk inspection failure: " + e.getMessage());
        }
    }

    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code).body(errJson.toString()).build();
    }

    private long calculateDirSizeHelper(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += calculateDirSizeHelper(file);
                }
            }
        }
        return size;
    }
}

