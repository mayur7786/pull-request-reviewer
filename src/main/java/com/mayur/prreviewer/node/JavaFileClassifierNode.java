package com.mayur.prreviewer.node;

import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.service.JavaFileClassifierService;
import java.util.Map;
import org.bsc.langgraph4j.action.NodeAction;

public class JavaFileClassifierNode implements NodeAction<ReviewGraphState> {

    private final JavaFileClassifierService service;

    public JavaFileClassifierNode(JavaFileClassifierService service) {
        this.service = service;
    }

    // Keeps only Java files in review scope.
    @Override
    public Map<String, Object> apply(ReviewGraphState state) {
        var reviewState = state.reviewState();
        reviewState.setChangedFiles(service.classify(reviewState.getChangedFiles()));
        return Map.of(ReviewGraphState.REVIEW_STATE, reviewState);
    }
}
