package com.example.app.services.maintenance;
import android.os.Environment;
import android.util.Log;
import com.example.app.MainActivity;
import com.example.app.AppConfig;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import com.example.app.services.WebServiceRegistry;
import com.example.app.UpdateManager; 
import com.example.app.StorageManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class AppController {
    private static final String TAG = "AppController";
    private static volatile String currentStatusMessage = "Idle";

    public AppController () {
    }

    @RequestMapping(path = "/api/app/export-localstorage", method = "POST")
    public ResponseContext exportLocalStorage(RequestContext request) {
        Log.i(TAG, " -> REST API: Triggering localstorage serialization export.");
        
        byte[] rawBody = request.getBody();
        if (rawBody.length == 0) {
            JSONObject errJson = new JSONObject();
            try {
                errJson.put("status", "error");
                errJson.put("message", "Payload packet empty.");
            } catch (JSONException ignored) {}

            return ResponseContext.status(400)
                    .body(errJson.toString())
                    .build();
        }

        try {
            String fileName = "ahm-localstorage-dump.json";
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadDir, fileName);
            
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(rawBody);
            fos.close();
            
            android.media.MediaScannerConnection.scanFile(request.getAndroidContext(), new String[]{file.getAbsolutePath()}, null, null);
            
            JSONObject successJson = new JSONObject();
            successJson.put("status", "success");
            successJson.put("message", "Exported to downloads: " + fileName);

            return ResponseContext.status(200)
                    .body(successJson.toString())
                    .build();
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed executing export pipeline: " + e.getMessage());
            
            JSONObject errJson = new JSONObject();
            try {
                errJson.put("status", "error");
                errJson.put("message", "Failed saving file to disk.");
            } catch (JSONException ignored) {}

            return ResponseContext.status(500)
                    .body(errJson.toString())
                    .build();
        }
    }

    @RequestMapping(path = "/api/app/import-localstorage", method = "GET")
    public ResponseContext importLocalStorage(RequestContext request) {
        Log.i(TAG, " -> REST API: Accessing disk files to read storage backup.");
        try {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadDir, "ahm-localstorage-dump.json");
            
            if (!file.exists() || !file.isFile()) {
                JSONObject missingJson = new JSONObject();
                missingJson.put("status", "error");
                missingJson.put("message", "Backup file 'ahm-localstorage-dump.json' not found in Downloads folder.");

                return ResponseContext.status(404)
                        .body(missingJson.toString())
                        .build();
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            int bytesRead = fis.read(fileBytes);
            fis.close();

            return ResponseContext.status(200)
                    .header("Cache-Control", "no-cache")
                    .body(fileBytes)
                    .build();
        } catch (IOException | JSONException e) {
            JSONObject errJson = new JSONObject();
            try {
                errJson.put("status", "error");
                errJson.put("message", "Internal storage read exception.");
            } catch (JSONException ignored) {}

            return ResponseContext.status(500)
                    .body(errJson.toString())
                    .build();
        }
    }

    @RequestMapping(path = "/api/app/device-status", method = "GET")
    public ResponseContext getDeviceStatus(RequestContext request) {
        JSONObject responseJson = new JSONObject();

        try {
            responseJson.put("status", "active");
            responseJson.put("protocol", request.getHttpVersion());
            responseJson.put("userAgent", request.getHeader("User-Agent"));
            responseJson.put("domain", request.getDomain());

            // Example reading dynamic url path glob parameter e.g., if path was /api/app/device-status/{id}
            String pathId = request.getPathParam("id");
            if (pathId != null) {
                responseJson.put("requestedId", pathId);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON assembly tracking failed", e);
        }

        return ResponseContext.status(200)
                .header("X-Powered-By", "Android Native Framework Interceptor")
                .body(responseJson.toString())
                .build();
    }



}
