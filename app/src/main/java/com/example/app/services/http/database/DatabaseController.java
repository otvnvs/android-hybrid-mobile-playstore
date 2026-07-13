package com.example.app.services.example;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class DatabaseController {
    private static final String TAG = "DatabaseController";

    public DatabaseController() {}

    @RequestMapping(path = "/api/database/create", method = "POST")
    public ResponseContext createDatabaseFile(RequestContext request) {
        SQLiteDatabase db = null;
        try {
            byte[] bodyBytes = request.getBody();
            String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
            JSONObject bodyJson = new JSONObject(rawBodyText);
            String dbPath = bodyJson.optString("path", "");

            if (dbPath.trim().isEmpty()) {
                return buildErrorResponse(400, "Required property missing from body payload: path");
            }

            File dbFile = new File(dbPath);
            if (dbFile.exists()) {
                JSONObject result = new JSONObject();
                result.put("status", "success");
                result.put("message", "Database file already exists");
                result.put("created", false);
                return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();
            }

            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

            int openFlags = SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY;
            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, openFlags);

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("message", "New empty SQLite database container initialized successfully");
            result.put("created", true);
            return ResponseContext.status(201).contentType("application/json").body(result.toString()).build();
        } catch (Exception e) {
            Log.e(TAG, "Database creation failure", e);
            return buildErrorResponse(500, "SQLite file creation error: " + e.getMessage());
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }

    @RequestMapping(path = "/api/database/query", method = "POST")
    public ResponseContext queryDatabase(RequestContext request) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            byte[] bodyBytes = request.getBody();
            String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
            
            JSONObject bodyJson = new JSONObject(rawBodyText);
            String dbPath = bodyJson.optString("path", "");
            String sql = bodyJson.optString("sql", "");
            JSONArray argsJson = bodyJson.optJSONArray("args"); // Optional parameter array

            if (dbPath.trim().isEmpty() || sql.trim().isEmpty()) {
                return buildErrorResponse(400, "Required properties missing from body payload: path, sql");
            }

            File dbFile = new File(dbPath);
            if (!dbFile.exists()) {
                return buildErrorResponse(404, "Database file not found: " + dbPath);
            }

            // Convert JSON argument array elements cleanly into an array of string parameters
            String[] selectionArgs = null;
            if (argsJson != null && argsJson.length() > 0) {
                selectionArgs = new String[argsJson.length()];
                for (int i = 0; i < argsJson.length(); i++) {
                    selectionArgs[i] = argsJson.isNull(i) ? null : argsJson.get(i).toString();
                }
            }

            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            // Pass selectionArgs array down to trigger parameterized pre-compilation
            cursor = db.rawQuery(sql, selectionArgs);
            
            JSONArray rowsArray = new JSONArray();
            if (cursor != null) {
                String[] columnNames = cursor.getColumnNames();
                while (cursor.moveToNext()) {
                    JSONObject row = new JSONObject();
                    for (int i = 0; i < columnNames.length; i++) {
                        int type = cursor.getType(i);
                        switch (type) {
                            case Cursor.FIELD_TYPE_INTEGER: row.put(columnNames[i], cursor.getLong(i)); break;
                            case Cursor.FIELD_TYPE_FLOAT: row.put(columnNames[i], cursor.getDouble(i)); break;
                            case Cursor.FIELD_TYPE_STRING: row.put(columnNames[i], cursor.getString(i)); break;
                            case Cursor.FIELD_TYPE_BLOB: row.put(columnNames[i], "[Raw Binary Blob]"); break;
                            case Cursor.FIELD_TYPE_NULL: default: row.put(columnNames[i], JSONObject.NULL); break;
                        }
                    }
                    rowsArray.put(row);
                }
            }

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("rows", rowsArray);
            result.put("row_count", rowsArray.length());
            return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();

        } catch (Exception e) {
            Log.e(TAG, "SQL parameterized query pipeline failure", e);
            return buildErrorResponse(500, "SQLite compilation engine error: " + e.getMessage());
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
        }
    }
    @RequestMapping(path = "/api/database/execute", method = "POST")
    public ResponseContext executeDatabaseStatement(RequestContext request) {
        SQLiteDatabase db = null;
        SQLiteStatement statement = null;
        try {
            byte[] bodyBytes = request.getBody();
            String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
            
            JSONObject bodyJson = new JSONObject(rawBodyText);
            String dbPath = bodyJson.optString("path", "");
            String sql = bodyJson.optString("sql", "");
            JSONArray argsJson = bodyJson.optJSONArray("args"); // Optional parameter array

            if (dbPath.trim().isEmpty() || sql.trim().isEmpty()) {
                return buildErrorResponse(400, "Required properties missing from body payload: path, sql");
            }

            String lowerSql = sql.trim().toLowerCase();
            if (lowerSql.contains("drop ") || lowerSql.contains("alter ") || lowerSql.contains("vacuum")) {
                return buildErrorResponse(403, "Structural alterations are blocked in test framework contexts");
            }

            File dbFile = new File(dbPath);
            if (!dbFile.exists()) {
                return buildErrorResponse(404, "Database not found. Call /create first: " + dbPath);
            }

            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);

            if (argsJson != null && argsJson.length() > 0) {
                // Compile the SQL statement layout structure into native device memory cache
                statement = db.compileStatement(sql);
                
                // Dynamically bind typed data variables into the query parameters index mapping
                for (int i = 0; i < argsJson.length(); i++) {
                    int bindIndex = i + 1; // SQLite prepared placeholders start at index 1
                    if (argsJson.isNull(i)) {
                        statement.bindNull(bindIndex);
                    } else {
                        Object val = argsJson.get(i);
                        if (val instanceof Integer || val instanceof Long) {
                            statement.bindLong(bindIndex, ((Number) val).longValue());
                        } else if (val instanceof Float || val instanceof Double) {
                            statement.bindDouble(bindIndex, ((Number) val).doubleValue());
                        } else if (val instanceof Boolean) {
                            statement.bindLong(bindIndex, (Boolean) val ? 1 : 0);
                        } else {
                            statement.bindString(bindIndex, val.toString());
                        }
                    }
                }
                // Run the compiled memory query
                statement.execute();
            } else {
                // Safe fallback pattern to legacy execution engine if no argument collection array passed
                db.execSQL(sql);
            }

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("message", "Parameterized statement statement compiled and executed successfully");
            return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();

        } catch (Exception e) {
            Log.e(TAG, "SQL parameterized execution pipeline failure", e);
            return buildErrorResponse(500, "SQLite compiled statement statement error: " + e.getMessage());
        } finally {
            if (statement != null) statement.close();
            if (db != null && db.isOpen()) db.close();
        }
    }

    @RequestMapping(path = "/api/database/delete", method = "POST")
    public ResponseContext deleteDatabaseFile(RequestContext request) {
        try {
            byte[] bodyBytes = request.getBody();
            String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
            JSONObject bodyJson = new JSONObject(rawBodyText);
            String dbPath = bodyJson.optString("path", "");

            if (dbPath.trim().isEmpty()) {
                return buildErrorResponse(400, "Required property missing from body payload: path");
            }

            File dbFile = new File(dbPath);
            if (!dbFile.exists()) {
                return buildErrorResponse(404, "Target database file does not exist: " + dbPath);
            }

            boolean deleted = SQLiteDatabase.deleteDatabase(dbFile);
            JSONObject result = new JSONObject();
            if (deleted) {
                result.put("status", "success");
                result.put("message", "Database and peripheral structural journals purged completely");
                return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();
            } else {
                return buildErrorResponse(500, "The OS rejected file deletion. Database connection pool may be locked.");
            }
        } catch (Exception e) {
            return buildErrorResponse(500, "SQLite file cleanup error: " + e.getMessage());
        }
    }

    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code).contentType("application/json").body(errJson.toString()).build();
    }
}

