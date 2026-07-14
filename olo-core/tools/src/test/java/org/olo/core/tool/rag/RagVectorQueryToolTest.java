/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.rag;

import org.junit.jupiter.api.Test;
import org.olo.core.runtime.DefaultExecutionContext;
import org.olo.spi.tool.ToolRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagVectorQueryToolTest {

    @Test
    void acceptsCapabilitySourceFromContextForPlainMessage() {
        RagVectorQueryTool tool = new RagVectorQueryTool();
        DefaultExecutionContext context = new DefaultExecutionContext("rag-chat", "run-1", "oloQueue2", "corr");
        context.setVariable("capabilitySource", "md3");

        var result = tool.invoke(
                new ToolRequest(
                        "olo-core:rag-query",
                        "rag-query",
                        Map.of("message", "What is revenue?"),
                        Map.of("extensionRef", "pgvector-store", "vectorTable", "documents")),
                context);

        assertThat(result.message()).doesNotContain("capabilitySource is required");
    }

    @Test
    void acceptsCapabilitySourceFromContextForJsonMessage() {
        RagVectorQueryTool tool = new RagVectorQueryTool();
        DefaultExecutionContext context = new DefaultExecutionContext("rag-chat", "run-1", "oloQueue2", "corr");
        context.setVariable("ragTag", "md3");

        var result = tool.invoke(
                new ToolRequest(
                        "olo-core:rag-query",
                        "rag-query",
                        Map.of("message", "{\"message\":\"What is revenue?\"}"),
                        Map.of("extensionRef", "pgvector-store", "vectorTable", "documents")),
                context);

        assertThat(result.message()).doesNotContain("capabilitySource is required");
    }
}