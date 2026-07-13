package com.example.app;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import android.util.Log;
import android.view.KeyEvent;
import com.example.app.services.WebServiceRegistry;

public class ConfigWebViewClient extends WebViewClient {
    private static final String TAG = "JAVA_ConfigWebViewClient";
    private final Context mContext;
    private final AppConfig mConfig;
    private final WebServiceRegistry mServiceRegistry;

    public ConfigWebViewClient(Context context, AppConfig config) {
        this.mContext = context;
        this.mConfig = config;
        this.mServiceRegistry = new WebServiceRegistry(context);
    }

    @Override 
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(com.example.app.services.WebScripts.INTERCEPT_SCRIPT, null);
            view.evaluateJavascript(com.example.app.services.WebScripts.WEBSOCKET_PROXY_SCRIPT, null);
        } else {
            view.loadUrl("javascript:" + com.example.app.services.WebScripts.INTERCEPT_SCRIPT);
            view.loadUrl("javascript:" + com.example.app.services.WebScripts.WEBSOCKET_PROXY_SCRIPT);
        }
    }

    @Override 
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
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
        String method = request.getMethod().toUpperCase();
        
        if (path != null && path.contains("favicon.ico")) {
            return new WebResourceResponse("image/x-icon", "UTF-8", new java.io.ByteArrayInputStream(new byte[0]));
        }
        
        String targetHost = uri.getHost();
        String rawVirtualHost = getRawVirtualHost();
        
        if (targetHost != null && targetHost.equals(rawVirtualHost) && path != null) {
            WebResourceResponse serviceResponse = mServiceRegistry.dispatch(mContext, mConfig, request, path, method);
            if (serviceResponse != null) {
                return serviceResponse;
            }
            
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
                Log.e(TAG, "Error loading local maintenance asset: " + e.toString());
                String errorHtml = "<html><body><h2>Maintenance Error</h2></body></html>";
                InputStream fallbackStream = new java.io.ByteArrayInputStream(errorHtml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return new WebResourceResponse("text/html", "UTF-8", fallbackStream);
            }
        }
        return super.shouldInterceptRequest(view, request);
    }

    /**
     * Refactored routing strategy evaluating config flags to provide strict path boundaries.
     */
    private InputStream resolveAssetStream(String relativePath) throws IOException {
        String formattedPath = "www/" + relativePath;

        // ◄ CONFIG-DRIVEN SWITCH: Check public memory only if public strategy is selected
        if (mConfig != null && mConfig.isPublicWorkspaceEnabled()) {
            String folderName = mConfig.getWorkspaceFolderName();
            File publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (publicDocsDir != null) {
                File workspaceFile = new File(new File(publicDocsDir, folderName), formattedPath);
                if (workspaceFile.exists() && workspaceFile.isFile()) {
                    Log.d(TAG, "[ROUTER-HIT] Serving asset from Public Shared Storage: " + formattedPath);
                    return new FileInputStream(workspaceFile);
                }
            }
        }

        // Strategy Default Option 1: Serve directly from secure App Private Sandbox Cache
        File sandboxFile = new File(mContext.getFilesDir(), formattedPath);
        if (sandboxFile.exists() && sandboxFile.isFile()) {
            Log.v(TAG, "[ROUTER-HIT] Serving asset from Private Sandbox isolation: " + formattedPath);
            return new FileInputStream(sandboxFile);
        }

        // Strategy Default Option 2: Fallback to the hardcoded maintenance assets inside the APK package
        Log.v(TAG, "[ROUTER-HIT] Serving asset from base application APK assets: " + formattedPath);
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
}

