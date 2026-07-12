/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.scenario.impl;

import org.olo.definition.human.HumanApprovalDefinition;

import java.util.List;

/** Shared human-in-the-loop intake step for scenario and preset workflows. */
public final class ScenarioHumanStepSupport {

    public static final String HUMAN_INPUT_NODE_ID = "human-input";
    public static final String DEFAULT_APPROVER = "operator";
    private static final long DEFAULT_TIMEOUT_SECONDS = 3600L;

    public static final String SCENARIO_SCOPE_PLUGIN_ID = "olo-core:human-input-scenario-scope";
    public static final String RESTART_CONTAINER_PLUGIN_ID = "olo-core:human-input-restart-container";
    public static final String BOOK_TICKET_PLUGIN_ID = "olo-core:human-input-book-ticket";
    public static final String CREATE_PULL_REQUEST_PLUGIN_ID = "olo-core:human-input-create-pull-request";
    public static final String GIT_REVERT_PLUGIN_ID = "olo-core:human-input-git-revert";

    private ScenarioHumanStepSupport() {
    }

    public static HumanApprovalDefinition intakeForScenario(String scenarioName, String shortDescription) {
        return intakeForScenario(scenarioName, shortDescription, null, null);
    }

    public static HumanApprovalDefinition intakeForScenario(
            String scenarioName, String shortDescription, String actionLabel, String requiredInputs) {
        return intakeForScenario(scenarioName, shortDescription, actionLabel, requiredInputs, null);
    }

    public static HumanApprovalDefinition intakeForScenario(
            String scenarioName,
            String shortDescription,
            String actionLabel,
            String requiredInputs,
            String inputPluginId) {
        String description =
                "Review the request and add any missing context, constraints, or priorities before "
                        + scenarioName
                        + " runs. "
                        + shortDescription;
        if (actionLabel != null && requiredInputs != null) {
            description +=
                    " For the final human-approved action (" + actionLabel + "), provide: " + requiredInputs + ".";
        }
        String resolvedPluginId = inputPluginId != null
                ? inputPluginId
                : resolveInputPluginId(actionLabel);
        return HumanApprovalDefinition.builder()
                .title(actionLabel == null ? "Confirm scope: " + scenarioName : "Approve " + actionLabel)
                .description(description)
                .inputPluginId(resolvedPluginId)
                .approvers(List.of(DEFAULT_APPROVER))
                .timeoutSeconds(DEFAULT_TIMEOUT_SECONDS)
                .build();
    }

    public static HumanApprovalDefinition agentToolIntake() {
        return HumanApprovalDefinition.builder()
                .title("Approve agent task and recovery action")
                .description(
                        "Review the user request and add clarifications (time window, parameters, or constraints) "
                                + "before the tool-using agent plans and executes.")
                .inputPluginId(RESTART_CONTAINER_PLUGIN_ID)
                .approvers(List.of(DEFAULT_APPROVER))
                .timeoutSeconds(DEFAULT_TIMEOUT_SECONDS)
                .build();
    }

    public static HumanApprovalDefinition dynamicGraphIntake() {
        return HumanApprovalDefinition.builder()
                .title("Confirm workflow requirements and publish action")
                .description(
                        "Review the graph-generation request and add nodes, tools, or constraints the generated "
                                + "workflow should include before planning begins.")
                .inputPluginId(CREATE_PULL_REQUEST_PLUGIN_ID)
                .approvers(List.of(DEFAULT_APPROVER))
                .timeoutSeconds(DEFAULT_TIMEOUT_SECONDS)
                .build();
    }

    public static String resolveInputPluginId(String actionLabel) {
        if (actionLabel == null || actionLabel.isBlank()) {
            return SCENARIO_SCOPE_PLUGIN_ID;
        }
        String normalized = actionLabel.toLowerCase();
        if (normalized.contains("restart") || normalized.contains("container")) {
            return RESTART_CONTAINER_PLUGIN_ID;
        }
        if (normalized.contains("ticket") || normalized.contains("book")) {
            return BOOK_TICKET_PLUGIN_ID;
        }
        if (normalized.contains("pull request") || normalized.contains("publish")) {
            return CREATE_PULL_REQUEST_PLUGIN_ID;
        }
        if (normalized.contains("revert") || normalized.contains("git")) {
            return GIT_REVERT_PLUGIN_ID;
        }
        return SCENARIO_SCOPE_PLUGIN_ID;
    }
}
