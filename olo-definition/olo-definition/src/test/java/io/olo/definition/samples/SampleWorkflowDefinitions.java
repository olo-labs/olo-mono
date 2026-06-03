package io.olo.definition.samples;

import io.olo.definition.edge.EdgeDefinition;
import io.olo.definition.extension.ExtensionDefinition;
import io.olo.definition.model.ModelProviderDefinition;
import io.olo.definition.model.ModelRoutingDefinition;
import io.olo.definition.node.NodeDefinition;
import io.olo.definition.node.NodeRouterDefinition;
import io.olo.definition.node.NodeType;
import io.olo.definition.variable.VariableDefinition;
import io.olo.definition.workflow.WorkflowBuilder;
import io.olo.definition.workflow.WorkflowDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Programmatic builders mirroring workflows under {@code samples/}.
 */
final class SampleWorkflowDefinitions {

    private SampleWorkflowDefinitions() {
    }

    static WorkflowDefinition minimalEcho() {
        return WorkflowBuilder.create("Minimal Echo")
                .id("minimal-echo")
                .version("1.0.0")
                .inputNode("input")
                .outputNode("output")
                .connect("input", "output")
                .metadata("description", "Smallest valid OLO workflow: passes input through to output.")
                .build();
    }

    static WorkflowDefinition stockAnalysis() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "symbol", Map.of("type", "string"),
                "question", Map.of("type", "string")));
        schema.put("required", List.of("symbol"));

        return WorkflowBuilder.create("Stock Workflow")
                .id("stock-analysis")
                .version("1.0.0")
                .addNode(NodeDefinition.builder()
                        .id("request")
                        .type(NodeType.INPUT)
                        .putConfiguration("schema", schema)
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("analysis")
                        .type(NodeType.MODEL)
                        .subtype("CHAT")
                        .putConfiguration("providerRef", "openai-default")
                        .putConfiguration(
                                "systemPrompt",
                                "You are a financial analyst. Use screener data when available.")
                        .putConfiguration("temperature", 0.2)
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("screener")
                        .type(NodeType.TOOL)
                        .putConfiguration("toolRef", "stock-screener")
                        .putConfiguration("timeoutSeconds", 30)
                        .build())
                .outputNode("response")
                .connect("request", "analysis")
                .connect("analysis", "screener")
                .connect("screener", "response")
                .variable(VariableDefinition.builder()
                        .name("symbol")
                        .type("string")
                        .required(true)
                        .description("Ticker symbol to analyze")
                        .build())
                .modelProvider(ModelProviderDefinition.builder()
                        .id("openai-default")
                        .provider("openai")
                        .model("gpt-4o")
                        .putConfiguration("apiKeyRef", "${env:OPENAI_API_KEY}")
                        .build())
                .extension(ExtensionDefinition.builder()
                        .id("stock-screener")
                        .type("TOOL")
                        .putConfiguration("implementation", "com.example.tools.StockScreener")
                        .build())
                .metadata("author", "olo-samples")
                .metadata("tags", List.of("finance", "demo"))
                .build();
    }

    static WorkflowDefinition ragChat() {
        return WorkflowBuilder.create("RAG Chat")
                .id("rag-chat")
                .version("1.0.0")
                .inputNode("input")
                .addNode(NodeDefinition.builder()
                        .id("embed")
                        .type(NodeType.MODEL)
                        .subtype("EMBEDDING")
                        .putConfiguration("providerRef", "openai-embeddings")
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("retrieve")
                        .type(NodeType.VECTOR_SEARCH)
                        .putConfiguration("extensionRef", "pgvector-store")
                        .putConfiguration("topK", 5)
                        .putConfiguration("scoreThreshold", 0.75)
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("llm1")
                        .type(NodeType.MODEL)
                        .subtype("CHAT")
                        .putConfiguration("providerRef", "openai-default")
                        .putConfiguration("systemPrompt", "Answer using only the provided context.")
                        .build())
                .outputNode("output")
                .connect("input", "embed")
                .connect("embed", "retrieve")
                .connect("retrieve", "llm1")
                .connect("llm1", "output")
                .modelProvider(ModelProviderDefinition.builder()
                        .id("openai-default")
                        .provider("openai")
                        .model("gpt-4o-mini")
                        .build())
                .modelProvider(ModelProviderDefinition.builder()
                        .id("openai-embeddings")
                        .provider("openai")
                        .model("text-embedding-3-small")
                        .build())
                .modelRouting(ModelRoutingDefinition.builder()
                        .id("default-routing")
                        .defaultProviderId("openai-default")
                        .rules(List.of(ModelRoutingDefinition.RoutingRule.builder()
                                .name("long-context")
                                .providerId("openai-default")
                                .match(Map.of("tokenEstimateGt", 8000))
                                .build()))
                        .build())
                .extension(ExtensionDefinition.builder()
                        .id("pgvector-store")
                        .type("VECTOR_STORE")
                        .putConfiguration("driver", "postgresql")
                        .putConfiguration("connectionRef", "${env:VECTOR_DB_URL}")
                        .putConfiguration("table", "documents")
                        .build())
                .metadata(
                        "description",
                        "Retrieve-augmented generation over a vector store.")
                .build();
    }

    static WorkflowDefinition analysisBase() {
        return WorkflowBuilder.create("Analysis")
                .id("analysis")
                .version("1.0.0")
                .inputNode("input")
                .addNode(NodeDefinition.builder()
                        .id("llm1")
                        .type(NodeType.MODEL)
                        .subtype("CHAT")
                        .putConfiguration("providerRef", "ollama-local")
                        .build())
                .outputNode("output")
                .connect("input", "llm1")
                .connect("llm1", "output")
                .modelProvider(ModelProviderDefinition.builder()
                        .id("ollama-local")
                        .provider("ollama")
                        .model("llama3.2")
                        .putConfiguration("baseUrl", "http://localhost:11434")
                        .build())
                .metadata(
                        "description",
                        "Base workflow before RAG extension (see workflow-extended.json).")
                .build();
    }

    static WorkflowDefinition analysisExtended() {
        return WorkflowBuilder.create("Analysis with RAG")
                .id("analysis")
                .version("1.1.0")
                .inputNode("input")
                .addNode(NodeDefinition.builder()
                        .id("rag1")
                        .type(NodeType.VECTOR_SEARCH)
                        .putConfiguration("extensionRef", "local-chroma")
                        .putConfiguration("topK", 3)
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("llm1")
                        .type(NodeType.MODEL)
                        .subtype("CHAT")
                        .putConfiguration("providerRef", "ollama-local")
                        .build())
                .outputNode("output")
                .connect("input", "rag1")
                .connect("rag1", "llm1")
                .connect("llm1", "output")
                .modelProvider(ModelProviderDefinition.builder()
                        .id("ollama-local")
                        .provider("ollama")
                        .model("llama3.2")
                        .putConfiguration("baseUrl", "http://localhost:11434")
                        .build())
                .extension(ExtensionDefinition.builder()
                        .id("local-chroma")
                        .type("VECTOR_STORE")
                        .putConfiguration("driver", "chroma")
                        .putConfiguration("collection", "docs")
                        .build())
                .metadata(
                        "description",
                        "Extended workflow: INPUT → VECTOR_SEARCH → MODEL → OUTPUT (git-branch style extension).")
                .metadata("derivedFrom", "workflow-base.json")
                .metadata("derivedFromVersion", "1.0.0")
                .build();
    }

    static WorkflowDefinition conditionBranch() {
        return WorkflowBuilder.create("Conditional Branch")
                .id("condition-branch")
                .version("1.0.0")
                .inputNode("input")
                .addNode(NodeDefinition.builder()
                        .id("router")
                        .type(NodeType.CONDITION)
                        .version("1.0.0")
                        .putConfiguration("expression", "input.intent == 'support'")
                        .addRouter(NodeRouterDefinition.builder()
                                .id("to-support")
                                .name("support")
                                .targetPort("true")
                                .targetNodeId("support-agent")
                                .match(Map.of("intent", "support"))
                                .build())
                        .addRouter(NodeRouterDefinition.builder()
                                .id("to-sales")
                                .name("sales")
                                .targetPort("false")
                                .targetNodeId("sales-agent")
                                .match(Map.of("intent", "sales"))
                                .build())
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("support-agent")
                        .type(NodeType.AGENT)
                        .version("1.0.0")
                        .putConfiguration("providerRef", "openai-default")
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("sales-agent")
                        .type(NodeType.AGENT)
                        .version("1.0.0")
                        .putConfiguration("providerRef", "openai-default")
                        .build())
                .outputNode("output")
                .connect("input", "router")
                .addEdge(EdgeDefinition.builder()
                        .sourceNodeId("router")
                        .sourcePort("true")
                        .targetNodeId("support-agent")
                        .build())
                .addEdge(EdgeDefinition.builder()
                        .sourceNodeId("router")
                        .sourcePort("false")
                        .targetNodeId("sales-agent")
                        .build())
                .connect("support-agent", "output")
                .connect("sales-agent", "output")
                .modelProvider(ModelProviderDefinition.builder()
                        .id("openai-default")
                        .provider("openai")
                        .model("gpt-4o-mini")
                        .build())
                .metadata("description", "Demonstrates port-aware edges on a CONDITION node.")
                .build();
    }
}
