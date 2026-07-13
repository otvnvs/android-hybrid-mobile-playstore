## Cross-Origin Network Proxy Broker (`NetController`)

### `POST /api/net/request`
*   **Description:** Bypasses browser CORS constraints by forwarding a custom HTTP/HTTPS connection stream to a target server via the native Android Java layer network architecture. Returns remote response structures and headers.
*   **Query Parameters:** None (Configuration arguments are now processed directly within the root JSON request envelope).
*   **Request Body:** `application/json` object detailing orchestration constraints and downstream connection requirements:
    ```json
    {
      "timeout_ms": 15000,
      "request": {
        "url": "https://external.com",
        "method": "POST",
        "headers": { 
          "Accept": "application/json",
          "Content-Type": "text/plain"
        },
        "body": "{\"param\": 123}"
      }
    }
    ```
    *   `timeout_ms` *(Optional Integer)*: Root-level parameter controlling connection and data read timeout constraints for the proxy broker itself in milliseconds. Defaults to `15000` (15 seconds) if omitted.
    *   `request` *(Required Object)*: Encapsulates all isolated delivery parameters intended strictly for transmission to the remote destination.
*   **Response Status:** `200 OK` (Standard operational proxy link container response wrapper status)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:** Container detailing the isolated proxy result outcome profile:
    ```json
    {
      "status": 200,
      "headers": { 
        "Server": "nginx", 
        "Content-Type": "application/json" 
      },
      "body": "{\n  \"received\": true\n}"
    }
    ```
