package org.olo.core.node;

import org.olo.annotation.OloDesigner;
import org.olo.annotation.OloExecutionModel;
import org.olo.annotation.OloNode;
import org.olo.annotation.OloNodeShape;
import org.olo.annotation.OloPort;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloStability;
import org.olo.spi.annotation.NodeType;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.node.Node;
import org.olo.spi.node.NodeRequest;
import org.olo.spi.node.NodeResult;

import java.util.LinkedHashMap;
import java.util.Map;

@OloNode(
        type = CoreNodeTypes.AGENT,
        name = "Agent",
        description = "Autonomous agent node — dispatches to a child workflow (provider runtime pending)",
        featured = true,
        stability = OloStability.EXPERIMENTAL,
        category = "agent",
        designer = @OloDesigner(
                paletteGroup = "Agents",
                searchKeywords = {"planning", "task", "agent", "core"},
                width = 300,
                height = 120,
                canvasShape = OloNodeShape.AGENT),
        emoji = "🤖",
        tags = {"agent", "core"},
        examples = {
            "Answer customer support questions",
            "Research a topic and draft a report",
            "Plan multi-step tasks autonomously"
        },
        inputs = @OloPort(id = "in", name = "in", schema = "any", required = true),
        outputs = @OloPort(id = "out", name = "out", schema = "any"),
        configuration = @OloProperty(
                name = "systemPrompt",
                label = "System Prompt",
                type = OloPropertyType.TEXTAREA,
                description = "System instructions passed to the agent runtime",
                help = "Optional instructions that shape agent behavior.",
                placeholder = "You are a helpful assistant…",
                group = "Model Settings",
                order = 0),
        capabilityInputSchema =
                "{\"type\":\"object\",\"properties\":{\"userQuery\":{\"type\":\"string\"}},\"required\":[\"userQuery\"]}",
        capabilityOutputSchema =
                "{\"type\":\"object\",\"properties\":{\"response\":{\"type\":\"string\"}},\"required\":[\"response\"]}",
        executionModel = OloExecutionModel.CHILD_WORKFLOW,
        timeoutAware = true)
@NodeType(CoreNodeTypes.AGENT)
public final class AgentNode implements Node {

    @Override
    public String nodeType() {
        return CoreNodeTypes.AGENT;
    }

    @Override
    public NodeResult execute(NodeRequest request, ExecutionContext context) {
        String systemPrompt = NodeConfig.string(request.configuration(), "systemPrompt", "");
        String query = NodeConfig.string(request.input(), "userQuery",
                NodeConfig.string(request.input(), "text", ""));

        String message = buildStubResponse(systemPrompt, query);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("response", message);
        output.put("userQuery", query);
        if (!systemPrompt.isBlank()) {
            output.put("systemPrompt", systemPrompt);
        }
        output.put("agentMode", "child-workflow-stub");
        output.put("executionModel", "CHILD_WORKFLOW");

        context.setVariable("agentStatus", "child-workflow-stub");
        return NodeResult.completed(message, output);
    }

    private static String buildStubResponse(String systemPrompt, String query) {
        if (query.isBlank()) {
            return systemPrompt.isBlank()
                    ? "Agent awaiting user query (child workflow dispatch not yet wired)"
                    : "Agent configured with system prompt; awaiting user query for child workflow dispatch";
        }
        if (systemPrompt.isBlank()) {
            return "Agent received query (child workflow dispatch pending): " + query;
        }
        return systemPrompt.trim() + "\n\nUser: " + query + "\n\n[stub: child workflow dispatch pending]";
    }
}
