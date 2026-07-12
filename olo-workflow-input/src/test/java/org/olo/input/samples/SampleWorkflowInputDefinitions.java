/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.input.samples;

import org.olo.input.model.WorkflowInput;
import org.olo.input.samples.impl.SampleWorkflowInputCoreBuilders;
import org.olo.input.samples.impl.SampleWorkflowInputScenarioBuilders;

/**
 * Programmatic builders mirroring workflow invocation payloads under {@code samples/}.
 */
public final class SampleWorkflowInputDefinitions {

    private SampleWorkflowInputDefinitions() {
    }

    public static WorkflowInput minimalLocal() {
        return SampleWorkflowInputCoreBuilders.minimalLocal();
    }

    /** LOCAL string, CACHE (Redis), and FILE reference in one payload. */
    public static WorkflowInput mixedStorage() {
        return SampleWorkflowInputCoreBuilders.mixedStorage();
    }

    /** Producer API: inline small strings, offload large strings to cache. */
    public static WorkflowInput producerOffload() {
        return SampleWorkflowInputCoreBuilders.producerOffload();
    }

    public static WorkflowInput cacheInMemory() {
        return SampleWorkflowInputCoreBuilders.cacheInMemory();
    }

    public static WorkflowInput typedInputs() {
        return SampleWorkflowInputScenarioBuilders.typedInputs();
    }

    public static WorkflowInput agentExecution() {
        return SampleWorkflowInputScenarioBuilders.agentExecution();
    }

    public static WorkflowInput workflowRun() {
        return SampleWorkflowInputScenarioBuilders.workflowRun();
    }

    /** Remote storage modes supported by the schema (value resolved at runtime). */
    public static WorkflowInput storageRemote() {
        return SampleWorkflowInputScenarioBuilders.storageRemote();
    }

    public static WorkflowInput ragMetadata() {
        return SampleWorkflowInputScenarioBuilders.ragMetadata();
    }
}
