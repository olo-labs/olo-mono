/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.releasereadinessreview;

import org.olo.definition.configuration.scenario.ScenarioActionToolsSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioAgentSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioHumanActionSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioToolSpec;
import org.olo.definition.configuration.scenario.ScenarioPromptSupport;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/** Programmatic builders for the {@code release-readiness-review} scenario collection. */
public final class ReleaseReadinessReviewDefinitions {

    public static final String QUEUE = "oloQueue2";
    public static final String ORCHESTRATOR_ID = "release-readiness-orchestrator";

    public static final String CHANGELOG_AGENT_ID = "changelog-agent";
    public static final String REGRESSION_AGENT_ID = "regression-risk-agent";
    public static final String QA_AGENT_ID = "qa-signoff-agent";
    public static final String RELEASE_NOTES_AGENT_ID = "release-notes-agent";

    public static final String RECENT_CODE_NODE_ID = "recently-changed-code";
    public static final String RECENT_CODE_TOOL_ID = "olo-core:recently-changed-code";
    public static final String LOG_READER_NODE_ID = "log-reader";
    public static final String LOG_READER_TOOL_ID = "olo-core:log-reader";
    public static final String CALCULATOR_NODE_ID = "calculator";
    public static final String CALCULATOR_TOOL_ID = "olo-core:calculator";

    static final String JSON_ONLY_PROMPT_TEMPLATE = ScenarioPromptSupport.plannerPromptHeader(
            "release readiness review before production deployment",
            """
            1. Change inventory — use olo-core:recently-changed-code to list recent PR/diff stubs.
               Delegate to changelog-agent to summarize user-facing and risky changes.
            2. Risk analysis — scan staging logs (olo-core:log-reader) for regressions; use olo-core:calculator
               for simple risk scoring if needed. Delegate to regression-risk-agent.
            3. QA sign-off — delegate to qa-signoff-agent with test gaps and blocking issues.
            4. Release notes — delegate to release-notes-agent to publish customer-facing release notes."""
                    + ScenarioPromptSupport.humanApprovedActionStep(
                            ScenarioActionToolsSupport.GIT_REVERT_TOOL_ID,
                            "{ \"commitSha\": \"a1b2c3d\", \"branch\": \"main\" }",
                            "revert a bad release commit when rollback is approved")
                    + ScenarioPromptSupport.humanApprovedActionStep(
                            ScenarioActionToolsSupport.CREATE_PULL_REQUEST_TOOL_ID,
                            "{ \"title\": \"hotfix: payment timeout\", \"headBranch\": \"hotfix/payment-timeout\" }",
                            "open a hotfix pull request when forward fix is approved"));

    private ReleaseReadinessReviewDefinitions() {
    }

    public static WorkflowDefinition orchestrator() {
        WorkflowDefinition workflow = ScenarioPlannerSupport.orchestratorBuilder(
                        ORCHESTRATOR_ID,
                        QUEUE,
                        "Release Readiness Orchestrator",
                        "Reviews recent changes, regression risk, and QA before publishing release notes",
                        "🚀",
                        JSON_ONLY_PROMPT_TEMPLATE,
                        List.of(
                                new ScenarioAgentSpec(CHANGELOG_AGENT_ID, "Changelog Agent"),
                                new ScenarioAgentSpec(REGRESSION_AGENT_ID, "Regression Risk Agent"),
                                new ScenarioAgentSpec(QA_AGENT_ID, "QA Sign-off Agent"),
                                new ScenarioAgentSpec(RELEASE_NOTES_AGENT_ID, "Release Notes Agent")),
                        List.of(
                                recentCodeTool(),
                                logReaderTool(),
                                calculatorTool(),
                                ScenarioActionToolsSupport.gitRevertTool(),
                                ScenarioActionToolsSupport.createPullRequestTool()),
                        new ScenarioHumanActionSpec(
                                "release action",
                                "commitSha+branch for git revert OR title+headBranch for hotfix pull request"))
                .build();
        WorkflowValidator.validateOrThrow(workflow);
        return workflow;
    }

    public static WorkflowDefinition changelogAgent() {
        return childAgent(CHANGELOG_AGENT_ID, "Changelog Agent",
                "Summarizes merged changes and deployment scope", "📝", "changelog", "release");
    }

    public static WorkflowDefinition regressionRiskAgent() {
        return childAgent(REGRESSION_AGENT_ID, "Regression Risk Agent",
                "Assesses regression risk from logs and recent diffs", "⚠️", "regression", "risk");
    }

    public static WorkflowDefinition qaSignoffAgent() {
        return childAgent(QA_AGENT_ID, "QA Sign-off Agent",
                "Lists test coverage gaps and go/no-go criteria", "✅", "qa", "signoff");
    }

    public static WorkflowDefinition releaseNotesAgent() {
        return childAgent(RELEASE_NOTES_AGENT_ID, "Release Notes Agent",
                "Publishes customer-facing release notes and upgrade guidance", "📢", "release-notes", "publish");
    }

    private static WorkflowDefinition childAgent(
            String id, String name, String description, String emoji, String... keywords) {
        return ScenarioPlannerSupport.childAgentPreset(id, QUEUE, name, description, emoji, keywords);
    }

    private static ScenarioToolSpec recentCodeTool() {
        return new ScenarioToolSpec(
                RECENT_CODE_NODE_ID, RECENT_CODE_TOOL_ID, "Recently Changed Code",
                "Lists recent PR/diff stubs included in the release candidate",
                "List patches merged for payment-service this week");
    }

    private static ScenarioToolSpec logReaderTool() {
        return new ScenarioToolSpec(
                LOG_READER_NODE_ID, LOG_READER_TOOL_ID, "Log Reader",
                "Reads staging logs for regressions before release",
                "Check staging ERROR logs in the last hour before release");
    }

    private static ScenarioToolSpec calculatorTool() {
        return new ScenarioToolSpec(
                CALCULATOR_NODE_ID, CALCULATOR_TOOL_ID, "Calculator",
                "Performs numeric risk scoring calculations",
                "Compute combined risk score from severity weights");
    }
}
