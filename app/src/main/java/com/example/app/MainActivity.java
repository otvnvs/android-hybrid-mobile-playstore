package com.example.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity implements SecretTriggerDetector.OnTriggerListener {
    private static final String TAG = "JS_CONSOLE_JAVA_MainActivity";
    private WebView mWebView;
    private WebView mMaintenanceWebView;
    private AppConfig mConfig;
    private StorageManager mStorageManager;
    private SecretTriggerDetector mSecretDetector;
    private com.example.app.services.IntentServiceRegistry mIntentRegistry;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConfig = new AppConfig(this);
        mStorageManager = new StorageManager(this, mConfig);
        mSecretDetector = new SecretTriggerDetector(this);

        if (mConfig.getVirtualHost().isEmpty()) return;

        mStorageManager.createPublicWorkspaceDirectory();

        mWebView = findViewById(R.id.activity_main_webview);
        configureWebViewSettings(mWebView);
        mWebView.addJavascriptInterface(new com.example.app.services.AndroidBridge(), "AndroidBridge");
        mWebView.addJavascriptInterface(new com.example.app.services.AndroidWebSocketBridge(this, mWebView), "AndroidWebSocketBridge");
        mWebView.setWebViewClient(new MyWebViewClient(this, mConfig));

        mMaintenanceWebView = findViewById(R.id.activity_maintenance_webview);
        configureWebViewSettings(mMaintenanceWebView);
        mMaintenanceWebView.addJavascriptInterface(new com.example.app.services.AndroidBridge(), "AndroidBridge");
        mMaintenanceWebView.addJavascriptInterface(new com.example.app.services.AndroidWebSocketBridge(this, mMaintenanceWebView), "AndroidWebSocketBridge");
        mMaintenanceWebView.setWebViewClient(new ConfigWebViewClient(this, mConfig));

        WebChromeClient unifiedChromeClient = new WebChromeClient() {
//            // ◄ NEW IMPLEMENTATION: Overrides default security blocks to grant browser-layer resource access
//            @Override
//            public void onPermissionRequest(final android.webkit.PermissionRequest request) {
//                Log.i(TAG, " -> WebView Browser Engine intercepting standard media resource request pipeline...");
//                
//                // Instruct the activity thread to process the layout authorization request asynchronously
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                            String[] requestedResources = request.getResources();
//                            Log.d(TAG, " -> Inner Web Components requesting access to: " + java.util.Arrays.toString(requestedResources));
//                            
//                            // ◄ THE BRIDGING WIN: Grant the browser engine immediate clearance!
//                            // Since our JS loop already forces the true Native OS system prompts first,
//                            // we can safely grant this inner request immediately here.
//                            request.grant(requestedResources);
//                            Log.i(TAG, " -> Success: Browser-layer media resource permission stream granted successfully.");
//                        }
//                    }
//                });
//            }
	    // zebra fix for lockups on lazy permission requests
	    @Override 
	    public void onPermissionRequest(final android.webkit.PermissionRequest request) {
		Log.i(TAG, " -> Web Engine intercepting camera hardware pipeline request...");
		// Fast-track grant directly to prevent UI threads from locking up on older Chromium baselines
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		    request.grant(request.getResources());
		}
	    }
            @Override 
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                String formattedMessage = String.format("[%s] Line %d of %s: %s",
                        consoleMessage.messageLevel().name(),
                        consoleMessage.lineNumber(),
                        consoleMessage.sourceId(),
                        consoleMessage.message());
                android.util.Log.d("JS_CONSOLE", formattedMessage);
                return true;
            }
        };

        mWebView.setWebChromeClient(unifiedChromeClient);
        mMaintenanceWebView.setWebChromeClient(unifiedChromeClient);

        String startupPath = mStorageManager.determineStartupPath();
        mWebView.loadUrl(mConfig.getVirtualHost() + startupPath);

    Log.d("JAVA_MainActivity", "Initializing native dynamic Intent routing engine mapping...");
    mIntentRegistry = new com.example.app.services.IntentServiceRegistry(this);
    
    // FIX: Pass 'this' as the current context execution reference parameter
    mIntentRegistry.dispatchIntent(this, getIntent());

    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
	    super.onNewIntent(intent);
	    setIntent(intent); 
	    
	    if (mIntentRegistry != null) {
		// FIX: Pass 'this' as the active context execution reference parameter
		mIntentRegistry.dispatchIntent(this, intent);
	    }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebViewSettings(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDomStorageEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setDatabaseEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
        }
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        //    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        //}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Mode 2 explicitly allows secure origins to stream insecure local frames
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        if (Build.VERSION.SDK_INT <= 30) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

    }

    public AppConfig getAppConfig() {
        return mConfig;
    }

//    @Override 
//    public void onSecretTriggered() {
//        if (mMaintenanceWebView == null || mConfig == null) return;
//        if (mMaintenanceWebView.getVisibility() == View.VISIBLE) {
//            closeMaintenanceView();
//        } else {
//            mMaintenanceWebView.loadUrl(mConfig.getVirtualHost() + "/maintenance/index.html");
//            mMaintenanceWebView.setVisibility(View.VISIBLE);
//            mMaintenanceWebView.requestFocus();
//            Log.i(TAG, "Maintenance WebView Displayed.");
//        }
//    }
@Override 
public void onSecretTriggered() {
    if (mMaintenanceWebView == null || mConfig == null) return;

    // 1. Establish a safe default fallback value
    boolean isSecretTriggerEnabled = true; 

    // 2. Query the local resource manager for the compiled boolean toggle rule
    try {
        int resId = this.getResources().getIdentifier(
            "enable_secret_trigger_combination", 
            "bool", 
            this.getPackageName()
        );
        if (resId != 0) {
            isSecretTriggerEnabled = this.getResources().getBoolean(resId);
        }
    } catch (Exception e) {
        Log.w(TAG, "Failed to read enable_secret_trigger_combination flag: " + e.getMessage());
    }

    // 3. If the resource explicitly turned off the feature, drop out here
    if (!isSecretTriggerEnabled) {
        Log.w(TAG, " -> Aborting trigger action: enable_secret_trigger_combination is disabled inside strings.xml.");
        return; 
    }

    // 4. Existing dual-viewport visualization toggling path
    if (mMaintenanceWebView.getVisibility() == View.VISIBLE) {
        closeMaintenanceView();
    } else {
        mMaintenanceWebView.loadUrl(mConfig.getVirtualHost() + "/maintenance/index.html");
        mMaintenanceWebView.setVisibility(View.VISIBLE);
        mMaintenanceWebView.requestFocus();
        Log.i(TAG, "Maintenance WebView Displayed.");
    }
}

    private void closeMaintenanceView() {
        if (mMaintenanceWebView != null) {
            mMaintenanceWebView.setVisibility(View.GONE);
            mMaintenanceWebView.loadUrl("about:blank");
            mWebView.requestFocus();
            Log.i(TAG, "Maintenance WebView Closed.");
        }
    }

    @Override 
    public void onBackPressed() {
        if (mMaintenanceWebView != null && mMaintenanceWebView.getVisibility() == View.VISIBLE) {
            closeMaintenanceView();
        } else if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override 
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mSecretDetector != null) {
            int keyCode = event.getKeyCode();
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (mSecretDetector.handleKeyDown(keyCode, event)) return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                if (mSecretDetector.handleKeyUp(keyCode, event)) return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override 
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mSecretDetector != null && mSecretDetector.handleKeyUp(keyCode, event)) return true;
        return super.onKeyUp(keyCode, event);
    }

    public void reloadPrimaryWebViewToRoot() {
        Log.i("JAVA_MainActivity", "reloadPrimaryWebViewToRoot() invoked. Refreshing production view canvas...");
        try {
            if (mWebView != null && mStorageManager != null && mConfig != null) {
                mWebView.clearCache(true);
                String freshStartupPath = mStorageManager.determineStartupPath();
                String targetUrl = mConfig.getVirtualHost() + freshStartupPath;
                Log.d("JAVA_MainActivity", " -> Loading freshly synchronized content path: " + targetUrl);
                mWebView.loadUrl(targetUrl);
                Log.i("JAVA_MainActivity", " -> Primary app workspace successfully refreshed to root level layout node.");
            }
        } catch (Exception e) {
            Log.e("JAVA_MainActivity", "Failed to force core viewport refresh: " + e.getMessage());
        }
    }
}
