package com.mayur.prreviewer.node;

import static org.assertj.core.api.Assertions.assertThat;

import com.mayur.prreviewer.domain.ChangedFile;
import com.mayur.prreviewer.domain.ChangedFileType;
import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.service.IntentInferenceService;
import com.mayur.prreviewer.state.ReviewState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IntentInferenceNodeTest {

    // Verifies that intent is inferred from branch and commit metadata.
    @Test
    void infersIntentFromBranchAndCommitData() {
        ReviewState reviewState = new ReviewState();
        reviewState.setCurrentBranch("feature/add-timeout");
        reviewState.setCommitMessages(List.of("Add timeout handling for payment gateway"));
        reviewState.setChangedFiles(List.of(new ChangedFile("src/PaymentService.java", ChangedFileType.MODIFIED_FILE, List.of(), 3, 0)));
        ReviewGraphState graphState = new ReviewGraphState(Map.of(ReviewGraphState.REVIEW_STATE, reviewState));

        new IntentInferenceNode(new IntentInferenceService()).apply(graphState);

        assertThat(graphState.reviewState().getInferredIntent()).contains("Add timeout handling");
        assertThat(graphState.reviewState().getIntentConfidence()).isGreaterThan(0.5);
    }
}
