/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.samples;

import org.olo.definition.samples.definitions.SampleAgentWorkflows;
import org.olo.definition.samples.definitions.SampleBasicWorkflows;
import org.olo.definition.samples.definitions.SampleMultiAgentWorkflows;
import org.olo.definition.samples.definitions.SampleOrchestrationWorkflows;
import org.olo.definition.samples.definitions.SampleStockWorkflow;
import org.olo.definition.workflow.WorkflowDefinition;

/**
 * Registry facade for programmatic sample workflow builders under {@code samples/}.
 */
final class SampleWorkflowDefinitions {

    private SampleWorkflowDefinitions() {
    }

    static WorkflowDefinition minimalEcho() {
        return SampleBasicWorkflows.minimalEcho();
    }

    static WorkflowDefinition stockAnalysis() {
        return SampleStockWorkflow.stockAnalysis();
    }

    static WorkflowDefinition ragChat() {
        return SampleBasicWorkflows.ragChat();
    }

    static WorkflowDefinition analysisBase() {
        return SampleBasicWorkflows.analysisBase();
    }

    static WorkflowDefinition analysisExtended() {
        return SampleBasicWorkflows.analysisExtended();
    }

    static WorkflowDefinition conditionBranch() {
        return SampleOrchestrationWorkflows.conditionBranch();
    }

    static WorkflowDefinition humanApprovalTrade() {
        return SampleOrchestrationWorkflows.humanApprovalTrade();
    }

    static WorkflowDefinition multiAgentOrchestration() {
        return SampleMultiAgentWorkflows.multiAgentOrchestration();
    }

    static WorkflowDefinition technicalAnalysisAgent() {
        return SampleAgentWorkflows.technicalAnalysisAgent();
    }

    static WorkflowDefinition parallelAgentFanOut() {
        return SampleOrchestrationWorkflows.parallelAgentFanOut();
    }

    static WorkflowDefinition researchAgent() {
        return SampleAgentWorkflows.researchAgent();
    }
}
