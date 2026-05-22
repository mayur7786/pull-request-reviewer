package com.mayur.prreviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mayur.prreviewer.config.LlmProvider;
import com.mayur.prreviewer.domain.ReviewResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class LlmReviewClient {

    private static final String DEFAULT_ANTHROPIC_VERSION = "2023-06-01";

    private final LlmProviderConfig config;
    private final ObjectMapper objectMapper;
    private final HttpTransport httpTransport;
    private final ApiKeyResolver apiKeyResolver;

    // Creates a client that uses the default HTTP transport and env lookup.
    public LlmReviewClient(LlmProviderConfig config, ObjectMapper objectMapper) {
        this(config, objectMapper, new JavaHttpTransport(objectMapper), System::getenv);
    }

    // Creates a client with a custom HTTP transport.
    public LlmReviewClient(LlmProviderConfig config, ObjectMapper objectMapper, HttpTransport httpTransport) {
        this(config, objectMapper, httpTransport, System::getenv);
    }

    // Creates a client with fully injectable transport and key lookup.
    public LlmReviewClient(
            LlmProviderConfig config,
            ObjectMapper objectMapper,
            HttpTransport httpTransport,
            ApiKeyResolver apiKeyResolver
    ) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpTransport = httpTransport;
        this.apiKeyResolver = apiKeyResolver;
    }

    // Sends the review prompt to the configured provider and parses JSON.
    public ReviewResponse review(String systemPrompt, String userPrompt) {
        ProviderRequest request = buildRequest(systemPrompt, userPrompt);
        String responseBody = httpTransport.postJson(request.uri(), request.headers(), request.payload());
        String content = extractText(request.provider(), responseBody);

        try {
            return objectMapper.readValue(content, ReviewResponse.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to parse review JSON from " + request.provider() + ":" + System.lineSeparator() + content, exception);
        }
    }

    // Builds the provider-specific HTTP request.
    private ProviderRequest buildRequest(String systemPrompt, String userPrompt) {
        return switch (config.provider()) {
            case OPENAI -> buildOpenAiRequest(systemPrompt, userPrompt, false);
            case OPENAI_COMPATIBLE -> buildOpenAiRequest(systemPrompt, userPrompt, true);
            case GEMINI -> buildGeminiRequest(systemPrompt, userPrompt);
            case CLAUDE -> buildClaudeRequest(systemPrompt, userPrompt);
        };
    }

    // Builds an OpenAI-style chat completions request.
    private ProviderRequest buildOpenAiRequest(String systemPrompt, String userPrompt, boolean compatible) {
        String apiKey = resolveApiKey(config.apiKeyEnvVar(), compatible ? "OPENAI_API_KEY" : "OPENAI_API_KEY");
        URI uri = URI.create(resolveOpenAiEndpoint(config.apiBaseUrl(), compatible));

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", config.model());
        payload.put("temperature", 0.1);
        ArrayNode messages = payload.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        return new ProviderRequest(LlmProvider.OPENAI, uri, Map.of("Authorization", "Bearer " + apiKey), payload);
    }

    // Builds a Gemini generateContent request.
    private ProviderRequest buildGeminiRequest(String systemPrompt, String userPrompt) {
        String apiKey = resolveApiKey(config.apiKeyEnvVar(), "GEMINI_API_KEY");
        String endpoint = config.apiBaseUrl() == null || config.apiBaseUrl().isBlank()
                ? "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent".formatted(config.model())
                : config.apiBaseUrl();
        URI uri = URI.create(endpoint);

        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode systemInstruction = payload.putObject("system_instruction");
        systemInstruction.putArray("parts").addObject().put("text", systemPrompt);

        ArrayNode contents = payload.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", userPrompt);

        payload.putObject("generationConfig").put("temperature", 0.1);

        return new ProviderRequest(LlmProvider.GEMINI, uri, Map.of("x-goog-api-key", apiKey), payload);
    }

    // Builds an Anthropic messages request.
    private ProviderRequest buildClaudeRequest(String systemPrompt, String userPrompt) {
        String apiKey = resolveApiKey(config.apiKeyEnvVar(), "ANTHROPIC_API_KEY");
        String endpoint = config.apiBaseUrl() == null || config.apiBaseUrl().isBlank()
                ? "https://api.anthropic.com/v1/messages"
                : config.apiBaseUrl();
        URI uri = URI.create(endpoint);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", config.model());
        payload.put("max_tokens", 4096);
        payload.put("temperature", 0.1);
        payload.put("system", systemPrompt);
        ArrayNode messages = payload.putArray("messages");
        messages.addObject().put("role", "user").put("content", userPrompt);

        return new ProviderRequest(
                LlmProvider.CLAUDE,
                uri,
                Map.of(
                        "x-api-key", apiKey,
                        "anthropic-version", DEFAULT_ANTHROPIC_VERSION
                ),
                payload
        );
    }

    // Extracts plain text content from a provider response body.
    private String extractText(LlmProvider provider, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return switch (provider) {
                case OPENAI, OPENAI_COMPATIBLE -> extractOpenAiText(root);
                case GEMINI -> extractGeminiText(root);
                case CLAUDE -> extractClaudeText(root);
            };
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to parse LLM response payload:" + System.lineSeparator() + responseBody, exception);
        }
    }

    // Reads assistant text from an OpenAI-style response.
    private String extractOpenAiText(JsonNode root) {
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            String joined = collectTextParts(contentNode, List.of("text"));
            if (!joined.isBlank()) {
                return joined;
            }
        }
        throw new IllegalStateException("OpenAI response did not include assistant text content.");
    }

    // Reads model text from a Gemini response.
    private String extractGeminiText(JsonNode root) {
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        String joined = collectTextParts(parts, List.of("text"));
        if (joined.isBlank()) {
            throw new IllegalStateException("Gemini response did not include text parts.");
        }
        return joined;
    }

    // Reads text blocks from a Claude response.
    private String extractClaudeText(JsonNode root) {
        JsonNode content = root.path("content");
        String joined = collectTextParts(content, List.of("text"));
        if (joined.isBlank()) {
            throw new IllegalStateException("Claude response did not include text blocks.");
        }
        return joined;
    }

    // Joins text parts from array-shaped provider responses.
    private String collectTextParts(JsonNode arrayNode, List<String> keys) {
        if (!arrayNode.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode item : arrayNode) {
            for (String key : keys) {
                JsonNode value = item.path(key);
                if (value.isTextual()) {
                    if (!builder.isEmpty()) {
                        builder.append(System.lineSeparator());
                    }
                    builder.append(value.asText());
                    break;
                }
            }
        }
        return builder.toString().trim();
    }

    // Resolves the provider API key from the configured env var.
    private String resolveApiKey(String configuredEnvVar, String defaultEnvVar) {
        String envVar = configuredEnvVar == null || configuredEnvVar.isBlank() ? defaultEnvVar : configuredEnvVar;
        String value = apiKeyResolver.resolve(envVar);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing API key. Set environment variable " + envVar + ".");
        }
        return value;
    }

    // Resolves the final OpenAI-style chat completions endpoint.
    private String resolveOpenAiEndpoint(String configuredBaseUrl, boolean compatible) {
        String rawBase = configuredBaseUrl;
        if (rawBase == null || rawBase.isBlank()) {
            rawBase = compatible ? System.getenv("OPENAI_BASE_URL") : null;
        }
        if (rawBase == null || rawBase.isBlank()) {
            rawBase = "https://api.openai.com/v1";
        }
        if (rawBase.endsWith("/chat/completions")) {
            return rawBase;
        }
        if (rawBase.endsWith("/")) {
            rawBase = rawBase.substring(0, rawBase.length() - 1);
        }
        if (rawBase.endsWith("/v1")) {
            return rawBase + "/chat/completions";
        }
        return rawBase + "/v1/chat/completions";
    }

    private record ProviderRequest(
            LlmProvider provider,
            URI uri,
            Map<String, String> headers,
            JsonNode payload
    ) {
    }
}
