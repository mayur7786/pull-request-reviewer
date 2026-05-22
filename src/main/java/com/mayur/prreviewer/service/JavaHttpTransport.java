package com.mayur.prreviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class JavaHttpTransport implements HttpTransport {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JavaHttpTransport(ObjectMapper objectMapper) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build(), objectMapper);
    }

    // Uses the provided HTTP client for outbound JSON requests.
    public JavaHttpTransport(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    // Sends a JSON request and checks for non-success HTTP responses.
    @Override
    public String postJson(URI uri, Map<String, String> headers, JsonNode body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));

            headers.forEach(builder::header);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("LLM request failed with status " + response.statusCode() + ":" + System.lineSeparator() + response.body());
            }
            return response.body();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize LLM request.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM request interrupted.", exception);
        }
    }
}
