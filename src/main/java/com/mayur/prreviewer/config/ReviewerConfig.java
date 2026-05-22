package com.mayur.prreviewer.config;

import java.nio.file.Path;
import java.util.List;

public record ReviewerConfig(
        Path repositoryRoot,
        String targetBranch,
        LlmProvider provider,
        String model,
        String apiBaseUrl,
        String apiKeyEnvVar,
        Path outputPath,
        String developerNote,
        int contextLines,
        List<String> fileFilters
) {
}
