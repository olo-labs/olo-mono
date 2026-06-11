package org.olo.annotation.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;

class CatalogValidationTest {

    @Test
    void failsOnNodeTypeMismatch() {
        assertFails(
                "PromptNode",
                """
                package sample;

                import org.olo.annotation.OloNode;
                import org.olo.spi.annotation.NodeType;

                @OloNode(type = "PROMPT", name = "Prompt")
                @NodeType("PROMPT_V2")
                public final class PromptNode {}
                """,
                "OLO-AP-001");
    }

    @Test
    void failsOnMissingNodeType() {
        assertFails(
                "PromptNode",
                """
                package sample;

                import org.olo.annotation.OloNode;

                @OloNode(type = "PROMPT", name = "Prompt")
                public final class PromptNode {}
                """,
                "OLO-AP-001");
    }

    @Test
    void failsOnDuplicateNodeType() {
        Compilation compilation =
                compile(
                        nodeSource("FirstNode", "PROMPT"),
                        nodeSource("SecondNode", "PROMPT"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("OLO-AP-004");
    }

    @Test
    void failsOnDuplicatePropertyName() {
        assertFails(
                "PromptNode",
                """
                package sample;

                import org.olo.annotation.OloNode;
                import org.olo.annotation.OloProperty;
                import org.olo.spi.annotation.NodeType;

                @OloNode(
                    type = "PROMPT",
                    name = "Prompt",
                    configuration = {
                        @OloProperty(name = "prompt"),
                        @OloProperty(name = "prompt")
                    })
                @NodeType("PROMPT")
                public final class PromptNode {}
                """,
                "OLO-AP-007");
    }

    @Test
    void failsOnDuplicatePortId() {
        assertFails(
                "PromptNode",
                """
                package sample;

                import org.olo.annotation.OloNode;
                import org.olo.annotation.OloPort;
                import org.olo.spi.annotation.NodeType;

                @OloNode(
                    type = "PROMPT",
                    name = "Prompt",
                    inputs = {
                        @OloPort(id = "in", schema = "any"),
                        @OloPort(id = "in", schema = "any")
                    })
                @NodeType("PROMPT")
                public final class PromptNode {}
                """,
                "OLO-AP-008");
    }

    @Test
    void failsOnEnumWithoutValues() {
        assertFails(
                "PromptNode",
                """
                package sample;

                import org.olo.annotation.OloNode;
                import org.olo.annotation.OloProperty;
                import org.olo.annotation.OloPropertyType;
                import org.olo.spi.annotation.NodeType;

                @OloNode(
                    type = "PROMPT",
                    name = "Prompt",
                    configuration = @OloProperty(name = "mode", type = OloPropertyType.ENUM))
                @NodeType("PROMPT")
                public final class PromptNode {}
                """,
                "OLO-AP-009");
    }

    @Test
    void failsOnToolIdMismatch() {
        assertFails(
                "HttpTool",
                """
                package sample;

                import org.olo.annotation.OloTool;
                import org.olo.spi.annotation.ToolId;

                @OloTool(id = "http-tool", name = "HTTP")
                @ToolId("http-v2")
                public final class HttpTool {}
                """,
                "OLO-AP-002");
    }

    @Test
    void failsOnInvalidDefaultTimeout() {
        assertFails(
                "BadTimeoutTool",
                """
                package sample;

                import org.olo.annotation.OloTool;
                import org.olo.spi.annotation.ToolId;

                @OloTool(
                    id = "bad-timeout",
                    name = "Bad",
                    defaultTimeout = "30s")
                @ToolId("bad-timeout")
                public final class BadTimeoutTool {}
                """,
                "OLO-AP-013");
    }

    @Test
    void failsOnInvalidCapabilitySchema() {
        assertFails(
                "BadSchemaTool",
                """
                package sample;

                import org.olo.annotation.OloTool;
                import org.olo.spi.annotation.ToolId;

                @OloTool(
                    id = "bad-schema",
                    name = "Bad",
                    capabilityInputSchema = "not-json")
                @ToolId("bad-schema")
                public final class BadSchemaTool {}
                """,
                "OLO-AP-012");
    }

    @Test
    void failsOnHookIdMismatch() {
        assertFails(
                "LoggingHook",
                """
                package sample;

                import org.olo.annotation.OloHook;
                import org.olo.spi.annotation.ImplementationId;

                @OloHook(implementationId = "logging-hook", name = "Logging")
                @ImplementationId("log-hook")
                public final class LoggingHook {}
                """,
                "OLO-AP-003");
    }

    private static void assertFails(String className, String source, String code) {
        Compilation compilation =
                compile(JavaFileObjects.forSourceString("sample." + className, source));
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining(code);
    }

    private static JavaFileObject nodeSource(String className, String type) {
        return JavaFileObjects.forSourceString(
                "sample." + className,
                """
                package sample;

                import org.olo.annotation.OloNode;
                import org.olo.spi.annotation.NodeType;

                @OloNode(type = "%s", name = "%s")
                @NodeType("%s")
                public final class %s {}
                """
                        .formatted(type, className, type, className));
    }

    private static Compilation compile(JavaFileObject... sources) {
        return Compiler.javac()
                .withProcessors(new OloExtensionCatalogProcessor())
                .withOptions("-Aolo.catalog.module=test")
                .compile(sources);
    }
}
