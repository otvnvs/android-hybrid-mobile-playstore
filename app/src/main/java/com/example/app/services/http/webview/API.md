## WebView Core Engine Profile & Settings Analysis (`WebViewController`)

Provides a comprehensive diagnostic mapping of the underlying Chromium engine parameters, session cookie parameters, application-level manifest cleartext transmission permissions, and absolute filesystem directory cache sizes inside the application workspace.

### `GET /api/webview/diagnostics`
*   **Description:** Aggregates a multi-layered engineering telemetry snapshot detailing the active rendering rules, sandbox persistence directories, and configuration settings of the engine layer.
*   **Query Parameters:** None.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (Success)
    *   `500 Internal Server Error` (If context processing or recursive cache size scanning crashes)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "security_policy": {
        "uses_cleartext_traffic_allowed": true,
        "target_sdk_compliance": 31
      },
      "cookie_engine": {
        "accept_cookies_enabled": true,
        "has_cookies_stored": false
      },
      "configurations": {
        "javascript_enabled": true,
        "dom_storage_enabled": true,
        "database_enabled": true,
        "file_access_enabled": true,
        "loads_images_automatically": true,
        "mixed_content_mode": 2,
        "active_cache_mode": "LOAD_DEFAULT",
        "status": "Thread-safe default values mapping applied"
      },
      "storage_allocation": {
        "webview_cache_directory_path": "/data/user/0/com.example.app/app_webview",
        "webview_cache_allocated_bytes": 2055208,
        "status": "success"
      }
    }
    ```

