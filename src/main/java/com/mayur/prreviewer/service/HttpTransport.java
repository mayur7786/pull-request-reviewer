package com.mayur.prreviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Map;

public interface HttpTransport {

    // Sends a JSON POST request and returns the raw response body.
    String postJson(URI uri, Map<String, String> headers, JsonNode body);
}
