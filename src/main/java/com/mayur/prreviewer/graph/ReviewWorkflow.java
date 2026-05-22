package com.mayur.prreviewer.graph;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mayur.prreviewer.config.ReviewerConfig;
import com.mayur.prreviewer.git.GitClient;
import com.mayur.prreviewer.node.ContextBuilderNode;
import com.mayur.prreviewer.node.GitDiffNode;
import com.mayur.prreviewer.node.IntentInferenceNode;
import com.mayur.prreviewer.node.JavaFileClassifierNode;
import com.mayur.prreviewer.node.OutputValidatorNode;
import com.mayur.prreviewer.node.ReportGeneratorNode;
import com.mayur.prreviewer.node.ReviewGatewayNode;
import com.mayur.prreviewer.service.ContextBuilderService;
import com.mayur.prreviewer.service.IntentInferenceService;
import com.mayur.prreviewer.service.JavaFileClassifierService;
import com.mayur.prreviewer.service.LlmReviewClient;
import com.mayur.prreviewer.service.LlmProviderConfig;
import com.mayur.prreviewer.service.MarkdownWriter;
import com.mayur.prreviewer.service.OutputValidatorService;
import com.mayur.prreviewer.service.ReportGeneratorService;
import com.mayur.prreviewer.service.ReviewGatewayService;
import com.mayur.prreviewer.service.ReviewPromptFactory;
import com.mayur.prreviewer.state.ReviewState;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.bsc.langgraph4j.StateGraph;

public class ReviewWorkflow {

    private final ReviewerConfig config;

    public ReviewWorkflow(ReviewerConfig config) {
        this.config = config;
    }

    // Runs the full review graph and returns the final state.
    public ReviewState run() {
        try {
            var compiledGraph = buildGraph().compile();
            var finalState = compiledGraph.invoke(Map.of(
                    ReviewGraphState.REVIEW_STATE, new ReviewState()
            )).get();
            return finalState.reviewState();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Workflow execution interrupted.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Workflow execution failed.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Workflow execution failed.", exception);
        }
    }

    // Wires the LangGraph workflow and its node dependencies.
    private StateGraph<ReviewGraphState> buildGraph() throws Exception {
        GitClient gitClient = new GitClient();
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        return new StateGraph<>(ReviewGraphState::new)
                .addNode("gitDiff", node_async(new GitDiffNode(gitClient, config)))
                .addNode("intentInference", node_async(new IntentInferenceNode(new IntentInferenceService())))
                .addNode("javaFileClassifier", node_async(new JavaFileClassifierNode(new JavaFileClassifierService())))
                .addNode("contextBuilder", node_async(new ContextBuilderNode(new ContextBuilderService(gitClient), config)))
                .addNode("reviewGateway", node_async(new ReviewGatewayNode(
                        new ReviewGatewayService(3),
                        new ReviewPromptFactory(),
                        new LlmReviewClient(new LlmProviderConfig(
                                config.provider(),
                                config.model(),
                                config.apiBaseUrl(),
                                config.apiKeyEnvVar()
                        ), objectMapper),
                        new OutputValidatorService()
                )))
                .addNode("outputValidator", node_async(new OutputValidatorNode()))
                .addNode("reportGenerator", node_async(new ReportGeneratorNode(
                        new ReportGeneratorService(),
                        new MarkdownWriter(),
                        config
                )))
                .addEdge(START, "gitDiff")
                .addEdge("gitDiff", "intentInference")
                .addEdge("intentInference", "javaFileClassifier")
                .addEdge("javaFileClassifier", "contextBuilder")
                .addEdge("contextBuilder", "reviewGateway")
                .addEdge("reviewGateway", "outputValidator")
                .addEdge("outputValidator", "reportGenerator")
                .addEdge("reportGenerator", END);
    }
}
