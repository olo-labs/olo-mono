package org.olo.core.node;

import org.olo.annotation.OloNode;
import org.olo.annotation.OloPort;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloStability;
import org.olo.spi.annotation.NodeType;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.node.Node;
import org.olo.spi.node.NodeRequest;
import org.olo.spi.node.NodeResult;

import java.util.Map;

@OloNode(
        type = CoreNodeTypes.AGENT,
        name = "Agent",
        description = "Autonomous agent node (stub until provider extensions are wired)",
        featured = true,
        stability = OloStability.EXPERIMENTAL,
        category = "agent",
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
        capabilityInputs = {"userQuery"},
        capabilityOutputs = {"response"})
@NodeType(CoreNodeTypes.AGENT)
public final class AgentNode implements Node {

    @Override
    public String nodeType() {
        return CoreNodeTypes.AGENT;
    }

    @Override
    public NodeResult execute(NodeRequest request, ExecutionContext context) {
        String query = NodeConfig.string(request.input(), "userQuery",
                NodeConfig.string(request.input(), "text", ""));
        String message = query.isBlank()
                ? "Agent node executed (no user query in input)"
                : "Agent node received query: " + query;
        context.setVariable("agentStatus", "stub");
        return NodeResult.completed(message, Map.of("response", message, "agentMode", "stub"));
    }
}
