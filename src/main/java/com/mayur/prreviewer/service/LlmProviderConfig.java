package com.mayur.prreviewer.service;

import com.mayur.prreviewer.config.LlmProvider;

public record LlmProviderConfig(
        LlmProvider provider,
        String model,
        String apiBaseUrl,
        String apiKeyEnvVar
) {
}
