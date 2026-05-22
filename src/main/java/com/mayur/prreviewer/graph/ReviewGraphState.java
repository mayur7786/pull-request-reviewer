package com.mayur.prreviewer.graph;

import com.mayur.prreviewer.state.ReviewState;
import java.util.Map;
import org.bsc.langgraph4j.state.AgentState;

public class ReviewGraphState extends AgentState {

    public static final String REVIEW_STATE = "reviewState";

    public ReviewGraphState(Map<String, Object> initData) {
        super(initData);
    }

    // Returns the typed review state from the graph state map.
    public ReviewState reviewState() {
        return this.<ReviewState>value(REVIEW_STATE)
                .orElseThrow(() -> new IllegalStateException("Review state is missing from graph context."));
    }
}
