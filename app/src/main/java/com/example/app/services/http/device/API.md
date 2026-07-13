## Device Runtime Hardware & Environment Inspection (`DeviceController`)

Low-level diagnostic utilities pull metrics directly from native system properties, internal hardware configurations, memory tracking subsystems, and localized runtime parameters. This endpoint provides deep monitoring capabilities for isolating hardware-specific defects.

### `GET /api/device/info`
*   **Description:** Aggregates a comprehensive multi-layered hardware snapshot, resource metric matrix, locale rule profiles, and active WebView core package specifications.
*   **Query Parameters:** None.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (Success)
    *   `500 Internal Server Error` (If device inspection runtime or hardware metric population crashes)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "hardware": {
        "brand": "samsung",
        "device": "a24",
        "model": "SM-A245F",
        "product": "a24nsxx",
        "manufacturer": "samsung",
        "hardware": "mt6789",
        "board": "a24",
        "bootloader": "A245FXXS9DYG1",
        "display_build_id": "AP3A.240905.015.A2.A245FXXS9DYG1"
      },
      "os": {
        "release_version": "15",
        "sdk_int": 35,
        "codename": "REL",
        "incremental": "A245FXXS9DYG1",
        "base_os": "samsung/a24nsxx/a24:15/AP3A.240905.015.A2/A245FXXU8DYE5:user/release-keys",
        "security_patch": "2025-08-01"
      },
      "build": {
        "fingerprint": "samsung/a24nsxx/a24:15/AP3A.240905.015.A2/A245FXXS9DYG1:user/release-keys",
        "id": "AP3A.240905.015.A2",
        "type": "user",
        "user": "dpi",
        "host": "SWDM8602",
        "tags": "release-keys",
        "time_epoch_ms": 1753784241000
      },
      "cpu": {
        "supported_abis": ["arm64-v8a", "armeabi-v7a", "armeabi"]
      },
      "memory": {
        "avail_ram_bytes": 837518336,
        "total_ram_bytes": 3833217024,
        "low_memory_flag": false,
        "threshold_bytes": 225443840,
        "status": "success"
      },
      "display": {
        "width_pixels": 1080,
        "height_pixels": 2340,
        "density_dpi": 450,
        "density_scale": 2.8125,
        "scaled_density_font": 2.8125,
        "xdpi": 391.885009765625,
        "ydpi": 398.89898681640625,
        "hardware_acceleration_enabled": true,
        "status": "MainActivity binding verified"
      },
      "storage": {
        "available_storage_bytes": 38750437376,
        "total_storage_bytes": 113429315584,
        "status": "success"
      },
      "webview_engine": {
        "package_name": "com.google.android.webview",
        "version_name": "149.0.7827.164"
      },
      "battery": {
        "percentage": 34.0,
        "temperature_celsius": 21.1,
        "is_charging": true,
        "plugged_source": "USB"
      },
      "locale": {
        "language_tag": "en-GB",
        "display_name": "English (United Kingdom)",
        "country_iso": "GB"
      }
    }
    ```
