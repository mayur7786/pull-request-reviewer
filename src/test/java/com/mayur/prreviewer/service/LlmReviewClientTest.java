package com.mayur.prreviewer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayur.prreviewer.config.LlmProvider;
import com.mayur.prreviewer.domain.ReviewResponse;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LlmReviewClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Verifies parsing of an OpenAI-style response payload.
    @Test
    void parsesOpenAiStyleResponse() {
        CapturingTransport transport = new CapturingTransport("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"summary\\":\\"ok\\",\\"findings\\":[]}"
                      }
                    }
                  ]
                }
                """);

        ReviewResponse response = new LlmReviewClient(
                new LlmProviderConfig(LlmProvider.OPENAI_COMPATIBLE, "gpt-test", "https://example.com/v1", "TEST_KEY"),
                objectMapper,
                transport,
                envVar -> "secret"
        ).review("system", "user");

        assertThat(response.summary()).isEqualTo("ok");
        assertThat(transport.uri()).isEqualTo(URI.create("https://example.com/v1/chat/completions"));
        assertThat(transport.headers()).containsKey("Authorization");
    }

    // Verifies parsing of a Gemini response payload.
    @Test
    void parsesGeminiResponse() {
        CapturingTransport transport = new CapturingTransport("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"summary\\":\\"gemini\\",\\"findings\\":[]}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """);

        ReviewResponse response = new LlmReviewClient(
                new LlmProviderConfig(LlmProvider.GEMINI, "gemini-2.5-flash", "https://example.com/models/gemini-2.5-flash:generateContent", "TEST_KEY"),
                objectMapper,
                transport,
                envVar -> "secret"
        ).review("system", "user");

        assertThat(response.summary()).isEqualTo("gemini");
        assertThat(transport.headers()).containsKey("x-goog-api-key");
        assertThat(transport.body().path("system_instruction").path("parts").path(0).path("text").asText()).isEqualTo("system");
    }

    // Verifies parsing of a Claude response payload.
    @Test
    void parsesClaudeResponse() {
        CapturingTransport transport = new CapturingTransport("""
                {
                  "content": [
                    {
                      "type": "text",
                      "text": "{\\"summary\\":\\"claude\\",\\"findings\\":[]}"
                    }
                  ]
                }
                """);

        ReviewResponse response = new LlmReviewClient(
                new LlmProviderConfig(LlmProvider.CLAUDE, "claude-sonnet-4-0", "https://example.com/v1/messages", "TEST_KEY"),
                objectMapper,
                transport,
                envVar -> "secret"
        ).review("system", "user");

        assertThat(response.summary()).isEqualTo("claude");
        assertThat(transport.headers()).containsEntry("anthropic-version", "2023-06-01");
        assertThat(transport.body().path("system").asText()).isEqualTo("system");
    }

    private static final class CapturingTransport implements HttpTransport {
        private final String response;
        private URI uri;
        private Map<String, String> headers;
        private JsonNode body;

        // Stores a canned response for transport tests.
        private CapturingTransport(String response) {
            this.response = response;
        }

        @Override
        // Captures the outbound request and returns the canned response.
        public String postJson(URI uri, Map<String, String> headers, JsonNode body) {
            this.uri = uri;
            this.headers = headers;
            this.body = body;
            return response;
        }

        // Returns the last requested URI.
        private URI uri() {
            return uri;
        }

        // Returns the last request headers.
        private Map<String, String> headers() {
            return headers;
        }

        // Returns the last request body.
        private JsonNode body() {
            return body;
        }
    }
}
