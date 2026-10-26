/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.definition.fresh;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.olo.definition.serializer.*;
import org.olo.definition.validation.WorkflowValidator;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FreshWorkflowsTest {
    @TempDir Path output;

    @Test void everyNewDefinitionValidatesAndRoundTripsThroughTheStrictBoundary() throws Exception {
        var serializer=new JsonWorkflowSerializer();
        for(var entry:FreshWorkflows.installation().entrySet()) {
            var definition=entry.getValue();
            WorkflowValidator.validateOrThrow(definition);
            String json=serializer.serialize(definition);
            FreshWorkflowContract.validate(serializer.getMapper().readTree(json));
            assertEquals(definition,serializer.deserialize(json),entry.getKey());
            var yaml=new YamlWorkflowSerializer();
            assertEquals(definition,yaml.deserialize(yaml.serialize(definition)),entry.getKey());
        }
    }

    @Test void memoryAndRagHaveNoGraphNodesOrAgentEntitlement() {
        var definition=FreshWorkflows.chat("agent");
        assertEquals(java.util.List.of("START","AGENT","END"),
                definition.getNodes().stream().map(node->node.getType()).toList());
        assertEquals(0,definition.getNodes().get(1).getConfiguration().get("maxToolRounds"));
        assertFalse(definition.getNodes().get(1).getConfiguration().containsKey("agentName"));
    }

    @Test void commanderOnlyDelegatesAndChildrenUseExplicitMappedBusinessActors() {
        var definition=FreshWorkflows.incident();
        assertEquals(3,definition.getChildWorkflows().size());
        var commander=definition.getNodes().stream().filter(node->node.getId().equals("commander")).findFirst().orElseThrow();
        assertEquals(true,commander.getConfiguration().get("agentCallPlanner"));
        assertEquals(0,commander.getConfiguration().get("maxToolRounds"));
        assertFalse(commander.getConfiguration().containsKey("agentName"));
        for(var actor:FreshWorkflows.INCIDENT_AGENTS) {
            var child=FreshWorkflows.incidentChild(actor);
            Map<String,Object> config=child.getNodes().get(1).getConfiguration();
            assertEquals(actor,config.get("agentName"));
            assertEquals(5,config.get("maxToolRounds"));
        }
    }

    @Test void generatorRefusesToConvertOrOverwriteAnInstalledWorkflow() throws Exception {
        FreshWorkflowGenerator.main(new String[]{output.toString()});
        assertTrue(Files.isRegularFile(output.resolve("current-active/agent.json")));
        assertThrows(IllegalArgumentException.class,()->FreshWorkflowGenerator.main(new String[]{output.toString()}));
    }
}
