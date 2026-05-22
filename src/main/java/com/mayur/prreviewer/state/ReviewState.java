package com.mayur.prreviewer.state;

import com.mayur.prreviewer.domain.ChangedFile;
import com.mayur.prreviewer.domain.ReviewContext;
import com.mayur.prreviewer.domain.ReviewFinding;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ReviewState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String targetBranch;
    private String currentBranch;
    private List<String> commitMessages = new ArrayList<>();
    private List<ChangedFile> changedFiles = new ArrayList<>();
    private String inferredIntent;
    private Double intentConfidence;
    private List<ReviewContext> reviewContexts = new ArrayList<>();
    private List<ReviewFinding> findings = new ArrayList<>();
    private String reviewSummary;
    private String markdownReport;
    private String developerNote;
    private String diffSummary;

    // Returns the requested base branch for review.
    public String getTargetBranch() {
        return targetBranch;
    }

    // Stores the requested base branch for review.
    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    // Returns the currently checked out branch.
    public String getCurrentBranch() {
        return currentBranch;
    }

    // Stores the currently checked out branch.
    public void setCurrentBranch(String currentBranch) {
        this.currentBranch = currentBranch;
    }

    // Returns commit messages in the review range.
    public List<String> getCommitMessages() {
        return commitMessages;
    }

    // Stores commit messages in the review range.
    public void setCommitMessages(List<String> commitMessages) {
        this.commitMessages = new ArrayList<>(commitMessages);
    }

    // Returns files selected for review.
    public List<ChangedFile> getChangedFiles() {
        return changedFiles;
    }

    // Stores files selected for review.
    public void setChangedFiles(List<ChangedFile> changedFiles) {
        this.changedFiles = new ArrayList<>(changedFiles);
    }

    // Returns the inferred purpose of the change.
    public String getInferredIntent() {
        return inferredIntent;
    }

    // Stores the inferred purpose of the change.
    public void setInferredIntent(String inferredIntent) {
        this.inferredIntent = inferredIntent;
    }

    // Returns confidence in the inferred intent.
    public Double getIntentConfidence() {
        return intentConfidence;
    }

    // Stores confidence in the inferred intent.
    public void setIntentConfidence(Double intentConfidence) {
        this.intentConfidence = intentConfidence;
    }

    // Returns grounded code contexts sent for review.
    public List<ReviewContext> getReviewContexts() {
        return reviewContexts;
    }

    // Stores grounded code contexts sent for review.
    public void setReviewContexts(List<ReviewContext> reviewContexts) {
        this.reviewContexts = new ArrayList<>(reviewContexts);
    }

    // Returns validated review findings.
    public List<ReviewFinding> getFindings() {
        return findings;
    }

    // Stores validated review findings.
    public void setFindings(List<ReviewFinding> findings) {
        this.findings = new ArrayList<>(findings);
    }

    // Returns the high-level review summary.
    public String getReviewSummary() {
        return reviewSummary;
    }

    // Stores the high-level review summary.
    public void setReviewSummary(String reviewSummary) {
        this.reviewSummary = reviewSummary;
    }

    // Returns the final markdown report text.
    public String getMarkdownReport() {
        return markdownReport;
    }

    // Stores the final markdown report text.
    public void setMarkdownReport(String markdownReport) {
        this.markdownReport = markdownReport;
    }

    // Returns the optional developer note.
    public String getDeveloperNote() {
        return developerNote;
    }

    // Stores the optional developer note.
    public void setDeveloperNote(String developerNote) {
        this.developerNote = developerNote;
    }

    // Returns the git diff summary text.
    public String getDiffSummary() {
        return diffSummary;
    }

    // Stores the git diff summary text.
    public void setDiffSummary(String diffSummary) {
        this.diffSummary = diffSummary;
    }
}
