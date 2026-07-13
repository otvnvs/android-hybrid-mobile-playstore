package com.example.app.services.maintenance;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.example.app.MainActivity;
import com.example.app.AppConfig;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import com.example.app.services.WebServiceRegistry;
import com.example.app.StorageManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.nio.charset.StandardCharsets;

public class NetController {
    private static final String TAG = "NetController";
    private static volatile String currentStatusMessage = "Idle";
    // =========================================================================
    // NET - New
    // =========================================================================
    private static final String DIAGNOSTIC_HOST = "google.com";

    public NetController () {
    }

    // Set a root storage sandbox folder inside Documents
//    private File getStorageRoot() {
//        //return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AppSandbox");
//        return Environment.getExternalStorageDirectory();
//    }
    /**
     * ◄ DYNAMIC STORAGE STRATEGY ROUTER
     * Dynamically anchors the file download path root based on your active panel configurations.
     */
    private File getStorageRoot(RequestContext request) {
        AppConfig config = request.getAppConfig();
        Context context = request.getAndroidContext();
        if (config != null && config.isPublicWorkspaceEnabled()) {
            return Environment.getExternalStorageDirectory(); // /storage/emulated/0
        } else {
            return context.getFilesDir(); // /data/user/0/com.example.app/files
        }
    }

    /**
     * Helper to resolve path inputs while preventing path traversal attacks
     */
//    private File resolveSafeFile(String relativePath) throws IOException {
//        File root = getStorageRoot();
//        if (!root.exists()) root.mkdirs();
//        if (relativePath == null || relativePath.isEmpty()) return root;
//        
//        File target = new File(root, relativePath);
//        if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
//            throw new SecurityException("Directory traversal attack detected!");
//        }
//        return target;
//    }
    private File resolveSafeFile(RequestContext request, String relativePath) throws IOException {
        File root = getStorageRoot(request);
        if (relativePath == null || relativePath.isEmpty()) return root;
        
        File target = new File(root, relativePath);
        // Canonical guardrail to check for directory traversal path manipulation exploits
        if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
            throw new SecurityException("Directory traversal validation escape attempt blocked cleanly.");
        }
        return target;
    }


    // =========================================================================
    // NET
    // =========================================================================

	@RequestMapping(path="/api/net/request", method="POST")
	public ResponseContext proxyHttpRequest(RequestContext request) {
	    try {
		// Read the incoming JSON configuration envelope
		byte[] requestBodyBytes = request.getBody();
		if (requestBodyBytes == null || requestBodyBytes.length == 0) {
		    return ResponseContext.status(400)
			.contentType("application/json")
			.body("{\"status\":\"error\",\"message\":\"Empty request body received by proxy broker.\"}")
			.build();
		}

		// Read and parse the root-level incoming JSON configuration envelope
		String jsonConfig = new String(requestBodyBytes, StandardCharsets.UTF_8);
		JSONObject rootEnvelope = new JSONObject(jsonConfig);

		// 1. Process Timeout Configuration (Prioritise payload config, fall back to query param, then default 15s)
		int customTimeoutMs = 15000; 
		if (rootEnvelope.has("timeout_ms")) {
		    customTimeoutMs = rootEnvelope.getInt("timeout_ms");
		} else {
		    String timeoutParam = request.getQueryParam("timeout_ms");
		    if (timeoutParam != null && !timeoutParam.isEmpty()) {
			try {
			    customTimeoutMs = Integer.parseInt(timeoutParam);
			} catch (NumberFormatException nfe) {
			    Log.w(TAG, "Invalid timeout parameter format, falling back to default.");
			}
		    }
		}

		// 2. Validate existence of the inner "request" metadata block
		if (!rootEnvelope.has("request")) {
		    return ResponseContext.status(400)
			.contentType("application/json")
			.body("{\"status\":\"error\",\"message\":\"Missing 'request' object containing delivery metadata.\"}")
			.build();
		}
		JSONObject innerRequest = rootEnvelope.getJSONObject("request");

		// 3. Extract the target downstream destination parameters
		String targetUrl = innerRequest.optString("url", "");
		String method = innerRequest.optString("method", "POST").toUpperCase();
		
		if (targetUrl.isEmpty()) {
		    return ResponseContext.status(400)
			.contentType("application/json")
			.body("{\"status\":\"error\",\"message\":\"Missing target destination 'url' parameter inside request metadata block.\"}")
			.build();
		}

		Log.i(TAG, " -> Unwrapping request envelope and executing curl-like dispatch to: " + targetUrl);

		java.net.URL url = new java.net.URL(targetUrl);
		java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();


        // =========================================================================
        // RECOVERY PATCH: BYPASS SSL CERTIFICATE ERRORS ON LEGACY ZEBRA TERMINAL
        // =========================================================================
        if (conn instanceof javax.net.ssl.HttpsURLConnection && android.os.Build.VERSION.SDK_INT <= 30) {
            Log.w(TAG, "[ZEBRA BROKER SSL RECOVERY] API level <= 30 detected. Overriding certificate chain validation.");
            
            javax.net.ssl.HttpsURLConnection sslConn = (javax.net.ssl.HttpsURLConnection) conn;
            
            // Build an omnipotent, permissive TrustManager array to ignore frozen store limits
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
            };
            
            // Mount the permissive managers to the active socket context
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            sslConn.setSSLSocketFactory(sc.getSocketFactory());
            
            // Bypass Hostname Verification mismatches completely
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



//	@RequestMapping(path="/api/net/download", method="GET")
//	public ResponseContext downloadFileRemote(RequestContext request) {
//	    Log.d(TAG, "public ResponseContext downloadFileRemote(RequestContext request):begin");
//	    try {
//		String sourceUrl = request.getQueryParam("url");
//		String targetPath = request.getQueryParam("path");
//
//		Log.d(TAG, "Initial Target Request -> URL: " + sourceUrl + " | Path: " + targetPath);
//
//		if (sourceUrl == null || sourceUrl.isEmpty() || targetPath == null || targetPath.isEmpty()) {
//		    return buildErrorResponse(400, "Bad Request: Missing parameters 'url' or 'path'.");
//		}
//
//		File targetFile = resolveSafeFile(targetPath);
//		File parentDir = targetFile.getParentFile();
//		if (parentDir != null && !parentDir.exists()) {
//		    parentDir.mkdirs();
//		}
//
//		java.net.HttpURLConnection conn = null;
//		int responseCode = -1;
//		int redirectCount = 0;
//		final int MAX_REDIRECTS = 5;
//
//		// Explicitly monitor redirection protocol routing changes
//		while (redirectCount < MAX_REDIRECTS) {
//		    Log.d(TAG, "Pipeline connecting to step [" + redirectCount + "]: " + sourceUrl);
//		    
//		    java.net.URL url = new java.net.URL(sourceUrl);
//		    conn = (java.net.HttpURLConnection) url.openConnection();
//		    conn.setRequestMethod("GET");
//		    
//		    // Disable default follower to handle protocol-crossing domains manually
//		    conn.setInstanceFollowRedirects(false); 
//		    conn.setConnectTimeout(15000);
//		    conn.setReadTimeout(15000);
//
//		    // Crucial browser string layout to bypass GitHub firewalls
//		    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
//
//		    // ONLY apply special authorization or custom request parameters on step 0 (GitHub)
//		    // AWS S3 bucket endpoints will throw a signature mismatch if these are present
//		    if (redirectCount == 0) {
//			conn.setRequestProperty("Accept", "application/vnd.github+json");
//		    }
//
//		    responseCode = conn.getResponseCode();
//		    Log.d(TAG, "Server responded to step [" + redirectCount + "] with code: HTTP " + responseCode);
//
//		    // Capture Redirection Status Blocks (301, 302, 303, 307, 308)
//		    if (responseCode == 301 || responseCode == 302 || responseCode == 303 
//			|| responseCode == 307 || responseCode == 308) {
//			
//			String locationHeader = conn.getHeaderField("Location");
//			Log.d(TAG, "Intercepted location redirect target: " + locationHeader);
//			
//			if (locationHeader == null || locationHeader.isEmpty()) {
//			    throw new IOException("Redirect header location string returned empty data chunks.");
//			}
//
//			// Deal with relative paths safely if the host is dropped
//			if (locationHeader.startsWith("/")) {
//			    sourceUrl = url.getProtocol() + "://" + url.getHost() + locationHeader;
//			} else {
//			    sourceUrl = locationHeader;
//			}
//
//			redirectCount++;
//			conn.disconnect();
//			continue; // Fire next connection hop cleanly
//		    }
//		    break; // Clear connection profile target found
//		}
//
//		if (responseCode < 200 || responseCode >= 300) {
//		    Log.e(TAG, "Download terminated with final structural error code: HTTP " + responseCode);
//		    return buildErrorResponse(responseCode, "Remote server returned failure code: " + responseCode);
//		}
//
//		// Verify the content type coming back from the server isn't text/html
//		String contentType = conn.getContentType();
//		Log.d(TAG, "Verified payload content envelope type: " + contentType);
//		
//		if (contentType != null && contentType.contains("text/html")) {
//		    Log.w(TAG, "Warning: Server is returning text/html content instead of raw binary application data streams!");
//		}
//
//		// Pipe binary stream exactly as you wrote it
//		Log.d(TAG, "Streaming high-fidelity file payload binaries straight onto flash block sectors...");
//		long bytesWrittenTotal = 0;
//		
//		try (java.io.InputStream is = conn.getInputStream();
//		     java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile)) {
//
//		    byte[] buffer = new byte[8192];
//		    int bytesRead;
//		    while ((bytesRead = is.read(buffer)) != -1) {
//			fos.write(buffer, 0, bytesRead);
//			bytesWrittenTotal += bytesRead;
//		    }
//		    Log.d(TAG, "Binary transfer complete. Total raw streaming bytes captured: " + bytesWrittenTotal);
//		} finally {
//		    if (conn != null) conn.disconnect();
//		}
//
//		if (targetPath.contains("Download")) {
//		    android.media.MediaScannerConnection.scanFile(
//			request.getAndroidContext(),
//			new String[]{targetFile.getAbsolutePath()},
//			null,
//			null
//		    );
//		}
//
//		JSONObject result = new JSONObject();
//		result.put("status", "success");
//		result.put("message", "Resource downloaded successfully via native pipeline.");
//		result.put("local_path", targetPath);
//		result.put("file_size_bytes", targetFile.length());
//
//		return ResponseContext.status(200)
//			.contentType("application/json")
//			.body(result.toString())
//			.build();
//
//	    } catch (SecurityException se) {
//		return buildErrorResponse(403, "Directory traversal safety violation: " + se.getMessage());
//	    } catch (Exception e) {
//		return buildErrorResponse(500, "Native download executor pipeline failed: " + e.getMessage());
//	    }
//	}

    @RequestMapping(path="/api/net/download", method="GET")
    public ResponseContext downloadFileRemote(RequestContext request) {
        Log.d(TAG, "public ResponseContext downloadFileRemote(RequestContext request):begin");
        try {
            String sourceUrl = request.getQueryParam("url");
            String targetPath = request.getQueryParam("path");
            Log.d(TAG, "Initial Target Request -> URL: " + sourceUrl + " | Path: " + targetPath);
            
            if (sourceUrl == null || sourceUrl.isEmpty() || targetPath == null || targetPath.isEmpty()) {
                return buildErrorResponse(400, "Bad Request: Missing parameters 'url' or 'path'.");
            }

            // ◄ ALIGNMENT FIX: Resolved path parameter safely using our adaptive context router helper
            File targetFile = resolveSafeFile(request, targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            java.net.HttpURLConnection conn = null;
            int responseCode = -1;
            int redirectCount = 0;
            final int MAX_REDIRECTS = 5;

            while (redirectCount < MAX_REDIRECTS) {
                Log.d(TAG, "Pipeline connecting to step [" + redirectCount + "]: " + sourceUrl);
                java.net.URL url = new java.net.URL(sourceUrl);
                conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
                
                if (redirectCount == 0) {
                    conn.setRequestProperty("Accept", "application/vnd.github+json");
                }
                
                responseCode = conn.getResponseCode();
                Log.d(TAG, "Server responded to step [" + redirectCount + "] with code: HTTP " + responseCode);
                
                if (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308) {
                    String locationHeader = conn.getHeaderField("Location");
                    Log.d(TAG, "Intercepted location redirect target: " + locationHeader);
                    if (locationHeader == null || locationHeader.isEmpty()) {
                        throw new IOException("Redirect header location string returned empty data chunks.");
                    }
                    if (locationHeader.startsWith("/")) {
                        sourceUrl = url.getProtocol() + "://" + url.getHost() + locationHeader;
                    } else {
                        sourceUrl = locationHeader;
                    }
                    redirectCount++;
                    conn.disconnect();
                    continue;
                }
                break;
            }

            if (responseCode < 200 || responseCode >= 300) {
                Log.e(TAG, "Download terminated with final structural error code: HTTP " + responseCode);
                return buildErrorResponse(responseCode, "Remote server returned failure code: " + responseCode);
            }

            String contentType = conn.getContentType();
            Log.d(TAG, "Verified payload content envelope type: " + contentType);
            
            Log.d(TAG, "Streaming high-fidelity file payload binaries straight onto flash block sectors...");
            long bytesWrittenTotal = 0;
            
            try (java.io.InputStream is = conn.getInputStream();
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    bytesWrittenTotal += bytesRead;
                }
                Log.d(TAG, "Binary transfer complete. Total raw streaming bytes captured: " + bytesWrittenTotal);
            } finally {
                if (conn != null) conn.disconnect();
            }

            if (targetPath.contains("Download")) {
                android.media.MediaScannerConnection.scanFile(request.getAndroidContext(), new String[]{targetFile.getAbsolutePath()}, null, null);
            }

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("message", "Resource downloaded successfully via native pipeline.");
            result.put("local_path", targetPath);
            result.put("file_size_bytes", targetFile.length());
            return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();
            
        } catch (SecurityException se) {
            return buildErrorResponse(403, "Directory traversal safety violation: " + se.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(500, "Native download executor pipeline failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    /**
     * Inline helper for generating uniform error messaging structures cleanly
     */
    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code).body(errJson.toString()).build();
    }


    // =========================================================================
    // NET - New
    // =========================================================================

    @RequestMapping(path = "/api/network/diagnostics", method = "GET")
    public ResponseContext getNetworkDiagnostics(RequestContext request) {
        try {
            JSONObject root = new JSONObject();
            android.content.Context context = request.getAndroidContext();

            // 1. Core Interface Connectivity Information
            JSONObject interfaces = new JSONObject();
            String transportType = "NONE";
            int downstreamKbps = 0;
            int upstreamKbps = 0;
            boolean isMetered = true;

            if (context != null) {
                android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);

                if (cm != null) {
                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                        android.net.Network activeNetwork = cm.getActiveNetwork();
                        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                        if (caps != null) {
                            if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                                transportType = "WIFI";
                            } else if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                transportType = "CELLULAR";
                            } else if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)) {
                                transportType = "ETHERNET";
                            } else if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)) {
                                transportType = "VPN";
                            }

                            downstreamKbps = caps.getLinkDownstreamBandwidthKbps();
                            upstreamKbps = caps.getLinkUpstreamBandwidthKbps();
                            isMetered = cm.isActiveNetworkMetered();
                        }
                    } else {
                        android.net.NetworkInfo info = cm.getActiveNetworkInfo();
                        if (info != null && info.isConnected()) {
                            transportType = info.getTypeName().toUpperCase();
                            isMetered = cm.isActiveNetworkMetered();
                        }
                    }
                }
            } else {
                interfaces.put("status", "Context unavailable");
            }

            interfaces.put("active_transport", transportType);
            interfaces.put("link_downstream_kbps", downstreamKbps);
            interfaces.put("link_upstream_kbps", upstreamKbps);
            interfaces.put("is_network_metered", isMetered);
            root.put("interfaces", interfaces);

            // 2. Global System Proxy Configuration Checks
            JSONObject proxyInfo = new JSONObject();
            String proxyHost = System.getProperty("http.proxyHost");
            String proxyPort = System.getProperty("http.proxyPort");
            
            proxyInfo.put("is_proxy_active", (proxyHost != null && !proxyHost.isEmpty()));
            proxyInfo.put("detected_host", proxyHost != null ? proxyHost : "none");
            proxyInfo.put("detected_port", proxyPort != null ? proxyPort : "none");
            root.put("system_proxy", proxyInfo);

            // 3. Dynamic DNS Resolution Performance Testing
            JSONObject dnsPerformance = new JSONObject();
            long startDnsTime = System.currentTimeMillis();
            boolean dnsSuccess = false;
            String resolvedIp = "unresolved";

            try {
                java.net.InetAddress address = java.net.InetAddress.getByName(DIAGNOSTIC_HOST);
                resolvedIp = address.getHostAddress();
                dnsSuccess = true;
            } catch (Exception e) {
                resolvedIp = "failed: " + e.getMessage();
            }
            long durationDnsMs = System.currentTimeMillis() - startDnsTime;

            dnsPerformance.put("diagnostic_target_host", DIAGNOSTIC_HOST);
            dnsPerformance.put("resolution_successful", dnsSuccess);
            dnsPerformance.put("resolved_ip_address", resolvedIp);
            dnsPerformance.put("resolution_latency_ms", dnsSuccess ? durationDnsMs : -1);
            root.put("dns_perf", dnsPerformance);

            return ResponseContext.status(200)
                    .contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .body(root.toString())
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "Network diagnostics collection engine crash", e);
            return buildErrorResponse(500, "Network inspection pipeline error: " + e.getMessage());
        }
    }
}
