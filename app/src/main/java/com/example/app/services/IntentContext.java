package com.example.app.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class IntentContext {
    private final Context androidContext; // Keeps the true reference handle alive
    private final Intent originIntent;
    private final Uri dataUri;

    public IntentContext(Context context, Intent intent) {
        // FIXED: Remove .getApplicationContext() so that Activity traits stay fully intact
        this.androidContext = context; 
        this.originIntent = intent;
        this.dataUri = (intent != null) ? intent.getData() : null;
    }

    public Context getContext() {
        return this.androidContext;
    }

    public Intent getOriginIntent() {
        return this.originIntent;
    }

    public Uri getDataUri() {
        return this.dataUri;
    }

    public String getQueryParam(String key) {
        if (dataUri == null || key == null) return null;
        try {
            return dataUri.getQueryParameter(key);
        } catch (Exception e) {
            return null;
        }
    }
}

