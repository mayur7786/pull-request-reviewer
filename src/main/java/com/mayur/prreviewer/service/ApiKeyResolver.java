package com.mayur.prreviewer.service;

@FunctionalInterface
public interface ApiKeyResolver {

    // Looks up an API key by environment variable name.
    String resolve(String envVarName);
}
