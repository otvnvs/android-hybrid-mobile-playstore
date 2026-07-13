## Application & System Services (`AppController`)

### `POST /api/app/export-localstorage`
*   **Description:** Serializes and dumps a raw string data packet or JSON string from the web environment directly into a backup file named `ahm-localstorage-dump.json` inside the shared public Android Downloads directory.
*   **Query Parameters:** None.
*   **Request Body:** `application/json` or raw text data packet strings (cannot be empty).
*   **Response Status:** 
    *   `200 OK` (On successful write)
    *   `400 Bad Request` (If payload packet is empty)
    *   `500 Internal Server Error` (If writing to external disk fails)
*   **Response Headers:** `Content-Type: text/plain`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "message": "Exported to downloads: ahm-localstorage-dump.json"
    }
    ```

### `GET /api/app/import-localstorage`
*   **Description:** Accesses the Android Downloads folder to locate and read back the raw bytes of the `ahm-localstorage-dump.json` storage file to restore state.
*   **Query Parameters:** None.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (On successful read)
    *   `404 Not Found` (If the backup file does not exist in Downloads)
    *   `500 Internal Server Error` (If file streaming hits an I/O exception)
*   **Response Headers:** `Cache-Control: no-cache`
*   **Response Body:** Returns raw byte contents or a serialized JSON string matching the original exported payload. If an error occurs:
    ```json
    {
      "status": "error",
      "message": "Backup file 'ahm-localstorage-dump.json' not found in Downloads folder."
    }
    ```

### `GET /api/app/device-status`
*   **Description:** Fetches structural diagnostic properties of the active environment wrapper context including user-agent strings, protocols, and mock tracking domain metadata.
*   **Query Parameters:**
    *   `id` (Optional) - Appends a requested client identifier into the JSON payload tracking tracking model.
*   **Request Body:** None.
*   **Response Status:** `200 OK`
*   **Response Headers:** `X-Powered-By: Android Native Framework Interceptor`
*   **Response Body:**
    ```json
    {
      "status": "active",
      "protocol": "HTTP/1.1",
      "userAgent": "Mozilla/5.0 ...",
      "domain": "your-mock-domain.local",
      "requestedId": "mobile_client"
    }
    ```
