## Application String Resource Services (`ResController`)

### `GET /api/res/string`
*   **Description:** Queries the application's compiled internal XML resource registry directly at runtime. It takes an identifier key name string, dynamically looks up its matching internal signature mapping, and extracts its evaluated definition payload directly from the project's compiled `strings.xml` bundle.
*   **Query Parameters:**
    *   `key` (String, Required): The exact string item key name to look up (e.g. `app_name`).
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (On successful key resource lookup resolution)
    *   `400 Bad Request` (If the mandatory query `key` parameter configuration is missing)
    *   `404 Not Found` (If the requested key does not exist inside strings.xml)
    *   `500 Internal Server Error` (If context elements are invalid or resource tracking arrays crash)
*   **Response Headers:** `Content-Type: application/json`, `X-Server-Response-Engine: Android-Native-JVM`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "key": "app_name",
      "value": "My Embedded Sandbox Application"
    }
    ```

### `GET /api/res/strings`
*   **Description:** Reflectively targets and inspects the inner class container `R.string` bound to the application's compiled resources footprint. It dynamically loops across all static resource field entries to resolve their underlying content mappings, yielding an aggregated complete key-value dictionary matrix of all string assets present inside `strings.xml`.
*   **Query Parameters:** None.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (On successful bulk database trace compilation)
    *   `500 Internal Server Error` (If context fields match invalid signatures or R$string mappings are obfuscated)
*   **Response Headers:** `Content-Type: application/json`, `X-Server-Response-Engine: Android-Native-JVM`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "package_name": "com.example.app",
      "total_strings_count": 2,
      "strings_matrix": {
        "app_name": "Hybrid-Mobile App",
        "alternative_setup_title": "Alternative Setup Profile Configuration"
      }
    }
    ```

