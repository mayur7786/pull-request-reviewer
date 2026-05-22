package com.mayur.prreviewer.node;

import static org.assertj.core.api.Assertions.assertThat;

import com.mayur.prreviewer.config.LlmProvider;
import com.mayur.prreviewer.config.ReviewerConfig;
import com.mayur.prreviewer.domain.ChangedFile;
import com.mayur.prreviewer.domain.ChangedFileType;
import com.mayur.prreviewer.domain.DiffHunk;
import com.mayur.prreviewer.git.GitClient;
import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.service.ContextBuilderService;
import com.mayur.prreviewer.state.ReviewState;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextBuilderNodeTest {

    // Verifies that modified-file context includes the expected snippet.
    @Test
    void buildsSnippetForModifiedFile() {
        GitClient gitClient = new GitClient() {
            @Override
            // Returns fixed file content for context building.
            public String readFile(Path repositoryRoot, String relativePath) {
                return """
                        package demo;
                        import java.util.Objects;
                        public class Main {
                            public void run() {
                                String value = "ok";
                                System.out.println(value);
                            }
                        }
                        """;
            }
        };

        ReviewState reviewState = new ReviewState();
        reviewState.setChangedFiles(List.of(new ChangedFile(
                "src/Main.java",
                ChangedFileType.MODIFIED_FILE,
                List.of(new DiffHunk(4, 1, 4, 2, List.of("@@ -4,1 +4,2 @@", "-x", "+y"))),
                1,
                1
        )));
        ReviewGraphState graphState = new ReviewGraphState(Map.of(ReviewGraphState.REVIEW_STATE, reviewState));
        ReviewerConfig config = new ReviewerConfig(Path.of("."), "main", LlmProvider.OPENAI, "gpt-5.2", null, null, Path.of(".review/ai-review.md"), null, 1, List.of());

        new ContextBuilderNode(new ContextBuilderService(gitClient), config).apply(graphState);

        assertThat(graphState.reviewState().getReviewContexts()).hasSize(1);
        assertThat(graphState.reviewState().getReviewContexts().getFirst().snippet()).contains("System.out.println");
    }
}
