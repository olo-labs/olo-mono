/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.documentsdelete;

import org.junit.jupiter.api.Test;
import org.olo.definition.configuration.scenario.ScenarioConfigurationTestSupport;
import org.olo.definition.designer.StudioDesignerAssertions;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentsDeleteConfigurationTest {

    @Test
    void onDiskPipelineMatchesDefinition() throws IOException {
        Path root = DocumentsDeletePaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertPreset(
                root, DocumentsDeleteDefinitions.PIPELINE_ID, DocumentsDeleteDefinitions.documentsDelete());
    }

    @Test
    void documentsDeletePipelineRemovesVectorDbEntryStepOnly() {
        WorkflowDefinition workflow = DocumentsDeleteDefinitions.documentsDelete();

        assertThat(workflow.getId()).isEqualTo(DocumentsDeleteDefinitions.PIPELINE_ID);
        assertThat(workflow.getQueue()).isEqualTo(DocumentsDeleteDefinitions.QUEUE);

        assertThat(workflow.getNodes()).hasSize(3);
        assertThat(workflow.getNodes().stream().map(NodeDefinition::getType))
                .containsExactlyInAnyOrder(NodeType.START.name(), NodeType.TOOL.name(), NodeType.END.name());
        assertThat(workflow.getNodes().stream().map(NodeDefinition::getType))
                .doesNotContain(NodeType.HUMAN.name(), NodeType.AGENT.name());

        assertThat(workflow.getNodes().stream().filter(DocumentsDeleteDefinitions::isRagDeleteToolNode))
                .hasSize(1);

        assertThat(workflow.getExtensions()).anyMatch(ext ->
                DocumentsDeleteDefinitions.VECTOR_STORE_EXTENSION_ID.equals(ext.getId())
                        && "VECTOR_STORE".equals(ext.getType()));

        assertThat(workflow.getTools()).anyMatch(tool ->
                DocumentsDeleteDefinitions.RAG_DELETE_TOOL_ID.equals(
                        tool.getRuntimeBinding().getImplementationId()));

        assertThat(workflow.getEdges()).hasSize(2);
        StudioDesignerAssertions.assertStudioToolPipelineBuildReady(workflow);
    }
}
