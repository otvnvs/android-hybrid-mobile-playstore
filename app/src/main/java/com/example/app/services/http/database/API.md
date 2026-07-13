## SQLite Database File Inspection & Manipulation (`DatabaseController`)

Direct path-driven persistence layer inspection resources provide low-level management of SQLite database files on the local filesystem. This interface enforces safe transaction execution rules, type-safe column conversions, protection against accidental typo mutations, and performance-optimised prepared statements via parameterised arguments.

### Query Parameterization Flexibility
The interface completely supports both pure raw SQL strings and parameterized prepared queries. The `args` parameter array is optional. If `args` is omitted or left empty, the underlying backend engine falls back seamlessly to evaluate the `sql` string layout directly as an explicit raw script. 

*   **Pure Raw Style (No Arguments):** Best used for static setup queries or analytical operations that require no variable substitution (e.g., `CREATE TABLE...` or standard table selection scans).
*   **Prepared Style (With Placeholder Array):** Highly recommended when inserting or filtering dynamic string or numerical variables derived from runtime input data points to ensure maximum execution speed and prevent layout evaluation faults.

Example:

```json
{
  "path": "/data/user/0/com.example.app/databases/prepared_test.db",
  "sql": "SELECT * FROM device_faults WHERE id = 104;"
}
```


### `POST /api/database/create`
*   **Description:** Explicitly initializes a completely new, blank SQLite database container file at the exact requested location. If parent directory structures are missing, they are created automatically.
*   **Query Parameters:** None.
*   **Request Body:**
    ```json
    {
      "path": "/data/user/0/com.example.app/databases/prepared_test.db"
    }
    ```
*   **Response Status:**
    *   `201 Created` (Success; new file initialized on disk)
    *   `200 OK` (Success; target file already exists, skipping generation)
    *   `400 Bad Request` (If path parameter string is blank or missing)
    *   `500 Internal Server Error` (If file creation or transaction engine crashes)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "message": "New empty SQLite database container initialized successfully",
      "created": true
    }
    ```

### `POST /api/database/query`
*   **Description:** Compiles and executes an analytical selection dataset read statement using optional data-bound parameter arguments. Maps SQLite native data type profiles seamlessly into structural JSON collections. Opens file streams in strict read-only isolation parameters.
*   **Query Parameters:** None.
*   **Request Body:**
    ```json
    {
      "path": "/data/user/0/com.example.app/databases/prepared_test.db",
      "sql": "SELECT id, error_tag, severity FROM device_faults WHERE error_tag = ? AND severity > ?;",
      "args": ["webview_crash_null_pointer", 50.0]
    }
    ```
*   **Response Status:**
    *   `200 OK` (Success)
    *   `400 Bad Request` (If path or query string parameters are missing)
    *   `404 Not Found` (If database target file does not exist on disk)
    *   `500 Internal Server Error` (If SQL query parsing or structure interpretation fails)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "rows": [
        {
          "id": 104,
          "error_tag": "webview_crash_null_pointer",
          "severity": 89.65
        }
      ],
      "row_count": 1
    }
    ```

### `POST /api/database/execute`
*   **Description:** Validates and executes mutating row statements (`INSERT`, `UPDATE`, `DELETE`) or physical schema definitions inside an existing target. Employs pre-compiled `SQLiteStatement` instances when parameter arrays are supplied for high-performance throughput. Blocks structural drop or vacuum actions to ensure safety.
*   **Query Parameters:** None.
*   **Request Body:**
    ```json
    {
      "path": "/data/user/0/com.example.app/databases/prepared_test.db",
      "sql": "INSERT INTO device_faults (id, error_tag, severity) VALUES (?, ?, ?);",
      "args": [104, "webview_crash_null_pointer", 89.65]
    }
    ```
*   **Response Status:**
    *   `200 OK` (Success)
    *   `400 Bad Request` (If mandatory payload parameters are absent)
    *   `403 Forbidden` (If structural queries like `DROP` or `ALTER` are intercepted)
    *   `404 Not Found` (If path is missing from storage, protecting against typo creation loops)
    *   `500 Internal Server Error` (If engine execution crashes)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "message": "Parameterized statement compiled and executed successfully"
    }
    ```

### `POST /api/database/delete`
*   **Description:** Purges a targeted database completely from storage. Safely tracks and deletes hidden peripheral transaction logs (`-wal`, `-journal`, `-shm`) created by the OS engine runtime layer.
*   **Query Parameters:** None.
*   **Request Body:**
    ```json
    {
      "path": "/data/user/0/com.example.app/databases/prepared_test.db"
    }
    ```
*   **Response Status:**
    *   `200 OK` (Success)
    *   `400 Bad Request` (If path layout parameter missing)
    *   `404 Not Found` (If target file does not exist on the file structure)
    *   `500 Internal Server Error` (If target file cannot be cleared or is locked by active thread connections)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "message": "Database and peripheral structural journals purged completely"
    }
    ```
