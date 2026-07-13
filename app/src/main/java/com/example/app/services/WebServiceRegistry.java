package com.example.app.services;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import com.example.app.AppConfig;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServiceRegistry {
    private static final String TAG = "WebServiceRegistry_DEBUG";
    private final List<RouteMappingMetadata> routeMetadataList = new ArrayList<>();
    private final List<Object> domainControllers = new ArrayList<>();

    public WebServiceRegistry(Context context) {
        Log.d(TAG, "Initializing WebServiceRegistry compilation sequence...");
        
        // Automatically scan and discover controllers matching our target sub-package footprint
        discoverControllersAutomated(context, "com.example.app.services");
        
        compileAllControllerRoutes();
    }

    @SuppressWarnings("deprecation")
    private void discoverControllersAutomated(Context context, String targetPackagePrefix) {
        try {
            String packageCodePath = context.getPackageCodePath();
            // Open the running compilation application package directly to iterate its DEX definition headers
            dalvik.system.DexFile dexFile = new dalvik.system.DexFile(packageCodePath);
            Enumeration<String> dexEntries = dexFile.entries();

            while (dexEntries.hasMoreElements()) {
                String className = dexEntries.nextElement();
                
                // Target matching classes within the target package namespace boundary rules
                if (className.startsWith(targetPackagePrefix)) {
                    // Ignore framework system utility abstractions or classes we are currently running inside
                    if (className.equals(WebServiceRegistry.class.getName()) || 
                        className.equals(RequestMapping.class.getName()) || 
                        className.equals(RequestContext.class.getName()) || 
                        className.equals(ResponseContext.class.getName())) {
                        continue;
                    }

                    try {
                        Class<?> clazz = Class.forName(className);
                        
                        // Optimize scan: Verify if the class contains any methods using our routing annotation
                        boolean isController = false;
                        for (Method m : clazz.getMethods()) {
                            if (m.isAnnotationPresent(RequestMapping.class)) {
                                isController = true;
                                break;
                            }
                        }

                        if (isController) {
                            Log.i(TAG, "[Automated Discovery] Found valid service component: " + className);
                            // Instantiate via the fallback public parameterless constructor signature layout
                            Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                            domainControllers.add(controllerInstance);
                        }
                    } catch (ClassNotFoundException | LinkageError e) {
                        // Suppress background configuration artifacts that fail compilation visibility layout checks
                        Log.v(TAG, "Skipping compilation check for inaccessible resource: " + className);
                    } catch (Exception ex) {
                        Log.w(TAG, "Failed instantiating auto-discovered target class matrix: " + className, ex);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Critical automated component scanning framework crash context: " + e.getMessage(), e);
        }
    }

    private void compileAllControllerRoutes() {
        for (Object controller : domainControllers) {
            Method[] methods = controller.getClass().getMethods();
            Log.d(TAG, "Reflecting class methods inside: " + controller.getClass().getSimpleName());
            
            for (Method method : methods) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                    String cleanPath = mapping.path();
                    List<String> pathTokenNames = new ArrayList<>();
                    Matcher tokenMatcher = Pattern.compile("\\{([^}]+)\\}").matcher(cleanPath);
                    while (tokenMatcher.find()) {
                        pathTokenNames.add(tokenMatcher.group(1));
                    }
                    String generalizedRegexPattern = cleanPath.replaceAll("\\{[^}]+\\}", "([^/]+)");
                    Pattern compiledRegex = Pattern.compile("^" + generalizedRegexPattern + "$");
                    
                    routeMetadataList.add(new RouteMappingMetadata(
                        mapping.method().toUpperCase(), 
                        cleanPath, 
                        compiledRegex, 
                        pathTokenNames, 
                        method, 
                        controller
                    ));
                    Log.d(TAG, "==> COMPILED ROUTE: [" + mapping.method().toUpperCase() + "] " + cleanPath);
                }
            }
        }
    }

    public WebResourceResponse dispatch(Context context, AppConfig config, WebResourceRequest request, String path, String method) {
        String cleanMethod = method.toUpperCase();
        String lookupPath = path;
        if (lookupPath.length() > 1 && lookupPath.endsWith("/")) {
            lookupPath = lookupPath.substring(0, lookupPath.length() - 1);
        }

        for (RouteMappingMetadata route : routeMetadataList) {
            if (route.httpMethod.equals(cleanMethod)) {
                Matcher matcher = route.regexPattern.matcher(lookupPath);
                if (matcher.matches()) {
                    try {
                        RequestContext ctx = new RequestContext(context, config, request, path);
                        for (int i = 0; i < matcher.groupCount(); i++) {
                            ctx.addPathParam(route.pathTokenKeys.get(i), matcher.group(i + 1));
                        }
                        
                        ResponseContext response = (ResponseContext) route.executionTarget.invoke(route.instanceOwner, ctx);
                        if (response != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                return new WebResourceResponse(response.getMimeType(), "UTF-8", response.getStatusCode(), response.getStatusMessage(), response.getHeaders(), new ByteArrayInputStream(response.getBody()));
                            } else {
                                return new WebResourceResponse(response.getMimeType(), "UTF-8", new ByteArrayInputStream(response.getBody()));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[CRASH] Exception caught during method invocation processing structural paths", e);
                    }
                }
            }
        }
        return null;
    }

    private static class RouteMappingMetadata {
        final String httpMethod;
        final String rawAnnotatedPath;
        final Pattern regexPattern;
        final List<String> pathTokenKeys;
        final Method executionTarget;
        final Object instanceOwner;

        RouteMappingMetadata(String httpMethod, String rawAnnotatedPath, Pattern regexPattern, List<String> pathTokenKeys, Method executionTarget, Object instanceOwner) {
            this.httpMethod = httpMethod;
            this.rawAnnotatedPath = rawAnnotatedPath;
            this.regexPattern = regexPattern;
            this.pathTokenKeys = pathTokenKeys;
            this.executionTarget = executionTarget;
            this.instanceOwner = instanceOwner;
        }
    }
}

