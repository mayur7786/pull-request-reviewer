package com.mayur.prreviewer.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitClientTest {

    @TempDir
    Path tempDir;

    // Verifies fallback to the previous commit when base and head match.
    @Test
    void fallsBackToPreviousCommitWhenTargetMatchesCurrentBranch() throws Exception {
        runGit("git", "init");
        runGit("git", "config", "user.name", "Test User");
        runGit("git", "config", "user.email", "test@example.com");
        Files.writeString(tempDir.resolve("Example.java"), "class Example { int value() { return 1; } }");
        runGit("git", "add", "Example.java");
        runGit("git", "commit", "-m", "Initial commit");
        Files.writeString(tempDir.resolve("Example.java"), "class Example { int value() { return 2; } }");
        runGit("git", "add", "Example.java");
        runGit("git", "commit", "-m", "Change return value");

        String currentBranch = runGit("git", "rev-parse", "--abbrev-ref", "HEAD").trim();
        GitDiffResult result = new GitClient().collectReviewData(tempDir, currentBranch, List.of());

        assertThat(result.commitMessages()).contains("Change return value");
        assertThat(result.changedFiles()).extracting(file -> file.path()).contains("Example.java");
        assertThat(result.diffSummary()).contains("Committed range (HEAD~1..HEAD)");
    }

    // Verifies that working tree Java changes are included in review scope.
    @Test
    void includesUncommittedWorkingTreeChanges() throws Exception {
        runGit("git", "init");
        runGit("git", "config", "user.name", "Test User");
        runGit("git", "config", "user.email", "test@example.com");
        Files.writeString(tempDir.resolve("Example.java"), "class Example { int value() { return 1; } }");
        runGit("git", "add", "Example.java");
        runGit("git", "commit", "-m", "Initial commit");
        Files.writeString(tempDir.resolve("Example.java"), "class Example { int value() { return 3; } }");
        Files.writeString(tempDir.resolve("Extra.java"), "class Extra {}");

        String currentBranch = runGit("git", "rev-parse", "--abbrev-ref", "HEAD").trim();
        GitDiffResult result = new GitClient().collectReviewData(tempDir, currentBranch, List.of());

        assertThat(result.changedFiles()).extracting(file -> file.path()).contains("Example.java", "Extra.java");
        assertThat(result.diffSummary()).contains("Working tree changes:");
    }

    // Verifies that file filters limit which changes are reviewed.
    @Test
    void restrictsReviewToRequestedFiles() throws Exception {
        runGit("git", "init");
        runGit("git", "config", "user.name", "Test User");
        runGit("git", "config", "user.email", "test@example.com");
        Files.writeString(tempDir.resolve("Example.java"), "class Example { int value() { return 1; } }");
        Files.writeString(tempDir.resolve("Ignored.java"), "class Ignored { int value() { return 1; } }");
        runGit("git", "add", "Example.java", "Ignored.java");
        runGit("git", "commit", "-m", "Initial commit");
        Files.writeString(tempDir.resolve("Example.java"), "class Example { int value() { return 3; } }");
        Files.writeString(tempDir.resolve("Ignored.java"), "class Ignored { int value() { return 4; } }");

        String currentBranch = runGit("git", "rev-parse", "--abbrev-ref", "HEAD").trim();
        GitDiffResult result = new GitClient().collectReviewData(tempDir, currentBranch, List.of("Example.java"));

        assertThat(result.changedFiles()).extracting(file -> file.path()).containsExactly("Example.java");
    }

    // Runs a git command inside the temporary test repository.
    private String runGit(String... command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(tempDir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        int exitCode = process.waitFor();
        assertThat(exitCode)
                .withFailMessage("Command failed: %s%n%s", List.of(command), output)
                .isEqualTo(0);
        return output;
    }
}
