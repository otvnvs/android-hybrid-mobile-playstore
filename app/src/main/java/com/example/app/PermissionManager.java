package com.example.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import android.os.Environment;
import android.provider.Settings;

public class PermissionManager {
    public static final int PERMISSION_REQUEST_CODE = 100;
    public static final int STORAGE_MANAGEMENT_REQUEST_CODE = 200;
    private static final String TAG = "JAVA_PermissionManager";

    private final Activity activity;
    private final String[] requiredPermissions;

    public PermissionManager(Activity activity) {
	Log.d(TAG, "PermissionManager(Activity activity)");
        this.activity = activity;
        this.requiredPermissions = Build.VERSION.SDK_INT >= 33 
                ? new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}
                : new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    public boolean hasStandardPermissions() {
	Log.d(TAG, "hasStandardPermissions()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : requiredPermissions) {
                if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void requestStandardPermissions() {
	Log.d(TAG, "requestStandardPermissions()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(requiredPermissions, PERMISSION_REQUEST_CODE);
        }
    }

    public boolean needsStorageManagerAccess() {
	Log.d(TAG, "needsStorageManagerAccess()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return !Environment.isExternalStorageManager();
        }
        return false;
    }

    public void requestStorageManagerPermission() {
	Log.d(TAG, "requestStorageManagerPermission()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivityForResult(intent, STORAGE_MANAGEMENT_REQUEST_CODE);
        }
    }
}
