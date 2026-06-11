package org.olo.core.node;

import org.olo.annotation.OloExecutionModel;
import org.olo.annotation.OloNode;
import org.olo.annotation.OloPort;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.spi.annotation.NodeType;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.node.Node;
import org.olo.spi.node.NodeRequest;
import org.olo.spi.node.NodeResult;

import java.util.LinkedHashMap;
import java.util.Map;

@OloNode(
        type = CoreNodeTypes.APPROVAL,
        name = "Approval",
        description = "Human-in-the-loop approval gate",
        featured = true,
        category = "human",
        emoji = "✋",
        tags = {"human", "approval", "core"},
        examples = {
            "Approve expense before payment",
            "Review generated content before publish",
            "Confirm destructive actions"
        },
        inputs = @OloPort(id = "in", schema = "any", required = true),
        outputs = @OloPort(id = "out", schema = "any"),
        configuration = {
            @OloProperty(
                    name = "message",
                    label = "Approval Message",
                    type = OloPropertyType.STRING,
                    description = "Approval gate user-facing message",
                    help = "Shown to the reviewer when this node runs.",
                    placeholder = "Do you approve this action?",
                    group = "General",
                    order = 0),
            @OloProperty(
                    name = "options",
                    label = "Options",
                    type = OloPropertyType.JSON,
                    description = "Structured approval choices for the gate",
                    help = "JSON array of choices presented to the reviewer.",
                    group = "General",
                    order = 1)
        },
        executionModel = OloExecutionModel.CHILD_WORKFLOW)
@NodeType(CoreNodeTypes.APPROVAL)
public final class ApprovalNode implements Node {

    @Override
    public String nodeType() {
        return CoreNodeTypes.APPROVAL;
    }

    @Override
    public NodeResult execute(NodeRequest request, ExecutionContext context) {
        String prompt = NodeConfig.string(request.configuration(), "prompt",
                NodeConfig.string(request.configuration(), "message", "Approval required"));
        context.setVariable("approvalStatus", "waiting");
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("message", prompt);
        Object options = request.configuration().get("options");
        if (options != null) {
            output.put("options", options);
        }
        return NodeResult.waiting(prompt, output);
    }
}
