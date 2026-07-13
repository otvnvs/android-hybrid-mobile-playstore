## Native Filesystem Archival Utilities (`ArcController`)

File system operations start at the root of either internal Application static storage, sandbox, or device storage at `/storage/emulated/0` as returned by `Environment.getExternalStorageDirectory()`

### `POST /api/arc/zip`
*   **Description:** Compresses a specified file or local directory tree structure into a standardized `.zip` archive payload block on the native storage filesystem.
*   **Query Parameters:** None.
*   **Request Body:** `application/json` object detailing compression requirements:
    ```json
    {
      "sourcePath": "arc_test_source_dir",
      "targetZipPath": "arc_test_payload.zip"
    }
    ```
*   **Response Status:** `200 OK` (Standard operational compression payload container wrapper status)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:** Container detailing the archival compression operational metrics outcome profile:
    ```json
    {
      "status": "success",
      "message": "Files compressed successfully into ZIP archive.",
      "archiveSize": 335
    }
    ```

### `POST /api/arc/unzip`
*   **Description:** Extracts the contents of a designated local `.zip` file archive binary layout block into a specified target directory destination layout hierarchy. Supports stripping leading path components from archive entries prior to extraction.
*   **Query Parameters:** None.
*   **Request Body:** `application/json` object detailing extraction requirements:
    ```json
    {
      "zipPath": "arc_test_payload.zip",
      "targetDirectory": "arc_test_extracted_out",
      "stripComponents": 1
    }
    ```
    *   `zipPath` (String, Required): The relative path to the source `.zip` archive file.
    *   `targetDirectory` (String, Required): The relative destination directory for extraction.
    *   `stripComponents` (Integer, Optional): Number of leading path elements to strip from internal entry paths (equivalent to `tar --strip-components=N`). Defaults to `0`.
*   **Response Status:** `200 OK` (Standard operational decompression payload container wrapper status)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:** Container detailing the extraction fulfillment state completion metadata profile:
    ```json
    {
      "status": "success",
      "message": "Archive successfully extracted onto native filesystem.",
      "targetDirectory": "arc_test_extracted_out",
      "componentsStripped": 1
    }
    ```

### `POST /api/arc/list`
*   **Description:** Inspects and catalogs the layout index inside a local `.zip` file without extracting it. Supports path-prefix directory filtering and paginated output boundaries to protect system memory resources against large archives.
*   **Query Parameters:** None.
*   **Request Body:** `application/json` object detailing lookup boundaries:
    ```json
    {
      "zipPath": "arc_list_payload.zip",
      "directoryPrefix": "documents",
      "offset": 1,
      "limit": 2
    }
    ```
    *   `zipPath` (String, Required): Relative path to the target archive file.
    *   `directoryPrefix` (String, Optional): Filter scope to list only items under a specific subdirectory branch name. Defaults to `""` (unfiltered).
    *   `offset` (Integer, Optional): Zero-indexed layout row to begin returning results. Defaults to `0`.
    *   `limit` (Integer, Optional): Maximum number of entry structures to slice into the response array container. Defaults to `100`.
*   **Response Status:** `200 OK` (Standard operational listing link container status)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:** Container detailing the structural zip data index inventory:
    ```json
    {
      "status": "success",
      "zipPath": "arc_list_payload.zip",
      "directoryPrefix": "documents/",
      "offset": 1,
      "limit": 2,
      "count": 2,
      "totalMatching": 3,
      "entries": [
        {
          "name": "documents/report2.txt",
          "isDirectory": false,
          "size": 20,
          "compressedSize": 20,
          "crc": 12847192
        },
        {
          "name": "documents/report3.txt",
          "isDirectory": false,
          "size": 20,
          "compressedSize": 20,
          "crc": 84910274
        }
      ]
    }
