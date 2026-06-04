package io.olo.definition.validation;

import io.olo.definition.edge.EdgeDefinition;
import io.olo.definition.error.ErrorRoute;
import io.olo.definition.error.OnFailureDefinition;
import io.olo.definition.error.RetryPolicy;
import io.olo.definition.human.HumanApprovalDefinition;
import io.olo.definition.node.NodeDefinition;
import io.olo.definition.node.NodeType;
import io.olo.definition.port.PortDefinition;
import io.olo.definition.input.WorkflowInputDefinition;
import io.olo.definition.parameter.WorkflowParameterDefinition;
import io.olo.definition.state.StateFieldDefinition;
import io.olo.definition.workflow.WorkflowBuilder;
import io.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowValidatorTest {

    @Test
    void acceptsValidWorkflow() {
        WorkflowDefinition workflow = WorkflowBuilder.create("Valid")
                .capability(ValidationTestFixtures.minimalCapability())
                .inputNode("in")
                .outputNode("out")
                .connect("in", "out")
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
        WorkflowValidator.validateOrThrow(workflow);
    }

    @Test
    void rejectsUnknownEdgeEndpoints() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("broken")
                .capability(ValidationTestFixtures.minimalCapability())
                .addEdge(EdgeDefinition.builder().sourceNodeId("a").targetNodeId("b").build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("unknown source node"));
        assertThat(result.errors()).anyMatch(e -> e.contains("unknown target node"));
    }

    @Test
    void acceptsCompatibleTypedPorts() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("typed-ok")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder()
                        .id("tool-a")
                        .type(NodeType.TOOL)
                        .addOutput(PortDefinition.builder().name("stockList").schema("Stock[]").build())
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("tool-b")
                        .type(NodeType.TOOL)
                        .addInput(PortDefinition.builder().name("stocks").schema("Stock[]").build())
                        .build())
                .addEdge(EdgeDefinition.builder()
                        .sourceNodeId("tool-a")
                        .sourcePort("stockList")
                        .targetNodeId("tool-b")
                        .targetPort("stocks")
                        .build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void rejectsIncompatibleTypedPorts() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("typed-bad")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder()
                        .id("tool-a")
                        .type(NodeType.TOOL)
                        .addOutput(PortDefinition.builder().name("result").schema("String").build())
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("tool-b")
                        .type(NodeType.TOOL)
                        .addInput(PortDefinition.builder().name("stocks").schema("Stock[]").build())
                        .build())
                .addEdge(EdgeDefinition.builder()
                        .sourceNodeId("tool-a")
                        .sourcePort("result")
                        .targetNodeId("tool-b")
                        .targetPort("stocks")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("schema mismatch"));
        assertThat(result.errors()).anyMatch(e -> e.contains("String"));
        assertThat(result.errors()).anyMatch(e -> e.contains("Stock[]"));
    }

    @Test
    void rejectsUnknownOutputPort() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("unknown-port")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder()
                        .id("tool-a")
                        .type(NodeType.TOOL)
                        .addOutput(PortDefinition.builder().name("out").schema("String").build())
                        .build())
                .addNode(NodeDefinition.builder().id("tool-b").type(NodeType.TOOL).build())
                .addEdge(EdgeDefinition.builder()
                        .sourceNodeId("tool-a")
                        .sourcePort("missing")
                        .targetNodeId("tool-b")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("unknown output port"));
    }

    @Test
    void acceptsHumanNodeWithApproval() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("hitl-ok")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder()
                        .id("trade-approval")
                        .type(NodeType.HUMAN)
                        .subtype("APPROVAL")
                        .approval(HumanApprovalDefinition.builder()
                                .title("Approve trade?")
                                .approvers(List.of("trading-desk"))
                                .build())
                        .build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void rejectsHumanNodeWithoutApproval() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("hitl-bad")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder().id("trade-approval").type(NodeType.HUMAN).build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("requires an approval block"));
    }

    @Test
    void acceptsOnFailureWithRetryAndRoute() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("failure-ok")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder()
                        .id("openai")
                        .type(NodeType.MODEL)
                        .onFailure(OnFailureDefinition.builder()
                                .retry(RetryPolicy.builder().attempts(3).build())
                                .route(ErrorRoute.builder().targetNodeId("fallback-model").build())
                                .build())
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("fallback-model")
                        .type(NodeType.MODEL)
                        .build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void rejectsOnFailureRouteToUnknownNode() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("failure-bad")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder()
                        .id("openai")
                        .type(NodeType.MODEL)
                        .onFailure(OnFailureDefinition.builder()
                                .route(ErrorRoute.builder().targetNodeId("missing").build())
                                .build())
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("unknown target node"));
    }

    @Test
    void rejectsOnFailureWithZeroAttempts() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("failure-retry")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder()
                        .id("openai")
                        .type(NodeType.MODEL)
                        .onFailure(OnFailureDefinition.builder()
                                .retry(RetryPolicy.builder().attempts(0).build())
                                .build())
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("attempts must be >= 1"));
    }

    @Test
    void requiresPortNameWhenMultipleOutputsDeclared() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("multi-out")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder()
                        .id("router")
                        .type(NodeType.CONDITION)
                        .addOutput(PortDefinition.builder().name("true").schema("any").build())
                        .addOutput(PortDefinition.builder().name("false").schema("any").build())
                        .build())
                .addNode(NodeDefinition.builder().id("sink").type(NodeType.OUTPUT).build())
                .addEdge(EdgeDefinition.builder().sourceNodeId("router").targetNodeId("sink").build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("outputPort is required"));
    }

    @Test
    void rejectsWriteToUnknownStateField() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("typo-write")
                .capability(ValidationTestFixtures.minimalCapability())
                .putState("analysis", StateFieldDefinition.builder().schema("Analysis").build())
                .addNode(NodeDefinition.builder()
                        .id("research")
                        .type(NodeType.TOOL)
                        .addWrite("state.analysys")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("writes unknown state field: state.analysys"));
        assertThat(result.errors()).anyMatch(e -> e.contains("no state field 'analysys'"));
    }

    @Test
    void rejectsReadFromUnknownStateField() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("typo-read")
                .capability(ValidationTestFixtures.minimalCapability())
                .putState("analysis", StateFieldDefinition.builder().schema("Analysis").build())
                .addNode(NodeDefinition.builder()
                        .id("risk")
                        .type(NodeType.TOOL)
                        .addRead("state.analysys")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("reads unknown state field: state.analysys"));
    }

    @Test
    void rejectsStateReadWhenFieldNotDeclared() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("no-state-schema")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder()
                        .id("agent")
                        .type(NodeType.TOOL)
                        .addRead("state.symbol")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("reads unknown state field: state.symbol"));
    }

    @Test
    void acceptsNestedAndIndexedStatePaths() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("nested-state")
                .capability(ValidationTestFixtures.minimalCapability())
                .putState("analysis", StateFieldDefinition.builder().schema("Analysis").build())
                .putState("news", StateFieldDefinition.builder().schema("News[]").build())
                .addNode(NodeDefinition.builder()
                        .id("scorer")
                        .type(NodeType.TOOL)
                        .addRead("state.analysis.score")
                        .addRead("state.news[0]")
                        .build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void acceptsInputAndParameterReadPaths() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("input-param-reads")
                .capability(ValidationTestFixtures.minimalCapability())
                .putInput("symbol", WorkflowInputDefinition.builder().schema("String").required(true).build())
                .putParameter("temperature", WorkflowParameterDefinition.builder().schema("number").build())
                .addNode(NodeDefinition.builder()
                        .id("llm")
                        .type(NodeType.MODEL)
                        .addRead("input.symbol")
                        .addRead("parameter.temperature")
                        .build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void rejectsWriteToInputOrParameter() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("bad-write-root")
                .capability(ValidationTestFixtures.minimalCapability())
                .putInput("symbol", WorkflowInputDefinition.builder().schema("String").build())
                .putState("analysis", StateFieldDefinition.builder().schema("Analysis").build())
                .addNode(NodeDefinition.builder()
                        .id("bad")
                        .type(NodeType.TOOL)
                        .addWrite("input.symbol")
                        .build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("write path must use state. namespace"));
    }

    @Test
    void acceptsValidReadsAndWrites() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("valid-state-access")
                .capability(ValidationTestFixtures.minimalCapability())
                .putState("symbol", StateFieldDefinition.builder().schema("String").build())
                .putState("analysis", StateFieldDefinition.builder().schema("Analysis").build())
                .addNode(NodeDefinition.builder()
                        .id("research")
                        .type(NodeType.TOOL)
                        .addRead("state.symbol")
                        .addWrite("state.analysis")
                        .build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void validateOrThrowThrows() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("broken")
                .capability(ValidationTestFixtures.minimalCapability())
                .addEdge(EdgeDefinition.builder().sourceNodeId("a").targetNodeId("b").build())
                .build();

        assertThatThrownBy(() -> WorkflowValidator.validateOrThrow(workflow))
                .isInstanceOf(WorkflowValidationException.class);
    }
}
