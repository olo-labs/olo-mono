/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.input.samples.impl;

import org.olo.input.model.Context;
import org.olo.input.model.Execution;
import org.olo.input.model.InputItem;
import org.olo.input.model.InputType;
import org.olo.input.model.Metadata;
import org.olo.input.model.Routing;
import org.olo.input.model.Storage;
import org.olo.input.model.StorageMode;
import org.olo.input.model.TransactionType;
import org.olo.input.model.WorkflowInput;

import java.util.List;

public final class SampleWorkflowInputScenarioBuilders {

    private SampleWorkflowInputScenarioBuilders() {
    }

    public static WorkflowInput typedInputs() {
        return WorkflowInput.builder()
                .version("1.0")
                .addInput(new InputItem(
                        "symbol",
                        "Symbol",
                        InputType.STRING,
                        new Storage(StorageMode.LOCAL, null, null),
                        "INFY"))
                .addInput(new InputItem(
                        "quantity",
                        "Quantity",
                        InputType.NUMBER,
                        new Storage(StorageMode.LOCAL, null, null),
                        "100"))
                .addInput(new InputItem(
                        "dryRun",
                        "Dry run",
                        InputType.BOOLEAN,
                        new Storage(StorageMode.LOCAL, null, null),
                        "true"))
                .addInput(new InputItem(
                        "filters",
                        "Filters",
                        InputType.JSON,
                        new Storage(StorageMode.LOCAL, null, null),
                        "{\"sector\":\"IT\",\"minVolume\":1000000}"))
                .addInput(new InputItem(
                        "profile",
                        "Profile",
                        InputType.OBJECT,
                        new Storage(StorageMode.LOCAL, null, null),
                        "{\"risk\":\"moderate\",\"horizon\":\"long\"}"))
                .routing(new Routing("stock-analysis", TransactionType.WORKFLOW_RUN, SampleWorkflowInputIds.TRANSACTION_ID))
                .build();
    }

    public static WorkflowInput agentExecution() {
        return WorkflowInput.builder()
                .version("1.0")
                .addInput(new InputItem(
                        "task",
                        "Task",
                        InputType.STRING,
                        new Storage(StorageMode.LOCAL, null, null),
                        "Summarize the latest earnings call"))
                .context(new Context(
                        SampleWorkflowInputIds.TENANT_ID,
                        "ops",
                        List.of("ADMIN"),
                        List.of("STORAGE", "CACHE"),
                        "sess-agent",
                        "run-001",
                        null,
                        "corr-xyz"))
                .execution(new Execution("https://api.example.com/callback", 300))
                .routing(new Routing("agent-pipeline", TransactionType.AGENT_EXECUTION, SampleWorkflowInputIds.TRANSACTION_ID))
                .build();
    }

    public static WorkflowInput workflowRun() {
        return WorkflowInput.builder()
                .version("1.0")
                .addInput(new InputItem(
                        "symbol",
                        "Symbol",
                        InputType.STRING,
                        new Storage(StorageMode.LOCAL, null, null),
                        "AAPL"))
                .addInput(new InputItem(
                        "question",
                        "Question",
                        InputType.STRING,
                        new Storage(StorageMode.LOCAL, null, null),
                        "What is the short-term outlook?"))
                .context(new Context(
                        SampleWorkflowInputIds.TENANT_ID, "research", List.of("ANALYST"), List.of("STORAGE"), "sess-wf"))
                .execution(new Execution(null, 300))
                .routing(new Routing(
                        "stock-analysis",
                        TransactionType.WORKFLOW_RUN,
                        SampleWorkflowInputIds.TRANSACTION_ID,
                        "1.0.0"))
                .build();
    }

    /** Remote storage modes supported by the schema (value resolved at runtime). */
    public static WorkflowInput storageRemote() {
        return WorkflowInput.builder()
                .version("1.0")
                .addInput(new InputItem(
                        "report",
                        "Report",
                        InputType.FILE,
                        new Storage(StorageMode.S3, null, null),
                        null))
                .addInput(new InputItem(
                        "auditId",
                        "Audit id",
                        InputType.STRING,
                        new Storage(StorageMode.DB, null, null),
                        null))
                .routing(new Routing("compliance-pipeline", TransactionType.WORKFLOW_RUN, SampleWorkflowInputIds.TRANSACTION_ID))
                .build();
    }

    public static WorkflowInput ragMetadata() {
        return WorkflowInput.builder()
                .version("1.0")
                .addInput(new InputItem(
                        "question",
                        "Question",
                        InputType.STRING,
                        new Storage(StorageMode.LOCAL, null, null),
                        "Explain the dividend policy"))
                .context(new Context(
                        SampleWorkflowInputIds.TENANT_ID, "finance", List.of("USER"), List.of("STORAGE"), "sess-rag"))
                .routing(new Routing("rag-chat", TransactionType.QUESTION_ANSWER, SampleWorkflowInputIds.TRANSACTION_ID))
                .metadata(new Metadata("finance-rag-v2", 1_771_740_578_582L))
                .build();
    }
}
