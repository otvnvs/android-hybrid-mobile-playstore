package com.example.app.services.res;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import com.example.app.MainActivity;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import org.json.JSONObject;

public class ResController {
    private static final String TAG = "ResController";

    public ResController() {}

    @RequestMapping(path="/api/res/string", method="GET")
    public ResponseContext getStringResourceValue(RequestContext request) {
        try {
            Log.i(TAG, " -> REST API [GET]: Intercepting string resource runtime query.");

            // 1. Resolve and validate activity context
            Context appCtx = request.getAndroidContext();
            if (!(appCtx instanceof MainActivity)) {
                return ResponseContext.status(500)
                    .contentType("application/json")
                    .body("{\"status\":\"error\",\"message\":\"Context context tracking configuration mismatch.\"}")
                    .build();
            }
            MainActivity activity = (MainActivity) appCtx;

            // 2. Extract target string key parameter
            String key = request.getQueryParam("key");
            if (key == null || key.trim().isEmpty()) {
                return ResponseContext.status(400)
                    .contentType("application/json")
                    .body("{\"status\":\"error\",\"message\":\"Missing mandatory 'key' query parameter.\"}")
                    .build();
            }

            String searchKey = key.trim();
            Resources res = activity.getResources();
            String packageName = activity.getPackageName();

            // 3. Resolve the key identifier dynamically from compiled app resources
            int resId = res.getIdentifier(searchKey, "string", packageName);

            if (resId == 0) {
                return ResponseContext.status(404)
                    .contentType("application/json")
                    .body("{\"status\":\"error\",\"message\":\"Requested key '" + searchKey + "' not found in strings.xml mappings.\"}")
                    .build();
            }

            // 4. Extract real string value and package confirmation payload
            String realValue = res.getString(resId);

            JSONObject payload = new JSONObject();
            payload.put("status", "success");
            payload.put("key", searchKey);
            payload.put("value", realValue);

            return ResponseContext.status(200)
                .contentType("application/json")
                .header("X-Server-Response-Engine", "Android-Native-JVM")
                .body(payload.toString())
                .build();

        } catch (Exception e) {
            Log.e(TAG, "Resource resolution pipeline crash", e);
            JSONObject errJson = new JSONObject();
            try {
                errJson.put("status", "error");
                errJson.put("message", "Internal pipeline failure: " + e.getMessage());
            } catch (Exception ignored) {}
            return ResponseContext.status(500)
                .contentType("application/json")
                .body(errJson.toString())
                .build();
        }
    }

@RequestMapping(path="/api/res/strings", method="GET")
public ResponseContext getAllStringResourcesMatrix(RequestContext request) {
    try {
        Log.i(TAG, " -> REST API [GET]: Initiating full-scale reflection sweep of compiled R.string definitions.");

        // 1. Resolve and validate activity context
        android.content.Context appCtx = request.getAndroidContext();
        if (!(appCtx instanceof com.example.app.MainActivity)) {
            return ResponseContext.status(500)
                .contentType("application/json")
                .body("{\"status\":\"error\",\"message\":\"Context context tracking configuration mismatch.\"}")
                .build();
        }
        com.example.app.MainActivity activity = (com.example.app.MainActivity) appCtx;

        android.content.res.Resources res = activity.getResources();
        String packageName = activity.getPackageName();

        // 2. Reflectively locate the dynamically generated R.string nested visibility class
        Class<?> rStringClass = Class.forName(packageName + ".R$string");
        java.lang.reflect.Field[] fields = rStringClass.getDeclaredFields();

        org.json.JSONObject stringDictionary = new org.json.JSONObject();
        int totalResolvedCount = 0;

        // 3. Iterate through all field properties inside the compiled R.string container
        for (java.lang.reflect.Field field : fields) {
            try {
                // Ensure the property tracks a standard compiled internal resource integer identifier
                if (field.getType() == int.class) {
                    String stringKeyName = field.getName();
                    int resId = field.getInt(null); // Static fields require no instance context

                    if (resId != 0) {
                        String realStringValue = res.getString(resId);
                        stringDictionary.put(stringKeyName, realStringValue);
                        totalResolvedCount++;
                    }
                }
            } catch (Exception fieldEx) {
                // Skip tracking failures on single hidden platform properties if present
                Log.w(TAG, "Skipping compilation trace field: " + field.getName() + " -> " + fieldEx.getMessage());
            }
        }

        // 4. Wrap the data payload response container 
        org.json.JSONObject result = new org.json.JSONObject();
        result.put("status", "success");
        result.put("package_name", packageName);
        result.put("total_strings_count", totalResolvedCount);
        result.put("strings_matrix", stringDictionary);

        return ResponseContext.status(200)
            .contentType("application/json")
            .header("X-Server-Response-Engine", "Android-Native-JVM")
            .body(result.toString())
            .build();

    } catch (ClassNotFoundException e) {
        Log.e(TAG, "Bypass aborted: R.string compilation class could not be resolved reflectively.", e);
        return ResponseContext.status(500)
            .contentType("application/json")
            .body("{\"status\":\"error\",\"message\":\"Reflective class discovery failed. Ensure ProGuard/R8 preserves R structure mapping rules.\"}")
            .build();
    } catch (Exception e) {
        Log.e(TAG, "Bulk string extraction framework crash", e);
        return ResponseContext.status(500)
            .contentType("application/json")
            .body("{\"status\":\"error\",\"message\":\"Bulk resolution pipeline execution error: " + e.getMessage() + "\"}")
            .build();
    }
}

}

