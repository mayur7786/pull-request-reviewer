package com.mayur.prreviewer.service;

import com.mayur.prreviewer.domain.ChangedFile;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public class IntentInferenceService {

    // Infers review intent from branch, commits, files, diff, and notes.
    public IntentResult inferIntent(
            String branchName,
            List<String> commitMessages,
            List<ChangedFile> changedFiles,
            String diffSummary,
            String developerNote
    ) {
        if (developerNote != null && !developerNote.isBlank()) {
            return new IntentResult(developerNote.trim(), 0.95);
        }

        String normalizedBranch = branchName == null ? "" : branchName.replace('-', ' ').replace('_', ' ').trim();
        String primaryCommit = commitMessages.isEmpty() ? "" : commitMessages.getFirst();
        String fileHints = changedFiles.stream()
                .limit(3)
                .map(ChangedFile::path)
                .map(this::fileHint)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");

        StringJoiner joiner = new StringJoiner(" ");
        if (!primaryCommit.isBlank()) {
            joiner.add(primaryCommit);
        }
        if (!normalizedBranch.isBlank()) {
            joiner.add("Branch suggests " + normalizedBranch + ".");
        }
        if (!fileHints.isBlank()) {
            joiner.add("Touches " + fileHints + ".");
        }
        if (joiner.length() == 0 && diffSummary != null && !diffSummary.isBlank()) {
            joiner.add("Update Java code based on diff summary: " + diffSummary.replaceAll("\\s+", " ").trim());
        }

        String raw = joiner.toString().trim();
        String intent = raw.isBlank() ? "Review Java changes for likely behavioral updates." : normalizeIntent(raw);
        double confidence = scoreConfidence(branchName, commitMessages, changedFiles, developerNote);
        return new IntentResult(intent, confidence);
    }

    // Cleans up the inferred intent into one readable sentence.
    private String normalizeIntent(String raw) {
        String intent = raw.replaceAll("\\s+", " ").trim();
        if (!intent.endsWith(".")) {
            intent = intent + ".";
        }
        return Character.toUpperCase(intent.charAt(0)) + intent.substring(1);
    }

    // Turns a file path into a lightweight feature hint.
    private String fileHint(String path) {
        String fileName = path.substring(path.lastIndexOf('/') + 1).replace(".java", "");
        return fileName.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
    }

    // Scores how trustworthy the inferred intent looks.
    private double scoreConfidence(
            String branchName,
            List<String> commitMessages,
            List<ChangedFile> changedFiles,
            String developerNote
    ) {
        double confidence = 0.35;
        if (branchName != null && !branchName.isBlank()) {
            confidence += 0.15;
        }
        if (!commitMessages.isEmpty()) {
            confidence += 0.2;
        }
        if (!changedFiles.isEmpty()) {
            confidence += 0.15;
        }
        if (developerNote != null && !developerNote.isBlank()) {
            confidence += 0.15;
        }
        return Math.min(confidence, 0.99);
    }

    public record IntentResult(String intent, double confidence) {
    }
}
