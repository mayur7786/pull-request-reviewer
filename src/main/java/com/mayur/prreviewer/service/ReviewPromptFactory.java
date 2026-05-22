package com.mayur.prreviewer.service;

import com.mayur.prreviewer.domain.ReviewContext;
import java.util.List;

public class ReviewPromptFactory {

    // Returns the system prompt with review rules and output schema.
    public String buildSystemPrompt() {
        return """
                You are a senior Java code reviewer.

                Review code against the inferred intent.

                Only report findings supported by evidence.

                Focus on:
                - correctness
                - bugs
                - security
                - testing
                - performance
                - maintainability
                - Java best practices when they materially affect reliability, readability, safety, or long-term maintenance

                Ignore:
                - formatting
                - naming preferences
                - speculative assumptions
                - best-practice suggestions that are purely stylistic or not supported by code evidence

                Return JSON only with this schema:
                {
                  "summary": "string",
                  "findings": [
                    {
                      "severity": "HIGH|MEDIUM|LOW",
                      "category": "BUG|SECURITY|PERFORMANCE|TESTING|MAINTAINABILITY",
                      "file": "string",
                      "line": 0,
                      "issue": "string",
                      "evidence": "string",
                      "suggestion": "string",
                      "confidence": 0.0
                    }
                  ]
                }
                """;
    }

    // Builds the grounded user prompt for one review batch.
    public String buildUserPrompt(String intent, List<ReviewContext> contexts, String diffSummary) {
        StringBuilder builder = new StringBuilder();
        builder.append("Intent: ").append(intent).append(System.lineSeparator());
        if (diffSummary != null && !diffSummary.isBlank()) {
            builder.append("Diff summary: ").append(diffSummary).append(System.lineSeparator());
        }
        builder.append("""
                
                Review policy:
                - Only use the grounded code context below
                - Every finding must cite file, line, and code evidence
                - Skip style-only comments
                - Skip speculative concerns without a concrete code path
                - Report Java best-practice issues only when they have clear engineering impact
                - Do not suggest subjective refactors unless they prevent a concrete problem or reduce meaningful maintenance risk
                
                Relevant Java best-practice concerns include:
                - unsafe null handling
                - poor exception handling
                - resource leaks
                - misuse of collections or streams
                - thread-unsafe shared state
                - weak encapsulation that can cause bugs
                - overly complex methods that increase maintenance risk
                - missing tests for important logic branches
                
                Contexts:
                """);

        for (ReviewContext context : contexts) {
            builder.append(System.lineSeparator())
                    .append("File: ").append(context.file()).append(System.lineSeparator())
                    .append("Type: ").append(context.fileType()).append(System.lineSeparator())
                    .append("Class: ").append(context.className()).append(System.lineSeparator())
                    .append("Method: ").append(context.methodName()).append(System.lineSeparator())
                    .append("Line range: ").append(context.startLine()).append("-").append(context.endLine()).append(System.lineSeparator())
                    .append("Generated: ").append(context.generatedCode()).append(System.lineSeparator())
                    .append("Imports: ").append(String.join(", ", context.imports())).append(System.lineSeparator())
                    .append("Code:").append(System.lineSeparator())
                    .append(context.snippet()).append(System.lineSeparator());
        }
        return builder.toString();
    }
}
