package com.example.app.services;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketServiceRegistry {
    private static final String TAG = "JAVA_WSS_Registry";
    
    private final Context mContext;
    private final List<WssRouteMetadata> messageRoutes = new ArrayList<>();
    private final List<WssRouteMetadata> openRoutes = new ArrayList<>();
    private final List<WssRouteMetadata> closeRoutes = new ArrayList<>();
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    public WebSocketServiceRegistry(Context context) {
        Log.d(TAG, "Initializing WebSocketServiceRegistry tracking compiled sequences...");
        this.mContext = context.getApplicationContext();
        discoverAndCompileRoutes(context, "com.example.app.services");
    }

    @SuppressWarnings("deprecation")
    private void discoverAndCompileRoutes(Context context, String targetPackagePrefix) {
        try {
            String packageCodePath = context.getPackageCodePath();
            dalvik.system.DexFile dexFile = new dalvik.system.DexFile(packageCodePath);
            Enumeration<String> dexEntries = dexFile.entries();

            while (dexEntries.hasMoreElements()) {
                String className = dexEntries.nextElement();
                if (className.startsWith(targetPackagePrefix)) {
                    if (className.equals(WebSocketServiceRegistry.class.getName()) || 
                        className.equals(WebSocketMapping.class.getName()) || 
                        className.equals(WebSocketOnOpen.class.getName()) || 
                        className.equals(WebSocketOnClose.class.getName()) || 
                        className.equals(WebSocketSession.class.getName())) {
                        continue;
                    }
                    try {
                        Class<?> clazz = Class.forName(className);
                        Object controllerInstance = null;

                        for (Method method : clazz.getMethods()) {
                            if (method.isAnnotationPresent(WebSocketMapping.class)) {
                                if (controllerInstance == null) controllerInstance = clazz.getDeclaredConstructor().newInstance();
                                WebSocketMapping mapping = method.getAnnotation(WebSocketMapping.class);
                                compileRoute(mapping.path(), method, controllerInstance, messageRoutes, "MESSAGE");
                            }
                            if (method.isAnnotationPresent(WebSocketOnOpen.class)) {
                                if (controllerInstance == null) controllerInstance = clazz.getDeclaredConstructor().newInstance();
                                WebSocketOnOpen mapping = method.getAnnotation(WebSocketOnOpen.class);
                                compileRoute(mapping.path(), method, controllerInstance, openRoutes, "ON_OPEN");
                            }
                            if (method.isAnnotationPresent(WebSocketOnClose.class)) {
                                if (controllerInstance == null) controllerInstance = clazz.getDeclaredConstructor().newInstance();
                                WebSocketOnClose mapping = method.getAnnotation(WebSocketOnClose.class);
                                compileRoute(mapping.path(), method, controllerInstance, closeRoutes, "ON_CLOSE");
                            }
                        }
                    } catch (ClassNotFoundException | LinkageError ignored) {
                    } catch (Exception ex) {
                        Log.w(TAG, "Failed instantiating custom scanned context: " + className, ex);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Critical class mapping framework extraction collapse: " + e.getMessage(), e);
        }
    }

    private void compileRoute(String path, Method method, Object controller, List<WssRouteMetadata> targetList, String logType) {
        List<String> pathTokenNames = new ArrayList<>();
        Matcher tokenMatcher = Pattern.compile("\\{([^}]+)\\}").matcher(path);
        while (tokenMatcher.find()) {
            pathTokenNames.add(tokenMatcher.group(1));
        }
        String generalizedRegexPattern = path.replaceAll("\\{[^}]+\\}", "([^/]+)");
        Pattern compiledRegex = Pattern.compile("^" + generalizedRegexPattern + "$");

        targetList.add(new WssRouteMetadata(path, compiledRegex, pathTokenNames, method, controller));
        Log.d(TAG, "==> COMPILED [" + logType + "] ROUTE: " + path);
    }

    /**
     * Bulletproof double-check path matching helper method
     */
    private boolean isPathMatched(WssRouteMetadata route, String rawPath) {
        if (rawPath == null) return false;
        
        // Setup sanitized path variations
        String cleanPath = rawPath;
        if (cleanPath.length() > 1 && cleanPath.endsWith("/")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
        }
        String slashedPath = cleanPath + "/";
        
        // Verify via exact path configuration mapping OR compiled regex expressions matches
        return route.getRawAnnotatedPath().equals(cleanPath) || 
               route.getRawAnnotatedPath().equals(slashedPath) ||
               route.getCompiledRegex().matcher(cleanPath).matches() ||
               route.getCompiledRegex().matcher(slashedPath).matches();
    }

    public void dispatchConnect(String socketId, String path, WebView webView) {
        Log.d(TAG, "dispatchConnect() executing target matching filters for path: " + path);
        WebSocketSession session = new WebSocketSession(mContext, socketId, path, webView);
        activeSessions.put(socketId, session);

        for (WssRouteMetadata route : openRoutes) {
            if (isPathMatched(route, path)) {
                try {
                    Log.i(TAG, "[WS_MATCH] Routing connection event reflectively to: " + route.getTargetMethod().getName());
                    route.getTargetMethod().invoke(route.getTargetController(), session);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Error executing custom @WebSocketOnOpen handler method: " + e.getMessage(), e);
                }
            }
        }
        Log.w(TAG, "dispatchConnect dropped: No match discovered for incoming path coordinate context: " + path);
    }

    public void dispatchMessage(String socketId, String message) {
        WebSocketSession session = activeSessions.get(socketId);
        if (session == null) return;

        for (WssRouteMetadata route : messageRoutes) {
            if (isPathMatched(route, session.getPath())) {
                try {
                    route.getTargetMethod().invoke(route.getTargetController(), session, message);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Error executing custom @WebSocketMapping handler method: " + e.getMessage(), e);
                    session.send("{\"error\":\"Internal operational failure: " + e.getMessage() + "\"}");
                }
            }
        }
    }

    public void dispatchClose(String socketId) {
        Log.d(TAG, "dispatchClose() executing lifecycle purge routing for key: " + socketId);
        WebSocketSession session = activeSessions.remove(socketId);
        if (session == null) return;

        for (WssRouteMetadata route : closeRoutes) {
            if (isPathMatched(route, session.getPath())) {
                try {
                    route.getTargetMethod().invoke(route.getTargetController(), session);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Error executing custom @WebSocketOnClose handler method: " + e.getMessage(), e);
                }
            }
        }
    }

    private static class WssRouteMetadata {
        private final String rawAnnotatedPath;
        private final Pattern regexPattern;
        private final List<String> pathTokenKeys;
        private final Method executionTarget;
        private final Object instanceOwner;

        public WssRouteMetadata(String rawAnnotatedPath, Pattern regexPattern, List<String> pathTokenKeys, Method executionTarget, Object instanceOwner) {
            this.rawAnnotatedPath = rawAnnotatedPath;
            this.regexPattern = regexPattern;
            this.pathTokenKeys = pathTokenKeys;
            this.executionTarget = executionTarget;
            this.instanceOwner = instanceOwner;
        }

        public String getRawAnnotatedPath() { return rawAnnotatedPath; }
        public Pattern getCompiledRegex() { return regexPattern; }
        public Method getTargetMethod() { return executionTarget; }
        public Object getTargetController() { return instanceOwner; }
    }
}

