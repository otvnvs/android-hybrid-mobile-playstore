package com.example.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import android.util.Log;

public class AppConfig {
    private static final String TAG = "JAVA_AppConfig";
    private static final String PREFS_NAME = "MaintenancePrefs";
    
    // Constant string identifiers representing the two explicit storage choices
    public static final String STORAGE_MODE_SANDBOX = "sandbox";
    public static final String STORAGE_MODE_PUBLIC = "public";
    
    private final Context context;

    public AppConfig(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "AppConfig module tracking initialized.");
    }

//    public String getVirtualHost() {
//        String host = context.getString(R.string.virtual_host);
//        if (host == null || host.isEmpty()) return "";
//        return host.startsWith("https://") ? host : "https://" + host;
//    }
public String getVirtualHost() {
    String host = null;
    
    try {
        // 1. Resolve the resource identifier dynamically by string name
        int resId = context.getResources().getIdentifier(
            "virtual_host", 
            "string", 
            context.getPackageName()
        );
        
        if (resId != 0) {
            String declaredHost = context.getString(resId);
            if (declaredHost != null && !declaredHost.trim().isEmpty()) {
                host = declaredHost.trim();
            }
        }
    } catch (Exception e) {
        Log.w(TAG, "Resource configuration lookup failure for virtual host matching: " + e.getMessage());
    }

    // 2. FALLBACK PATHWAY: If missing or blank, apply a secure operational default domain
    if (host == null) {
        host = "localhost";
        Log.i(TAG, " -> [CONFIG FALLBACK] virtual_host is empty or unspecified. defaulting to local signature: " + host);
    }

    // 3. Keep existing protocol tracking enforcement layer intact
    return host.startsWith("https://") ? host : "https://" + host;
}


//    public String getWorkspaceFolderName() {
//        return context.getString(R.string.config_workspace_folder_name);
//    }
	public String getWorkspaceFolderName() {
	    try {
		// 1. Fetch the value from strings.xml dynamically using its resource identifier
		int resId = context.getResources().getIdentifier(
		    "config_workspace_folder_name", 
		    "string", 
		    context.getPackageName()
		);
		
		if (resId != 0) {
		    String folderName = context.getString(resId);
		    if (folderName != null && !folderName.trim().isEmpty()) {
			return folderName.trim();
		    }
		}
	    } catch (Exception e) {
		Log.w(TAG, "Resource configuration lookup failure for workspace name folder mapping: " + e.getMessage());
	    }

	    // 2. FALLBACK PATHWAY: If missing, empty, or unresolvable, return the native app package namespace string
	    String fallbackIdentifier = context.getPackageName();
	    Log.i(TAG, " -> [CONFIG FALLBACK] config_workspace_folder_name is empty or unspecified. defaulting to unique package bundle key: " + fallbackIdentifier);
	    return fallbackIdentifier;
	}


    /**
     * Refactored signature to accept and persist the explicit target storage strategy choice.
     */
    public void saveMaintenanceSettings(String autoUpdate, String interval, String url, String useAuth, 
                                        String user, String pass, String subpath, String storageMode) {
        Log.i(TAG, "saveMaintenanceSettings() invoked. Synchronizing application configuration state...");
        try {
            boolean parsedAutoUpdate = Boolean.parseBoolean(autoUpdate);
            int parsedInterval = Integer.parseInt(interval);
            boolean parsedUseAuth = Boolean.parseBoolean(useAuth);

            // Sanitize incoming storage strategy parameter, defaulting strictly to safe sandbox isolation
            String verifiedStorageMode = STORAGE_MODE_PUBLIC.equalsIgnoreCase(storageMode) ? STORAGE_MODE_PUBLIC : STORAGE_MODE_SANDBOX;

            Log.d(TAG, " -> Extracted profile data fields matrix:");
            Log.d(TAG, "    [Auto Update]: " + parsedAutoUpdate);
            Log.d(TAG, "    [Interval Hours]: " + parsedInterval);
            Log.d(TAG, "    [Target Remote URL]: " + url);
            Log.d(TAG, "    [Target SubPath]: " + (subpath != null ? subpath.trim() : ""));
            Log.d(TAG, "    [Requires Auth Proxy]: " + parsedUseAuth);
            Log.d(TAG, "    [Username Field]: " + user);
            Log.d(TAG, "    [Target Storage Strategy Mode]: " + verifiedStorageMode);

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("auto_update_enabled", parsedAutoUpdate);
            editor.putInt("update_interval_hours", parsedInterval);
            editor.putString("update_target_url", url);
            editor.putBoolean("use_authentication", parsedUseAuth);
            editor.putString("auth_username", user);
            editor.putString("auth_password", pass);
            editor.putString("update_target_subpath", subpath != null ? subpath.trim() : "");
            
            // Persist the explicit selection string
            editor.putString("active_storage_mode", verifiedStorageMode);
            editor.apply();
            Log.i(TAG, " -> Transaction committed successfully to Shared Preference database storage.");
        } catch (Exception e) {
            Log.e(TAG, " -> Transaction breakdown! Failed to write settings data layout to disk: " + e.getMessage());
        }
    }

    /**
     * Overloaded fallback implementation to preserve legacy method signatures safely across companion modules.
     */
    public void saveMaintenanceSettings(String autoUpdate, String interval, String url, String useAuth, String user, String pass, String subpath) {
        saveMaintenanceSettings(autoUpdate, interval, url, useAuth, user, pass, subpath, STORAGE_MODE_SANDBOX);
    }

    /**
     * Explicit getter providing programmatic inspection rules for dynamic path engines.
     */
    public String getActiveStorageMode() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String mode = prefs.getString("active_storage_mode", STORAGE_MODE_SANDBOX);
        Log.d(TAG, "getActiveStorageMode() requested. Operational anchor path verified as: " + mode);
        return mode;
    }

    /**
     * Diagnostic helper function returning whether public external partition access is active.
     */
    public boolean isPublicWorkspaceEnabled() {
        return STORAGE_MODE_PUBLIC.equals(getActiveStorageMode());
    }

    public String getUpdateTargetSubpath() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("update_target_subpath", "");
    }

    public boolean isAutoUpdateEnabled() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("auto_update_enabled", false);
    }

    public int getUpdateIntervalHours() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt("update_interval_hours", 24);
    }

    public String getUpdateTargetUrl() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("update_target_url", "");
    }

    public boolean useAuthentication() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("use_authentication", false);
    }

    public String getAuthUsername() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("auth_username", "");
    }

    public String getAuthPassword() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("auth_password", "");
    }

    /**
     * Expanded JSON payload constructor mapping properties directly to the Vue frontend config tree.
     */
    public String getMaintenanceConfigJson() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            JSONObject json = new JSONObject();
            json.put("autoUpdate", prefs.getBoolean("auto_update_enabled", false));
            json.put("interval", prefs.getInt("update_interval_hours", 24));
            json.put("url", prefs.getString("update_target_url", ""));
            json.put("useAuth", prefs.getBoolean("use_authentication", false));
            json.put("user", prefs.getString("auth_username", ""));
            json.put("pass", prefs.getString("auth_password", ""));
            json.put("subpath", prefs.getString("update_target_subpath", ""));
            
            // Injects the selection string outbound into the web data stream
            json.put("storageMode", prefs.getString("active_storage_mode", STORAGE_MODE_SANDBOX));
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
public String getAppVersionTag() {
    try {
        int resId = context.getResources().getIdentifier(
            "app_version_tag", 
            "string", 
            context.getPackageName()
        );
        if (resId != 0) {
            String version = context.getString(resId);
            if (version != null && !version.trim().isEmpty()) {
                return version.trim();
            }
        }
    } catch (Exception e) {
        Log.w(TAG, "Resource configuration lookup failure for version tag: " + e.getMessage());
    }

    // Safe fallback if omitted or left blank in the resource dictionary xml
    return "1.0.0-release"; 
}

}

