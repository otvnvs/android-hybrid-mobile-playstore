package com.example.app.services.example;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.BatteryManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.ActivityManager;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.content.pm.PackageInfo;

import com.example.app.MainActivity; 
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public class DeviceController {
    private static final String TAG = "DeviceController";

    public DeviceController() {}

    @RequestMapping(path = "/api/device/info", method = "GET")
    public ResponseContext getDeviceInfo(RequestContext request) {
        try {
            JSONObject root = new JSONObject();
            Context context = request.getAndroidContext();

            // 1. Hardware Metrics
            JSONObject hardware = new JSONObject();
            hardware.put("brand", Build.BRAND);
            hardware.put("device", Build.DEVICE);
            hardware.put("model", Build.MODEL);
            hardware.put("product", Build.PRODUCT);
            hardware.put("manufacturer", Build.MANUFACTURER);
            hardware.put("hardware", Build.HARDWARE);
            hardware.put("board", Build.BOARD);
            hardware.put("bootloader", Build.BOOTLOADER);
            hardware.put("display_build_id", Build.DISPLAY);
            root.put("hardware", hardware);

            // 2. OS Specifications
            JSONObject os = new JSONObject();
            os.put("release_version", Build.VERSION.RELEASE);
            os.put("sdk_int", Build.VERSION.SDK_INT);
            os.put("codename", Build.VERSION.CODENAME);
            os.put("incremental", Build.VERSION.INCREMENTAL);
            os.put("base_os", Build.VERSION.SDK_INT >= 23 ? Build.VERSION.BASE_OS : "unknown");
            os.put("security_patch", Build.VERSION.SDK_INT >= 23 ? Build.VERSION.SECURITY_PATCH : "unknown");
            root.put("os", os);

            // 3. Firmware Build Metadata
            JSONObject buildInfo = new JSONObject();
            buildInfo.put("fingerprint", Build.FINGERPRINT);
            buildInfo.put("id", Build.ID);
            buildInfo.put("type", Build.TYPE);
            buildInfo.put("user", Build.USER);
            buildInfo.put("host", Build.HOST);
            buildInfo.put("tags", Build.TAGS);
            buildInfo.put("time_epoch_ms", Build.TIME);
            root.put("build", buildInfo);
            // 4. CPU Mappings
            JSONObject cpu = new JSONObject();
            JSONArray supportedAbis = new JSONArray();
            if (Build.VERSION.SDK_INT >= 21) {
                for (String abi : Build.SUPPORTED_ABIS) { supportedAbis.put(abi); }
            } else {
                supportedAbis.put(Build.CPU_ABI);
                supportedAbis.put(Build.CPU_ABI2);
            }
            cpu.put("supported_abis", supportedAbis);
            root.put("cpu", cpu);

            // 5. Memory Profiles (RAM)
            JSONObject memory = new JSONObject();
            if (context != null) {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                if (am != null) {
                    am.getMemoryInfo(mi);
                    memory.put("avail_ram_bytes", mi.availMem);
                    memory.put("total_ram_bytes", Build.VERSION.SDK_INT >= 16 ? mi.totalMem : 0);
                    memory.put("low_memory_flag", mi.lowMemory);
                    memory.put("threshold_bytes", mi.threshold);
                    memory.put("status", "success");
                }
            } else { memory.put("status", "Context unavailable"); }
            root.put("memory", memory);

            // 6. Display Screen Metrics & Rendering Profiles (Activity Layer)
            JSONObject display = new JSONObject();
            boolean isHardwareAccelerated = false;

            if (context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;
                DisplayMetrics dm = new DisplayMetrics();
                if (Build.VERSION.SDK_INT >= 17) {
                    activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
                } else {
                    activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
                }
                display.put("width_pixels", dm.widthPixels);
                display.put("height_pixels", dm.heightPixels);
                display.put("density_dpi", dm.densityDpi);
                display.put("density_scale", dm.density);
                display.put("scaled_density_font", dm.scaledDensity);
                display.put("xdpi", dm.xdpi);
                display.put("ydpi", dm.ydpi);

                Window window = activity.getWindow();
                if (window != null) {
                    int flags = window.getAttributes().flags;
                    isHardwareAccelerated = (flags & WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED) != 0;
                }
                display.put("hardware_acceleration_enabled", isHardwareAccelerated);
                display.put("status", "MainActivity binding verified");
            } else if (context != null) {
                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                display.put("width_pixels", dm.widthPixels);
                display.put("height_pixels", dm.heightPixels);
                display.put("density_dpi", dm.densityDpi);
                display.put("density_scale", dm.density);
                display.put("scaled_density_font", dm.scaledDensity);
                display.put("hardware_acceleration_enabled", "unknown_context_fallback");
                display.put("status", "Resource Context fallback applied");
            } else { display.put("status", "Context unavailable"); }
            root.put("display", display);

            // 7. Internal System Partition Storage
            JSONObject storage = new JSONObject();
            try {
                File path = Environment.getDataDirectory();
                StatFs stat = new StatFs(path.getPath());
                long blockSize = Build.VERSION.SDK_INT >= 18 ? stat.getBlockSizeLong() : stat.getBlockSize();
                long availableBlocks = Build.VERSION.SDK_INT >= 18 ? stat.getAvailableBlocksLong() : stat.getAvailableBlocks();
                long totalBlocks = Build.VERSION.SDK_INT >= 18 ? stat.getBlockCountLong() : stat.getBlockCount();
                storage.put("available_storage_bytes", availableBlocks * blockSize);
                storage.put("total_storage_bytes", totalBlocks * blockSize);
                storage.put("status", "success");
            } catch (Exception e) {
                storage.put("status", "error");
                storage.put("error", e.getMessage());
            }
            root.put("storage", storage);

            // 8. WebView Engine Diagnostics
            JSONObject webviewEngine = new JSONObject();
            if (Build.VERSION.SDK_INT >= 26) {
                PackageInfo pi = WebView.getCurrentWebViewPackage();
                if (pi != null) {
                    webviewEngine.put("package_name", pi.packageName);
                    webviewEngine.put("version_name", pi.versionName);
                } else { webviewEngine.put("status", "unknown_or_embedded"); }
            } else { webviewEngine.put("status", "sdk_below_api26"); }
            root.put("webview_engine", webviewEngine);

            // 9. Power & Battery Status Metrics
            JSONObject battery = new JSONObject();
            if (context != null) {
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = context.registerReceiver(null, ifilter);
                if (batteryStatus != null) {
                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    int tempCelsiusTenths = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);

                    battery.put("percentage", (level / (float) scale) * 100);
                    battery.put("temperature_celsius", tempCelsiusTenths / 10.0);
                    battery.put("is_charging", status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
                    battery.put("plugged_source", chargePlug == BatteryManager.BATTERY_PLUGGED_USB ? "USB" : (chargePlug == BatteryManager.BATTERY_PLUGGED_AC ? "AC" : "Battery"));
                }
            } else { battery.put("status", "Context unavailable"); }
            root.put("battery", battery);

            // 10. Native System Localization & Locale
            JSONObject localeInfo = new JSONObject();
            Locale currentLocale = Locale.getDefault();
            localeInfo.put("language_tag", currentLocale.toLanguageTag());
            localeInfo.put("display_name", currentLocale.getDisplayName());
            localeInfo.put("country_iso", currentLocale.getCountry());
            root.put("locale", localeInfo);

            return ResponseContext.status(200)
                    .contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .body(root.toString())
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "Failed to compile device info dump", e);
            return buildErrorResponse(500, "Device inspection crash: " + e.getMessage());
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

