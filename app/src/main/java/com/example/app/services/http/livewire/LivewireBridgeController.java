package com.example.app.services.livewire;

import android.util.Log;
import android.webkit.WebView;
import com.example.app.services.WebSocketMapping;
import com.example.app.services.WebSocketOnClose;
import com.example.app.services.WebSocketOnOpen;
import com.example.app.services.WebSocketSession;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LivewireBridgeController {
    private static final String TAG = "JS_CONSOLE_JAVA_LivewireBridge";

    // Maps local WebView WebSocket socketIds to their corresponding long-running laptop TCP tunnel workers
    private final ConcurrentHashMap<String, LivewireTunnelWorker> activeSessionWorkers = new ConcurrentHashMap<>();
    private final ExecutorService tunnelThreadPool = Executors.newCachedThreadPool();

    public static WebView globalWebViewFallbackInstance = null;

    public LivewireBridgeController() {
        Log.d(TAG, "LivewireBridgeController direct WebSocket engine compiled successfully.");
    }

//    @WebSocketOnOpen(path = "/api/ws/livewire/bridge")
//    public void onBridgeSocketOpen(WebSocketSession session) {
//        Log.d(TAG, "📡 Local WebSocket open handshake intercepted for session: " + session.getSocketId());
//
//        // Extract the laptop target and connection mode directly using your custom parameter parser
//        String targetLaptopAddress = session.getQueryParam("target");
//        String connectionMode = session.getQueryParam("mode");
//
//        Log.d(TAG, "📦 Parsed parameters -> target: '" + targetLaptopAddress + "', mode: '" + connectionMode + "'");
//
//        if (targetLaptopAddress == null || targetLaptopAddress.isEmpty()) {
//            sendJsonError(session, "Missing critical laptop 'target' query parameter.");
//            session.send("{\"event\":\"tunnel_destroyed\"}");
//            return;
//        }
//
//        WebView webView = globalWebViewFallbackInstance;
//        if (webView == null) {
//            Log.e(TAG, "❌ Aborting tunnel: Static global fallback WebView instance is uninitialized.");
//            sendJsonError(session, "WebView user interface context reference is null on host layout.");
//            return;
//        }
//
//        try {
//            boolean isEvaluationReplMode = "repl".equalsIgnoreCase(connectionMode);
//
//            // Spawning a clean background thread worker mapped directly to this active WebSocket channel lifecycle
//            LivewireTunnelWorker worker = new LivewireTunnelWorker(targetLaptopAddress, webView, session, isEvaluationReplMode);
//            activeSessionWorkers.put(session.getSocketId(), worker);
//            
//            Log.d(TAG, "🧵 Dispatching TCP worker loop execution context to background thread pool...");
//            tunnelThreadPool.submit(worker);
//
//        } catch (Exception e) {
//            Log.e(TAG, "💥 Critical exception during WebSocket session mapping initialization:", e);
//            sendJsonError(session, "Java engine socket initialization failure: " + e.getMessage());
//        }
//    }

    @WebSocketMapping(path = "/api/ws/livewire/bridge")
    public void handleIncomingWebViewTraffic(WebSocketSession session, String rawMessage) {
        Log.d(TAG, "📥 Inbound traffic frame intercepted from WebView window channel -> " + rawMessage);
        
        try {
            JSONObject incomingPacket = new JSONObject(rawMessage);
            String packetType = incomingPacket.optString("type");
            String payloadData = incomingPacket.optString("payload");

            LivewireTunnelWorker worker = activeSessionWorkers.get(session.getSocketId());
            if (worker == null) {
                Log.w(TAG, "⚠️ Received packet but found no matching active background worker cached for session: " + session.getSocketId());
                return;
            }

            // Route standard typed user text inputs straight to your laptop netcat view
            if ("client_message".equals(packetType)) {
                Log.d(TAG, "➡️ Forwarding text line back to laptop: " + payloadData);
                worker.writeToLaptop(payloadData);
            }
            
            // Route intercepted global browser console messages back to your laptop netcat view
            if ("console_bridge".equals(packetType)) {
                worker.writeToLaptop(payloadData);
            }

        } catch (Exception e) {
            // Fallback: If loose non-JSON text frames flow from frontend, pass them raw to laptop
            LivewireTunnelWorker worker = activeSessionWorkers.get(session.getSocketId());
            if (worker != null) {
                worker.writeToLaptop(rawMessage);
            }
        }
    }

    @WebSocketOnClose(path = "/api/ws/livewire/bridge")
    public void onBridgeSocketClose(WebSocketSession session) {
        Log.i(TAG, "🔌 WebView unlinked session closed. Purging background resource pools for ID: " + session.getSocketId());
        LivewireTunnelWorker worker = activeSessionWorkers.remove(session.getSocketId());
        if (worker != null) {
            worker.terminate();
        }
    }

    private void sendJsonError(WebSocketSession session, String errorMsg) {
        try {
            JSONObject res = new JSONObject();
            res.put("event", "tunnel_error");
            res.put("message", errorMsg);
            session.send(res.toString());
        } catch (Exception ignored) {}
    }
    /**
     * Long-running background networking runner.
     * Captures incoming characters from netcat, dynamically selecting text output routing paths.
     */
    private class LivewireTunnelWorker implements Runnable {
        private final String targetUrl;
        private final WebView webViewRef;
        private final WebSocketSession localWssSession;
        private final boolean evaluationReplModeActive;
        private volatile boolean running = true;
        private java.net.Socket rawSocket = null;

        public LivewireTunnelWorker(String targetUrl, WebView webViewRef, WebSocketSession wssSession, boolean replMode) {
            this.targetUrl = targetUrl;
            this.webViewRef = webViewRef;
            this.localWssSession = wssSession;
            this.evaluationReplModeActive = replMode;
        }

        @Override
        public void run() {
            try {
                String cleanTarget = targetUrl.replace("ws://", "").replace("wss://", "");
                String[] parts = cleanTarget.split(":");
                String ip = parts[0];
                int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 8080;

                Log.i(TAG, "🔌 [Background Task] Opening outbound socket link targeting -> " + ip + ":" + port);
                rawSocket = new java.net.Socket(ip, port);
                Log.i(TAG, "🚀 [Background Task] Outbound connection established cleanly.");

                InputStreamReader reader = new InputStreamReader(rawSocket.getInputStream(), "UTF-8");
                
                // Confirm connection status back to HTML WebView terminal UI view layout
                localWssSession.send("{\"event\":\"tunnel_connected\",\"target\":\"" + targetUrl + "\"}");

                char[] buffer = new char[4096];
                int bytesRead;

                while (running && (bytesRead = reader.read(buffer)) != -1) {
                    if (bytesRead == 0) continue;

                    final String incomingPayload = new String(buffer, 0, bytesRead).trim();
                    if (incomingPayload.isEmpty()) continue;

                    Log.i(TAG, "📥 [TCP Input from Laptop]: '" + incomingPayload + "'");

                    if (webViewRef != null) {
                        webViewRef.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!evaluationReplModeActive) {
                                    // ECHO MODE: Wrap loose incoming strings into structured tracking telemetry packages
                                    try {
                                        JSONObject echoFrame = new JSONObject();
                                        echoFrame.put("event", "raw_text_received");
                                        echoFrame.put("payload", incomingPayload);
                                        localWssSession.send(echoFrame.toString());
                                    } catch (Exception ignored) {}
                                } else {
                                    // REPL MODE: Run raw incoming strings directly as code patches inside the browser engine
                                    try {
                                        localWssSession.send("{\"event\":\"executing_payload\",\"bytes\":" + incomingPayload.length() + "}");
                                    } catch (Exception ignored) {}

                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                        webViewRef.evaluateJavascript(incomingPayload, value -> {
                                            try {
                                                JSONObject completeFrame = new JSONObject();
                                                completeFrame.put("event", "execution_completed");
                                                completeFrame.put("result", value);
                                                localWssSession.send(completeFrame.toString());
                                            } catch (Exception ignored) {}
                                        });
                                    } else {
                                        webViewRef.loadUrl("javascript:" + incomingPayload);
                                    }
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "💥 Exception tracking background TCP data receiver thread worker channels:", e);
                try {
                    localWssSession.send("{\"event\":\"tunnel_error\",\"message\":\"" + e.getMessage() + "\"}");
                } catch (Exception ignored) {}
                terminate();
            }
        }

        public void writeToLaptop(String message) {
            tunnelThreadPool.submit(() -> {
                try {
                    if (rawSocket != null && !rawSocket.isClosed()) {
                        PrintWriter out = new PrintWriter(rawSocket.getOutputStream(), true);
                        out.println(message);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed writing text data stream packet back to laptop:", e);
                }
            });
        }

        public void terminate() {
            running = false;
            try {
                if (rawSocket != null && !rawSocket.isClosed()) {
                    rawSocket.close();
                }
            } catch (Exception ignored) {}
            try {
                localWssSession.send("{\"event\":\"tunnel_destroyed\"}");
            } catch (Exception ignored) {}
            Log.d(TAG, "Background networking worker runner gracefully shut down.");
        }
    }
}


