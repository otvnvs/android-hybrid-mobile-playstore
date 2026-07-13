package com.example.app.services.intent.installation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.app.UpdateManager;
import com.example.app.AppConfig;
import com.example.app.MainActivity;
import com.example.app.services.IntentContext;
import com.example.app.services.IntentMapping;

public class AssetInstallationHandler {
    private static final String TAG = "JS_CONSOLE_JAVA_AssetInstaller";

    public AssetInstallationHandler() {}

    /**
     * Deep-link entry route handler target.
     * Intercepts "otv-app://deploy?package_url=..." and triggers UpdateManager.
     */
    @IntentMapping(host = "deploy")
    public void handleAssetDeployment(final IntentContext context) {
        String packageUrl = context.getQueryParam("package_url");
        if (packageUrl == null || packageUrl.isEmpty()) {
            Log.w(TAG, "Aborting asset deployment sequence: 'package_url' parameter is missing or empty.");
            return;
        }

        Log.i(TAG, "Native Intent worker matched route. Launching deployment loop for URL: " + packageUrl);

        // Instantiate infrastructure configurations natively via context references safely
        AppConfig config = new AppConfig(context.getContext());
        UpdateManager installer = new UpdateManager(context.getContext(), config);

        // Execute the overloaded worker download thread pipeline
        installer.startZipDownloadWithCustomUrl(packageUrl, new UpdateManager.OnUpdateCompleteListener() {
            @Override
            public void onUpdateFinished() {
                Log.i(TAG, "Dynamic file system replication completed cleanly. Scheduling canvas reload...");
                
                // Get the application context reference
                Context appCtx = context.getContext();
                
                // Force the UI interaction tasks back onto Android's main loop rendering engine thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // If the context wraps an instance of MainActivity, resolve it directly
                            if (appCtx instanceof MainActivity) {
                                Log.d(TAG, " -> Context verified as MainActivity. Invoking reloadPrimaryWebViewToRoot()");
                                ((MainActivity) appCtx).reloadPrimaryWebViewToRoot();
                            } else {
                                Log.w(TAG, " -> Context look up failed to map onto MainActivity directly.");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed forcing viewport layout refresh stream: " + e.getMessage());
                        }
                    }
                });
            }
        });
    }
}

