package org.olo.core.node;

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
        type = CoreNodeTypes.PROMPT,
        name = "Prompt",
        description = "Template prompt assembly without external LLM call",
        featured = true,
        category = "llm",
        emoji = "💬",
        tags = {"prompt", "core"},
        examples = {
            "Summarize a document",
            "Generate release notes",
            "Translate text"
        },
        inputs = @OloPort(id = "in", name = "in", schema = "any", required = true),
        outputs = @OloPort(id = "out", name = "out", schema = "string"),
        configuration = @OloProperty(
                name = "prompt",
                label = "Prompt Template",
                type = OloPropertyType.TEXTAREA,
                description = "Template used by PromptNode",
                help = "Use {{input}} to reference workflow input.",
                placeholder = "Summarize the following content",
                group = "General",
                order = 0,
                examples = {"Summarize document", "Generate email"}))
@NodeType(CoreNodeTypes.PROMPT)
public final class PromptNode implements Node {

    @Override
    public String nodeType() {
        return CoreNodeTypes.PROMPT;
    }

    @Override
    public NodeResult execute(NodeRequest request, ExecutionContext context) {
        String template = NodeConfig.string(request.configuration(), "prompt", "");
        String userText = NodeConfig.string(request.input(), "userQuery",
                NodeConfig.string(request.input(), "text", ""));
        String rendered = template.isBlank() ? userText : template.replace("{{input}}", userText);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("prompt", rendered);
        output.put("content", rendered);
        context.setVariable("lastPrompt", rendered);
        return NodeResult.completed("Prompt rendered", output);
    }
}
