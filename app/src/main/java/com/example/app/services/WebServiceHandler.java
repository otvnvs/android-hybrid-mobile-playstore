package com.example.app.services;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import com.example.app.AppConfig;

public interface WebServiceHandler {
    /**
     * Checks if this handler matches the requested path and method.
     */
    boolean canHandle(String path, String method);

    /**
     * Executes the service logic and returns the WebResourceResponse.
     */
    WebResourceResponse handle(Context context, AppConfig config, WebResourceRequest request, Uri uri);
}

