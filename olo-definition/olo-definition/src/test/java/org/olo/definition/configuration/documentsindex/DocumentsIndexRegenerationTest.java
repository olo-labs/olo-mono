/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.documentsindex;

import org.junit.jupiter.api.Test;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentsIndexRegenerationTest {

    @Test
    void regeneratesDocumentsIndexPipeline() throws IOException {
        Path configurationRoot = DocumentsIndexPaths.resolveConfigurationRoot();
        new DocumentsIndexGenerator().generate(configurationRoot);
        Path generated = configurationRoot.resolve(DocumentsIndexDefinitions.PIPELINE_ID + ".json");
        Path currentActive = configurationRoot.getParent()
                .resolve("current-active")
                .resolve(DocumentsIndexDefinitions.PIPELINE_ID + ".json");

        assertThat(generated).exists();
        assertThat(currentActive).exists();
        assertHasNoHumanStep(generated);
        assertHasNoHumanStep(currentActive);
    }

    private static void assertHasNoHumanStep(Path workflowPath) throws IOException {
        WorkflowDefinition workflow = new JsonWorkflowSerializer().deserialize(Files.readString(workflowPath));

        assertThat(workflow.getNodes().stream().map(NodeDefinition::getType))
                .containsExactlyInAnyOrder(NodeType.START.name(), NodeType.TOOL.name(), NodeType.END.name())
                .doesNotContain(NodeType.HUMAN.name());
        assertThat(workflow.getNodes().stream().map(NodeDefinition::getApproval))
                .allMatch(java.util.Objects::isNull);
    }
}
