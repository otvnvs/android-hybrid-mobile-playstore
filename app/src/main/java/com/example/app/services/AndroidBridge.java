//package com.example.app.services;
//
//import android.webkit.JavascriptInterface;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class AndroidBridge {
//    private static final ConcurrentHashMap<String, String> bodyMapCache = new ConcurrentHashMap<>();
//
//    // NEW FORMAT INTERFACE: Expects direct METHOD and URL path strings from JavaScript
//    @JavascriptInterface 
//    public void captureRequestBody(String method, String urlPath, String bodyContent) {
//        if (method != null && urlPath != null && bodyContent != null) {
//            String lookupKey = method.toUpperCase() + ":" + cleanPathString(urlPath);
//            bodyMapCache.put(lookupKey, bodyContent);
//        }
//    }
//
//    // Static consumption lookup method helper called by your WebViewClient interceptors
//    public static byte[] getAndClearBody(String method, String urlPath) {
//        if (method == null || urlPath == null) return new byte[0];
//        
//        String lookupKey = method.toUpperCase() + ":" + cleanPathString(urlPath);
//        String match = bodyMapCache.remove(lookupKey);
//        
//        return match != null ? match.getBytes(java.nio.charset.StandardCharsets.UTF_8) : new byte[0];
//    }
//
//    // Strips out redundant domain hosts or query components to guarantee unified key matching handles
//    private static String cleanPathString(String rawUrl) {
//        String path = rawUrl;
//        if (path.contains("?")) {
//            path = path.substring(0, path.indexOf("?"));
//        }
//        if (path.contains("://")) {
//            // Cut out protocol and domain strings to isolate path paths natively
//            int pathStart = path.indexOf("/", path.indexOf("://") + 3);
//            if (pathStart != -1) {
//                path = path.substring(pathStart);
//            }
//        }
//        return path;
//    }
//}
package com.example.app.services;

import android.webkit.JavascriptInterface;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;

public class AndroidBridge {
    private static final String TAG = "TELEMETRY_BRIDGE";
    private static final ConcurrentHashMap<String, String> bodyMapCache = new ConcurrentHashMap<>();

    @JavascriptInterface 
    public void captureRequestBody(String method, String urlPath, String bodyContent, String jsTimestampStr) {
        long javaReceiveTime = System.nanoTime();
        if (method == null || urlPath == null || bodyContent == null) return;

        try {
            String lookupKey = method.toUpperCase() + ":" + cleanPathString(urlPath);
            bodyMapCache.put(lookupKey, bodyContent);

            // Calculate the exact bridge transit time
            double jsTime = Double.parseDouble(jsTimestampStr);
            double currentJavaMs = (double) System.currentTimeMillis();
            double bridgeTransitMs = currentJavaMs - jsTime;

            Log.i(TAG, String.format(" -> [PORTAL-XING] Key: %s | Transit Latency: %.2fms | Payload Size: %d bytes", 
                lookupKey, bridgeTransitMs, bodyContent.length()));

        } catch (Exception e) {
            Log.e(TAG, "Telemetry parsing error: " + e.getMessage());
        }
    }

    public static byte[] getAndClearBody(String method, String urlPath) {
        long startLookup = System.nanoTime();
        if (method == null || urlPath == null) return new byte[0];
        
        String lookupKey = method.toUpperCase() + ":" + cleanPathString(urlPath);
        String match = bodyMapCache.remove(lookupKey);
        
        long endLookup = System.nanoTime();
        double lookupDurationMs = (endLookup - startLookup) / 1000000.0;
        
        if (match != null) {
            Log.d(TAG, String.format(" -> [CACHE-HIT] Key: %s | Map Seek Duration: %.4fms", lookupKey, lookupDurationMs));
            return match.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return new byte[0];
    }

    private static String cleanPathString(String rawUrl) {
        String path = rawUrl;
        if (path.contains("?")) { path = path.substring(0, path.indexOf("?")); }
        if (path.contains("://")) {
            int pathStart = path.indexOf("/", path.indexOf("://") + 3);
            if (pathStart != -1) { path = path.substring(pathStart); }
        }
        return path;
    }
}
