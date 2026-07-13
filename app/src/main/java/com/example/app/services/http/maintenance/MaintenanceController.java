package com.example.app.services.maintenance;

import android.util.Log;
import com.example.app.MainActivity;
import com.example.app.AppConfig;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import com.example.app.UpdateManager;
import com.example.app.StorageManager;

public class MaintenanceController {
    private static final String TAG = "MaintenanceController";

    public MaintenanceController() {}

    @RequestMapping(path="/api/app/device-status", method="GET")
    public ResponseContext getDeviceStatusSignature(RequestContext request) {
        Log.i(TAG, " -> REST API [GET]: Processing environmental container audit check request.");
        
        // Build the precise payload required by your frontend's environment audit mapping logic
        org.json.JSONObject statusJson = new org.json.JSONObject();
        try {
            statusJson.put("status", "active");
            statusJson.put("platform", "android");
            statusJson.put("engine", "JVM-Bridge");
        } catch (Exception ignored) {}

        return ResponseContext.status(200)
                .contentType("application/json")
                .header("X-Server-Response-Engine", "Android-Native-JVM")
                .body(statusJson.toString())
                .build();
    }


    @RequestMapping(path="/api/maintenance/config", method="GET")
    public ResponseContext getMaintenanceConfig(RequestContext request) {
        Log.i(TAG, " -> REST API [GET]: Fetching maintenance configuration values.");
        String configJson = request.getAppConfig().getMaintenanceConfigJson();
        return ResponseContext.status(200).contentType("application/json").body(configJson).build();
    }

    @RequestMapping(path="/api/maintenance/save", method="POST")
    public ResponseContext saveMaintenanceConfig(RequestContext request) {
        Log.i(TAG, " -> REST API [POST]: Committing maintenance profile data bundle properties.");
        String autoUpdate = request.getQueryParam("autoUpdate");
        String interval = request.getQueryParam("interval");
        String url = request.getQueryParam("url");
        String useAuth = request.getQueryParam("useAuth");
        String user = request.getQueryParam("user");
        String pass = request.getQueryParam("pass");
        String subpath = request.getQueryParam("subpath");
        
        // ◄ COMPONENTIAL ARCHITECTURE UPDATE: Capture explicit storage strategy selection string
        String storageMode = request.getQueryParam("storageMode"); 

        request.getAppConfig().saveMaintenanceSettings(autoUpdate, interval, url, useAuth, user, pass, subpath, storageMode);
        return ResponseContext.status(200).contentType("application/json")
                .body("{\"status\":\"success\",\"message\":\"Settings synchronized cleanly.\"}").build();
    }

    @RequestMapping(path="/api/maintenance/download", method="POST")
    public ResponseContext triggerMaintenanceDownload(RequestContext request) {
        Log.i(TAG, " -> REST API [POST]: Manual network background update package pull sequence triggered.");
        final android.content.Context appCtx = request.getAndroidContext();
        
        if (!(appCtx instanceof com.example.app.MainActivity)) {
            return ResponseContext.status(500).contentType("application/json")
                    .body("{\"status\":\"error\",\"message\":\"Context context tracking configuration mismatch.\"}").build();
        }

        final com.example.app.MainActivity activity = (com.example.app.MainActivity) appCtx;
        final com.example.app.AppConfig appConfig = request.getAppConfig();

        // ◄ PIPELINE DECOUPLING WIN: Redirect manual button requests straight out into your modular UpdateManager class!
        UpdateManager managerInstance = new UpdateManager(appCtx, appConfig);
        managerInstance.startZipDownload(new UpdateManager.OnUpdateCompleteListener() {
            @Override
            public void onUpdateFinished() {
                Log.i(TAG, " -> UpdateManager download completed. Forcing primary viewport layout canvas refresh...");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.reloadPrimaryWebViewToRoot();
                    }
                });
            }
        });

        return ResponseContext.status(200).contentType("application/json")
                .body("{\"status\":\"success\",\"message\":\"Asynchronous update processing pipeline launched via core manager.\"}").build();
    }
    @RequestMapping(path="/api/maintenance/migrate-to-public", method="POST")
    public ResponseContext exportSandboxToPublicStorage(RequestContext request) {
        Log.i(TAG, " -> REST API [POST]: Manual asset export operation triggered via UI interface.");
        MainActivity activity = (MainActivity) request.getAndroidContext();
        StorageManager directStorageManager = new StorageManager(activity, request.getAppConfig());
        
        // Triggers background worker to mirror secure application sandbox data out to public Document slots
        directStorageManager.migrateSandboxToPublic();
        
        return ResponseContext.status(200).contentType("application/json")
                .body("{\"status\":\"success\",\"message\":\"Private sandbox files replication task spawned safely.\"}").build();
    }

    @RequestMapping(path="/api/maintenance/migrate-to-sandbox", method="POST")
    public ResponseContext importPublicToSandboxStorage(RequestContext request) {
        Log.i(TAG, " -> REST API [POST]: Manual asset ingestion operation triggered via UI interface.");
        MainActivity activity = (MainActivity) request.getAndroidContext();
        StorageManager directStorageManager = new StorageManager(activity, request.getAppConfig());
        
        // Triggers background worker to capture workspace revisions back down into system-protected memory
        directStorageManager.migratePublicToSandbox();
        
        return ResponseContext.status(200).contentType("application/json")
                .body("{\"status\":\"success\",\"message\":\"Public files ingestion task spawned safely.\"}").build();
    }

    @RequestMapping(path="/api/maintenance/sync-sd", method="POST")
    public ResponseContext legacySyncSDCardFallbackWrapper(RequestContext request) {
        Log.w(TAG, " -> REST API [POST]: Legacy /api/maintenance/sync-sd endpoint invoked. Forwarding to migrateSandboxToPublic().");
        return exportSandboxToPublicStorage(request);
    }

    @RequestMapping(path="/api/maintenance/close", method="POST")
    public ResponseContext closeMaintenanceInterface(RequestContext request) {
        Log.i(TAG, " -> REST API [POST]: Interface canvas exit action requested.");
        MainActivity activity = (MainActivity) request.getAndroidContext();
        activity.runOnUiThread(activity::onSecretTriggered);
        return ResponseContext.status(200).contentType("application/json")
                .body("{\"status\":\"success\",\"message\":\"Teardown configuration panel signal passed.\"}").build();
    }

    @RequestMapping(path="/api/maintenance/status", method="GET")
    public ResponseContext getMaintenanceStatus(RequestContext request) {
        String currentStatus = UpdateManager.getCurrentStatus();
        String payload = String.format("{\"status\":\"%s\"}", currentStatus);
        return ResponseContext.status(200).contentType("application/json").body(payload).build();
    }


	@RequestMapping(path="/api/maintenance/show", method="GET")
	public ResponseContext showMaintenanceInterfaceDirectly(RequestContext request) {
	    try {
		Log.i(TAG, " -> REST API [GET]: Processing explicit maintenance display trigger request.");
		
		android.content.Context appCtx = request.getAndroidContext();
		if (!(appCtx instanceof com.example.app.MainActivity)) {
		    return ResponseContext.status(500)
			.contentType("application/json")
			.body("{\"status\":\"error\",\"message\":\"Context context tracking configuration mismatch.\"}")
			.build();
		}
		final com.example.app.MainActivity activity = (com.example.app.MainActivity) appCtx;

		// Execute on the UI thread to bypass private property barriers safely via reflection
		activity.runOnUiThread(new Runnable() {
		    @Override
		    public void run() {
			try {
			    java.lang.reflect.Field field = com.example.app.MainActivity.class.getDeclaredField("mMaintenanceWebView");
			    field.setAccessible(true);
			    android.webkit.WebView maintWebView = (android.webkit.WebView) field.get(activity);
			    
			    java.lang.reflect.Field configField = com.example.app.MainActivity.class.getDeclaredField("mConfig");
			    configField.setAccessible(true);
			    com.example.app.AppConfig appConfig = (com.example.app.AppConfig) configField.get(activity);

			    if (maintWebView != null && appConfig != null) {
				maintWebView.loadUrl(appConfig.getVirtualHost() + "/maintenance/index.html");
				maintWebView.setVisibility(android.view.View.VISIBLE);
				maintWebView.requestFocus();
				Log.i(TAG, " -> Reflective show operation forced view display initialization success.");
			    }
			} catch (Exception e) {
			    Log.e(TAG, "Failed executing reflective show sequence inside background thread", e);
			}
		    }
		});

		return ResponseContext.status(200)
		    .contentType("application/json")
		    .body("{\"status\":\"success\",\"message\":\"Maintenance view visibility flag altered to VISIBLE.\"}")
		    .build();
		    
	    } catch (Exception e) {
		Log.e(TAG, "Maintenance show pipeline crash: " + e.getMessage());
		return ResponseContext.status(500).contentType("application/json").body("{\"status\":\"error\"}").build();
	    }
	}

	@RequestMapping(path="/api/maintenance/hide", method="GET")
	public ResponseContext hideMaintenanceInterfaceDirectly(RequestContext request) {
	    try {
		Log.i(TAG, " -> REST API [GET]: Processing explicit maintenance visibility destruction request.");
		
		android.content.Context appCtx = request.getAndroidContext();
		if (!(appCtx instanceof com.example.app.MainActivity)) {
		    return ResponseContext.status(500)
			.contentType("application/json")
			.body("{\"status\":\"error\",\"message\":\"Context context tracking configuration mismatch.\"}")
			.build();
		}
		final com.example.app.MainActivity activity = (com.example.app.MainActivity) appCtx;

		activity.runOnUiThread(new Runnable() {
		    @Override
		    public void run() {
			try {
			    java.lang.reflect.Field maintField = com.example.app.MainActivity.class.getDeclaredField("mMaintenanceWebView");
			    maintField.setAccessible(true);
			    android.webkit.WebView maintWebView = (android.webkit.WebView) maintField.get(activity);
			    
			    java.lang.reflect.Field mainField = com.example.app.MainActivity.class.getDeclaredField("mWebView");
			    mainField.setAccessible(true);
			    android.webkit.WebView mainWebView = (android.webkit.WebView) mainField.get(activity);

			    if (maintWebView != null) {
				maintWebView.setVisibility(android.view.View.GONE);
				maintWebView.loadUrl("about:blank");
				if (mainWebView != null) {
				    mainWebView.requestFocus();
				}
				Log.i(TAG, " -> Reflective hide operation dismantled overlay viewport configurations.");
			    }
			} catch (Exception e) {
			    Log.e(TAG, "Failed executing reflective hide sequence inside background thread", e);
			}
		    }
		});

		return ResponseContext.status(200)
		    .contentType("application/json")
		    .body("{\"status\":\"success\",\"message\":\"Maintenance view visibility flag altered to GONE.\"}")
		    .build();
		    
	    } catch (Exception e) {
		Log.e(TAG, "Maintenance hide pipeline crash: " + e.getMessage());
		return ResponseContext.status(500).contentType("application/json").body("{\"status\":\"error\"}").build();
	    }
	}

}
