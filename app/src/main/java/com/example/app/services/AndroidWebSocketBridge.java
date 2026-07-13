package com.example.app.services;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.util.Log;

public class AndroidWebSocketBridge {
    private static final String TAG = "JAVA_AndroidWSBridge";
    private final WebSocketServiceRegistry registry;
    private final WebView webView;

    public AndroidWebSocketBridge(Context context, WebView webView) {
        Log.d(TAG, "AndroidWebSocketBridge initialization sequence tracking invoked.");
        this.webView = webView;

        this.registry = new WebSocketServiceRegistry(context);
    }

    @JavascriptInterface 
    public void connectNative(String socketId, String path) {
        if (socketId == null || path == null) return;
        Log.i(TAG, "-> Connect interface invoked. Socket ID: " + socketId + " | Mapped Path: " + path);
        registry.dispatchConnect(socketId, path, webView);
    }

    @JavascriptInterface 
    public void sendNative(String socketId, String message) {
        if (socketId == null || message == null) return;
        Log.v(TAG, "-> Message transfer packet routing: [" + socketId + "] | Size: " + message.length() + " bytes");
        registry.dispatchMessage(socketId, message);
    }

    @JavascriptInterface 
    public void closeNative(String socketId) {
        if (socketId == null) return;
        Log.i(TAG, "-> Close frame transaction received for ID: " + socketId);
        registry.dispatchClose(socketId);
    }
}

