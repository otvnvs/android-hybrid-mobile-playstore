//package com.example.app.services;
//
//import android.content.Context;
//import android.webkit.WebView;
//
//public class WebSocketSession {
//    private final String socketId;
//    private final String path;
//    private final WebView webView;
//    private final Context androidContext;
//
//    public WebSocketSession(Context context, String socketId, String path, WebView webView) {
//        this.androidContext = context.getApplicationContext(); // Safeguard memory context
//        this.socketId = socketId;
//        this.path = path;
//        this.webView = webView;
//    }
//
//    public String getSocketId() { 
//        return this.socketId; 
//    }
//    
//    public String getPath() { 
//        return this.path; 
//    }
//    
//    public Context getContext() { 
//        return this.androidContext; 
//    }
//
//    public void send(final String payload) {
//        if (webView == null) return;
//        
//        webView.post(new Runnable() {
//            @Override 
//            public void run() {
//                // Safeguard backslashes and single quote literals to block runtime parsing drops
//                String safePayload = payload.replace("\\", "\\\\").replace("'", "\\'");
//                
//                // FIXED: Use Java formatting to evaluate the MESSAGE string template variable explicitly
//                String jsCode = String.format(
//                    com.example.app.services.WebScripts.WEBSOCKET_MESSAGE_FRAME_SCRIPT,
//                    socketId,
//                    safePayload
//                );
//                
//                webView.evaluateJavascript(jsCode, null);
//            }
//        });
//    }
//}
//--------------------------------------------------------------------------------
//package com.example.app.services;
//
//import android.content.Context;
//import android.net.Uri;
//import android.webkit.WebView;
//import java.util.HashMap;
//import java.util.Map;
//
//public class WebSocketSession {
//    private final String socketId;
//    private final String cleanPath;
//    private final WebView webView;
//    private final Context androidContext;
//    private final Map<String, String> queryParameters = new HashMap<>();
//
//    public WebSocketSession(Context context, String socketId, String rawPath, WebView webView) {
//        this.androidContext = context.getApplicationContext();
//        this.socketId = socketId;
//        this.webView = webView;
//
//        // Parse query parameters and isolate the route path context
//        if (rawPath != null && rawPath.contains("?")) {
//            int queryIndex = rawPath.indexOf('?');
//            this.cleanPath = rawPath.substring(0, queryIndex);
//            String queryString = rawPath.substring(queryIndex + 1);
//            parseQueryString(queryString);
//        } else {
//            this.cleanPath = rawPath;
//        }
//    }
//
//    private void parseQueryString(String query) {
//        if (query == null || query.isEmpty()) return;
//        String[] pairs = query.split("&");
//        for (String pair : pairs) {
//            String[] idx = pair.split("=");
//            if (idx.length > 1) {
//                // Using Uri.decode to handle URL-encoded components gracefully
//                queryParameters.put(idx[0], Uri.decode(idx[1]));
//            } else if (idx.length == 1) {
//                queryParameters.put(idx[0], "");
//            }
//        }
//    }
//
//    public String getQueryParam(String key) {
//        return queryParameters.get(key);
//    }
//
//    public String getSocketId() {
//        return this.socketId;
//    }
//
//    // Returns the clean path so WebSocketServiceRegistry path matching stays happy
//    public String getPath() {
//        return this.cleanPath;
//    }
//
//    public Context getContext() {
//        return this.androidContext;
//    }
//
//    public WebView getWebView() {
//        return this.webView;
//    }
//
//    public void send(final String payload) {
//        if (webView == null) return;
//        webView.post(new Runnable() {
//            @Override public void run() {
//                String safePayload = payload.replace("\\", "\\\\").replace("'", "\\'");
//                String jsCode = String.format(com.example.app.services.WebScripts.WEBSOCKET_MESSAGE_FRAME_SCRIPT, socketId, safePayload);
//                webView.evaluateJavascript(jsCode, null);
//            }
//        });
//    }
//}
//--------------------------------------------------------------------------------
package com.example.app.services;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;
import java.util.HashMap;
import java.util.Map;

public class WebSocketSession {
    private final String socketId;
    private final String cleanPath;
    private final WebView webView;
    private final Context androidContext;
    private final Map<String, String> queryParameters = new HashMap<>();

    public WebSocketSession(Context context, String socketId, String rawPath, WebView webView) {
        this.androidContext = context.getApplicationContext();
        this.socketId = socketId;
        this.webView = webView;

        // Parse query parameters and isolate the baseline routing path
        if (rawPath != null && rawPath.contains("?")) {
            int queryIndex = rawPath.indexOf('?');
            this.cleanPath = rawPath.substring(0, queryIndex);
            String queryString = rawPath.substring(queryIndex + 1);
            parseQueryString(queryString);
        } else {
            this.cleanPath = rawPath;
        }
    }

    private void parseQueryString(String query) {
        if (query == null || query.isEmpty()) return;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] idx = pair.split("=");
            if (idx.length > 1) {
                // Using Uri.decode to cleanly handle URL-encoded components (like colons %3A)
                queryParameters.put(idx[0], Uri.decode(idx[1]));
            } else if (idx.length == 1) {
                queryParameters.put(idx[0], "");
            }
        }
    }

    public String getQueryParam(String key) {
        return queryParameters.get(key);
    }

    public String getSocketId() {
        return this.socketId;
    }

    // Returns the clean path layout so WebSocketServiceRegistry path matching stays happy
    public String getPath() {
        return this.cleanPath;
    }

    public Context getContext() {
        return this.androidContext;
    }

    public WebView getWebView() {
        return this.webView;
    }

    public void send(final String payload) {
        if (webView == null) return;
        webView.post(new Runnable() {
            @Override public void run() {
                String safePayload = payload.replace("\\", "\\\\").replace("'", "\\'");
                String jsCode = String.format(com.example.app.services.WebScripts.WEBSOCKET_MESSAGE_FRAME_SCRIPT, socketId, safePayload);
                webView.evaluateJavascript(jsCode, null);
            }
        });
    }
}

