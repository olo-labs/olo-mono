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
                    },
                    capabilityInputs = {"input"},
                    capabilityOutputs = {"output"})
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
        assertThat(catalog).contains("\"id\" : \"PROMPT\"");
        assertThat(catalog).contains("\"version\" : \"1.0.0\"");
        assertThat(catalog).contains("\"provider\" : \"test\"");
        assertThat(catalog).contains("\"stability\" : \"stable\"");
        assertThat(catalog).contains("\"implementationClass\" : \"sample.PromptNode\"");
        assertThat(catalog).contains("\"moduleId\" : \"test\"");
        assertThat(catalog).contains("\"schemaVersion\" : \"1.0\"");
        assertThat(catalog).contains("\"generatedBy\" : \"olo-annotation-processor\"");
        assertThat(catalog).contains("\"generatedByVersion\" : \"1.0.0\"");
        assertThat(catalog).contains("\"catalogType\" : \"nodes\"");
        assertThat(catalog).contains("\"examples\" : [ \"Summarize a document\", \"Generate release notes\" ]");
        assertThat(catalog).contains("\"featured\" : true");
        assertThat(catalog).doesNotContain("\"deprecated\"");
        assertThat(catalog).doesNotContain("\"experimental\"");
        assertThat(catalog).contains("\"label\" : \"Prompt Template\"");
        assertThat(catalog).contains("\"label\" : \"Max Iterations\"");
        assertThat(catalog).doesNotContain("\"group\" : \"General\"");
        assertThat(catalog).contains("\"order\" : 0");
        assertThat(catalog).doesNotContain("2147483647");
        assertThat(catalog).doesNotContain("\"enumValues\"");
        assertThat(catalog).doesNotContain("\"secret\"");
        assertThat(catalog).contains("\"name\" : \"in\"");
        assertThat(catalog).contains("\"inputs\" : [ \"input\" ]");
        assertThat(catalog).contains("\"outputs\" : [ \"output\" ]");
        assertThat(catalog).doesNotContain("null");
        assertThat(catalog).doesNotContain("required_inputs");
        assertThat(catalog).doesNotContain("required_outputs");
    }
}
