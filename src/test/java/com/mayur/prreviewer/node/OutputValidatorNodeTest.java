package com.mayur.prreviewer.node;

import static org.assertj.core.api.Assertions.assertThat;

import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.state.ReviewState;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OutputValidatorNodeTest {

    // Verifies that a missing summary gets a fallback value.
    @Test
    void backfillsSummaryWhenMissing() {
        ReviewState reviewState = new ReviewState();
        ReviewGraphState graphState = new ReviewGraphState(Map.of(ReviewGraphState.REVIEW_STATE, reviewState));

        new OutputValidatorNode().apply(graphState);

        assertThat(graphState.reviewState().getReviewSummary()).isEqualTo("No review findings were produced.");
    }
}
