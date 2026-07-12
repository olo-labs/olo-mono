/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.samples.definitions;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.error.ErrorRoute;
import org.olo.definition.error.OnFailureDefinition;
import org.olo.definition.error.RetryPolicy;
import org.olo.definition.hook.HookActionDefinition;
import org.olo.definition.hook.HookDefinition;
import org.olo.definition.hook.NodeHooksDefinition;
import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.buildSample;
import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.nodeWithDefaultPorts;
import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.nodeWithOptionalInputPorts;

/** Finance demo workflow with model fallback, tools, hooks, and retries. */
public final class SampleStockWorkflow {

    private SampleStockWorkflow() {
    }

    public static WorkflowDefinition stockAnalysis() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "symbol", Map.of("type", "string"),
                "question", Map.of("type", "string")));
        schema.put("required", List.of("symbol"));

        return buildSample(WorkflowBuilder.create("Stock Workflow")
                .id("stock-analysis")
                .version("1.0.0")
                .capability(CapabilityDefinition.builder()
                        .name("Stock Analysis Workflow")
                        .description(
                                "Produces investment analysis by combining technical, sector, risk and news analysis.")
                        .addInput("symbol")
                        .addInput("question")
                        .addOutput("stocks")
                        .addTag("finance")
                        .addTag("demo")
                        .build())
                .tool(ToolDefinition.builder()
                        .id("screener-tool")
                        .capability(CapabilityDefinition.builder()
                                .name("Stock Screener")
                                .description(
                                        "Filters stocks based on volume, RSI, moving averages and price action.")
                                .addExample("Find breakout candidates")
                                .addExample("Find oversold stocks")
                                .build())
                        .runtimeBinding(RuntimeBindingDefinition.builder()
                                .implementationId("stock-screener")
                                .build())
                        .build())
                .addNode(nodeWithDefaultPorts("request", NodeType.START)
                        .putConfiguration("schema", schema)
                        .build())
                .addNode(nodeWithDefaultPorts("analysis", NodeType.MODEL)
                        .subtype("CHAT")
                        .putConfiguration("providerRef", "openai-default")
                        .putConfiguration(
                                "systemPrompt",
                                "You are a financial analyst. Use screener data when available.")
                        .putConfiguration("temperature", 0.2)
                        .onFailure(OnFailureDefinition.builder()
                                .retry(RetryPolicy.builder().attempts(3).build())
                                .route(ErrorRoute.builder().targetNodeId("fallback-model").build())
                                .build())
                        .hooks(NodeHooksDefinition.builder()
                                .addPre(HookActionDefinition.builder().implementationId("tracing-start").build())
                                .addOnError(HookActionDefinition.builder().implementationId("audit-error").build())
                                .build())
                        .build())
                .addNode(nodeWithOptionalInputPorts("fallback-model", NodeType.MODEL)
                        .subtype("CHAT")
                        .putConfiguration("providerRef", "ollama-fallback")
                        .putConfiguration("temperature", 0.1)
                        .build())
                .addNode(nodeWithDefaultPorts("screener", NodeType.TOOL)
                        .putConfiguration("toolRef", "screener-tool")
                        .putConfiguration("timeoutSeconds", 30)
                        .addPort(PortDefinition.outputPort("stockList", "Stock[]"))
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("response")
                        .type(NodeType.END)
                        .addPort(PortDefinition.inputPort("stocks", "Stock[]"))
                        .build())
                .connect("request", "analysis")
                .connect("analysis", "screener")
                .connect("screener", "stockList", "response", "stocks")
                .input("symbol", WorkflowInputDefinition.builder()
                        .schema("string")
                        .required(true)
                        .description("Ticker symbol to analyze")
                        .build())
                .modelProvider(ModelProviderDefinition.builder()
                        .id("openai-default")
                        .provider("openai")
                        .model("gpt-4o")
                        .putConfiguration("apiKeyRef", "${env:OPENAI_API_KEY}")
                        .build())
                .modelProvider(ModelProviderDefinition.builder()
                        .id("ollama-fallback")
                        .provider("ollama")
                        .model("llama3.2")
                        .putConfiguration("baseUrl", "http://localhost:11434")
                        .build())
                .metadata("author", "olo-samples")
                .metadata("tags", List.of("finance", "demo"))
                .hook(HookDefinition.builder()
                        .id("tracing")
                        .pattern("analysis.*")
                        .pre(HookActionDefinition.builder().implementationId("tracing-start").build())
                        .onFinally(HookActionDefinition.builder().implementationId("tracing-end").build())
                        .build())
                .hook(HookDefinition.builder()
                        .id("metrics")
                        .pattern("**")
                        .pre(HookActionDefinition.builder().implementationId("metrics-start").build())
                        .onFinally(HookActionDefinition.builder().implementationId("metrics-stop").build())
                        .build())
                .hook(HookDefinition.builder()
                        .id("audit")
                        .pattern("trading.*")
                        .onError(HookActionDefinition.builder().implementationId("audit-error").build())
                        .build()));
    }
}
