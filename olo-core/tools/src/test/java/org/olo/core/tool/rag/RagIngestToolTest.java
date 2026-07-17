/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.rag;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.olo.core.runtime.DefaultExecutionContext;
import org.olo.spi.tool.ToolRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagIngestToolTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsCapabilitySourceFromRagTagArgument() {
        RagIngestTool tool = new RagIngestTool();
        DefaultExecutionContext context = new DefaultExecutionContext("documents-index", "run-1", "oloQueue2", "corr");

        var result = tool.invoke(
                new ToolRequest(
                        "olo-core:rag-ingest",
                        "rag-ingest",
                        Map.of("ragTag", "finance-rag"),
                        Map.of("extensionRef", "pgvector-store", "vectorTable", "documents")),
                context);

        assertThat(result.message()).doesNotContain("capabilitySource is required");
    }

    @Test
    void mergesCapabilitySourceFromJsonMessage() {
        RagIngestTool tool = new RagIngestTool();
        DefaultExecutionContext context = new DefaultExecutionContext("documents-index", "run-1", "oloQueue2", "corr");

        var result = tool.invoke(
                new ToolRequest(
                        "olo-core:rag-ingest",
                        "rag-ingest",
                        Map.of("message", "{\"capabilitySource\":\"policy-rag\",\"fileNames\":[\"a.txt\"]}"),
                        Map.of("extensionRef", "pgvector-store", "vectorTable", "documents")),
                context);

        assertThat(result.message()).doesNotContain("capabilitySource is required");
    }

    @Test
    void mergesCapabilitySourceFromStructuredMessage() {
        RagIngestTool tool = new RagIngestTool();
        DefaultExecutionContext context = new DefaultExecutionContext("documents-index", "run-1", "oloQueue2", "corr");

        var result = tool.invoke(
                new ToolRequest(
                        "olo-core:rag-ingest",
                        "rag-ingest",
                        Map.of("message", Map.of(
                                "capabilitySource", "policy-rag",
                                "fileNames", List.of("a.txt"))),
                        Map.of("extensionRef", "pgvector-store", "vectorTable", "documents")),
                context);

        assertThat(result.message()).doesNotContain("capabilitySource is required");
    }

    @Test
    void indexesPdfUploads() throws Exception {
        Path uploadBase = tempDir.resolve("uploads");
        Path sourceDir = uploadBase.resolve("finance-rag");
        Files.createDirectories(sourceDir);
        writePdf(sourceDir.resolve("report.pdf"), "Consolidated standalone revenue");

        var result = RagVectorStoreSupport.ingestDocuments(
                uploadBase,
                tempDir.resolve("index"),
                "finance-rag",
                List.of("report.pdf"),
                512,
                Map.of("driver", "file-json"));

        assertThat(result.filesProcessed()).isEqualTo(1);
        assertThat(result.chunksIndexed()).isEqualTo(1);
        assertThat(result.fileResults()).anySatisfy(fileResult -> assertThat(fileResult)
                .containsEntry("fileName", "report.pdf")
                .containsEntry("status", "INDEXED")
                .containsEntry("chunksIndexed", 1));
    }

    @Test
    void skipsUnreadableFilesWithoutFailingIngest() throws Exception {
        Path uploadBase = tempDir.resolve("uploads");
        Path sourceDir = uploadBase.resolve("finance-rag");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("notes.txt"), "hello policy world");
        Files.write(sourceDir.resolve("bad.bin"), new byte[] {'%', 'P', 'D', 'F', (byte) 0xF0, (byte) 0x9F, (byte) 0x92});

        var result = RagVectorStoreSupport.ingestDocuments(
                uploadBase,
                tempDir.resolve("index"),
                "finance-rag",
                List.of("notes.txt", "bad.bin"),
                512,
                Map.of("driver", "file-json"));

        assertThat(result.filesProcessed()).isEqualTo(1);
        assertThat(result.chunksIndexed()).isEqualTo(1);
        assertThat(result.fileResults()).anySatisfy(fileResult -> assertThat(fileResult)
                .containsEntry("fileName", "bad.bin")
                .containsEntry("status", "SKIPPED")
                .containsEntry("reason", "file is not valid UTF-8 text"));
    }

    private static void writePdf(Path path, String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText(text);
                content.endText();
            }
            document.save(path.toFile());
        }
    }
}
