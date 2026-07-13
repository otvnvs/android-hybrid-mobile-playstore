package com.example.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import android.util.Log;

public class AppConfig {
    private static final String TAG = "JAVA_AppConfig";
    private final Context context;

    public AppConfig(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "AppConfig module tracking initialized.");
    }

    public String getVirtualHost() {
        return "https://decabase.com";
    }

    public String getWorkspaceFolderName() {
        return context.getPackageName();
    }
}
