## Program Runtime Profile & Diagnostics (`ProgramController`)

Application container diagnostic resources pull telemetry regarding active process execution attributes, JVM virtual machine heap resource metrics, concurrent worker thread tracking, instance uptime counters, and manifest package configurations.

### `GET /api/program/info`
*   **Description:** Generates a real-time tracking matrix measuring the true execution lifespan, operational headroom, and configuration settings of the program container layer.
*   **Query Parameters:** None.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (Success)
    *   `500 Internal Server Error` (If process interrogation mechanics drop or crash)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "process": {
        "pid": 4975,
        "uid": 10560,
        "is_64bit": true
      },
      "jvm_memory": {
        "free_heap_bytes": 58883472,
        "total_heap_bytes": 67108864,
        "max_heap_bytes": 268435456
      },
      "threading": {
        "active_thread_count": 33
      },
      "timeline": {
        "device_boot_uptime_ms": 1762785220,
        "app_uptime_ms": 4224
      },
      "package": {
        "package_name": "com.example.app",
        "version_name": "1.0",
        "target_sdk": 31,
        "version_code": 1,
        "first_install_time": 1782779634495,
        "status": "success"
      }
    }
    ```

