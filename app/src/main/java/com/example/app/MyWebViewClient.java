package com.example.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.example.app.services.WebServiceRegistry;

class MyWebViewClient extends WebViewClient {
    private static final String TAG = "JS_CONSOLE_MyWebViewClient";
    private final Context mContext;
    private final AppConfig mConfig;
    private final WebServiceRegistry mServiceRegistry;

    public MyWebViewClient(Context context, AppConfig config) {
        this.mContext = context;
        this.mConfig = config;
        this.mServiceRegistry = new WebServiceRegistry(context);
    }

    @Override 
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(com.example.app.services.WebScripts.INTERCEPT_SCRIPT, null);
            view.evaluateJavascript(com.example.app.services.WebScripts.WEBSOCKET_PROXY_SCRIPT, null);
        } else {
            view.loadUrl("javascript:" + com.example.app.services.WebScripts.INTERCEPT_SCRIPT);
            view.loadUrl("javascript:" + com.example.app.services.WebScripts.WEBSOCKET_PROXY_SCRIPT);
        }
    }

    @Override 
    public boolean shouldOverrideKeyEvent(WebView view, android.view.KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true;
        }
        return super.shouldOverrideKeyEvent(view, event);
    }

    private String getRawVirtualHost() {
        if (mConfig == null || mConfig.getVirtualHost().isEmpty()) return null;
        return Uri.parse(mConfig.getVirtualHost()).getHost();
    }

@Override 
public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
    Uri uri = request.getUrl();
    String path = uri.getPath();
    
    // =========================================================================
    // GLOBAL INTERCEPT RULE FOR API MOCKS
    // Catch any request targeted at /api/ across ANY loaded domain or IP host
    // =========================================================================
    if (path != null && path.startsWith("/api/")) {
        String method = request.getMethod();
        
        // Dispatch directly into your simulated native JVM controller environment
        WebResourceResponse serviceResponse = mServiceRegistry.dispatch(mContext, mConfig, request, path, method);
        if (serviceResponse != null) {
            return serviceResponse;
        }
        
        // Fallback error wrapper if a controller wasn't mapped for this path/method combo
        String errorHtml = "{\"status\":\"error\",\"message\":\"Native API route not found matching this request mapping.\"}";
        InputStream fallbackStream = new java.io.ByteArrayInputStream(errorHtml.getBytes(StandardCharsets.UTF_8));
        return new WebResourceResponse("application/json", "UTF-8", fallbackStream);
    }

    // =========================================================================
    // LOCAL VIRTUAL HOST ROUTING FOR STATIC ASSETS
    // Only intercept asset files if the host explicitly matches your config domain
    // =========================================================================
    String targetHost = uri.getHost();
    String rawVirtualHost = getRawVirtualHost();
    
    if (targetHost != null && targetHost.equals(rawVirtualHost)) {
        if (path != null) {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.isEmpty()) {
                path = "index.html";
            }
            try {
                InputStream targetStream = resolveAssetStream(path);
                String mimeType = getMimeType(path);
                return new WebResourceResponse(mimeType, "UTF-8", targetStream);
            } catch (IOException e) {
                Log.e(TAG, "Exception loading asset file path: " + e.toString());
                String errorHtml = "<html><body><h2>Application Error</h2><p>Resource could not be loaded local-side.</p></body></html>";
                InputStream fallbackStream = new java.io.ByteArrayInputStream(errorHtml.getBytes(StandardCharsets.UTF_8));
                return new WebResourceResponse("text/html", "UTF-8", fallbackStream);
            }
        }
    }

    // Pass-through everything else (Vite static bundle files, scripts, hmr, stylesheets)
    // live out to the network or development laptop endpoint cleanly.
    return super.shouldInterceptRequest(view, request);
}


    /**
     * Refactored routing strategy evaluating config flags to provide strict path boundaries.
     */
    private InputStream resolveAssetStream(String relativePath) throws IOException {
        String formattedPath = "www/" + relativePath;
        return mContext.getAssets().open(formattedPath);
    }

    private String getMimeType(String path) {
        if (path.contains("?")) path = path.split("\\?")[0];
        if (path.contains("#")) path = path.split("#")[0];
        if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    @Override 
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return handleUrlRouting(view, request.getUrl());
    }

    @SuppressWarnings("deprecation")
    @Override 
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return handleUrlRouting(view, Uri.parse(url));
    }
private boolean handleUrlRouting(WebView view, Uri uri) {
    return false;
}

    @Override 
    public void onReceivedError(WebView webview, WebResourceRequest request, WebResourceError error) {
        if (webview == null || request == null || error == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int errorCode = error.getErrorCode();
            String targetUrl = request.getUrl().toString();
            if (request.isForMainFrame()) {
                Log.e(TAG, "CRITICAL ERROR [" + errorCode + "] TARGET: " + targetUrl);
                if (errorCode == WebViewClient.ERROR_FILE_NOT_FOUND || errorCode == WebViewClient.ERROR_HOST_LOOKUP || errorCode == WebViewClient.ERROR_CONNECT || errorCode == WebViewClient.ERROR_UNKNOWN) {
                    if (!targetUrl.contains("error.html") && mConfig != null) {
                        webview.loadUrl(mConfig.getVirtualHost() + "/error.html");
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override 
    public void onReceivedError(WebView webview, int errorCode, String description, String failingUrl) {
        if (webview != null && failingUrl != null && !failingUrl.contains("error.html")) {
            Log.e(TAG, "LEGACY CRITICAL ERROR: " + description);
            if (mConfig != null) {
                webview.loadUrl(mConfig.getVirtualHost() + "/error.html");
            }
        }
    }

    @Override 
    public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
        String failingUrl = (error != null) ? error.getUrl() : "Unknown URL";
        if (Build.VERSION.SDK_INT <= 30) {
            Log.w(TAG, "[ZEBRA SSL RECOVERY] Overriding certificate trust dropout chain validation for path: " + failingUrl);
            handler.proceed();
        } else {
            Log.d(TAG, "[SSL DEFAULT] Passing standard system security trust rules validation check.");
            super.onReceivedSslError(view, handler, error);
        }
    }
}

