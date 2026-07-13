package com.example.app;

import android.view.KeyEvent;
import android.util.Log;
public class SecretTriggerDetector {
    private static final String TAG = "JAVA_SecretTriggerDetector";
    public interface OnTriggerListener {
        void onSecretTriggered();
    }

    private final OnTriggerListener listener;
    private boolean isVolumeUpPressed = false;

    public SecretTriggerDetector(OnTriggerListener listener) {
        Log.d(TAG, "SecretTriggerDetector(OnTriggerListener listener)");
        this.listener = listener;
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "handleKeyDown(int keyCode, KeyEvent event)");
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            isVolumeUpPressed = true;
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (isVolumeUpPressed) {
                if (listener != null) {
                    listener.onSecretTriggered();
                }
                return true;
            }
        }
        return false;
    }

    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "handleKeyUp(int keyCode, KeyEvent event)");
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            isVolumeUpPressed = false;
            return true;
        }
        return false;
    }
}

