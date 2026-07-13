package com.example.app.services.example;

import com.example.app.services.WebSocketMapping;
import com.example.app.services.WebSocketOnOpen;
import com.example.app.services.WebSocketOnClose;
import com.example.app.services.WebSocketSession;
import android.util.Log;
import org.json.JSONObject;

public class ExampleWssController {
    private static final String TAG = "ExampleWssController";

    public ExampleWssController() {}

    /**
     * Connection established event callback listener hook.
     * Fires immediately upon frontend connection initialization.
     */
    @WebSocketOnOpen(path = "/api/ws/testing-suite")
    public void onClientConnect(WebSocketSession session) {
        Log.i(TAG, "Native @WebSocketOnOpen intercept active for socket: " + session.getSocketId());
        try {
            JSONObject welcomeFrame = new JSONObject();
            welcomeFrame.put("status", "connected");
            welcomeFrame.put("message", "Welcome from Native Android JVM Lifecycle Handler");
            welcomeFrame.put("assigned_id", session.getSocketId());
            
            // Automatically push connection confirmation frame back to the UI view
            session.send(welcomeFrame.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error compiling onOpen frame: " + e.getMessage());
        }
    }

    /**
     * Standard message router framework listener node.
     */
    @WebSocketMapping(path = "/api/ws/testing-suite")
    public void executeSuiteEcho(final WebSocketSession session, String message) {
        try {
            JSONObject incomingPayload = new JSONObject(message);
            String inputCommand = incomingPayload.optString("command", "none");

            if ("start_heartbeat_stream".equals(inputCommand)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (int tick = 1; tick <= 3; tick++) {
                                Thread.sleep(1000);

                                JSONObject broadcastMsg = new JSONObject();
                                broadcastMsg.put("status", "streaming");
                                broadcastMsg.put("tick_index", tick);
                                broadcastMsg.put("engine_layer", "Android-Native-JVM");

                                session.send(broadcastMsg.toString());
                            }
                            
                            JSONObject finalMsg = new JSONObject();
                            finalMsg.put("status", "complete");
                            session.send(finalMsg.toString());
                            
                        } catch (Exception ex) {
                            Log.e(TAG, "Stream loop exception context: " + ex.getMessage());
                        }
                    }
                }).start();
                return;
            }

            JSONObject outgoingResponse = new JSONObject();
            outgoingResponse.put("status", "success");
            outgoingResponse.put("echo_command", inputCommand);
            session.send(outgoingResponse.toString());

        } catch (Exception e) {
            session.send("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Connection terminated event callback listener hook.
     * Fires immediately when the browser requests or performs a close operation sequence.
     */
    @WebSocketOnClose(path = "/api/ws/testing-suite")
    public void onClientDisconnect(WebSocketSession session) {
        // Safe infrastructure teardown logic points block
        Log.i(TAG, "Native @WebSocketOnClose intercept cleared resources tracking for socket: " + session.getSocketId());
    }
}

