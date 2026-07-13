package com.example.app.services.example;

import android.util.Log;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import org.json.JSONObject;

public class VersionController {
    private static final String TAG = "VersionController";

    public VersionController() {}

    @RequestMapping(path="/api/version", method="GET")
    public ResponseContext getSystemVersionSignature(RequestContext request) {
        try {
            Log.i(TAG, " -> REST API [GET]: Processing system build version signature query request.");
            
            com.example.app.AppConfig appConfig = request.getAppConfig();
            if (appConfig == null) {
                return ResponseContext.status(500)
                    .contentType("application/json")
                    .body("{\"status\":\"error\",\"message\":\"Application configuration tracking engine uninitialized.\"}")
                    .build();
            }

            String buildVersion = appConfig.getAppVersionTag();

            JSONObject resultJson = new JSONObject();
            resultJson.put("status", "success");
            resultJson.put("version", buildVersion);
            resultJson.put("build_engine", "JVM-Bridge-Embedded");

            return ResponseContext.status(200)
                .contentType("application/json")
                .header("X-Server-Response-Engine", "Android-Native-JVM")
                .body(resultJson.toString())
                .build();

        } catch (Exception e) {
            Log.e(TAG, "Version processing pipeline encountered an error", e);
            return ResponseContext.status(500)
                .contentType("application/json")
                .body("{\"status\":\"error\",\"message\":\"Internal metadata retrieval exception.\"}")
                .build();
        }
    }
}

