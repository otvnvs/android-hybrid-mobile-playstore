package com.example.app.services.example;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.example.app.MainActivity;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import android.os.Environment;

public class PermissionsController {
    private static final String TAG = "PermissionsController";

    public PermissionsController() {}

@RequestMapping(path="/api/permissions/declared", method="GET")
public ResponseContext getManifestDeclaredPermissions(RequestContext request) {
    try {
        Log.i(TAG, " -> REST API [GET]: Querying application package manifest for declared permissions.");
        
        Context context = request.getAndroidContext();
        if (context == null) {
            return ResponseContext.status(500)
                .contentType("application/json")
                .body("{\"status\":\"error\",\"message\":\"Android context unavailable\"}")
                .build();
        }

        // Fetch our own package info, specifically requesting the requested permissions array
        android.content.pm.PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
            context.getPackageName(), 
            android.content.pm.PackageManager.GET_PERMISSIONS
        );

        org.json.JSONArray manifestPermissionsArray = new org.json.JSONArray();
        
        // Ensure the app actually has requested permissions listed
        if (packageInfo.requestedPermissions != null) {
            for (String permission : packageInfo.requestedPermissions) {
                manifestPermissionsArray.put(permission);
            }
        }

        org.json.JSONObject result = new org.json.JSONObject();
        result.put("status", "success");
        result.put("package_name", context.getPackageName());
        result.put("declared_permissions", manifestPermissionsArray);
        result.put("total_count", manifestPermissionsArray.length());

        return ResponseContext.status(200)
            .contentType("application/json")
            .header("X-Server-Response-Engine", "Android-Native-JVM")
            .body(result.toString())
            .build();

    } catch (Exception e) {
        Log.e(TAG, "Failed parsing native manifest structures reflectively", e);
        return ResponseContext.status(500)
            .contentType("application/json")
            .body("{\"status\":\"error\",\"message\":\"Manifest lookup failure: " + e.getMessage() + "\"}")
            .build();
    }
}


    @RequestMapping(path = "/api/permissions/status", method = "POST")
    public ResponseContext checkPermissions(RequestContext request) {
        try {
            Context context = request.getAndroidContext();
            byte[] bodyBytes = request.getBody();
            String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
            
            JSONObject bodyJson = new JSONObject(rawBodyText);
            JSONArray requestedPermissions = bodyJson.optJSONArray("permissions");

            if (requestedPermissions == null || requestedPermissions.length() == 0) {
                return buildErrorResponse(400, "Missing required array parameter: permissions");
            }
            JSONObject checkResults = new JSONObject();

            if (context != null) {
//                for (int i = 0; i < requestedPermissions.length(); i++) {
//                    String perm = requestedPermissions.getString(i);
//                    int status = context.checkCallingOrSelfPermission(perm);
//                    checkResults.put(perm, (status == PackageManager.PERMISSION_GRANTED) ? "GRANTED" : "DENIED");
//                }
		for (int i = 0; i < requestedPermissions.length(); i++) {
		    String perm = requestedPermissions.getString(i);
		    boolean isGranted = false;

		    // ◄ ADAPTIVE OS STATUS MATCHING CHECK
		    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && 
			("android.permission.WRITE_EXTERNAL_STORAGE".equals(perm) || "android.permission.READ_EXTERNAL_STORAGE".equals(perm))) {
			isGranted = Environment.isExternalStorageManager();
		    } else {
			isGranted = (context.checkCallingOrSelfPermission(perm) == PackageManager.PERMISSION_GRANTED);
		    }

		    checkResults.put(perm, isGranted ? "GRANTED" : "DENIED");
		}

                JSONObject result = new JSONObject();
                result.put("status", "success");
                result.put("permissions_matrix", checkResults);
                return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();
            }
            return buildErrorResponse(500, "Android context unavailable");
        } catch (Exception e) {
            return buildErrorResponse(500, "Error: " + e.getMessage());
        }
    }

@RequestMapping(path="/api/permissions/request", method="POST")
public ResponseContext requestPermissions(RequestContext request) {
    try {
        Context context = request.getAndroidContext();
        byte[] bodyBytes = request.getBody();
        String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
        JSONObject bodyJson = new JSONObject(rawBodyText);
        JSONArray standardPermissions = bodyJson.optJSONArray("permissions");
        
        if (standardPermissions == null || standardPermissions.length() == 0) {
            return buildErrorResponse(400, "Missing required array parameter: permissions");
        }
        
        if (!(context instanceof MainActivity)) {
            return buildErrorResponse(422, "Active context must be an instance of MainActivity");
        }
        
        final MainActivity activity = (MainActivity) context;
        final int totalPerms = standardPermissions.length();
        final String[] permissionsArray = new String[totalPerms];
        boolean requestsStorage = false;

        for (int i = 0; i < totalPerms; i++) {
            permissionsArray[i] = standardPermissions.getString(i);
            if ("android.permission.WRITE_EXTERNAL_STORAGE".equals(permissionsArray[i]) || 
                "android.permission.READ_EXTERNAL_STORAGE".equals(permissionsArray[i])) {
                requestsStorage = true;
            }
        }

        final boolean finalRequestsStorage = requestsStorage;

        activity.runOnUiThread(new Runnable() {
            @Override 
            public void run() {
                // ◄ DYNAMIC COMPATIBILITY GATEWAY: Handle Android 11 (API 30) and above
                if (finalRequestsStorage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Log.i("PermissionsController", " -> Redirecting system workspace authorization request to All Files Access Settings menu.");
                    try {
                        android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        android.net.Uri uri = android.net.Uri.fromParts("package", activity.getPackageName(), null);
                        intent.setData(uri);
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        // Fallback for custom OS flavors if specific settings intent routing fails
                        android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        activity.startActivity(intent);
                    }
                } else {
                    // Standard platform popup fallback behavior for legacy API levels or alternative feature permissions (Camera, Microphone)
                    if (Build.VERSION.SDK_INT >= 23) {
                        //activity.requestPermissions(permissionsArray, 2002);
                        activity.requestPermissions(permissionsArray, 9825);
                    }
                }
            }
        });

        JSONObject result = new JSONObject();
        result.put("status", "success");
        result.put("message", "System authorization window sequence successfully triggered.");
        return ResponseContext.status(202).contentType("application/json").body(result.toString()).build();
    } catch (Exception e) {
        Log.e(TAG, "Permissions request pipeline failure", e);
        return buildErrorResponse(500, "System execution failure: " + e.getMessage());
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
}

