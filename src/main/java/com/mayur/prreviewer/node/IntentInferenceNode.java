package com.mayur.prreviewer.node;

import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.service.IntentInferenceService;
import java.util.Map;
import org.bsc.langgraph4j.action.NodeAction;

public class IntentInferenceNode implements NodeAction<ReviewGraphState> {

    private final IntentInferenceService service;

    public IntentInferenceNode(IntentInferenceService service) {
        this.service = service;
    }

    // Infers the likely purpose of the change.
    @Override
    public Map<String, Object> apply(ReviewGraphState state) {
        var reviewState = state.reviewState();
        var result = service.inferIntent(
                reviewState.getCurrentBranch(),
                reviewState.getCommitMessages(),
                reviewState.getChangedFiles(),
                reviewState.getDiffSummary(),
                reviewState.getDeveloperNote()
        );
        reviewState.setInferredIntent(result.intent());
        reviewState.setIntentConfidence(result.confidence());
        return Map.of(ReviewGraphState.REVIEW_STATE, reviewState);
    }
}
