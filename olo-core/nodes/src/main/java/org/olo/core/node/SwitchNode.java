/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.node;

import org.olo.annotation.OloConnectionPolicy;
import org.olo.annotation.OloNode;
import org.olo.annotation.OloPort;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.spi.annotation.NodeType;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.node.Node;
import org.olo.spi.node.NodeRequest;
import org.olo.spi.node.NodeResult;

import java.util.Map;

@OloNode(
        type = CoreNodeTypes.SWITCH,
        name = "Switch",
        description = "Conditional branch selection",
        category = "control",
        emoji = "🔀",
        tags = {"branch", "core"},
        examples = {
            "Route by user intent",
            "Branch on API response status",
            "Select workflow path by category"
        },
        connectionPolicy = @OloConnectionPolicy(maxInputs = 1, maxOutputs = -1),
        inputs = @OloPort(id = "in", schema = "any", required = true),
        outputs = @OloPort(id = "out", schema = "any"),
        configuration = @OloProperty(
                name = "defaultBranch",
                type = OloPropertyType.STRING,
                defaultValue = "default"))
@NodeType(CoreNodeTypes.SWITCH)
public final class SwitchNode implements Node {

    @Override
    public String nodeType() {
        return CoreNodeTypes.SWITCH;
    }

    @Override
    public NodeResult execute(NodeRequest request, ExecutionContext context) {
        String branch = NodeConfig.string(request.input(), "branch",
                NodeConfig.string(request.configuration(), "defaultBranch", "default"));
        context.setVariable("selectedBranch", branch);
        return NodeResult.completed("Branch selected: " + branch, Map.of("branch", branch));
    }
}
