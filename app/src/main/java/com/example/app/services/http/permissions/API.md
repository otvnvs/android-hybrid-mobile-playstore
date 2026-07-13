## 8. Android Core Security Runtime Permissions (`PermissionsController`)

Provides lightweight, deadlock-free structural tracking and programmatic user dialog dispatching capabilities over the application container permission lifecycle context. 

To eliminate low-level JVM-to-WebView threading deadlocks on modern Android versions, the request API operates strictly asynchronously, allowing the client testing framework to implement non-blocking polling loops during synchronous evaluation scenarios.

### `POST /api/permissions/status`
*   **Description:** Batch-evaluates the active system clearance metrics for an incoming list of Android manifest permission strings.
*   **Query Parameters:** None.
*   **Request Body:**
    ```json
    {
      "permissions": [
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO"
      ]
    }
    ```
*   **Response Status:** `200 OK` (Success)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "permissions_matrix": {
        "android.permission.CAMERA": "GRANTED",
        "android.permission.RECORD_AUDIO": "DENIED"
      }
    }
    ```

### `POST /api/permissions/request`
*   **Description:** Asynchronously dispatches an invitation request to cross over onto the native Android UI thread loop and inflate the platform user authorization prompt dialog box modal over the active viewport layout.
*   **Query Parameters:** None.
*   **Request Body:**
    ```json
    {
      "permissions": [
        "android.permission.CAMERA"
      ]
    }
    ```
*   **Response Status:** `202 Accepted` (Request acknowledged and safely queued for native user interaction)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "message": "System dialog sequence triggered successfully"
    }
    ```

### `GET /api/permissions/declared`
*   **Description:** Queries Android's localized `PackageManager` system layer at runtime to unpack the compiled package configuration block. It dynamically extracts every permission explicitly declared in the application's structural `AndroidManifest.xml` file, providing a verified baseline of operations the app is authorized to request from a user.
*   **Query Parameters:** None.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (On successful package info inspection)
    *   `500 Internal Server Error` (If context elements are missing or security checks block inspection)
*   **Response Headers:** `Content-Type: application/json`, `X-Server-Response-Engine: Android-Native-JVM`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "package_name": "com.example.app",
      "declared_permissions": [
        "android.permission.ACTIVITY_RECOGNITION",
        "android.permission.HIGH_SAMPLING_RATE_SENSORS",
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.MODIFY_AUDIO_SETTINGS",
        "android.permission.VIBRATE",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
      ],
      "total_count": 15
    }
    ```

