package com.example.app;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import android.os.Environment;

public class UpdateManager {
    private static final String TAG = "JS_CONSOLE_JAVA_UpdateManager";
    private final Context context;
    private final AppConfig config;
    private static volatile String currentStatusMessage = "Idle";

    public interface OnUpdateCompleteListener {
        void onUpdateFinished();
    }

    public UpdateManager(Context context, AppConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
        Log.d(TAG, "UpdateManager system worker monitoring initialized.");
    }

    public void startZipDownload(final OnUpdateCompleteListener listener) {
        Log.i(TAG, "startZipDownload() invoked. Launching remote network pipeline thread...");
        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            try {
                String targetUrlStr = config.getUpdateTargetUrl();
                if (targetUrlStr == null || targetUrlStr.isEmpty()) {
                    throw new IOException("Aborting network task: Remote Update URL property configuration is empty.");
                }
                Log.d(TAG, " -> Connecting to target endpoint: " + targetUrlStr);
                URL url = new URL(targetUrlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestMethod("GET");

                if (config.useAuthentication()) {
                    Log.d(TAG, " -> Injecting dynamic Basic Authentication credentials headers matrix.");
                    String authStr = config.getAuthUsername() + ":" + config.getAuthPassword();
                    String base64Auth = Base64.encodeToString(authStr.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                    connection.setRequestProperty("Authorization", "Basic " + base64Auth);
                }

                currentStatusMessage = "Downloading archive...";
                connection.connect();
                int responseCode = connection.getResponseCode();
                Log.d(TAG, " -> Server endpoint responded with code signature: " + responseCode);
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Server returned invalid response state: " + responseCode + " - " + connection.getResponseMessage());
                }

                File tempZipFile = new File(context.getCacheDir(), "remote_deployment_package.zip");
                if (tempZipFile.exists()) tempZipFile.delete();

                inputStream = new BufferedInputStream(connection.getInputStream());
                outputStream = new FileOutputStream(tempZipFile);
                byte[] dataBuffer = new byte[4096];
                int bytesRead;
                long totalBytesDownloaded = 0;
                
                Log.i(TAG, " -> Streaming remote packet payload chunks down into internal cache storage cell...");
                while ((bytesRead = inputStream.read(dataBuffer, 0, 4096)) != -1) {
                    totalBytesDownloaded += bytesRead;
                    outputStream.write(dataBuffer, 0, bytesRead);
                }
                outputStream.flush();
                Log.i(TAG, " -> Binary packet stream fetch complete. Bytes Written: " + totalBytesDownloaded);

                File sandboxDir = new File(context.getFilesDir(), "www");
                Log.i(TAG, " -> Initiating sandbox extraction pipeline into: " + sandboxDir.getAbsolutePath());
                
                currentStatusMessage = "Extracting files...";
                extractZipToSandbox(tempZipFile, sandboxDir);

                // ◄ CONFIG-DRIVEN STORAGE UPDATE MATRIX LOGIC TREE
                if (config.isPublicWorkspaceEnabled()) {
                    currentStatusMessage = "Synchronizing storage tiers...";
                    Log.i(TAG, " -> Public workspace enabled. Triggering directory sync to public storage.");
                    syncSandboxToExternalFolder(sandboxDir);
                } else {
                    Log.i(TAG, " -> Public workspace disabled. Skipping automated SD Card directory replication sync.");
                }

                if (tempZipFile.exists()) tempZipFile.delete();
                Log.i(TAG, " -> App Workspace fully updated across storage tiers. Teardown finalized.");
                
                if (listener != null) {
                    Log.d(TAG, " -> Dispatching update finalized signal to orchestrator listener hook.");
                    currentStatusMessage = "Update complete!";
                    listener.onUpdateFinished();
                }

                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {}
                    if ("Update complete!".equals(currentStatusMessage)) {
                        currentStatusMessage = "Idle";
                    }
                }).start();

            } catch (Exception e) {
                Log.e(TAG, " -> Critical network loop exception encountered during file update task: " + e.getMessage());
                currentStatusMessage = "Error: " + e.getMessage();
            } finaliseUpdateResources(outputStream, inputStream, connection);
        }).start();
    }


/**
 * Overloaded download trigger. Accepts a transient explicit package URL 
 * from deep-link contexts instead of reading statically from AppConfig parameters.
 */
public void startZipDownloadWithCustomUrl(final String customUrlStr, final OnUpdateCompleteListener listener) {
    Log.i(TAG, "startZipDownloadWithCustomUrl() invoked from native deep link context.");
    executeZipDownloadWorker(customUrlStr, listener);
}


/**
 * Main execution worker block containing your core network pipeline streaming layers.
 */
private void executeZipDownloadWorker(final String targetUrlStr, final OnUpdateCompleteListener listener) {
    new Thread(() -> {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            if (targetUrlStr == null || targetUrlStr.isEmpty()) {
                throw new java.io.IOException("Aborting deployment: Target download package path string is empty.");
            }
            Log.d(TAG, " -> Connecting natively to deep-link target endpoint: " + targetUrlStr);
            URL url = new URL(targetUrlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");

            // Standard credentials configuration layer mapping checks
            if (config.useAuthentication()) {
                String authStr = config.getAuthUsername() + ":" + config.getAuthPassword();
                String base64Auth = android.util.Base64.encodeToString(authStr.getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
                connection.setRequestProperty("Authorization", "Basic " + base64Auth);
            }

            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new java.io.IOException("Server responded with invalid error token state: " + responseCode);
            }

            File tempZipFile = new File(context.getCacheDir(), "remote_deployment_package.zip");
            if (tempZipFile.exists()) tempZipFile.delete();

            inputStream = new java.io.BufferedInputStream(connection.getInputStream());
            outputStream = new FileOutputStream(tempZipFile);
            byte[] dataBuffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(dataBuffer, 0, 4096)) != -1) {
                outputStream.write(dataBuffer, 0, bytesRead);
            }
            outputStream.flush();

            File sandboxDir = new File(context.getFilesDir(), "www");
            extractZipToSandbox(tempZipFile, sandboxDir);
            syncSandboxToExternalFolder(sandboxDir);
            
            if (tempZipFile.exists()) tempZipFile.delete();

            if (listener != null) {
                Log.i(TAG, " -> Dynamic deep-link asset replication completed successfully. Dispatching notification signal...");
                listener.onUpdateFinished();
            }
        } catch (Exception e) {
            Log.e(TAG, " !! CRITICAL DEPLOYMENT EXCEPTION !! -> " + e.getMessage());
        } finally {
            try { if (outputStream != null) outputStream.close(); } catch (Exception ignored) {}
            try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
    }).start();
}


    private void finaliseUpdateResources(FileOutputStream outputStream, InputStream inputStream, HttpURLConnection connection) {
        try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {}
        try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) {}
        if (connection != null) connection.disconnect();
    }
    private void syncSandboxToExternalFolder(File sandboxDir) {
        Log.i(TAG, "─── STARTING SD CARD SYNC TRANSACTION ───");
        try {
            String folderName = config.getWorkspaceFolderName();
            File publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (publicDocsDir == null) {
                throw new IOException("Public storage path returned null. Shared directory is unmounted.");
            }

            File externalTargetDir = new File(new File(publicDocsDir, folderName), "www");
            Log.d(TAG, " -> Parameters Verification Matrix:");
            Log.d(TAG, "    [Source Dir Exists]: " + sandboxDir.exists() + " (" + sandboxDir.getAbsolutePath() + ")");
            Log.d(TAG, "    [Target Destination]: " + externalTargetDir.getAbsolutePath());

            if (externalTargetDir.exists()) {
                Log.d(TAG, " -> Target directory detected. Pre-purging old public structures...");
                deleteDirectoryRecursive(externalTargetDir);
            }
            if (!externalTargetDir.mkdirs()) {
                Log.w(TAG, " -> Warning: mkdirs() returned false. Directory might exist or folder write was blocked.");
            }

            int totalFilesCloned = copyDirectoryRecursiveWithLogs(sandboxDir, externalTargetDir);
            Log.i(TAG, "─── SD CARD SYNC COMPLETE (Files Cloned: " + totalFilesCloned + ") ───");
        } catch (Exception e) {
            Log.e(TAG, " !! CRITICAL SYNC BREAKDOWN !! Exception: " + e.getMessage(), e);
        }
    }

    private int copyDirectoryRecursiveWithLogs(File source, File destination) throws IOException {
        int fileCount = 0;
        if (source.isDirectory()) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw new IOException("OS permission engine blocked folder creation target path: " + destination.getAbsolutePath());
            }
            String[] children = source.list();
            if (children != null) {
                for (String child : children) {
                    File nextSource = new File(source, child);
                    File nextDest = new File(destination, child);
                    fileCount += copyDirectoryRecursiveWithLogs(nextSource, nextDest);
                }
            }
        } else {
            Log.d(TAG, "    [Copying File] " + source.getName() + " -> " + destination.getAbsolutePath());
            try (FileChannel sourceChannel = new java.io.FileInputStream(source).getChannel();
                 FileChannel destChannel = new java.io.FileOutputStream(destination).getChannel()) {
                long bytesTransferred = sourceChannel.transferTo(0, sourceChannel.size(), destChannel);
                Log.v(TAG, "    [Bytes Written]: " + bytesTransferred);
                fileCount++;
            } catch (IOException ioException) {
                Log.e(TAG, "    !! FILE WRITE DENIED !! FAILED PATH: " + destination.getAbsolutePath() + " Error: " + ioException.getMessage());
                throw ioException;
            }
        }
        return fileCount;
    }

    private void extractZipToSandbox(File zipFile, File targetDirectory) throws IOException {
        if (targetDirectory.exists()) {
            Log.d(TAG, "    Clearing out existing file nodes in destination target folder...");
            deleteDirectoryRecursive(targetDirectory);
        }
        if (!targetDirectory.mkdirs()) {
            throw new IOException("Failed to initialize system internal sandbox destination folder trees layout.");
        }

        String commonRootPrefix = "";
        try (ZipInputStream scanIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry firstEntry = scanIn.getNextEntry();
            if (firstEntry != null && firstEntry.isDirectory()) {
                commonRootPrefix = firstEntry.getName();
                Log.i(TAG, " -> GitHub root wrapper directory prefix detected: " + commonRootPrefix);
            }
        }

        String subpathConfig = config.getUpdateTargetSubpath();
        Log.i(TAG, " -> Target scoped subpath constraint rule evaluation: \"" + subpathConfig + "\"");
        
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String entryName = entry.getName();
                if (!commonRootPrefix.isEmpty() && entryName.startsWith(commonRootPrefix)) {
                    entryName = entryName.substring(commonRootPrefix.length());
                }
                if (entryName.startsWith("/")) {
                    entryName = entryName.substring(1);
                }
                
                if (!entryName.isEmpty()) {
                    boolean shouldExtract = true;
                    String finalExtractionPath = entryName;
                    
                    if (!subpathConfig.isEmpty()) {
                        String cleanSubpath = subpathConfig.endsWith("/") ? subpathConfig : subpathConfig + "/";
                        if (cleanSubpath.startsWith("/")) {
                            cleanSubpath = cleanSubpath.substring(1);
                        }
                        if (entryName.startsWith(cleanSubpath)) {
                            finalExtractionPath = entryName.substring(cleanSubpath.length());
                            shouldExtract = !finalExtractionPath.isEmpty();
                        } else {
                            shouldExtract = false;
                        }
                    }
                    
                    if (shouldExtract) {
                        File filePath = new File(targetDirectory, finalExtractionPath);
                        String canonicalTarget = targetDirectory.getCanonicalPath();
                        String canonicalEntry = filePath.getCanonicalPath();
                        if (!canonicalEntry.startsWith(canonicalTarget)) {
                            throw new SecurityException("Security Violation: Zip entry tried to escape sandbox boundaries: " + entry.getName());
                        }
                        
                        if (!entry.isDirectory()) {
                            File parentDir = filePath.getParentFile();
                            if (parentDir != null && !parentDir.exists()) {
                                parentDir.mkdirs();
                            }
                            Log.v(TAG, "    Unpacking targeted scoped element: " + finalExtractionPath);
                            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                                byte[] buffer = new byte[4096];
                                int readLen;
                                while ((readLen = zipIn.read(buffer)) != -1) {
                                    fos.write(buffer, 0, readLen);
                                }
                            }
                        } else {
                            filePath.mkdirs();
                        }
                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            Log.i(TAG, " -> Subpath-aware extraction execution loop completed successfully.");
        }
    }

    private void deleteDirectoryRecursive(File element) {
        if (element.isDirectory()) {
            File[] files = element.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    deleteDirectoryRecursive(subFile);
                }
            }
        }
        element.delete();
    }

    public static String getCurrentStatus() {
        return currentStatusMessage;
    }
}

