package com.mayur.prreviewer.node;

import com.mayur.prreviewer.config.ReviewerConfig;
import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.service.MarkdownWriter;
import com.mayur.prreviewer.service.ReportGeneratorService;
import java.util.Map;
import org.bsc.langgraph4j.action.NodeAction;

public class ReportGeneratorNode implements NodeAction<ReviewGraphState> {

    private final ReportGeneratorService reportGeneratorService;
    private final MarkdownWriter markdownWriter;
    private final ReviewerConfig config;

    public ReportGeneratorNode(ReportGeneratorService reportGeneratorService, MarkdownWriter markdownWriter, ReviewerConfig config) {
        this.reportGeneratorService = reportGeneratorService;
        this.markdownWriter = markdownWriter;
        this.config = config;
    }

    // Builds the final markdown report and writes it to disk.
    @Override
    public Map<String, Object> apply(ReviewGraphState state) {
        var reviewState = state.reviewState();
        String markdown = reportGeneratorService.generate(reviewState);
        reviewState.setMarkdownReport(markdown);
        markdownWriter.write(config.outputPath(), markdown);
        return Map.of(ReviewGraphState.REVIEW_STATE, reviewState);
    }
}
