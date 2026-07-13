# Custom Micro-Framework Suite for Android Hybrid WebView

This directory houses a lightweight, high-performance, annotation-driven hybrid architecture that bridges web frontends (Vue/React/Vanilla JS) with native Android hardware layers. It functions entirely without third-party web dependencies or libraries, utilizing core Java reflection, low-level Dalvik scans, and strict thread-isolated message queues.

---

## 1. Directory Structure Blueprint

```text
./services/
├── RequestMapping.java         # Target annotation layer mapping HTTP routes/verbs
├── RequestContext.java         # Decoupled inbound container for URLs, headers, and params
├── ResponseContext.java        # Fluid response builder controlling statuses, headers, and bytes
├── StorageService.java         # Low-level file system driver managing recursive safe I/O
├── WebServiceRegistry.java     # Stateless reflection-based REST routing engine
│
├── WebSocketMapping.java       # Target annotation layer mapping incoming socket text messages
├── WebSocketOnOpen.java        # Lifecycle annotation targeting connection handshake hooks
├── WebSocketOnClose.java       # Lifecycle annotation targeting connection cleanup hooks
├── WebSocketSession.java       # Stateful data container controlling direct UI post threads
├── WebSocketServiceRegistry.java# Stateful reflection-based WebSocket scan and routing loop
│
├── telemetry/
│   ├── SensorTelemetryController.java # Functional controller exposing streaming endpoints
│   └── NativeSensorHardwareDriver.java# Hardware layer reading real-time OS physics matrices
│
└── WebScripts.java             # Source of truth for frontend injection polyfill strings
```

---

## 2. Core Architectural Engineering Design

### Stateless REST Proxy Gateway
*   **Request Interception:** Captures frontend actions inside `MyWebViewClient` and maps them natively. This bypasses browser-level Mixed Content (`HTTPS` -> `HTTP`) blocks.
*   **Body Caching System:** Employs a unique frontend injection hook (`WebScripts.INTERCEPT_SCRIPT`) that caches outgoing payloads via a synchronous `@JavascriptInterface` channel before requests fire, solving the invisible request body limitation of Android's `shouldInterceptRequest`.

### Stateful WebSocket Pipeline
*   **Isolate Class Polyfilling:** Redefines `window.WebSocket` constructor prototypes to cleanly break away from the engine's native C++ slot maps. This completely eliminates platform-level `TypeError: Illegal invocation` crashes.
*   **Coexistence Router:** Features automated URL token checks. It intercepts virtual local routes while letting external endpoints (like live cloud resources) pass directly to the standard Android browser web-stack.
*   **Resource Health Guardians:** Couples connection lifecycle hooks with native background teardowns. When a tab drops or closes, listeners are unregistered instantly to prevent zombie threads and background battery drain.

---

## 3. Direct Integration Framework Reference

Below is a complete implementation sample demonstrating how to set up entry hooks, stream high-frequency time-series datasets via `org.json`, and safely unregister physical OS listeners upon disconnect:

```java
package com.example.app.services.telemetry;

import com.example.app.services.WebSocketMapping;
import com.example.app.services.WebSocketOnOpen;
import com.example.app.services.WebSocketOnClose;
import com.example.app.services.WebSocketSession;
import android.util.Log;
import org.json.JSONObject;
import java.util.concurrent.ConcurrentHashMap;

public class SensorTelemetryController {
    private static final String TAG = "JAVA_TelemetryController";
    private final ConcurrentHashMap<String, NativeSensorHardwareDriver> activeDrivers = new ConcurrentHashMap<>();

    @WebSocketOnOpen(path = "/api/ws/telemetry/sensors")
    public void onTelemetryConnect(WebSocketSession session) {
        try {
            // 1. Dispatch Handshake Greeting
            JSONObject welcomeFrame = new JSONObject();
            welcomeFrame.put("status", "connected");
            welcomeFrame.put("message", "Welcome from Native Android JVM Lifecycle Handler");
            welcomeFrame.put("assigned_id", session.getSocketId());
            session.send(welcomeFrame.toString());
            
            // 2. Initialize and Boot Hardware Thread Drivers
            NativeSensorHardwareDriver driver = new NativeSensorHardwareDriver(session.getContext(), session);
            activeDrivers.put(session.getSocketId(), driver);
            driver.startStreaming();
        } catch (Exception e) {
            Log.e(TAG, "Initialization failure: " + e.getMessage());
        }
    }

    @WebSocketMapping(path = "/api/ws/telemetry/sensors")
    public void handleIncomingTelemetryCommands(WebSocketSession session, String message) {
        Log.d(TAG, "Received operational instruction frame: " + message);
    }

    @WebSocketOnClose(path = "/api/ws/telemetry/sensors")
    public void onTelemetryDisconnect(WebSocketSession session) {
        // 3. Clear Active Caches and Unregister Listeners to Save Battery Health
        NativeSensorHardwareDriver driver = activeDrivers.remove(session.getSocketId());
        if (driver != null) {
            driver.stopStreaming();
            Log.d(TAG, "-> Native SensorEventListener unregistered completely for: " + session.getSocketId());
        }
    }
}
```

