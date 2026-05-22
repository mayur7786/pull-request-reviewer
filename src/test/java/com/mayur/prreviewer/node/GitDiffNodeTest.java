package com.mayur.prreviewer.node;

import static org.assertj.core.api.Assertions.assertThat;

import com.mayur.prreviewer.config.LlmProvider;
import com.mayur.prreviewer.config.ReviewerConfig;
import com.mayur.prreviewer.domain.ChangedFile;
import com.mayur.prreviewer.domain.ChangedFileType;
import com.mayur.prreviewer.git.GitClient;
import com.mayur.prreviewer.git.GitDiffResult;
import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.state.ReviewState;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GitDiffNodeTest {

    // Verifies that git metadata is copied into review state.
    @Test
    void populatesReviewStateFromGitData() {
        GitClient gitClient = new GitClient() {
            @Override
            // Returns a fixed git diff result for this test.
            public GitDiffResult collectReviewData(Path repositoryRoot, String targetBranch, List<String> fileFilters) {
                return new GitDiffResult(
                        "feature/test",
                        List.of("Add test"),
                        List.of(new ChangedFile("src/Main.java", ChangedFileType.MODIFIED_FILE, List.of(), 2, 1)),
                        "1 file changed"
                );
            }
        };

        ReviewGraphState graphState = new ReviewGraphState(Map.of(ReviewGraphState.REVIEW_STATE, new ReviewState()));
        ReviewerConfig config = new ReviewerConfig(Path.of("."), "main", LlmProvider.OPENAI, "gpt-5.2", null, null, Path.of(".review/ai-review.md"), null, 10, List.of());

        new GitDiffNode(gitClient, config).apply(graphState);

        assertThat(graphState.reviewState().getCurrentBranch()).isEqualTo("feature/test");
        assertThat(graphState.reviewState().getChangedFiles()).hasSize(1);
        assertThat(graphState.reviewState().getDiffSummary()).isEqualTo("1 file changed");
    }
}
