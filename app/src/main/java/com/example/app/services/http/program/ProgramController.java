package com.example.app.services.example;

import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.content.Context;
import android.content.pm.PackageInfo;

import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import org.json.JSONObject;

public class ProgramController {
    private static final String TAG = "ProgramController";
    // Captures the exact hardware tick moment the class is loaded into memory by the app process
    private static final long APP_START_TIME_MS = SystemClock.elapsedRealtime();


    public ProgramController() {}


    @RequestMapping(path = "/api/program/info", method = "GET")
    public ResponseContext getProgramInfo(RequestContext request) {
        try {
            JSONObject root = new JSONObject();
            Context context = request.getAndroidContext();

            // 1. Process Container Metadata
            JSONObject processInfo = new JSONObject();
            processInfo.put("pid", android.os.Process.myPid());
            processInfo.put("uid", android.os.Process.myUid());
            processInfo.put("is_64bit", android.os.Process.is64Bit());
            root.put("process", processInfo);

            // 2. JVM Virtual Machine Heap Memory Allocations
            JSONObject jvmMemory = new JSONObject();
            Runtime runtime = Runtime.getRuntime();
            jvmMemory.put("free_heap_bytes", runtime.freeMemory());
            jvmMemory.put("total_heap_bytes", runtime.totalMemory());
            jvmMemory.put("max_heap_bytes", runtime.maxMemory());
            root.put("jvm_memory", jvmMemory);

            // 3. Thread Group Concurrency Counts
            JSONObject threading = new JSONObject();
            threading.put("active_thread_count", Thread.activeCount());
            root.put("threading", threading);

            // 4. FIXED: Application Lifespan Timeline Tracker
            JSONObject timeline = new JSONObject();
            long currentTick = SystemClock.elapsedRealtime();
            long trueUptimeMs = currentTick - APP_START_TIME_MS; // Real application uptime duration
            
            timeline.put("device_boot_uptime_ms", currentTick);
            timeline.put("app_uptime_ms", trueUptimeMs); 
            root.put("timeline", timeline);

            // 5. App Package Metadata Specification
            JSONObject packageInfoObj = new JSONObject();
            if (context != null) {
                String pkgName = context.getPackageName();
                PackageInfo pi = context.getPackageManager().getPackageInfo(pkgName, 0);
                packageInfoObj.put("package_name", pkgName);
                packageInfoObj.put("version_name", pi.versionName);
                packageInfoObj.put("target_sdk", context.getApplicationInfo().targetSdkVersion);
                
                if (Build.VERSION.SDK_INT >= 28) {
                    packageInfoObj.put("version_code", pi.getLongVersionCode());
                } else {
                    packageInfoObj.put("version_code", pi.versionCode);
                }
                packageInfoObj.put("first_install_time", pi.firstInstallTime);
                packageInfoObj.put("status", "success");
            } else {
                packageInfoObj.put("status", "Context unavailable");
            }
            root.put("package", packageInfoObj);

            return ResponseContext.status(200)
                    .contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .body(root.toString())
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "Failed to compile program status metrics", e);
            return buildErrorResponse(500, "Program status runtime inspection failure: " + e.getMessage());
        }
    }

    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code)
                .contentType("application/json")
                .body(errJson.toString())
                .build();
    }
}
