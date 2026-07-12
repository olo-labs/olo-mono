/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.input.samples.impl;

import org.olo.input.model.CacheProvider;
import org.olo.input.model.CacheStorage;
import org.olo.input.model.Context;
import org.olo.input.model.FileStorage;
import org.olo.input.model.InputItem;
import org.olo.input.model.InputType;
import org.olo.input.model.Metadata;
import org.olo.input.model.Routing;
import org.olo.input.model.Storage;
import org.olo.input.model.StorageMode;
import org.olo.input.model.TransactionType;
import org.olo.input.model.WorkflowInput;
import org.olo.input.producer.CacheWriter;
import org.olo.input.producer.InputStorageKeys;
import org.olo.input.producer.WorkflowInputProducer;

import java.util.List;

public final class SampleWorkflowInputCoreBuilders {

    private SampleWorkflowInputCoreBuilders() {
    }

    public static WorkflowInput minimalLocal() {
        return WorkflowInput.builder()
                .version("1.0")
                .addInput(new InputItem(
                        "query",
                        "Query",
                        InputType.STRING,
                        new Storage(StorageMode.LOCAL, null, null),
                        "Hello, OLO"))
                .routing(new Routing("default-pipeline", TransactionType.QUESTION_ANSWER, SampleWorkflowInputIds.TRANSACTION_ID))
                .build();
    }

    /** LOCAL string, CACHE (Redis), and FILE reference in one payload. */
    public static WorkflowInput mixedStorage() {
        String cacheKey = InputStorageKeys.cacheKey(
                SampleWorkflowInputIds.TENANT_ID, SampleWorkflowInputIds.TRANSACTION_ID, "input2");
        return WorkflowInput.builder()
                .version("1.0")
                .addInput(new InputItem(
                        "input1",
                        "input1",
                        InputType.STRING,
                        new Storage(StorageMode.LOCAL, null, null),
                        "Hi!"))
                .addInput(new InputItem(
                        "input2",
                        "input2",
                        InputType.STRING,
                        new Storage(StorageMode.CACHE, new CacheStorage(CacheProvider.REDIS, cacheKey), null),
                        null))
                .addInput(new InputItem(
                        "input3",
                        "input3",
                        InputType.FILE,
                        new Storage(
                                StorageMode.LOCAL,
                                null,
                                new FileStorage("rag/" + SampleWorkflowInputIds.TRANSACTION_ID + "/", "readme.md")),
                        null))
                .context(new Context(
                        "",
                        "",
                        List.of("PUBLIC", "ADMIN"),
                        List.of("STORAGE", "CACHE", "S3"),
                        "<UUID>"))
                .routing(new Routing("chat-queue-ollama", TransactionType.QUESTION_ANSWER, SampleWorkflowInputIds.TRANSACTION_ID))
                .metadata(new Metadata(null, 1_771_740_578_582L))
                .build();
    }

    /** Producer API: inline small strings, offload large strings to cache. */
    public static WorkflowInput producerOffload() {
        CacheWriter cacheWriter = (key, value) -> { /* sample: no-op store */ };
        return WorkflowInputProducer.create(100, cacheWriter, SampleWorkflowInputIds.TRANSACTION_ID, "1.0")
                .context(new Context(
                        SampleWorkflowInputIds.TENANT_ID, "finance", List.of("USER"), List.of("CACHE"), "sess-producer"))
                .routing(new Routing("chat-queue", TransactionType.QUESTION_ANSWER, SampleWorkflowInputIds.TRANSACTION_ID))
                .addStringInput("prompt", "Prompt", "Analyze INFY")
                .addStringInput("context", "Context", "x".repeat(200))
                .build();
    }

    public static WorkflowInput cacheInMemory() {
        String cacheKey = InputStorageKeys.cacheKey(
                SampleWorkflowInputIds.TENANT_ID, SampleWorkflowInputIds.TRANSACTION_ID, "sessionState");
        return WorkflowInput.builder()
                .version("1.0")
                .addInput(new InputItem(
                        "sessionState",
                        "Session state",
                        InputType.JSON,
                        new Storage(StorageMode.CACHE, new CacheStorage(CacheProvider.IN_MEMORY, cacheKey), null),
                        null))
                .context(new Context(
                        SampleWorkflowInputIds.TENANT_ID, "dev", List.of("USER"), List.of("CACHE"), "sess-mem"))
                .routing(new Routing("dev-pipeline", TransactionType.QUESTION_ANSWER, SampleWorkflowInputIds.TRANSACTION_ID))
                .build();
    }
}
