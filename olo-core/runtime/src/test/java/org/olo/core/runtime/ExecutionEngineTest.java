/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.runtime;

import org.junit.jupiter.api.Test;
import org.olo.core.hook.CoreHookIds;
import org.olo.core.node.CoreNodeTypes;
import org.olo.core.tool.CoreToolIds;
import org.olo.spi.hook.HookPhase;
import org.olo.spi.hook.HookRequest;
import org.olo.spi.node.NodeRequest;
import org.olo.spi.node.NodeStatus;
import org.olo.spi.tool.ToolRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionEngineTest {

    @Test
    void executesBuiltInNodeToolAndHook() {
        ExecutionEngine engine = ExecutionEngine.withDefaults();
        DefaultExecutionContext context = new DefaultExecutionContext("wf-1", "run-1", "agent", "corr-1");

        var nodeResult = engine.executeNode(
                new NodeRequest("n1", CoreNodeTypes.PROMPT, Map.of("userQuery", "hello"), Map.of("prompt", "Q: {{input}}")),
                context);
        assertThat(nodeResult.status()).isEqualTo(NodeStatus.COMPLETED);
        assertThat(nodeResult.output()).containsEntry("content", "Q: hello");

        var toolResult = engine.invokeTool(
                new ToolRequest(CoreToolIds.CALCULATOR, null, Map.of("a", 2, "b", 3, "op", "+"), Map.of()),
                context);
        assertThat(toolResult.output()).containsEntry("result", 5.0);

        var hookResult = engine.runHook(
                new HookRequest(HookPhase.PRE, CoreHookIds.LOGGING, "n1", CoreNodeTypes.PROMPT, null, Map.of()),
                context);
        assertThat(hookResult.status().name()).isEqualTo("OK");
    }

    @Test
    void approvalNodeWaits() {
        ExecutionEngine engine = ExecutionEngine.withDefaults();
        DefaultExecutionContext context = new DefaultExecutionContext("wf-1", "run-1", "agent", null);

        var result = engine.executeNode(
                new NodeRequest("approve", CoreNodeTypes.APPROVAL, Map.of(), Map.of("message", "Confirm trade")),
                context);

        assertThat(result.status()).isEqualTo(NodeStatus.WAITING);
        assertThat(context.getVariable("approvalStatus")).isEqualTo("waiting");
    }
}
