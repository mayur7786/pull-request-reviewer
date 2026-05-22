package com.mayur.prreviewer.node;

import com.mayur.prreviewer.graph.ReviewGraphState;
import java.util.Map;
import org.bsc.langgraph4j.action.NodeAction;

public class OutputValidatorNode implements NodeAction<ReviewGraphState> {

    // Ensures the review summary is never left blank.
    @Override
    public Map<String, Object> apply(ReviewGraphState state) {
        var reviewState = state.reviewState();
        if (reviewState.getReviewSummary() == null || reviewState.getReviewSummary().isBlank()) {
            reviewState.setReviewSummary("No review findings were produced.");
        }
        return Map.of(ReviewGraphState.REVIEW_STATE, reviewState);
    }
}
