package com.mayur.prreviewer.node;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayur.prreviewer.config.LlmProvider;
import com.mayur.prreviewer.domain.ChangedFile;
import com.mayur.prreviewer.domain.ChangedFileType;
import com.mayur.prreviewer.domain.ReviewCategory;
import com.mayur.prreviewer.domain.ReviewContext;
import com.mayur.prreviewer.domain.ReviewFinding;
import com.mayur.prreviewer.domain.ReviewResponse;
import com.mayur.prreviewer.domain.Severity;
import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.service.LlmProviderConfig;
import com.mayur.prreviewer.service.LlmReviewClient;
import com.mayur.prreviewer.service.OutputValidatorService;
import com.mayur.prreviewer.service.ReviewGatewayService;
import com.mayur.prreviewer.service.ReviewPromptFactory;
import com.mayur.prreviewer.state.ReviewState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReviewGatewayNodeTest {

    // Verifies that gateway output becomes final review findings.
    @Test
    void mergesValidatedFindingsAcrossChunks() {
        LlmReviewClient client = new LlmReviewClient(new LlmProviderConfig(LlmProvider.OPENAI, "test-model", null, null), new ObjectMapper()) {
            @Override
            // Returns a fixed model response for this test.
            public ReviewResponse review(String systemPrompt, String userPrompt) {
                return new ReviewResponse(
                        "Found one issue.",
                        List.of(new ReviewFinding(
                                Severity.HIGH,
                                ReviewCategory.BUG,
                                "src/Main.java",
                                12,
                                "Possible null dereference",
                                "value is read before null check",
                                "Guard the read with a null check",
                                0.91
                        ))
                );
            }
        };

        ReviewState reviewState = new ReviewState();
        reviewState.setInferredIntent("Add timeout handling.");
        reviewState.setDiffSummary("1 file changed");
        reviewState.setChangedFiles(List.of(new ChangedFile("src/Main.java", ChangedFileType.MODIFIED_FILE, List.of(), 2, 0)));
        reviewState.setReviewContexts(List.of(new ReviewContext(
                "src/Main.java",
                ChangedFileType.MODIFIED_FILE,
                "Main",
                "run",
                List.of("import java.util.Objects;"),
                10,
                20,
                "12 | value.length();",
                false
        )));

        ReviewGraphState graphState = new ReviewGraphState(Map.of(ReviewGraphState.REVIEW_STATE, reviewState));

        new ReviewGatewayNode(
                new ReviewGatewayService(2),
                new ReviewPromptFactory(),
                client,
                new OutputValidatorService()
        ).apply(graphState);

        assertThat(graphState.reviewState().getFindings()).hasSize(1);
        assertThat(graphState.reviewState().getReviewSummary()).contains("Found one issue");
    }
}
