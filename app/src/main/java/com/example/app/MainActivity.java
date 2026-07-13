package com.example.app;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

	    @Override
	    public void onPermissionRequest(final android.webkit.PermissionRequest request) {
		Log.i(TAG, "Permission requested: " + java.util.Arrays.toString(request.getResources()));
		request.deny();
	    }

	    @Override
	    public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
		String formattedMessage = String.format("[%s] Line %d of %s: %s",
		    consoleMessage.messageLevel().name(),
		    consoleMessage.lineNumber(),
		    consoleMessage.sourceId(),
		    consoleMessage.message());
		Log.d("JS_CONSOLE", formattedMessage);
		return true;
	    }

	};
        mWebView.setWebChromeClient(unifiedChromeClient);
        mWebView.loadUrl(mConfig.getVirtualHost());
	mIntentRegistry = new com.example.app.services.IntentServiceRegistry(this);
	mIntentRegistry.dispatchIntent(this, getIntent());
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
	    super.onNewIntent(intent);
	    setIntent(intent); 
	    if (mIntentRegistry != null) {
		mIntentRegistry.dispatchIntent(this, intent);
	    }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebViewSettings(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowUniversalAccessFromFileURLs(false);
            settings.setAllowFileAccessFromFileURLs(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
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

    public void reloadPrimaryWebViewToRoot() {
        try {
            if (mWebView != null) {
                mWebView.clearCache(true);
                mWebView.loadUrl("file:///android_asset/index.html");
            }
        } catch (Exception e) {
        }
    }
}
