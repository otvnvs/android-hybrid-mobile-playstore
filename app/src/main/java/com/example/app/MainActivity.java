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

public class MainActivity extends Activity {
    private static final String TAG = "JS_CONSOLE_JAVA_MainActivity";
    private WebView mWebView;
    private AppConfig mConfig;
    private com.example.app.services.IntentServiceRegistry mIntentRegistry;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConfig = new AppConfig(this);

        if (mConfig.getVirtualHost().isEmpty()) return;


        mWebView = findViewById(R.id.activity_main_webview);
        configureWebViewSettings(mWebView);
        mWebView.addJavascriptInterface(new com.example.app.services.AndroidBridge(), "AndroidBridge");
        mWebView.addJavascriptInterface(new com.example.app.services.AndroidWebSocketBridge(this, mWebView), "AndroidWebSocketBridge");
        mWebView.setWebViewClient(new MyWebViewClient(this, mConfig));


        WebChromeClient unifiedChromeClient = new WebChromeClient() {
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

        mWebView.loadUrl(mConfig.getVirtualHost());

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

    @Override 
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override 
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override 
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    public void reloadPrimaryWebViewToRoot() {
        Log.i("JAVA_MainActivity", "reloadPrimaryWebViewToRoot() invoked. Refreshing production view canvas...");
        try {
            if (mWebView != null) {
                mWebView.clearCache(true);
                mWebView.loadUrl("file:///android_asset/index.html");
            }
        } catch (Exception e) {
            Log.e("JAVA_MainActivity", "Failed to force core viewport refresh: " + e.getMessage());
        }
    }
}
