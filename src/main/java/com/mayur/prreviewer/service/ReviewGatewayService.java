package com.mayur.prreviewer.service;

import com.mayur.prreviewer.domain.ReviewContext;
import java.util.List;

public class ReviewGatewayService {

    private final int maxContextsPerRequest;

    // Stores the maximum number of contexts to send per model request.
    public ReviewGatewayService(int maxContextsPerRequest) {
        this.maxContextsPerRequest = maxContextsPerRequest;
    }

    // Splits review contexts into small LLM request batches and skips batches that contain generated code.
    public List<List<ReviewContext>> chunk(List<ReviewContext> contexts) {
        return java.util.stream.IntStream.iterate(0, index -> index < contexts.size(), index -> index + maxContextsPerRequest)
                .mapToObj(index -> contexts.subList(index, Math.min(contexts.size(), index + maxContextsPerRequest)))
                .filter(chunk -> chunk.stream().noneMatch(ReviewContext::generatedCode))
                .toList();
    }
}
