package com.mayur.prreviewer.node;

import static org.assertj.core.api.Assertions.assertThat;

import com.mayur.prreviewer.domain.ChangedFile;
import com.mayur.prreviewer.domain.ChangedFileType;
import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.service.JavaFileClassifierService;
import com.mayur.prreviewer.state.ReviewState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JavaFileClassifierNodeTest {

    // Verifies that non-Java files are excluded from review scope.
    @Test
    void keepsOnlyJavaFiles() {
        ReviewState reviewState = new ReviewState();
        reviewState.setChangedFiles(List.of(
                new ChangedFile("src/Main.java", ChangedFileType.MODIFIED_FILE, List.of(), 1, 0),
                new ChangedFile("README.md", ChangedFileType.MODIFIED_FILE, List.of(), 1, 0)
        ));
        ReviewGraphState graphState = new ReviewGraphState(Map.of(ReviewGraphState.REVIEW_STATE, reviewState));

        new JavaFileClassifierNode(new JavaFileClassifierService()).apply(graphState);

        assertThat(graphState.reviewState().getChangedFiles())
                .extracting(ChangedFile::path)
                .containsExactly("src/Main.java");
    }
}
