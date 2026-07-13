### `WS /api/ws/telemetry/sensors`
*   **Description:** Opens a persistent, bidirectional WebSocket session loop over the custom virtual proxy matrix. Upon a successful handshake, it immediately triggers the native Android `SensorManager` to bind an active hardware event listener on a background thread and stream raw physical coordinates down to the client view layout.
*   **Connection Initialization (OnOpen Event Frame):** Sent automatically by the native JVM layer the millisecond the connection transitions to an open state.
    ```json
    {
      "status": "connected",
      "message": "Welcome from Native Android JVM Lifecycle Handler",
      "assigned_id": "xkg26mzwb1783376902075"
    }
    ```
*   **Inbound Streaming Frame Payload:** Emitted continuously at high frequency directly from the device's physical acceleration hardware or time-series background fallback engine threads.
    ```json
    {
      "status": "telemetry_update",
      "sensor_type": "accelerometer",
      "data": {
        "x": 1.7836803197860718,
        "y": -0.5147533416748047,
        "z": 9.78989028930664,
        "timestamp": 1783376902558
      }
    }
    ```
*   **Connection Closure (OnClose Lifecycle Action):** Triggered immediately when the browser tab closes, navigates away, or explicitly executes a close sequence command. The framework automatically cuts background runtime workers and releases system hooks to eliminate battery and memory resource leaks.

