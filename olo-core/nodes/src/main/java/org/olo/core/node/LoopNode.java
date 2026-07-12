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
        type = CoreNodeTypes.LOOP,
        name = "Loop",
        description = "Iteration counter for loop constructs",
        category = "control",
        emoji = "🔁",
        tags = {"loop", "core"},
        examples = {
            "Retry until validation passes",
            "Process each item in a batch",
            "Poll status until complete"
        },
        connectionPolicy = @OloConnectionPolicy(maxInputs = 1, maxOutputs = -1),
        inputs = @OloPort(id = "in", schema = "any", required = true),
        outputs = @OloPort(id = "out", schema = "any"),
        configuration = @OloProperty(
                name = "maxIterations",
                type = OloPropertyType.NUMBER,
                defaultValue = "1"))
@NodeType(CoreNodeTypes.LOOP)
public final class LoopNode implements Node {

    private static final String ITERATION_VAR = "loopIteration";

    @Override
    public String nodeType() {
        return CoreNodeTypes.LOOP;
    }

    @Override
    public NodeResult execute(NodeRequest request, ExecutionContext context) {
        int maxIterations = NodeConfig.integer(request.configuration(), "maxIterations", 1);
        int current = context.hasVariable(ITERATION_VAR)
                ? NodeConfig.integer(context.getVariables(), ITERATION_VAR, 0)
                : 0;
        int next = current + 1;
        context.setVariable(ITERATION_VAR, next);
        boolean continueLoop = next < maxIterations;
        return NodeResult.completed(
                continueLoop ? "Loop continues" : "Loop completed",
                Map.of(
                        "iteration", next,
                        "maxIterations", maxIterations,
                        "continueLoop", continueLoop));
    }
}
