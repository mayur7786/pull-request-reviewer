package com.mayur.prreviewer.service;

import com.mayur.prreviewer.domain.ReviewCategory;
import com.mayur.prreviewer.domain.ReviewFinding;
import com.mayur.prreviewer.domain.Severity;
import com.mayur.prreviewer.state.ReviewState;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ReportGeneratorService {

    // Builds the final markdown review report.
    public String generate(ReviewState state) {
        StringBuilder builder = new StringBuilder();
        builder.append("# AI Code Review").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("## Intent").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append(state.getInferredIntent()).append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("Confidence: ").append(String.format("%.2f", state.getIntentConfidence())).append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("## Summary").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append(state.getReviewSummary()).append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("Reviewed ").append(state.getChangedFiles().size()).append(" Java files.").append(System.lineSeparator()).append(System.lineSeparator());
        appendSeveritySection(builder, "High Severity", Severity.HIGH, state.getFindings());
        appendSeveritySection(builder, "Medium Severity", Severity.MEDIUM, state.getFindings());
        appendSeveritySection(builder, "Low Severity", Severity.LOW, state.getFindings());
        appendTestingGaps(builder, state.getFindings());
        return builder.toString();
    }

    // Writes one severity section of the report.
    private void appendSeveritySection(StringBuilder builder, String title, Severity severity, List<ReviewFinding> findings) {
        builder.append("## ").append(title).append(System.lineSeparator()).append(System.lineSeparator());
        List<ReviewFinding> matching = findings.stream()
                .filter(finding -> finding.severity() == severity)
                .sorted(Comparator.comparing(ReviewFinding::file).thenComparingInt(ReviewFinding::line))
                .toList();

        if (matching.isEmpty()) {
            builder.append("None.").append(System.lineSeparator()).append(System.lineSeparator());
            return;
        }

        for (ReviewFinding finding : matching) {
            builder.append("- ")
                    .append(finding.file())
                    .append(":")
                    .append(finding.line())
                    .append(" [")
                    .append(finding.category())
                    .append("] ")
                    .append(finding.issue())
                    .append(System.lineSeparator());
            builder.append("  - Evidence: ").append(finding.evidence()).append(System.lineSeparator());
            builder.append("  - Suggestion: ").append(finding.suggestion()).append(System.lineSeparator());
            builder.append("  - Confidence: ").append(String.format("%.2f", finding.confidence())).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());
    }

    // Writes the testing gaps section of the report.
    private void appendTestingGaps(StringBuilder builder, List<ReviewFinding> findings) {
        builder.append("## Testing Gaps").append(System.lineSeparator()).append(System.lineSeparator());
        List<ReviewFinding> testingFindings = findings.stream()
                .filter(finding -> finding.category() == ReviewCategory.TESTING)
                .collect(Collectors.toList());
        if (testingFindings.isEmpty()) {
            builder.append("No explicit testing gaps identified.").append(System.lineSeparator());
            return;
        }
        for (ReviewFinding finding : testingFindings) {
            builder.append("- ").append(finding.file()).append(":").append(finding.line()).append(" ").append(finding.issue()).append(System.lineSeparator());
        }
    }
}
