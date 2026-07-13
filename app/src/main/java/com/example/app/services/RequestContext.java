package com.example.app.services;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceRequest;
import com.example.app.AppConfig;
import java.util.HashMap;
import java.util.Map;

public class RequestContext {
    private final Context androidContext;
    private final AppConfig appConfig;
    private final String method;
    private final String path;
    private final String protocol;
    private final String domain;
    private final String httpVersion;
    private final Map<String, String> queryParams = new HashMap<>();
    private final Map<String, String> pathParams = new HashMap<>();
    private final Map<String, String> headers;
    private final byte[] body;
    private final String queryString;

    public RequestContext(Context context, AppConfig config, WebResourceRequest request, String path) {
        this.androidContext = context;
        this.appConfig = config;
        this.method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
        this.httpVersion = "HTTP/1.1";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Map<String, String> rawHeaders = request.getRequestHeaders();
            this.headers = rawHeaders != null ? rawHeaders : new HashMap<>();
        } else {
            this.headers = new HashMap<>();
        }

        Uri uri = request.getUrl();
        this.protocol = uri.getScheme();
        this.domain = uri.getHost();
        this.queryString = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ? uri.getQuery() : null;

        String calculatedPath = path;
        if (uri.getPath() != null && path != null && path.contains("proxy")) {
            String rawUriString = uri.toString();
            int proxyIndex = rawUriString.indexOf("/api/net/proxy/");
            if (proxyIndex != -1) {
                calculatedPath = rawUriString.substring(proxyIndex);
                if (calculatedPath.contains("?")) {
                    calculatedPath = calculatedPath.substring(0, calculatedPath.indexOf("?"));
                }
            }
        }
        this.path = calculatedPath;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && uri.getQuery() != null) {
            for (String queryParamName : uri.getQueryParameterNames()) {
                this.queryParams.put(queryParamName, uri.getQueryParameter(queryParamName));
            }
        }

        // =========================================================================
        // REFACTORED STANDARD BODY ACCESS LAYER
        // Pulls the cached request payload dynamically right before the controller fires
        // =========================================================================
        //if ("POST".equals(this.method) || "PUT".equals(this.method) || "PATCH".equals(this.method)) {
        if ("POST".equals(this.method) || "PUT".equals(this.method) || "PATCH".equals(this.method) || "DELETE".equals(this.method)) {
            this.body = com.example.app.services.AndroidBridge.getAndClearBody(this.method, calculatedPath);
        } else {
            this.body = new byte[0];
        }
    }

    public void addPathParam(String key, String value) { this.pathParams.put(key, value); }
    public Context getAndroidContext() { return androidContext; }
    public AppConfig getAppConfig() { return appConfig; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getProtocol() { return protocol; }
    public String getDomain() { return domain; }
    public String getHttpVersion() { return httpVersion; }
    public String getQueryParam(String key) { return queryParams.get(key); }
    public String getPathParam(String key) { return pathParams.get(key); }
    public Map<String, String> getHeaders() { return headers; }
    public String getHeader(String key) { return headers.get(key); }
    public byte[] getBody() { return body; }
    public String getQueryString() { return queryString; }
}

