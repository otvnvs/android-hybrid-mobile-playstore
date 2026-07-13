package com.example.app.services.example;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

public class ExampleController {
    private static final String TAG = "ExampleController";

    // Constructor remains completely empty and un-modified
    public ExampleController() {}

    @RequestMapping(path="/api/example/get-test", method="GET")
    public ResponseContext testGetMethod(RequestContext request) {
        try {
            String trackingId = request.getQueryParam("tracking_id");
            String filterType = request.getQueryParam("filter");

            // Extract context directly out of the incoming request wrapper context
            Context context = request.getAndroidContext();
            showNativeToast(context, "GET Endpoint Intercepted Natively!");

            JSONObject result = new JSONObject();
            result.put("status", "processed");
            result.put("received_tracking_id", trackingId != null ? trackingId : "none");
            result.put("received_filter", filterType != null ? filterType : "none");

            return ResponseContext.status(200)
                    .contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .header("X-Echo-Tracking-ID", trackingId != null ? trackingId : "none")
                    .body(result.toString())
                    .build();
        } catch (Exception e) {
            return buildErrorResponse(request, 500, e.getMessage());
        }
    }

    private ResponseContext handleMutation(RequestContext request) {
        try {
            String activeMethod = request.getMethod().toUpperCase();
            String customResponseEchoMsg = "Default Echo response";
            int targetStatusCode = 200;

            byte[] bodyBytes = request.getBody();
            if (bodyBytes != null && bodyBytes.length > 0) {
                String rawBodyText = new String(bodyBytes, StandardCharsets.UTF_8);
                JSONObject bodyJson = new JSONObject(rawBodyText);
                targetStatusCode = bodyJson.optInt("requested_status_code", 200);
                customResponseEchoMsg = bodyJson.optString("message_payload", "Default Echo response");
            }

            // Extract context dynamically and show toast matching the execution params
            Context context = request.getAndroidContext();
            showNativeToast(context, "Native " + activeMethod + " Intercepted: " + customResponseEchoMsg);

            JSONObject result = new JSONObject();
            result.put("echo_method", activeMethod);
            result.put("echo_message", customResponseEchoMsg);
            result.put("payload_integrity_check", true);

            return ResponseContext.status(targetStatusCode)
                    .contentType("application/json")
                    .header("X-Processed-By-Method", activeMethod)
                    .header("Cache-Control", "no-store, max-age=0")
                    .body(result.toString())
                    .build();
        } catch (Exception e) {
            return buildErrorResponse(request, 400, "Mutation runtime violation: " + e.getMessage());
        }
    }

    // Thread-safe dispatch assistant method mapping toast onto UI loop thread
    private void showNativeToast(final Context context, final String message) {
        if (context == null) {
            Log.w(TAG, "Skipping native toast display; Android context lookup returned null.");
            return;
        }
        
        // Web Interceptor threads must step onto Main Thread to draw UI components
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ResponseContext buildErrorResponse(RequestContext request, int code, String message) {
        if (request != null) {
            showNativeToast(request.getAndroidContext(), "JVM Error [" + code + "]: " + message);
        }
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code).contentType("application/json").body(errJson.toString()).build();
    }

    @RequestMapping(path="/api/example/mutation-test", method="POST")
    public ResponseContext testPostMethod(RequestContext request) { return handleMutation(request); }

    @RequestMapping(path="/api/example/mutation-test", method="PUT")
    public ResponseContext testPutMethod(RequestContext request) { return handleMutation(request); }

    @RequestMapping(path="/api/example/mutation-test", method="PATCH")
    public ResponseContext testPatchMethod(RequestContext request) { return handleMutation(request); }

    @RequestMapping(path="/api/example/mutation-test", method="DELETE")
    public ResponseContext testDeleteMethod(RequestContext request) { return handleMutation(request); }
}

