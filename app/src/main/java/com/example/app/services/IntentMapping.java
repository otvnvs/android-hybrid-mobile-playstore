package com.example.app.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IntentMapping {
    /**
     * The targeted Host route configuration rule signature.
     * Matches incoming custom URL schemas like: otv-app://[host] -> e.g., "deploy"
     */
    String host();
}

