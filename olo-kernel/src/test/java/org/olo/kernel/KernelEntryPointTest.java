/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel;

import org.junit.jupiter.api.Test;
import org.olo.bootstrap.model.CachedWorkflowDefinition;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.input.model.WorkflowInput;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class KernelEntryPointTest {
    @Test void executesFreshQueueThroughContextBuilderWithoutExternalServices() throws Exception {
        var graph = WorkflowBuilder.create("Fresh echo")
                .id("fresh-echo").queue("fresh-echo").workflowType("olo").version("1.0.0")
                .startNodeWithMessageInput("start").endNode("end").connect("start", "end").build();
        var registry = WorkflowDefinitionRegistry.of(Path.of("."),
                List.of(new CachedWorkflowDefinition("fresh-echo.json", graph)));
        var input = WorkflowInput.fromJson("""
                {"version":"1.0","inputs":[{"name":"message","displayName":"Message",
                  "type":"STRING","storage":{"mode":"LOCAL"},"value":"Hello, OLO"}],
                  "routing":{"pipeline":"fresh-echo","transactionType":"QUESTION_ANSWER",
                  "transactionId":"fresh-kernel-entry","configVersion":"1.0.0"}}
                """);
        assertThat(KernelEntryPoint.execute("fresh-echo", input, registry)).isEqualTo("Hello, OLO");
    }
}
