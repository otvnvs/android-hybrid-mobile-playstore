package com.example.app.services;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ResponseContext {
    private final int statusCode;
    private final String statusMessage;
    private final String mimeType;
    private final Map<String, String> headers;
    private final byte[] body;

    private ResponseContext(Builder builder) {
        this.statusCode = builder.statusCode;
        this.statusMessage = builder.statusMessage;
        this.mimeType = builder.mimeType;
        this.headers = builder.headers;
        this.body = builder.body;
    }

    public int getStatusCode() { return statusCode; }
    public String getStatusMessage() { return statusMessage; }
    public String getMimeType() { return mimeType; }
    public Map<String, String> getHeaders() { return headers; }
    public byte[] getBody() { return body; }

    // Entry point for the fluid builder
    public static Builder status(int code) {
        return new Builder(code, getStandardStatusMessage(code));
    }

    public static Builder status(int code, String message) {
        return new Builder(code, message);
    }

    public static class Builder {
        private final int statusCode;
        private final String statusMessage;
        private String mimeType = "application/json";
        private final Map<String, String> headers = new HashMap<>();
        private byte[] body = new byte[0];

        public Builder(int statusCode, String statusMessage) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
        }

        public Builder contentType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder body(byte[] bodyBytes) {
            this.body = bodyBytes != null ? bodyBytes : new byte[0];
            return this;
        }

        public Builder body(String bodyString) {
            if (bodyString != null) {
                this.body = bodyString.getBytes(StandardCharsets.UTF_8);
            }
            return this;
        }

        public ResponseContext build() {
            return new ResponseContext(this);
        }
    }

    private static String getStandardStatusMessage(int code) {
        switch (code) {
            case 200: return "OK";
            case 201: return "Created";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            default: return "OK";
        }
    }
}

