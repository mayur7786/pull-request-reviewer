package com.mayur.prreviewer.service;

import com.mayur.prreviewer.domain.ReviewFinding;
import com.mayur.prreviewer.domain.ReviewResponse;
import java.util.List;
import java.util.Objects;

public class OutputValidatorService {

    // Checks that the model output matches the expected review schema.
    public ReviewResponse validate(ReviewResponse response, List<String> allowedFiles) {
        if (response == null) {
            throw new IllegalArgumentException("Review response must not be null.");
        }
        if (response.summary() == null || response.summary().isBlank()) {
            throw new IllegalArgumentException("Review response summary is required.");
        }

        response.findings().forEach(finding -> validateFinding(finding, allowedFiles));
        return response;
    }

    // Validates one finding and keeps it inside grounded file scope.
    private void validateFinding(ReviewFinding finding, List<String> allowedFiles) {
        Objects.requireNonNull(finding.severity(), "Finding severity is required.");
        Objects.requireNonNull(finding.category(), "Finding category is required.");
        requireText(finding.file(), "Finding file is required.");
        requireText(finding.issue(), "Finding issue is required.");
        requireText(finding.evidence(), "Finding evidence is required.");
        requireText(finding.suggestion(), "Finding suggestion is required.");
        if (finding.line() <= 0) {
            throw new IllegalArgumentException("Finding line must be positive.");
        }
        if (finding.confidence() <= 0.0 || finding.confidence() > 1.0) {
            throw new IllegalArgumentException("Finding confidence must be between 0 and 1.");
        }
        if (!allowedFiles.contains(finding.file())) {
            throw new IllegalArgumentException("Finding references file outside grounded context: " + finding.file());
        }
    }

    // Rejects missing or blank text fields.
    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
