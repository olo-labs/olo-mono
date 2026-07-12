/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

class OloExtensionCatalogProcessorTest {

    @Test
    void generatesNodeCatalog() throws IOException {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "sample.PromptNode",
                """
                package sample;

                import org.olo.annotation.OloNode;
                import org.olo.annotation.OloPort;
                import org.olo.annotation.OloProperty;
                import org.olo.annotation.OloPropertyType;
                import org.olo.spi.annotation.NodeType;

                @OloNode(
                    type = "PROMPT",
                    name = "Prompt",
                    description = "Prompt node",
                    featured = true,
                    category = "llm",
                    emoji = "💬",
                    tags = {"core"},
                    examples = {
                        "Summarize a document",
                        "Generate release notes"
                    },
                    inputs = @OloPort(id = "in", schema = "any", required = true),
                    outputs = @OloPort(id = "out", schema = "any"),
                    configuration = {
                        @OloProperty(
                            name = "prompt",
                            label = "Prompt Template",
                            type = OloPropertyType.STRING,
                            description = "Template used by PromptNode",
                            help = "Use {{input}} to reference workflow input.",
                            placeholder = "Summarize the following content",
                            order = 0),
                        @OloProperty(
                            name = "maxIterations",
                            type = OloPropertyType.NUMBER)
                    })
                @NodeType("PROMPT")
                public final class PromptNode {}
                """);

        Compilation compilation = Compiler.javac()
                .withProcessors(new OloExtensionCatalogProcessor())
                .withOptions("-Aolo.catalog.module=test")
                .compile(source);

        com.google.testing.compile.CompilationSubject.assertThat(compilation).succeeded();
        String catalog = compilation
                .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/olo/catalog/nodes.json")
                .orElseThrow()
                .getCharContent(true)
                .toString();
        assertThat(catalog).contains("\"id\" : \"test:PROMPT\"");
        assertThat(catalog).contains("\"version\" : \"1.0.0\"");
        assertThat(catalog).contains("\"provider\" : \"test\"");
        assertThat(catalog).contains("\"stability\" : \"stable\"");
        assertThat(catalog).doesNotContain("implementationClass");
        assertThat(catalog).doesNotContain("spiInterface");

        String runtime = compilation
                .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/olo/catalog/runtime.json")
                .orElseThrow()
                .getCharContent(true)
                .toString();
        assertThat(runtime).contains("\"id\" : \"test:PROMPT\"");
        assertThat(runtime).contains("\"implementationClass\" : \"sample.PromptNode\"");
        assertThat(runtime).contains("\"spiInterface\" : \"org.olo.spi.node.Node\"");
        assertThat(catalog).contains("\"moduleId\" : \"test\"");
        assertThat(catalog).contains("\"schemaVersion\" : \"1.0\"");
        assertThat(catalog).contains("\"generatedBy\" : \"olo-annotation-processor\"");
        assertThat(catalog).contains("\"generatedByVersion\" : \"1.0.0\"");
        assertThat(catalog).contains("\"generatedAt\" :");
        assertThat(catalog).contains("\"catalogType\" : \"nodes\"");
        assertThat(catalog).contains("\"examples\" : [ \"Summarize a document\", \"Generate release notes\" ]");
        assertThat(catalog).contains("\"featured\" : true");
        assertThat(catalog).doesNotContain("\"deprecated\"");
        assertThat(catalog).doesNotContain("\"experimental\"");
        assertThat(catalog).contains("\"id\" : \"prompt\"");
        assertThat(catalog).contains("\"name\" : \"Prompt Template\"");
        assertThat(catalog).contains("\"id\" : \"maxIterations\"");
        assertThat(catalog).contains("\"name\" : \"Max Iterations\"");
        assertThat(catalog).contains("\"type\" : \"string\"");
        assertThat(catalog).contains("\"type\" : \"number\"");
        assertThat(catalog).contains("\"widget\" : \"STRING\"");
        assertThat(catalog).contains("\"widget\" : \"NUMBER\"");
        assertThat(catalog).contains("\"ui\" : {");
        assertThat(catalog).doesNotContain("\"group\" : \"General\"");
        assertThat(catalog).contains("\"order\" : 0");
        assertThat(catalog).doesNotContain("2147483647");
        assertThat(catalog).doesNotContain("\"enumValues\"");
        assertThat(catalog).contains("\"parameters\"");
        assertThat(catalog).doesNotContain("\"configuration\"");
        assertThat(catalog).doesNotContain("\"secret\"");
        assertThat(catalog).doesNotContain("\"type\" : \"TEXTAREA\"");
        assertThat(catalog).contains("\"name\" : \"in\"");
        assertThat(catalog).doesNotContain("\"capability\"");
        assertThat(catalog).contains("\"contractVersion\" : \"1.0\"");
        assertThat(catalog).doesNotContain("apiVersion");
        assertThat(catalog).contains("\"executionModel\" : \"INLINE\"");
        assertThat(catalog).doesNotContain("null");
        assertThat(catalog).doesNotContain("required_inputs");
        assertThat(catalog).doesNotContain("required_outputs");
    }

    @Test
    void emitsConnectionPolicyAndVisibleWhen() throws IOException {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "sample.SwitchNode",
                """
                package sample;

                import org.olo.annotation.OloConnectionPolicy;
                import org.olo.annotation.OloNode;
                import org.olo.annotation.OloPort;
                import org.olo.annotation.OloProperty;
                import org.olo.annotation.OloPropertyType;
                import org.olo.spi.annotation.NodeType;

                @OloNode(
                    type = "SWITCH",
                    name = "Switch",
                    connectionPolicy = @OloConnectionPolicy(maxInputs = 1, maxOutputs = -1),
                    inputs = @OloPort(id = "in", schema = "any", required = true),
                    outputs = @OloPort(id = "out", schema = "any"),
                    configuration = @OloProperty(
                        name = "body",
                        type = OloPropertyType.JSON,
                        visibleWhen = {"method=POST"}))
                @NodeType("SWITCH")
                public final class SwitchNode {}
                """);

        Compilation compilation = Compiler.javac()
                .withProcessors(new OloExtensionCatalogProcessor())
                .withOptions("-Aolo.catalog.module=test")
                .compile(source);

        com.google.testing.compile.CompilationSubject.assertThat(compilation).succeeded();
        String catalog = compilation
                .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/olo/catalog/nodes.json")
                .orElseThrow()
                .getCharContent(true)
                .toString();
        assertThat(catalog).contains("\"connectionPolicy\"");
        assertThat(catalog).contains("\"maxInputs\" : 1");
        assertThat(catalog).contains("\"maxOutputs\" : -1");
        assertThat(catalog).contains("\"id\" : \"body\"");
        assertThat(catalog).contains("\"name\" : \"Body\"");
        assertThat(catalog).contains("\"visibleWhen\"");
        assertThat(catalog).contains("\"method\" : \"POST\"");
    }
}
