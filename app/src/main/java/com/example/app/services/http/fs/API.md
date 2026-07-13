## Persistent Local File System Storage (`FsController`)

File system operations start at the root of either internal Application static storage, sandbox, or device storage at `/storage/emulated/0` as returned by `Environment.getExternalStorageDirectory()`

### `GET /api/fs/list`
*   **Description:** Lists the explicit files and directory contents array for a targeted location relative to the storage environment root.
*   **Query Parameters:**
    *   `path` (Required) - Target directory subdirectory mapping string.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (Success)
    *   `400 Bad Request` (If path resolution or directory traversal rules fail)
*   **Response Headers:** `Content-Type: text/plain`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "files": ["manifest.json", "assets", "configurations"]
    }
    ```

### `POST /api/fs/write`
*   **Description:** Persists plain UTF-8 encoded text or stringified JSON arguments directly into an explicitly targeted filesystem location on the device.
*   **Query Parameters:**
    *   `path` (Required) - The destination file path relative to the root directory (must be URL-encoded).
*   **Request Body:** The raw content string or payload buffer to commit directly to the file on the device flash sectors.
*   **Response Status:**
    *   `200 OK` (Successfully saved to disk)
    *   `500 Internal Server Error` (File persisting runtime layer failures or write execution drops)
*   **Response Headers:**
    *   `Content-Type: application/json` (Outbound Response)
*   **Response Body:**
    ```json
    {
      "status": "success"
    }
    ```

### `GET /api/fs/read`
*   **Description:** Extracts the complete uncompressed file contents matching localized extensions (`.txt`, `.json`) into designated buffer outputs.
*   **Query Parameters:**
    *   `path` (Required) - Target local filesystem path to read.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (Success)
    *   `404 Not Found` (If file path processing throws structural file exceptions)
*   **Response Headers:** Dynamically converted depending on target extension:
    *   `.txt` → `Content-Type: text/plain`
    *   `.json` → `Content-Type: application/json`
    *   Default → `Content-Type: application/octet-stream`
*   **Response Body:** Raw uncompressed byte string array matching target payload.

### `GET /api/fs/locations`
*   **Description:** Dynamically resolves the true absolute file system path strings for the device shared external storage root and the private application sandbox databases folder.
*   **Query Parameters:** None.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (Success)
    *   `500 Internal Server Error` (If context processing or system path routing fails)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "locations": {
        "external_storage_root": "/storage/emulated/0",
        "sandbox_databases_root": "/data/user/0/com.example.app/databases",
        "package_name": "com.example.app"
      }
    }
    ```

### `GET /api/fs/diskspace`
*   **Description:** Extracts real-time hardware storage allocations, structural block partition availability, app sandbox cache footprints, and active secondary Micro-SD external storage mount variables.
*   **Query Parameters:** None.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (Success)
    *   `500 Internal Server Error` (If native hardware partition query mechanics or sandbox cache scans fail)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "internal_partition": {
        "partition_path": "/data",
        "total_space_bytes": 113432854528,
        "available_space_bytes": 38783815680,
        "status": "success"
      },
      "secondary_partition": {
        "removable_sdcard_mounted": false,
        "partition_path": "unmounted",
        "total_space_bytes": 0,
        "available_space_bytes": 0
      },
      "app_sandbox_cache": {
        "sandbox_cache_path": "/data/user/0/com.example.app/cache",
        "active_cache_usage_bytes": 63522,
        "status": "success"
      }
    }
    ```

