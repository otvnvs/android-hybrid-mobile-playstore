package com.example.app.services.webview;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;

import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;

import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

public class WebViewController {
    private static final String TAG = "WebViewController";

    public WebViewController() {}

    @RequestMapping(path = "/api/webview/diagnostics", method = "GET")
    public ResponseContext getWebViewDiagnostics(RequestContext request) {
        try {
            JSONObject root = new JSONObject();
            Context context = request.getAndroidContext();

            // 1. Structural Application Cleartext Security Policies (Reflection-Safe)
            JSONObject security = new JSONObject();
            boolean cleartextAllowed = true; 

            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    Class<?> nspClass = Class.forName("android.security.NetworkSecurityPolicy");
                    Object nspInstance = nspClass.getMethod("getInstance").invoke(null);
                    try {
                        cleartextAllowed = (Boolean) nspClass.getMethod("isCleartextTrafficAllowed").invoke(nspInstance);
                    } catch (NoSuchMethodException nsme) {
                        cleartextAllowed = (Boolean) nspClass.getMethod("isCleartextTrafficAllowed", String.class).invoke(nspInstance, "localhost");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Reflection failed to extract cleartext traffic rules: " + e.getMessage());
                }
            }

            security.put("uses_cleartext_traffic_allowed", cleartextAllowed);
            security.put("target_sdk_compliance", context != null ? context.getApplicationInfo().targetSdkVersion : -1);
            root.put("security_policy", security);

            // 2. Active Cookie Engine Parameter Checks
            JSONObject cookies = new JSONObject();
            try {
                CookieManager cookieManager = CookieManager.getInstance();
                if (cookieManager != null) {
                    cookies.put("accept_cookies_enabled", cookieManager.acceptCookie());
                    if (Build.VERSION.SDK_INT >= 21) {
                        cookies.put("has_cookies_stored", cookieManager.hasCookies());
                    } else {
                        cookies.put("has_cookies_stored", "unknown_below_api21");
                    }
                } else {
                    cookies.put("status", "CookieManager unavailable");
                }
            } catch (Exception e) {
                cookies.put("status", "error_reading_cookies: " + e.getMessage());
            }
            root.put("cookie_engine", cookies);

            // 3. Thread-Safe Base Setting Configurations
            JSONObject configurations = new JSONObject();
            // Statically map core active properties instead of instantiating widgets on background sockets
            configurations.put("javascript_enabled", true); 
            configurations.put("dom_storage_enabled", true);
            configurations.put("database_enabled", true);
            configurations.put("file_access_enabled", true);
            configurations.put("loads_images_automatically", true);
            configurations.put("mixed_content_mode", 2); // COMPATIBILITY_MODE default for security verification
            configurations.put("active_cache_mode", "LOAD_DEFAULT");
            configurations.put("status", "Thread-safe default values mapping applied");
            root.put("configurations", configurations);

            // 4. Low-Level Web Cache & Local Database Partition Sizes
            JSONObject webStorage = new JSONObject();
            if (context != null) {
                java.io.File cacheDir = context.getCacheDir();
                java.io.File appCacheDir = new java.io.File(cacheDir.getParentFile(), "app_webview");
                
                long cacheSizeBytes = 0;
                if (appCacheDir.exists() && appCacheDir.isDirectory()) {
                    cacheSizeBytes = calculateDirectorySize(appCacheDir);
                } else if (cacheDir.exists()) {
                    cacheSizeBytes = calculateDirectorySize(cacheDir);
                }
                
                webStorage.put("webview_cache_directory_path", appCacheDir.getAbsolutePath());
                webStorage.put("webview_cache_allocated_bytes", cacheSizeBytes);
                webStorage.put("status", "success");
            } else {
                webStorage.put("status", "Context unavailable");
            }
            root.put("storage_allocation", webStorage);

            return ResponseContext.status(200)
                    .contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .body(root.toString())
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "WebView diagnostics interrogation pipeline crash", e);
            return buildErrorResponse(500, "WebView inspection pipeline error: " + e.getMessage());
        }
    }

	@RequestMapping(path="/api/webview/navigate", method="POST")
	public ResponseContext navigateMainWebViewReflectiveFix(RequestContext request) {
	    try {
		Log.i(TAG, " -> REST API [POST]: Starting explicit view-ID identification pass.");
		
		// 1. Resolve and validate activity context
		android.content.Context appCtx = request.getAndroidContext();
		if (!(appCtx instanceof com.example.app.MainActivity)) {
		    return ResponseContext.status(500)
			.contentType("application/json")
			.body("{\"status\":\"error\",\"message\":\"Context tracking configuration mismatch.\"}")
			.build();
		}
		final com.example.app.MainActivity activity = (com.example.app.MainActivity) appCtx;

		// 2. Parse out the target url query parameter
		final String targetUrl = request.getQueryParam("url");
		if (targetUrl == null || targetUrl.trim().isEmpty()) {
		    return ResponseContext.status(400)
			.contentType("application/json")
			.body("{\"status\":\"error\",\"message\":\"Missing mandatory 'url' tracking parameter.\"}")
			.build();
		}

		// 3. Reflectively loop through ALL declared fields on MainActivity to look for the exact View ID
		android.webkit.WebView identifiedMainWebView = null;
		java.lang.reflect.Field[] fields = com.example.app.MainActivity.class.getDeclaredFields();
		
		// Target Resource ID we need to find (corresponds to R.id.activity_main_webview)
		int targetMainViewId = activity.getResources().getIdentifier("activity_main_webview", "id", activity.getPackageName());

		for (java.lang.reflect.Field field : fields) {
		    if (android.webkit.WebView.class.isAssignableFrom(field.getType())) {
			field.setAccessible(true);
			android.webkit.WebView wvInstance = (android.webkit.WebView) field.get(activity);
			
			// If the instance exists, check its native Android runtime view ID
			if (wvInstance != null && wvInstance.getId() == targetMainViewId) {
			    identifiedMainWebView = wvInstance;
			    Log.i(TAG, " -> Success: Explicitly located Main WebView field named: " + field.getName());
			    break;
			}
		    }
		}

		// 4. Fallback: If view lookups fail due to uninitialized layouts, try the direct field name pointer
		if (identifiedMainWebView == null) {
		    Log.w(TAG, " -> ID lookup yielded no results. Dropping back to strict field string identifier fallback.");
		    java.lang.reflect.Field fallbackField = com.example.app.MainActivity.class.getDeclaredField("mWebView");
		    fallbackField.setAccessible(true);
		    identifiedMainWebView = (android.webkit.WebView) fallbackField.get(activity);
		}

		if (identifiedMainWebView == null) {
		    return ResponseContext.status(500)
			.contentType("application/json")
			.body("{\"status\":\"error\",\"message\":\"Unable to safely isolate the primary production WebView instance container.\"}")
			.build();
		}

		// 5. Dispatch navigation instruction directly to the verified primary viewport
		final android.webkit.WebView finalMainWebView = identifiedMainWebView;
		activity.runOnUiThread(new Runnable() {
		    @Override
		    public void run() {
			try {
			    Log.i(TAG, " -> Routing instruction sent directly to Main WebView container: " + targetUrl);
			    finalMainWebView.loadUrl(targetUrl);
			} catch (Exception e) {
			    Log.e(TAG, "UI thread navigation dispatch crashed reflectively", e);
			}
		    }
		});

		// 6. Return response confirmation back to Vue layer
		org.json.JSONObject payload = new org.json.JSONObject();
		payload.put("status", "success");
		payload.put("navigated_main_to", targetUrl);
		
		return ResponseContext.status(200)
		    .contentType("application/json")
		    .header("X-Server-Response-Engine", "Android-Native-JVM")
		    .body(payload.toString())
		    .build();

	    } catch (Exception e) {
		Log.e(TAG, "Main WebView remote control filter runtime failure", e);
		return buildErrorResponse(500, "Navigation pipeline execution error: " + e.getMessage());
	    }
	}

    private long calculateDirectorySize(java.io.File directory) {
        long length = 0;
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isFile()) {
                    length += file.length();
                } else {
                    length += calculateDirectorySize(file);
                }
            }
        }
        return length;
    }

    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code)
                .contentType("application/json")
                .body(errJson.toString())
                .build();
    }
}

