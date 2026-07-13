package com.example.app.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class IntentServiceRegistry {
    private static final String TAG = "JAVA_IntentRegistry";
    private final List<IntentRouteMetadata> intentMetadataList = new ArrayList<>();
    private final Context mContext;

    public IntentServiceRegistry(Context context) {
        this.mContext = context.getApplicationContext();
        Log.d(TAG, "Initializing IntentServiceRegistry automated compilation sequence...");
        discoverAndCompileIntentHandlers(context, "com.example.app.services");
    }

    @SuppressWarnings("deprecation")
    private void discoverAndCompileIntentHandlers(Context context, String targetPackagePrefix) {
        try {
            String packageCodePath = context.getPackageCodePath();
            dalvik.system.DexFile dexFile = new dalvik.system.DexFile(packageCodePath);
            Enumeration<String> dexEntries = dexFile.entries();

            while (dexEntries.hasMoreElements()) {
                String className = dexEntries.nextElement();
                if (className.startsWith(targetPackagePrefix)) {
                    // Skip core architecture plumbing components to optimize scanner reflection passes
                    if (className.equals(IntentServiceRegistry.class.getName()) || 
                        className.equals(IntentMapping.class.getName()) || 
                        className.equals(IntentContext.class.getName())) {
                        continue;
                    }
                    try {
                        Class<?> clazz = Class.forName(className);
                        Object handlerInstance = null;

                        for (Method method : clazz.getMethods()) {
                            if (method.isAnnotationPresent(IntentMapping.class)) {
                                if (handlerInstance == null) {
                                    handlerInstance = clazz.getDeclaredConstructor().newInstance();
                                }
                                IntentMapping mapping = method.getAnnotation(IntentMapping.class);
                                intentMetadataList.add(new IntentRouteMetadata(mapping.host().toLowerCase(), method, handlerInstance));
                                Log.d(TAG, "==> COMPILED DEEP-LINK INTENT ROUTE: otv-app://" + mapping.host().toLowerCase());
                            }
                        }
                    } catch (ClassNotFoundException | LinkageError ignored) {
                    } catch (Exception ex) {
                        Log.w(TAG, "Failed instantiating auto-discovered intent target context: " + className, ex);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Critical class mapping framework extraction collapse for intents: " + e.getMessage(), e);
        }
    }

    /**
     * Intercepts, identifies, and dispatches custom deep-link URLs to their registered modular handles.
     * Returns true if a match was successfully found and processed.
     */
//    public boolean dispatchIntent(Intent intent) {
//        if (intent == null) return false;
//        Uri uri = intent.getData();
//        if (uri == null) return false;
//
//        String incomingHost = uri.getHost();
//        if (incomingHost == null) return false;
//        
//        String cleanHost = incomingHost.toLowerCase();
//        Log.d(TAG, "dispatchIntent() evaluating incoming deep-link host signature rule: " + cleanHost);
//
//        for (IntentRouteMetadata route : intentMetadataList) {
//            if (route.getHost().equals(cleanHost)) {
//                try {
//                    IntentContext intentContext = new IntentContext(mContext, intent);
//                    Log.i(TAG, "[INTENT MATCH] Executing dynamic deep-link handler for: otv-app://" + cleanHost);
//                    route.getTargetMethod().invoke(route.getHandlerInstance(), intentContext);
//                    return true;
//                } catch (Exception e) {
//                    Log.e(TAG, "Error executing custom dynamic intent method pipeline: " + e.getMessage(), e);
//                }
//            }
//        }
//        Log.w(TAG, "No matching Intent route signature verified for deep link host target: " + cleanHost);
//        return false;
//    }
// Change the method signature to accept a Context parameter directly:
public boolean dispatchIntent(Context context, Intent intent) {
    if (intent == null || context == null) return false;
    Uri uri = intent.getData();
    if (uri == null) return false;

    String incomingHost = uri.getHost();
    if (incomingHost == null) return false;
    
    String cleanHost = incomingHost.toLowerCase();
    Log.d(TAG, "dispatchIntent() evaluating incoming deep-link host signature rule: " + cleanHost);

    for (IntentRouteMetadata route : intentMetadataList) {
        if (route.getHost().equals(cleanHost)) {
            try {
                // FIX: Pass the fresh incoming 'context' (which will be MainActivity) 
                // instead of using the class-level 'mContext' field.
                IntentContext intentContext = new IntentContext(context, intent);
                Log.i(TAG, "[INTENT MATCH] Executing deep-link handler for: ahm-app://" + cleanHost);
                route.getTargetMethod().invoke(route.getHandlerInstance(), intentContext);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error executing custom dynamic intent method pipeline: " + e.getMessage(), e);
            }
        }
    }
    Log.w(TAG, "dispatchConnect dropped: No match discovered for incoming path coordinate context: " + cleanHost);
    return false;
}


    private static class IntentRouteMetadata {
        private final String host;
        private final Method targetMethod;
        private final Object handlerInstance;

        public IntentRouteMetadata(String host, Method targetMethod, Object handlerInstance) {
            this.host = host;
            this.targetMethod = targetMethod;
            this.handlerInstance = handlerInstance;
        }

        public String getHost() { return host; }
        public Method getTargetMethod() { return targetMethod; }
        public Object getHandlerInstance() { return handlerInstance; }
    }
}

