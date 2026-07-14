/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.documentsindex;

import org.junit.jupiter.api.Test;
import org.olo.definition.configuration.scenario.ScenarioConfigurationTestSupport;
import org.olo.definition.designer.StudioDesignerAssertions;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentsIndexConfigurationTest {

    @Test
    void onDiskPipelineMatchesDefinition() throws IOException {
        Path root = DocumentsIndexPaths.resolveConfigurationRoot();
        ScenarioConfigurationTestSupport.assertPreset(
                root, DocumentsIndexDefinitions.PIPELINE_ID, DocumentsIndexDefinitions.documentsIndex());
    }

    @Test
    void documentsIndexPipelineCreatesVectorDbEntryStepOnly() {
        WorkflowDefinition workflow = DocumentsIndexDefinitions.documentsIndex();

        assertThat(workflow.getId()).isEqualTo(DocumentsIndexDefinitions.PIPELINE_ID);
        assertThat(workflow.getQueue()).isEqualTo(DocumentsIndexDefinitions.QUEUE);

        assertThat(workflow.getNodes()).hasSize(3);
        assertThat(workflow.getNodes().stream().map(NodeDefinition::getType))
                .containsExactlyInAnyOrder(NodeType.START.name(), NodeType.TOOL.name(), NodeType.END.name());
        assertThat(workflow.getNodes().stream().map(NodeDefinition::getType))
                .doesNotContain(NodeType.HUMAN.name(), NodeType.AGENT.name());

        assertThat(workflow.getNodes().stream().filter(DocumentsIndexDefinitions::isRagIngestToolNode))
                .hasSize(1);

        assertThat(workflow.getExtensions()).anyMatch(ext ->
                DocumentsIndexDefinitions.VECTOR_STORE_EXTENSION_ID.equals(ext.getId())
                        && "VECTOR_STORE".equals(ext.getType()));

        assertThat(workflow.getTools()).anyMatch(tool ->
                DocumentsIndexDefinitions.RAG_INGEST_TOOL_ID.equals(
                        tool.getRuntimeBinding().getImplementationId()));

        assertThat(workflow.getEdges()).hasSize(2);
        StudioDesignerAssertions.assertStudioToolPipelineBuildReady(workflow);
    }
}
