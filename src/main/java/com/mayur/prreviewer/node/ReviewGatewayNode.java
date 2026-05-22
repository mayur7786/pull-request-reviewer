package com.mayur.prreviewer.node;

import com.mayur.prreviewer.graph.ReviewGraphState;
import com.mayur.prreviewer.service.LlmReviewClient;
import com.mayur.prreviewer.service.OutputValidatorService;
import com.mayur.prreviewer.service.ReviewGatewayService;
import com.mayur.prreviewer.service.ReviewPromptFactory;
import java.util.ArrayList;
import java.util.Map;
import org.bsc.langgraph4j.action.NodeAction;

public class ReviewGatewayNode implements NodeAction<ReviewGraphState> {

    private final ReviewGatewayService gatewayService;
    private final ReviewPromptFactory promptFactory;
    private final LlmReviewClient llmReviewClient;
    private final OutputValidatorService outputValidatorService;

    public ReviewGatewayNode(
            ReviewGatewayService gatewayService,
            ReviewPromptFactory promptFactory,
            LlmReviewClient llmReviewClient,
            OutputValidatorService outputValidatorService
    ) {
        this.gatewayService = gatewayService;
        this.promptFactory = promptFactory;
        this.llmReviewClient = llmReviewClient;
        this.outputValidatorService = outputValidatorService;
    }

    // Sends review chunks to the model and collects validated findings.
    @Override
    public Map<String, Object> apply(ReviewGraphState state) {
        var reviewState = state.reviewState();
        var allFindings = new ArrayList<com.mayur.prreviewer.domain.ReviewFinding>();
        var summaries = new ArrayList<String>();

        for (var chunk : gatewayService.chunk(reviewState.getReviewContexts())) {
            var prompt = promptFactory.buildUserPrompt(
                    reviewState.getInferredIntent(),
                    chunk,
                    reviewState.getDiffSummary()
            );
            var response = llmReviewClient.review(promptFactory.buildSystemPrompt(), prompt);
            var validated = outputValidatorService.validate(
                    response,
                    reviewState.getChangedFiles().stream().map(com.mayur.prreviewer.domain.ChangedFile::path).toList()
            );
            summaries.add(validated.summary());
            allFindings.addAll(validated.findings());
        }

        reviewState.setReviewSummary(String.join(" ", summaries).trim());
        reviewState.setFindings(allFindings);
        return Map.of(ReviewGraphState.REVIEW_STATE, reviewState);
    }
}
