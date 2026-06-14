package org.olo.kernel.traversal.step.handler.impl;

import org.olo.core.runtime.ExecutionEngine;
import org.olo.core.tool.CoreToolIds;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.context.ExecutionContextFactory;
import org.olo.kernel.traversal.context.impl.VariableScopeBridge;
import org.olo.kernel.traversal.input.impl.MessageVariableInputBinder;
import org.olo.kernel.traversal.log.TraversalDiagnostics;
import org.olo.kernel.traversal.step.handler.NodeTypeHandler;
import org.olo.core.runtime.DefaultExecutionContext;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.node.NodeResult;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;
import org.olo.spi.tool.ToolStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Executes {@code TOOL} graph nodes by invoking a registered {@link org.olo.spi.tool.Tool}.
 */
public final class ToolNodeTypeHandler implements NodeTypeHandler {

    static final String CONFIG_TOOL_ID = "toolId";
    static final String CONFIG_ARGUMENTS = "arguments";

    private static final String DEMO_LOG_START = "2026-06-14T14:30:00Z";
    private static final String DEMO_LOG_END = "2026-06-14T14:31:00Z";
    private static final String DEMO_METRIC_START = "2026-06-14T14:29:00Z";
    private static final String DEMO_METRIC_END = "2026-06-14T14:32:00Z";

    private static final Map<String, String> TOOL_ID_ALIASES = Map.of(
            "vector-search", CoreToolIds.WEB_SEARCH,
            "web-search", CoreToolIds.WEB_SEARCH,
            "search", CoreToolIds.WEB_SEARCH);

    private final ExecutionEngine executionEngine;
    private final ExecutionContextFactory executionContextFactory;

    public ToolNodeTypeHandler(ExecutionEngine executionEngine, ExecutionContextFactory executionContextFactory) {
        this.executionEngine = Objects.requireNonNull(executionEngine, "executionEngine");
        this.executionContextFactory = Objects.requireNonNull(executionContextFactory, "executionContextFactory");
    }

    @Override
    public boolean supports(String nodeType) {
        return NodeType.TOOL.name().equals(nodeType);
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        String configuredToolId = readToolId(node);
        if (configuredToolId == null || configuredToolId.isBlank()) {
            return NodeResult.failed("TOOL node requires configuration.toolId", null);
        }

        String toolId = resolveToolId(configuredToolId.trim());
        Map<String, Object> arguments = buildArguments(context, node, toolId);
        Map<String, Object> toolConfiguration = readToolConfiguration(node);

        ExecutionContext executionContext = executionContextFactory.create(context, node.getId());
        ToolRequest request = new ToolRequest(toolId, node.getId(), arguments, toolConfiguration);
        TraversalDiagnostics.logNodeRequest(
                node.getId(), node.getType(), "TOOL", arguments, Map.of(CONFIG_TOOL_ID, toolId));

        try {
            ToolResult result = executionEngine.invokeTool(request, executionContext);
            if (executionContext instanceof DefaultExecutionContext defaultContext) {
                VariableScopeBridge.copyFromExecutionContext(defaultContext, context.getVariables());
            }
            if (result.status() == ToolStatus.SUCCESS) {
                Map<String, Object> output = new LinkedHashMap<>(result.output());
                output.put("toolId", toolId);
                output.put("response", formatToolResponse(result));
                return NodeResult.completed(result.message(), output);
            }
            return NodeResult.failed(
                    result.message() == null ? "tool invocation failed for " + toolId : result.message(),
                    result.error());
        } catch (RuntimeException e) {
            return NodeResult.failed("tool invocation failed for " + toolId + ": " + e.getMessage(), e);
        }
    }

    static String resolveToolId(String configuredToolId) {
        String alias = TOOL_ID_ALIASES.get(configuredToolId);
        if (alias != null) {
            return alias;
        }
        if (configuredToolId.contains(":")) {
            return configuredToolId;
        }
        return CoreToolIds.PROVIDER + ":" + configuredToolId;
    }

    private static String readToolId(NodeDefinition node) {
        if (node.getConfiguration() == null) {
            return null;
        }
        Object value = node.getConfiguration().get(CONFIG_TOOL_ID);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readToolConfiguration(NodeDefinition node) {
        if (node.getConfiguration() == null) {
            return Map.of();
        }
        Map<String, Object> configuration = new LinkedHashMap<>(node.getConfiguration());
        configuration.remove(CONFIG_TOOL_ID);
        configuration.remove(CONFIG_ARGUMENTS);
        return configuration;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildArguments(
            KernelRuntimeContext context, NodeDefinition node, String toolId) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        if (node.getConfiguration() != null) {
            Object configured = node.getConfiguration().get(CONFIG_ARGUMENTS);
            if (configured instanceof Map<?, ?> argumentMap && !argumentMap.isEmpty()) {
                arguments.putAll((Map<String, Object>) argumentMap);
            }
        }

        String message = context.getVariables().getString(MessageVariableInputBinder.MESSAGE_VARIABLE);
        if (message != null && !message.isBlank()) {
            arguments.putIfAbsent("query", message);
            arguments.putIfAbsent("userQuery", message);
            arguments.putIfAbsent("text", message);
            arguments.putIfAbsent("message", message);
        }

        enrichObservabilityArguments(toolId, arguments);
        return arguments;
    }

    static void enrichObservabilityArguments(String toolId, Map<String, Object> arguments) {
        if (toolId == null || arguments == null) {
            return;
        }
        boolean hasStart = hasNonBlank(arguments, "startTime");
        boolean hasEnd = hasNonBlank(arguments, "endTime");
        if (hasStart && hasEnd) {
            return;
        }
        if (CoreToolIds.LOG_READER.equals(toolId)) {
            arguments.putIfAbsent("startTime", DEMO_LOG_START);
            arguments.putIfAbsent("endTime", DEMO_LOG_END);
            return;
        }
        if (CoreToolIds.CPU_USAGE.equals(toolId)
                || CoreToolIds.MEMORY_USAGE.equals(toolId)
                || CoreToolIds.NUMERIC_METRIC.equals(toolId)) {
            arguments.putIfAbsent("startTime", DEMO_METRIC_START);
            arguments.putIfAbsent("endTime", DEMO_METRIC_END);
        }
    }

    private static boolean hasNonBlank(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        return value != null && !String.valueOf(value).isBlank();
    }

    private static String formatToolResponse(ToolResult result) {
        if (result.message() != null && !result.message().isBlank()) {
            return result.message();
        }
        Object results = result.output().get("results");
        if (results != null) {
            return String.valueOf(results);
        }
        return result.output().isEmpty() ? "tool completed" : String.valueOf(result.output());
    }
}
