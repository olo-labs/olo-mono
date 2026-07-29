/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.olo.core.runtime.DefaultExecutionContext;
import org.olo.spi.tool.ToolRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagDeleteToolTest {

    @TempDir
    Path tempDir;

    @Test
    void deletesFileJsonEntriesForSourceCollection() throws Exception {
        Path uploadBase = tempDir.resolve("uploads");
        Path sourceDir = uploadBase.resolve("finance-source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("policy.txt"), "Revenue policy and standalone reporting guidance");

        Path indexDir = tempDir.resolve("index");
        Map<String, Object> extensionConfig = Map.of("driver", "file-json");
        RagVectorStoreSupport.ingestDocuments(
                uploadBase,
                indexDir,
                "finance-source",
                List.of("policy.txt"),
                512,
                extensionConfig);

        assertThat(RagVectorStoreSupport.search(
                indexDir,
                "finance-source",
                "revenue policy",
                5,
                0.1,
                extensionConfig).hits()).isEqualTo(1);

        RagVectorStoreSupport.DeleteResult deleted = RagVectorStoreSupport.deleteKnowledge(
                indexDir,
                "finance-rag",
                "finance-source",
                extensionConfig);

        assertThat(deleted.knowledgeName()).isEqualTo("finance-rag");
        assertThat(deleted.deletedSources()).containsExactly("finance-source");
        assertThat(deleted.chunksDeleted()).isEqualTo(1);
        assertThat(deleted.filesAffected()).isEqualTo(1);
        assertThat(RagVectorStoreSupport.search(
                indexDir,
                "finance-source",
                "revenue policy",
                5,
                0.1,
                extensionConfig).hits()).isZero();
    }

    @Test
    void acceptsKnowledgeNameFromJsonMessage() {
        RagDeleteTool tool = new RagDeleteTool();
        DefaultExecutionContext context = new DefaultExecutionContext("documents-delete", "run-1", "oloQueue2", "corr");

        var result = tool.invoke(
                new ToolRequest(
                        "olo-core:rag-delete",
                        "rag-delete",
                        Map.of("message", "{\"knowledgeName\":\"policy-rag\",\"sourceCollection\":\"policy-source\"}"),
                        Map.of("driver", "file-json", "connectionRef", tempDir.resolve("index").toString())),
                context);

        assertThat(result.message()).doesNotContain("knowledgeName is required");
        assertThat(result.message()).contains("Deleted RAG knowledge source policy-rag");
    }
}
