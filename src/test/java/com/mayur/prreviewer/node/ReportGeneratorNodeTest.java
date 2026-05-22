package com.mayur.prreviewer.node;

import static org.assertj.core.api.Assertions.assertThat;

import com.mayur.prreviewer.config.LlmProvider;
import com.mayur.prreviewer.config.ReviewerConfig;
import com.mayur.prreviewer.domain.ReviewCategory;
import com.mayur.prreviewer.domain.ReviewFinding;
import com.mayur.prreviewer.domain.Severity;
import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.service.MarkdownWriter;
import com.mayur.prreviewer.service.ReportGeneratorService;
import com.mayur.prreviewer.state.ReviewState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportGeneratorNodeTest {

    @TempDir
    Path tempDir;

    // Verifies that the markdown report is written to disk.
    @Test
    void writesMarkdownReport() throws Exception {
        Path output = tempDir.resolve(".review/ai-review.md");
        ReviewState reviewState = new ReviewState();
        reviewState.setInferredIntent("Add timeout handling.");
        reviewState.setIntentConfidence(0.82);
        reviewState.setReviewSummary("Reviewed one file.");
        reviewState.setChangedFiles(List.of());
        reviewState.setFindings(List.of(new ReviewFinding(
                Severity.MEDIUM,
                ReviewCategory.MAINTAINABILITY,
                "src/Main.java",
                7,
                "Method is doing two jobs",
                "The method both validates and persists",
                "Split validation from persistence",
                0.77
        )));

        ReviewGraphState graphState = new ReviewGraphState(Map.of(ReviewGraphState.REVIEW_STATE, reviewState));
        ReviewerConfig config = new ReviewerConfig(tempDir, "main", LlmProvider.OPENAI, "gpt-5.2", null, null, output, null, 10, List.of());

        new ReportGeneratorNode(new ReportGeneratorService(), new MarkdownWriter(), config).apply(graphState);

        assertThat(Files.readString(output)).contains("# AI Code Review");
        assertThat(graphState.reviewState().getMarkdownReport()).contains("Medium Severity");
    }
}
