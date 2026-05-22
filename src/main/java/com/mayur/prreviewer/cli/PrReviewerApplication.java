package com.mayur.prreviewer.cli;

import com.mayur.prreviewer.config.ReviewerConfig;
import com.mayur.prreviewer.config.LlmProvider;
import com.mayur.prreviewer.graph.ReviewWorkflow;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "pr-reviewer", mixinStandardHelpOptions = true, description = "Run a local AI pull request review.")
public class PrReviewerApplication implements Runnable {

    @Option(names = "--target-branch", required = true, description = "Branch to diff against.")
    private String targetBranch;

    @Option(names = "--repo", defaultValue = ".", description = "Repository root path.")
    private Path repositoryRoot;

    @Option(names = "--model", defaultValue = "gpt-5.2", description = "OpenAI-compatible chat model name.")
    private String model;

    @Option(names = "--provider", defaultValue = "OPENAI", description = "LLM provider: ${COMPLETION-CANDIDATES}")
    private LlmProvider provider;

    @Option(names = "--api-base-url", description = "Optional provider base URL or endpoint override.")
    private String apiBaseUrl;

    @Option(names = "--api-key-env", description = "Optional environment variable name holding the provider API key.")
    private String apiKeyEnvVar;

    @Option(names = "--output", defaultValue = ".review/ai-review.md", description = "Markdown output path.")
    private Path outputPath;

    @Option(names = "--developer-note", description = "Optional note describing intent.")
    private String developerNote;

    @Option(names = "--context-lines", defaultValue = "20", description = "Surrounding context lines for modified hunks.")
    private int contextLines;

    @Option(names = "--file", description = "Specific Java file(s) to review, relative to the repository root.")
    private List<String> fileFilters = new ArrayList<>();

    // Starts the CLI command.
    public static void main(String[] args) {
        int exitCode = new CommandLine(new PrReviewerApplication()).execute(args);
        System.exit(exitCode);
    }

    // Builds runtime config and runs the review workflow.
    @Override
    public void run() {
        Path normalizedRepositoryRoot = repositoryRoot.toAbsolutePath().normalize();
        Path resolvedOutputPath = outputPath.isAbsolute()
                ? outputPath.toAbsolutePath().normalize()
                : normalizedRepositoryRoot.resolve(outputPath).normalize();

        ReviewerConfig config = new ReviewerConfig(
                normalizedRepositoryRoot,
                targetBranch,
                provider,
                model,
                apiBaseUrl,
                apiKeyEnvVar,
                resolvedOutputPath,
                developerNote,
                contextLines,
                List.copyOf(fileFilters)
        );

        var state = new ReviewWorkflow(config).run();
        System.out.println(state.getMarkdownReport());
        System.out.println();
        System.out.println("Report written to " + config.outputPath());
    }
}
