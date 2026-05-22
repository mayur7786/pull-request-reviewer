package com.mayur.prreviewer.node;

import com.mayur.prreviewer.config.ReviewerConfig;
import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.service.ContextBuilderService;
import java.util.Map;
import org.bsc.langgraph4j.action.NodeAction;

public class ContextBuilderNode implements NodeAction<ReviewGraphState> {

    private final ContextBuilderService service;
    private final ReviewerConfig config;

    public ContextBuilderNode(ContextBuilderService service, ReviewerConfig config) {
        this.service = service;
        this.config = config;
    }

    // Builds grounded code context for each file or hunk under review.
    @Override
    public Map<String, Object> apply(ReviewGraphState state) {
        var reviewState = state.reviewState();
        reviewState.setReviewContexts(service.build(
                config.repositoryRoot(),
                reviewState.getChangedFiles(),
                config.contextLines()
        ));
        return Map.of(ReviewGraphState.REVIEW_STATE, reviewState);
    }
}
