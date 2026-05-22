package com.mayur.prreviewer.git;

import com.mayur.prreviewer.domain.ChangedFile;
import com.mayur.prreviewer.domain.ChangedFileType;
import com.mayur.prreviewer.domain.DiffHunk;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GitClient {

    private static final Pattern HUNK_HEADER = Pattern.compile("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*");

    // Collects committed and working tree Java changes for one review run.
    public GitDiffResult collectReviewData(Path repositoryRoot, String targetBranch, List<String> fileFilters) {
        ensureGitRepository(repositoryRoot);

        String currentBranch = execute(repositoryRoot, "git", "rev-parse", "--abbrev-ref", "HEAD").trim();
        String baseRef = resolveCommittedBaseRef(repositoryRoot, currentBranch, targetBranch);
        Set<String> normalizedFilters = normalizeFilters(fileFilters);
        DiffData committedDiff = collectCommittedDiff(repositoryRoot, baseRef, normalizedFilters);
        DiffData workingTreeDiff = collectWorkingTreeDiff(repositoryRoot, normalizedFilters);

        return new GitDiffResult(
                currentBranch,
                committedDiff.commitMessages(),
                mergeChangedFiles(committedDiff.changedFiles(), workingTreeDiff.changedFiles()),
                joinSummaries(committedDiff.summary(), workingTreeDiff.summary())
        );
    }

    // Reads the current file content from the working tree or HEAD fallback.
    public String readFile(Path repositoryRoot, String relativePath) {
        Path absolutePath = repositoryRoot.resolve(relativePath).normalize();
        if (Files.exists(absolutePath)) {
            try {
                return Files.readString(absolutePath);
            } catch (IOException exception) {
                throw new GitCommandException("Unable to read file from working tree: " + absolutePath, exception);
            }
        }
        return execute(repositoryRoot, "git", "show", "HEAD:" + relativePath);
    }

    // Verifies that the target directory is a git repository.
    private void ensureGitRepository(Path repositoryRoot) {
        String result = execute(repositoryRoot, "git", "rev-parse", "--is-inside-work-tree").trim();
        if (!"true".equalsIgnoreCase(result)) {
            throw new GitCommandException("Current directory is not a Git repository: " + repositoryRoot);
        }
    }

    // Chooses the best committed comparison range for the current branch.
    private String resolveCommittedBaseRef(Path repositoryRoot, String currentBranch, String targetBranch) {
        if (targetBranch != null
                && !targetBranch.isBlank()
                && !Objects.equals(targetBranch, currentBranch)
                && refExists(repositoryRoot, targetBranch)) {
            return targetBranch + "...HEAD";
        }
        if (hasParentCommit(repositoryRoot)) {
            return "HEAD~1..HEAD";
        }
        return null;
    }

    // Collects committed Java changes for the selected base range.
    private DiffData collectCommittedDiff(Path repositoryRoot, String baseRef, Set<String> fileFilters) {
        if (baseRef == null) {
            return new DiffData(List.of(), List.of(), "");
        }

        List<String> commitMessages = readCommitMessages(repositoryRoot, baseRef);
        String diffSummary = executeDiff(repositoryRoot, "--stat", baseRef, fileFilters).trim();
        String nameStatus = executeDiff(repositoryRoot, "--name-status", baseRef, fileFilters);
        String unifiedDiff = executeDiff(repositoryRoot, "-U3", baseRef, fileFilters);
        String summary = diffSummary.isBlank() ? "" : "Committed range (" + baseRef + "):" + System.lineSeparator() + diffSummary;
        return new DiffData(commitMessages, parseChangedFiles(nameStatus, unifiedDiff), summary);
    }

    // Collects uncommitted Java changes from the working tree.
    private DiffData collectWorkingTreeDiff(Path repositoryRoot, Set<String> fileFilters) {
        String trackedNameStatus = executeDiff(repositoryRoot, "--name-status", "HEAD", fileFilters);
        String trackedUnifiedDiff = executeDiff(repositoryRoot, "-U3", "HEAD", fileFilters);
        String trackedSummary = executeDiff(repositoryRoot, "--stat", "HEAD", fileFilters).trim();
        List<ChangedFile> trackedFiles = parseChangedFiles(trackedNameStatus, trackedUnifiedDiff);

        String porcelainStatus = executeStatus(repositoryRoot, fileFilters);
        List<ChangedFile> untrackedFiles = parseUntrackedFiles(porcelainStatus, fileFilters);
        String untrackedSummary = untrackedFiles.isEmpty()
                ? ""
                : untrackedFiles.size() + " untracked Java file(s)";

        List<ChangedFile> allWorkingTreeFiles = mergeChangedFiles(trackedFiles, untrackedFiles);
        if (allWorkingTreeFiles.isEmpty()) {
            return new DiffData(List.of(), List.of(), "");
        }

        StringBuilder summary = new StringBuilder("Working tree changes:");
        if (!trackedSummary.isBlank()) {
            summary.append(System.lineSeparator()).append(trackedSummary);
        }
        if (!untrackedSummary.isBlank()) {
            if (!trackedSummary.isBlank()) {
                summary.append(System.lineSeparator());
            }
            summary.append(untrackedSummary);
        }
        return new DiffData(List.of(), allWorkingTreeFiles, summary.toString());
    }

    // Reads commit messages for the active review range.
    private List<String> readCommitMessages(Path repositoryRoot, String baseRef) {
        String raw;
        if ("HEAD~1..HEAD".equals(baseRef)) {
            raw = execute(repositoryRoot, "git", "log", "-1", "--format=%s", "HEAD");
        } else {
            String range = baseRef.replace("...HEAD", "..HEAD");
            raw = execute(repositoryRoot, "git", "log", "--format=%s", range);
        }
        return Arrays.stream(raw.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    // Turns untracked Java files into reviewable changed-file entries.
    private List<ChangedFile> parseUntrackedFiles(String porcelainStatus, Set<String> fileFilters) {
        return Arrays.stream(porcelainStatus.split("\\R"))
                .map(String::trim)
                .filter(line -> line.startsWith("?? "))
                .map(line -> line.substring(3).trim())
                .filter(path -> path.endsWith(".java"))
                .filter(path -> fileFilters.isEmpty() || fileFilters.contains(normalizePath(path)))
                .map(path -> new ChangedFile(path, ChangedFileType.NEW_FILE, List.of(), 0, 0))
                .toList();
    }

    // Runs a filtered git diff command for one output mode.
    private String executeDiff(Path repositoryRoot, String mode, String ref, Set<String> fileFilters) {
        List<String> command = new ArrayList<>(List.of("git", "diff", mode, ref, "--"));
        if (fileFilters.isEmpty()) {
            command.add("*.java");
        } else {
            command.addAll(fileFilters);
        }
        return execute(repositoryRoot, command.toArray(String[]::new));
    }

    // Runs git status for the current file filter set.
    private String executeStatus(Path repositoryRoot, Set<String> fileFilters) {
        List<String> command = new ArrayList<>(List.of("git", "status", "--porcelain", "--untracked-files=all", "--"));
        if (fileFilters.isEmpty()) {
            command.add("*.java");
        } else {
            command.addAll(fileFilters);
        }
        return execute(repositoryRoot, command.toArray(String[]::new));
    }

    // Parses name-status output and diff hunks into changed file objects.
    private List<ChangedFile> parseChangedFiles(String nameStatusOutput, String diffOutput) {
        Map<String, FileAccumulator> files = new LinkedHashMap<>();

        for (String line : nameStatusOutput.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\t");
            if (parts.length < 2) {
                continue;
            }
            ChangedFileType type = switch (parts[0].charAt(0)) {
                case 'A' -> ChangedFileType.NEW_FILE;
                case 'D' -> ChangedFileType.DELETED_FILE;
                default -> ChangedFileType.MODIFIED_FILE;
            };
            files.put(parts[1], new FileAccumulator(parts[1], type));
        }

        FileAccumulator current = null;
        List<String> currentHunkLines = null;
        int oldStart = 0;
        int oldCount = 0;
        int newStart = 0;
        int newCount = 0;

        for (String line : diffOutput.split("\\R", -1)) {
            if (line.startsWith("diff --git")) {
                if (current != null && currentHunkLines != null) {
                    current.hunks.add(new DiffHunk(oldStart, oldCount, newStart, newCount, List.copyOf(currentHunkLines)));
                }
                current = null;
                currentHunkLines = null;
                continue;
            }

            if (line.startsWith("+++ b/")) {
                String path = line.substring("+++ b/".length());
                current = files.computeIfAbsent(path, key -> new FileAccumulator(key, ChangedFileType.MODIFIED_FILE));
                continue;
            }

            Matcher matcher = HUNK_HEADER.matcher(line);
            if (matcher.matches()) {
                if (current != null && currentHunkLines != null) {
                    current.hunks.add(new DiffHunk(oldStart, oldCount, newStart, newCount, List.copyOf(currentHunkLines)));
                }
                currentHunkLines = new ArrayList<>();
                oldStart = Integer.parseInt(matcher.group(1));
                oldCount = matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2));
                newStart = Integer.parseInt(matcher.group(3));
                newCount = matcher.group(4) == null ? 1 : Integer.parseInt(matcher.group(4));
                if (currentHunkLines != null) {
                    currentHunkLines.add(line);
                }
                continue;
            }

            if (current != null && currentHunkLines != null) {
                currentHunkLines.add(line);
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    current.additions++;
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    current.deletions++;
                }
            }
        }

        if (current != null && currentHunkLines != null) {
            current.hunks.add(new DiffHunk(oldStart, oldCount, newStart, newCount, List.copyOf(currentHunkLines)));
        }

        return files.values().stream()
                .map(FileAccumulator::toChangedFile)
                .filter(file -> file.path().endsWith(".java"))
                .toList();
    }

    // Runs a git command and throws on failure.
    private String execute(Path repositoryRoot, String... command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repositoryRoot.toFile());
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new GitCommandException("Git command failed: " + String.join(" ", command) + System.lineSeparator() + output);
            }
            return output;
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GitCommandException("Unable to run git command: " + String.join(" ", command), exception);
        }
    }

    // Checks whether a git ref exists.
    private boolean refExists(Path repositoryRoot, String ref) {
        return executeAllowFailure(repositoryRoot, "git", "rev-parse", "--verify", ref).exitCode() == 0;
    }

    // Checks whether the current branch has a parent commit.
    private boolean hasParentCommit(Path repositoryRoot) {
        return executeAllowFailure(repositoryRoot, "git", "rev-parse", "--verify", "HEAD~1").exitCode() == 0;
    }

    // Runs a git command without failing the whole flow on non-zero exit.
    private CommandResult executeAllowFailure(Path repositoryRoot, String... command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repositoryRoot.toFile());
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
            int exitCode = process.waitFor();
            return new CommandResult(exitCode, output);
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GitCommandException("Unable to run git command: " + String.join(" ", command), exception);
        }
    }

    // Merges committed and working tree changes by file path.
    private List<ChangedFile> mergeChangedFiles(List<ChangedFile> primary, List<ChangedFile> secondary) {
        Map<String, ChangedFile> merged = new LinkedHashMap<>();
        primary.forEach(file -> merged.put(file.path(), file));
        for (ChangedFile file : secondary) {
            merged.merge(file.path(), file, this::mergeChangedFile);
        }
        return List.copyOf(merged.values());
    }

    // Merges two changed-file views for the same file.
    private ChangedFile mergeChangedFile(ChangedFile left, ChangedFile right) {
        ChangedFileType mergedType = switch (left.type()) {
            case NEW_FILE -> ChangedFileType.NEW_FILE;
            case DELETED_FILE -> right.type() == ChangedFileType.NEW_FILE ? ChangedFileType.MODIFIED_FILE : ChangedFileType.DELETED_FILE;
            case MODIFIED_FILE -> right.type() == ChangedFileType.NEW_FILE ? ChangedFileType.NEW_FILE : right.type() == ChangedFileType.DELETED_FILE ? ChangedFileType.DELETED_FILE : ChangedFileType.MODIFIED_FILE;
        };

        List<DiffHunk> hunks = new ArrayList<>(left.hunks());
        hunks.addAll(right.hunks());
        return new ChangedFile(
                left.path(),
                mergedType,
                List.copyOf(hunks),
                left.additions() + right.additions(),
                left.deletions() + right.deletions()
        );
    }

    // Joins committed and working tree summaries into one report block.
    private String joinSummaries(String committedSummary, String workingTreeSummary) {
        return java.util.stream.Stream.of(committedSummary, workingTreeSummary)
                .filter(summary -> summary != null && !summary.isBlank())
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
    }

    // Normalizes requested file filters into repo-style paths.
    private Set<String> normalizeFilters(List<String> fileFilters) {
        if (fileFilters == null) {
            return Set.of();
        }
        return fileFilters.stream()
                .filter(path -> path != null && !path.isBlank())
                .map(this::normalizePath)
                .collect(Collectors.toSet());
    }

    // Converts Windows separators to git-style path separators.
    private String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    private static final class FileAccumulator {
        private final String path;
        private final ChangedFileType type;
        private final List<DiffHunk> hunks = new ArrayList<>();
        private int additions;
        private int deletions;

        // Starts collecting diff data for one file.
        private FileAccumulator(String path, ChangedFileType type) {
            this.path = path;
            this.type = type;
        }

        // Converts collected diff data into an immutable changed file.
        private ChangedFile toChangedFile() {
            return new ChangedFile(path, type, List.copyOf(hunks), additions, deletions);
        }
    }

    private record DiffData(
            List<String> commitMessages,
            List<ChangedFile> changedFiles,
            String summary
    ) {
    }

    private record CommandResult(
            int exitCode,
            String output
    ) {
    }
}
