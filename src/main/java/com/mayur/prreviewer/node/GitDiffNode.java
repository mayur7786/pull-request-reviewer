package com.mayur.prreviewer.node;

import com.mayur.prreviewer.config.ReviewerConfig;
import com.mayur.prreviewer.git.GitClient;
import com.mayur.prreviewer.git.GitDiffResult;
import com.mayur.prreviewer.graph.ReviewGraphState;
import java.util.Map;
import org.bsc.langgraph4j.action.NodeAction;

public class GitDiffNode implements NodeAction<ReviewGraphState> {

    private final GitClient gitClient;
    private final ReviewerConfig config;

    public GitDiffNode(GitClient gitClient, ReviewerConfig config) {
        this.gitClient = gitClient;
        this.config = config;
    }

    // Loads git metadata and changed files into the review state.
    @Override
    public Map<String, Object> apply(ReviewGraphState state) {
        GitDiffResult result = gitClient.collectReviewData(config.repositoryRoot(), config.targetBranch(), config.fileFilters());
        var reviewState = state.reviewState();
        reviewState.setTargetBranch(config.targetBranch());
        reviewState.setCurrentBranch(result.currentBranch());
        reviewState.setCommitMessages(result.commitMessages());
        reviewState.setChangedFiles(result.changedFiles());
        reviewState.setDiffSummary(result.diffSummary());
        reviewState.setDeveloperNote(config.developerNote());
        return Map.of(ReviewGraphState.REVIEW_STATE, reviewState);
    }
}
