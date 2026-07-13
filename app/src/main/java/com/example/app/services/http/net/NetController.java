//package com.example.app.services.maintenance;
//
//import android.content.Context;
//import android.os.Environment;
//import android.util.Log;
//import com.example.app.MainActivity;
//import com.example.app.AppConfig;
//import com.example.app.services.RequestMapping;
//import com.example.app.services.RequestContext;
//import com.example.app.services.ResponseContext;
//import com.example.app.services.WebServiceRegistry;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.json.JSONArray;
//import java.nio.charset.StandardCharsets;
//
//public class NetController {
//    private static final String TAG = "JS_CONSOLE_NetController";
//    private static volatile String currentStatusMessage = "Idle";
//    public NetController () {
//    }
//    @RequestMapping(path="/api/net/request", method="POST")
//    public ResponseContext proxyHttpRequest(RequestContext request) {
//        Log.d(TAG,"public ResponseContext proxyHttpRequest(RequestContext request):begin");
//        try {
//        // Read the incoming JSON configuration envelope
//        byte[] requestBodyBytes = request.getBody();
//        if (requestBodyBytes == null || requestBodyBytes.length == 0) {
//            return ResponseContext.status(400)
//            .contentType("application/json")
//            .body("{\"status\":\"error\",\"message\":\"Empty request body received by proxy broker.\"}")
//            .build();
//        }
//
//        // Read and parse the root-level incoming JSON configuration envelope
//        String jsonConfig = new String(requestBodyBytes, StandardCharsets.UTF_8);
//        JSONObject rootEnvelope = new JSONObject(jsonConfig);
//
//        // 1. Process Timeout Configuration (Prioritise payload config, fall back to query param, then default 15s)
//        int customTimeoutMs = 15000; 
//        if (rootEnvelope.has("timeout_ms")) {
//            customTimeoutMs = rootEnvelope.getInt("timeout_ms");
//        } else {
//            String timeoutParam = request.getQueryParam("timeout_ms");
//            if (timeoutParam != null && !timeoutParam.isEmpty()) {
//            try {
//                customTimeoutMs = Integer.parseInt(timeoutParam);
//            } catch (NumberFormatException nfe) {
//                Log.w(TAG, "Invalid timeout parameter format, falling back to default.");
//            }
//            }
//        }
//
//        // 2. Validate existence of the inner "request" metadata block
//        if (!rootEnvelope.has("request")) {
//            return ResponseContext.status(400)
//            .contentType("application/json")
//            .body("{\"status\":\"error\",\"message\":\"Missing 'request' object containing delivery metadata.\"}")
//            .build();
//        }
//        JSONObject innerRequest = rootEnvelope.getJSONObject("request");
//
//        // 3. Extract the target downstream destination parameters
//        String targetUrl = innerRequest.optString("url", "");
//        String method = innerRequest.optString("method", "POST").toUpperCase();
//        
//        if (targetUrl.isEmpty()) {
//            return ResponseContext.status(400)
//            .contentType("application/json")
//            .body("{\"status\":\"error\",\"message\":\"Missing target destination 'url' parameter inside request metadata block.\"}")
//            .build();
//        }
//
//        Log.i(TAG, " -> Unwrapping request envelope and executing curl-like dispatch to: " + targetUrl);
//
//        java.net.URL url = new java.net.URL(targetUrl);
//        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
//
//
//        // =========================================================================
//        // RECOVERY PATCH: BYPASS SSL CERTIFICATE ERRORS ON LEGACY ZEBRA TERMINAL
//        // =========================================================================
//        if (conn instanceof javax.net.ssl.HttpsURLConnection && android.os.Build.VERSION.SDK_INT <= 30) {
//            Log.w(TAG, "[ZEBRA BROKER SSL RECOVERY] API level <= 30 detected. Overriding certificate chain validation.");
//            
//            javax.net.ssl.HttpsURLConnection sslConn = (javax.net.ssl.HttpsURLConnection) conn;
//            
//            // Build an omnipotent, permissive TrustManager array to ignore frozen store limits
//            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
//                new javax.net.ssl.X509TrustManager() {
//                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
//                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
//                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
//                }
//            };
//            
//            // Mount the permissive managers to the active socket context
//            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
//            sc.init(null, trustAllCerts, new java.security.SecureRandom());
//            sslConn.setSSLSocketFactory(sc.getSocketFactory());
//            
//            // Bypass Hostname Verification mismatches completely
//            sslConn.setHostnameVerifier((hostname, session) -> true);
//        }
//        
//        conn.setRequestMethod(method);
//        conn.setDoInput(true);
//        conn.setConnectTimeout(customTimeoutMs);
//        conn.setReadTimeout(customTimeoutMs);
//
//        // 4. Extract and forward the target headers requested by the inner metadata block
//        if (innerRequest.has("headers")) {
//            JSONObject customHeaders = innerRequest.getJSONObject("headers");
//            java.util.Iterator<String> keys = customHeaders.keys();
//            while (keys.hasNext()) {
//                String key = keys.next();
//                    if (key == null) continue;
//                    String lowerKey = key.toLowerCase();
//                    // Filter out transport headers to prevent protocol clashing or manual payload length errors
//                    if (lowerKey.equals("host") || 
//                        lowerKey.equals("content-length") || 
//                        lowerKey.equals("connection") || 
//                        lowerKey.equals("accept-encoding")) {
//                        continue;
//                    }
//                    conn.setRequestProperty(key, customHeaders.getString(key));
//
//            }
//
//        }
//
//        // 5. Extract and cleanly stream the isolated text body downstream
//        if (innerRequest.has("body") && ("POST".equals(method) || "PUT".equals(method))) {
//            String cleanDownstreamBody = innerRequest.getString("body");
//            byte[] rawPayloadBytes = cleanDownstreamBody.getBytes(StandardCharsets.UTF_8);
//            
//            if (rawPayloadBytes.length > 0) {
//            conn.setDoOutput(true);
//            try (java.io.OutputStream os = conn.getOutputStream()) {
//                os.write(rawPayloadBytes);
//            }
//            }
//        }
//
//        // 6. Connect and process downstream server reaction
//        int responseCode = conn.getResponseCode();
//        JSONObject responseHeaders = new JSONObject();
//        for (java.util.Map.Entry<String, java.util.List<String>> entries : conn.getHeaderFields().entrySet()) {
//            String headerKey = entries.getKey();
//            if (headerKey != null && !entries.getValue().isEmpty()) {
//            
//            // RECOVERY PATCH: Concatenate multi-value headers (like multiple Set-Cookie properties)
//            StringBuilder combinedValues = new StringBuilder();
//            for (int i = 0; i < entries.getValue().size(); i++) {
//                combinedValues.append(entries.getValue().get(i));
//                if (i < entries.getValue().size() - 1) {
//                combinedValues.append(", ");
//                }
//            }
//            
//            // Force the header key name to lowercase to make it easy for your JS client to read
//            responseHeaders.put(headerKey.toLowerCase(), combinedValues.toString());
//            }
//        }
//
//        java.io.InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
//        byte[] responseBytes = new byte[0];
//        if (is != null) {
//            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
//            byte[] buffer = new byte[4096];
//            int length;
//            while ((length = is.read(buffer)) != -1) {
//            bos.write(buffer, 0, length);
//            }
//            responseBytes = bos.toByteArray();
//            is.close();
//        }
//
//        // 7. Enclose raw target payload output inside a wrapper returned back to the front-end fetch client
//        JSONObject wrapperResult = new JSONObject();
//        wrapperResult.put("status", responseCode);
//        wrapperResult.put("headers", responseHeaders);
//        wrapperResult.put("body", new String(responseBytes, StandardCharsets.UTF_8));
//
//        return ResponseContext.status(200)
//            .contentType("application/json")
//            .body(wrapperResult.toString())
//            .build();
//
//        } catch (Exception e) {
//        Log.e(TAG, e.toString());
//        return ResponseContext.status(500)
//            .contentType("application/json")
//            .body("{\"status\":\"error\",\"message\":\"Native broker envelope routing failed: " + e.getMessage() + "\"}")
//            .build();
//        }
//    }
//}
//--------------------------------------------------------------------------------
package com.example.app.services.http.net;

import android.content.Context;
import android.util.Log;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class NetController {

    private static final String TAG = "JS_CONSOLE_NetController";

    private final Set<String> trustedDomains = new HashSet<>();

    public NetController(Context context) {
        loadTrustedDomains(context);
    }

    private void loadTrustedDomains(Context context) {
        try {
            // Load from string array (easy & reliable)
            int arrayId = context.getResources().getIdentifier("trusted_domains", "array", context.getPackageName());
            if (arrayId != 0) {
                String[] domains = context.getResources().getStringArray(arrayId);
                for (String domain : domains) {
                    if (!domain.trim().isEmpty()) {
                        trustedDomains.add(domain.trim().toLowerCase());
                    }
                }
            }
            Log.i(TAG, "Loaded " + trustedDomains.size() + " trusted domains for proxy.");
        } catch (Exception e) {
            Log.w(TAG, "Failed to load trusted domains", e);
        }
    }

    private boolean isTrusted(String targetUrl) {
        if (targetUrl == null || targetUrl.isEmpty()) return false;

        try {
            java.net.URI uri = new java.net.URI(targetUrl);
            String host = uri.getHost();
            if (host == null) return false;

            host = host.toLowerCase().trim();

            for (String trusted : trustedDomains) {
                if (host.equals(trusted) || host.endsWith("." + trusted)) {
                    Log.d(TAG, "Trusted");
                    return true;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse URL for trust check: " + targetUrl, e);
        }
                    Log.d(TAG, "Not trusted");
        return false;
    }

    @RequestMapping(path = "/api/net/request", method = "POST")
    public ResponseContext proxyHttpRequest(RequestContext request) {
        Log.d(TAG,"public ResponseContext proxyHttpRequest(RequestContext request):begin");
        try {
            byte[] requestBodyBytes = request.getBody();
            if (requestBodyBytes == null || requestBodyBytes.length == 0) {
                return ResponseContext.status(400).contentType("application/json")
                    .body("{\"status\":\"error\",\"message\":\"Empty request body received by proxy broker.\"}")
                    .build();
            }

            String jsonConfig = new String(requestBodyBytes, StandardCharsets.UTF_8);
            JSONObject rootEnvelope = new JSONObject(jsonConfig);

            int customTimeoutMs = 15000;
            if (rootEnvelope.has("timeout_ms")) {
                customTimeoutMs = rootEnvelope.getInt("timeout_ms");
            } else {
                String timeoutParam = request.getQueryParam("timeout_ms");
                if (timeoutParam != null && !timeoutParam.isEmpty()) {
                    try {
                        customTimeoutMs = Integer.parseInt(timeoutParam);
                    } catch (NumberFormatException nfe) {
                        Log.w(TAG, "Invalid timeout parameter format.");
                    }
                }
            }

            if (!rootEnvelope.has("request")) {
                return ResponseContext.status(400).contentType("application/json")
                    .body("{\"status\":\"error\",\"message\":\"Missing 'request' object.\"}")
                    .build();
            }

            JSONObject innerRequest = rootEnvelope.getJSONObject("request");
            String targetUrl = innerRequest.optString("url", "");
            String method = innerRequest.optString("method", "POST").toUpperCase();

            if (targetUrl.isEmpty()) {
                return ResponseContext.status(400).contentType("application/json")
                    .body("{\"status\":\"error\",\"message\":\"Missing target destination 'url' parameter.\"}")
                    .build();
            }

            // === Play Store friendly trust check ===
            if (!isTrusted(targetUrl)) {
                Log.w(TAG, "🚫 BLOCKED untrusted request to: " + targetUrl);
                return ResponseContext.status(403).contentType("application/json")
                    .body("{\"status\":\"error\",\"message\":\"Target domain is not whitelisted.\"}")
                    .build();
            }

            Log.i(TAG, "✅ Trusted request: " + targetUrl);

            // ====================== YOUR ORIGINAL PROXY CODE ======================
            java.net.URL url = new java.net.URL(targetUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

            if (conn instanceof javax.net.ssl.HttpsURLConnection && android.os.Build.VERSION.SDK_INT <= 30) {
                Log.w(TAG, "[ZEBRA BROKER SSL RECOVERY] API level <= 30 detected.");
                javax.net.ssl.HttpsURLConnection sslConn = (javax.net.ssl.HttpsURLConnection) conn;
                javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }
                };
                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                sslConn.setSSLSocketFactory(sc.getSocketFactory());
                sslConn.setHostnameVerifier((hostname, session) -> true);
            }

            conn.setRequestMethod(method);
            conn.setDoInput(true);
            conn.setConnectTimeout(customTimeoutMs);
            conn.setReadTimeout(customTimeoutMs);
        

        // 4. Extract and forward the target headers requested by the inner metadata block
        if (innerRequest.has("headers")) {
            JSONObject customHeaders = innerRequest.getJSONObject("headers");
            java.util.Iterator<String> keys = customHeaders.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                    if (key == null) continue;
                    String lowerKey = key.toLowerCase();
                    // Filter out transport headers to prevent protocol clashing or manual payload length errors
                    if (lowerKey.equals("host") || 
                        lowerKey.equals("content-length") || 
                        lowerKey.equals("connection") || 
                        lowerKey.equals("accept-encoding")) {
                        continue;
                    }
                    conn.setRequestProperty(key, customHeaders.getString(key));

            }

        }

        // 5. Extract and cleanly stream the isolated text body downstream
        if (innerRequest.has("body") && ("POST".equals(method) || "PUT".equals(method))) {
            String cleanDownstreamBody = innerRequest.getString("body");
            byte[] rawPayloadBytes = cleanDownstreamBody.getBytes(StandardCharsets.UTF_8);
            
            if (rawPayloadBytes.length > 0) {
            conn.setDoOutput(true);
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(rawPayloadBytes);
            }
            }
        }

        // 6. Connect and process downstream server reaction
        int responseCode = conn.getResponseCode();
        JSONObject responseHeaders = new JSONObject();
        for (java.util.Map.Entry<String, java.util.List<String>> entries : conn.getHeaderFields().entrySet()) {
            String headerKey = entries.getKey();
            if (headerKey != null && !entries.getValue().isEmpty()) {
            
            // RECOVERY PATCH: Concatenate multi-value headers (like multiple Set-Cookie properties)
            StringBuilder combinedValues = new StringBuilder();
            for (int i = 0; i < entries.getValue().size(); i++) {
                combinedValues.append(entries.getValue().get(i));
                if (i < entries.getValue().size() - 1) {
                combinedValues.append(", ");
                }
            }
            
            // Force the header key name to lowercase to make it easy for your JS client to read
            responseHeaders.put(headerKey.toLowerCase(), combinedValues.toString());
            }
        }

        java.io.InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
        byte[] responseBytes = new byte[0];
        if (is != null) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) != -1) {
            bos.write(buffer, 0, length);
            }
            responseBytes = bos.toByteArray();
            is.close();
        }

        // 7. Enclose raw target payload output inside a wrapper returned back to the front-end fetch client
        JSONObject wrapperResult = new JSONObject();
        wrapperResult.put("status", responseCode);
        wrapperResult.put("headers", responseHeaders);
        wrapperResult.put("body", new String(responseBytes, StandardCharsets.UTF_8));

        return ResponseContext.status(200)
            .contentType("application/json")
            .body(wrapperResult.toString())
            .build();

        } catch (Exception e) {
        Log.e(TAG, e.toString());
        return ResponseContext.status(500)
            .contentType("application/json")
            .body("{\"status\":\"error\",\"message\":\"Native broker envelope routing failed: " + e.getMessage() + "\"}")
            .build();
        }






    }
}
