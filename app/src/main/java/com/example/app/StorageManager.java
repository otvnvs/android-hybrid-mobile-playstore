package com.example.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class StorageManager {
    private static final String TAG = "JAVA_StorageManager";
    private final Context context;
    private final AppConfig config;

    public StorageManager(Context context, AppConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
        Log.d(TAG, "StorageManager tracking initialized.");
    }

    /**
     * Verified Scoped Storage workspace trace initializer.
     * Safely checks/creates directories inside public shared storage locations.
     */
    public void createPublicWorkspaceDirectory() {
        String folderName = config.getWorkspaceFolderName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "placeholder.txt");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/" + folderName + "/www");
            Uri externalUri = MediaStore.Files.getContentUri("external");
            try {
                Uri fileUri = resolver.insert(externalUri, values);
                if (fileUri != null) {
                    resolver.delete(fileUri, null, null);
                    Log.i(TAG, "MediaStore Workspace Checked: Documents/" + folderName + "/www");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error generating Scoped Storage trace: " + e.getMessage());
            }
        } else {
            File legacyDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (legacyDocs != null) {
                File workspace = new File(new File(legacyDocs, folderName), "www");
                if (!workspace.exists()) {
                    workspace.mkdirs();
                }
            }
        }
    }

    /**
     * DIRECTIONAL UTILITY 1: EXPORT
     * Safely clones private sandbox contents to the device public Documents directory.
     */
    public void migrateSandboxToPublic() {
        Log.i(TAG, "migrateSandboxToPublic() initiated. Exporting private sandbox files to public storage...");
        new Thread(() -> {
            try {
                File sourceSandboxDir = new File(context.getFilesDir(), "www");
                String folderName = config.getWorkspaceFolderName();
                File publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                
                if (publicDocsDir == null) {
                    throw new IOException("Public shared directory space is currently unavailable or unmounted.");
                }
                if (!sourceSandboxDir.exists()) {
                    throw new IOException("Source internal sandbox path directory does not exist yet. Run an update first.");
                }

                File externalTargetDir = new File(new File(publicDocsDir, folderName), "www");
                Log.d(TAG, " -> Starting Sandbox-to-Public replication loop:");
                Log.d(TAG, "    [SRC Private Sandbox]: " + sourceSandboxDir.getAbsolutePath());
                Log.d(TAG, "    [DST Public Location]: " + externalTargetDir.getAbsolutePath());

                // Clean out old files in public destination to guarantee an exact clean mirror
                deleteDirectoryRecursiveHelper(externalTargetDir);
                copyDirectoryRecursive(sourceSandboxDir, externalTargetDir);
                
                Log.i(TAG, " -> SUCCESS: Sandbox assets successfully exported to public storage layout.");
            } catch (Exception e) {
                Log.e(TAG, " -> CRITICAL ERROR: Sandbox-to-Public asset migration encountered failure: " + e.getMessage());
            }
        }).start();
    }

    /**
     * DIRECTIONAL UTILITY 2: IMPORT
     * Ingests files modified in the device public folder directly back into private sandbox storage.
     */
    public void migratePublicToSandbox() {
        Log.i(TAG, "migratePublicToSandbox() initiated. Ingesting public storage files into private sandbox...");
        new Thread(() -> {
            try {
                String folderName = config.getWorkspaceFolderName();
                File publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                
                if (publicDocsDir == null) {
                    throw new IOException("Public shared directory space is currently unavailable or unmounted.");
                }

                File externalSourceDir = new File(new File(publicDocsDir, folderName), "www");
                if (!externalSourceDir.exists()) {
                    throw new IOException("Source public documents directory does not exist yet at: " + externalSourceDir.getAbsolutePath());
                }

                File targetSandboxDir = new File(context.getFilesDir(), "www");
                Log.d(TAG, " -> Starting Public-to-Sandbox replication loop:");
                Log.d(TAG, "    [SRC Public Location]: " + externalSourceDir.getAbsolutePath());
                Log.d(TAG, "    [DST Private Sandbox]: " + targetSandboxDir.getAbsolutePath());

                // Clean out old sandbox assets to ensure a pristine configuration sync overwrite
                deleteDirectoryRecursiveHelper(targetSandboxDir);
                copyDirectoryRecursive(externalSourceDir, targetSandboxDir);
                
                Log.i(TAG, " -> SUCCESS: Public asset revisions successfully imported into secure app private storage.");
            } catch (Exception e) {
                Log.e(TAG, " -> CRITICAL ERROR: Public-to-Sandbox asset migration encountered failure: " + e.getMessage());
            }
        }).start();
    }

    private void copyDirectoryRecursive(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw new IOException("Failed to create destination workspace branch node: " + destination.getAbsolutePath());
            }
            String[] layoutItems = source.list();
            if (layoutItems != null) {
                for (String item : layoutItems) {
                    copyDirectoryRecursive(new File(source, item), new File(destination, item));
                }
            }
        } else {
            File parentDir = destination.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
                 FileChannel destChannel = new FileOutputStream(destination).getChannel()) {
                Log.v(TAG, "    Replicating payload item: " + source.getName());
                sourceChannel.transferTo(0, sourceChannel.size(), destChannel);
            }
        }
    }

    private void deleteDirectoryRecursiveHelper(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            File[] files = fileOrDir.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteDirectoryRecursiveHelper(child);
                }
            }
        }
        fileOrDir.delete();
    }

    public String determineStartupPath() {
        // Look up workspace file nodes in complete synchronization with your AppConfig storage settings
        if (config != null && config.isPublicWorkspaceEnabled()) {
            String folderName = config.getWorkspaceFolderName();
            File publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File workspaceIndex = (publicDocsDir != null) ? new File(new File(publicDocsDir, folderName), "www/index.html") : null;
            if (workspaceIndex != null && workspaceIndex.exists() && workspaceIndex.isFile()) {
                return "/index.html";
            }
        }

        File sandboxIndex = new File(context.getFilesDir(), "www/index.html");
        if (sandboxIndex.exists() && sandboxIndex.isFile()) {
            return "/index.html";
        }

        try {
            String[] assetsList = context.getAssets().list("www");
            if (assetsList != null) {
                for (String file : assetsList) {
                    if ("index.html".equals(file)) {
                        return "/index.html";
                    }
                }
            }
        } catch (Exception ignored) {}

        return "/error.html";
    }

//    public void syncSandboxToExternal() {//todo:temporary wrapper, remove later
//        Log.w(TAG, " -> Legacy syncSandboxToExternal() wrapper invoked. Redirecting to migrateSandboxToPublic().");
//        migrateSandboxToPublic();
//    }
}

