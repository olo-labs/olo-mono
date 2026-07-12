/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.logrcaanalysis;

import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/** Writes the {@code log-rca-analysis} scenario collection under {@code olo-configuration/log-rca-analysis/}. */
public final class LogRcaAnalysisGenerator {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    private static final List<ScenarioEntry> ENTRIES = List.of(
            new ScenarioEntry(LogRcaAnalysisDefinitions.ORCHESTRATOR_ID, LogRcaAnalysisDefinitions::orchestrator),
            new ScenarioEntry(LogRcaAnalysisDefinitions.LOG_FAILURE_AGENT_ID, LogRcaAnalysisDefinitions::logFailureAgent),
            new ScenarioEntry(LogRcaAnalysisDefinitions.METRICS_RCA_AGENT_ID, LogRcaAnalysisDefinitions::metricsRcaAgent),
            new ScenarioEntry(
                    LogRcaAnalysisDefinitions.CODE_CHANGE_RCA_AGENT_ID, LogRcaAnalysisDefinitions::codeChangeRcaAgent),
            new ScenarioEntry(
                    LogRcaAnalysisDefinitions.INCIDENT_SUMMARY_AGENT_ID, LogRcaAnalysisDefinitions::incidentSummaryAgent));

    public static void main(String[] args) throws IOException {
        Path configurationRoot = args.length > 0
                ? Path.of(args[0])
                : LogRcaAnalysisPaths.resolveConfigurationRoot();
        new LogRcaAnalysisGenerator().generate(configurationRoot);
    }

    public void generate(Path configurationRoot) throws IOException {
        Files.createDirectories(configurationRoot);
        for (ScenarioEntry entry : ENTRIES) {
            writePreset(configurationRoot, entry.fileName(), entry.factory());
        }
    }

    private void writePreset(Path configurationRoot, String fileName, Supplier<WorkflowDefinition> factory)
            throws IOException {
        WorkflowDefinition workflow = factory.get();
        WorkflowValidator.validateOrThrow(workflow);
        Path target = configurationRoot.resolve(fileName + ".json");
        Files.writeString(target, json.serialize(workflow));
    }

    private record ScenarioEntry(String fileName, Supplier<WorkflowDefinition> factory) {
    }
}
