package org.olo.spi;

import org.junit.jupiter.api.Test;
import org.olo.spi.hook.HookPhase;
import org.olo.spi.hook.HookRequest;
import org.olo.spi.hook.HookResult;
import org.olo.spi.node.NodeRequest;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;
import org.olo.spi.tool.ToolStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpiContractsTest {

    @Test
    void nodeResultFactories() {
        NodeResult result = NodeResult.completed("done", Map.of("key", "value"));
        assertThat(result.status()).isEqualTo(NodeStatus.COMPLETED);
        assertThat(result.message()).isEqualTo("done");
        assertThat(result.output()).containsEntry("key", "value");
    }

    @Test
    void toolResultFactories() {
        ToolResult result = ToolResult.success(Map.of("answer", 42));
        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(result.output()).containsEntry("answer", 42);
    }

    @Test
    void hookRequestAndResult() {
        HookRequest request = new HookRequest(
                HookPhase.PRE,
                "tracing-start",
                "analyze",
                "MODEL",
                null,
                Map.of("trace", true));
        HookResult result = HookResult.ok(Map.of("spanId", "abc"));

        assertThat(request.phase()).isEqualTo(HookPhase.PRE);
        assertThat(request.hookId()).isEqualTo("tracing-start");
        assertThat(result.status().name()).isEqualTo("OK");
    }

    @Test
    void requestModelsDefensiveCopy() {
        NodeRequest request = new NodeRequest("n1", "MODEL", Map.of("in", 1), Map.of());
        ToolRequest toolRequest = new ToolRequest("stock-screener", null, Map.of(), Map.of());

        assertThat(request.nodeId()).isEqualTo("n1");
        assertThat(toolRequest.toolId()).isEqualTo("stock-screener");
        assertThat(toolRequest.invocationId()).isEqualTo("stock-screener");
    }
}
