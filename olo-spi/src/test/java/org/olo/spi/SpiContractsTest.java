/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi;

import org.junit.jupiter.api.Test;
import org.olo.spi.hook.HookPhase;
import org.olo.spi.hook.HookRequest;
import org.olo.spi.hook.HookResult;
import org.olo.spi.node.NodeRequest;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;

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

        assertThat(request.nodeId()).isEqualTo("n1");
    }
}
