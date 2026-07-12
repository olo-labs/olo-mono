/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.logrcaanalysis;

import org.junit.jupiter.api.Test;
import org.olo.definition.designer.StudioDesignerAssertions;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LogRcaAnalysisConfigurationTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void onDiskCollectionMatchesDefinitions() throws IOException {
        Path configurationRoot = LogRcaAnalysisPaths.resolveConfigurationRoot();
        assertPreset(configurationRoot, LogRcaAnalysisDefinitions.ORCHESTRATOR_ID, LogRcaAnalysisDefinitions.orchestrator());
        assertPreset(configurationRoot, LogRcaAnalysisDefinitions.LOG_FAILURE_AGENT_ID, LogRcaAnalysisDefinitions.logFailureAgent());
        assertPreset(configurationRoot, LogRcaAnalysisDefinitions.METRICS_RCA_AGENT_ID, LogRcaAnalysisDefinitions.metricsRcaAgent());
        assertPreset(
                configurationRoot,
                LogRcaAnalysisDefinitions.CODE_CHANGE_RCA_AGENT_ID,
                LogRcaAnalysisDefinitions.codeChangeRcaAgent());
        assertPreset(
                configurationRoot,
                LogRcaAnalysisDefinitions.INCIDENT_SUMMARY_AGENT_ID,
                LogRcaAnalysisDefinitions.incidentSummaryAgent());
    }

    @Test
    void orchestratorRegistersChildAgentsAndObservabilityTools() throws IOException {
        Path configurationRoot = LogRcaAnalysisPaths.resolveConfigurationRoot();
        WorkflowDefinition orchestrator = json.deserialize(Files.readString(
                configurationRoot.resolve(LogRcaAnalysisDefinitions.ORCHESTRATOR_ID + ".json")));

        assertThat(orchestrator.getChildWorkflows()).hasSize(4);
        assertThat(orchestrator.getAvailableAgents()).hasSize(4);
        assertThat(orchestrator.getTools()).extracting(tool -> tool.getRuntimeBinding().getImplementationId())
                .contains(
                        LogRcaAnalysisDefinitions.LOG_READER_TOOL_ID,
                        LogRcaAnalysisDefinitions.CPU_USAGE_TOOL_ID,
                        LogRcaAnalysisDefinitions.RECENT_CODE_TOOL_ID);
        assertThat(orchestrator.getMetadata()).containsKey(ToolCallPlannerSupport.METADATA_DYNAMIC_TOOL_EXECUTION);
        assertThat(orchestrator.getVariables().stream().map(v -> v.getName()))
                .contains(ToolCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE);

        for (String childAgentId : new String[] {
            LogRcaAnalysisDefinitions.LOG_FAILURE_AGENT_ID,
            LogRcaAnalysisDefinitions.METRICS_RCA_AGENT_ID,
            LogRcaAnalysisDefinitions.CODE_CHANGE_RCA_AGENT_ID,
            LogRcaAnalysisDefinitions.INCIDENT_SUMMARY_AGENT_ID
        }) {
            assertThat(orchestrator.getEdges()).anyMatch(edge ->
                    childAgentId.equals(edge.getSourceNodeId())
                            && "agentPlug".equals(edge.getSourcePortId())
                            && ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID.equals(edge.getTargetNodeId())
                            && "agentPlug".equals(edge.getTargetPortId()));
            assertThat(orchestrator.getNodes().stream()
                    .filter(node -> childAgentId.equals(node.getId()))
                    .findFirst()
                    .orElseThrow()
                    .getConfiguration())
                    .containsEntry("delegateAgentId", childAgentId);
        }

        assertThat(LogRcaAnalysisDefinitions.logFailureAgent().getNodes().stream()
                .filter(node -> "agent".equals(node.getId()))
                .findFirst()
                .orElseThrow()
                .getExecutionModel())
                .isEqualTo(ExecutionModel.INLINE);

        StudioDesignerAssertions.assertStudioBuildReady(orchestrator);
    }

    @Test
    void plannerPromptGuidesFailureIdentificationRootCauseAndSummary() {
        String prompt = LogRcaAnalysisDefinitions.JSON_ONLY_PROMPT_TEMPLATE;
        assertThat(prompt).contains("Failure identification");
        assertThat(prompt).contains(LogRcaAnalysisDefinitions.LOG_FAILURE_AGENT_ID);
        assertThat(prompt).contains(LogRcaAnalysisDefinitions.METRICS_RCA_AGENT_ID);
        assertThat(prompt).contains(LogRcaAnalysisDefinitions.CODE_CHANGE_RCA_AGENT_ID);
        assertThat(prompt).contains(LogRcaAnalysisDefinitions.INCIDENT_SUMMARY_AGENT_ID);
        assertThat(prompt).contains("child workflow");
    }

    private void assertPreset(Path configurationRoot, String workflowId, WorkflowDefinition expected) throws IOException {
        Path file = configurationRoot.resolve(workflowId + ".json");
        assertThat(file).as("expected preset file %s", file).exists();
        WorkflowDefinition onDisk = json.deserialize(Files.readString(file));
        assertThat(WorkflowValidator.validate(onDisk).valid())
                .as("validation errors for %s", file)
                .isTrue();
        assertThat(onDisk).isEqualTo(expected);
    }
}
