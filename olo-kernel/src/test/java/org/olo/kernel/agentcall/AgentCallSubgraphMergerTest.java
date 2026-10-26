/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.agentcall;

import org.junit.jupiter.api.Test;
import org.olo.definition.execution.ExecutionModel;
import org.olo.kernel.agentcall.model.ParsedAgentCall;
import org.olo.kernel.agentcall.model.AgentCallValidationResult;
import org.olo.kernel.agentcall.support.AgentCallSubgraphMergerTestWorkflows;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class AgentCallSubgraphMergerTest {
    private static final AgentCallSubgraphMerger MERGER = AgentCallFactories.defaultAgentCallSubgraphMerger();
    private static final String CATALOG = "[{\"agentId\":\"literature-agent\"},{\"agentId\":\"synthesis-agent\"}]";
    private static final String CALL = "{\"agentCalls\":[{\"agentId\":\"literature-agent\",\"message\":\"Find papers\"}],\"directResponse\":null}";

    @Test void validatesOnlyExplicitlyAvailableAgents() {
        var result = MERGER.validate(CALL, CATALOG);
        assertThat(result.valid()).isTrue();
        assertThat(result.kind()).isEqualTo(AgentCallValidationResult.Kind.AGENT_CALLS);
        assertThat(result.agentCalls()).containsExactly(new ParsedAgentCall("literature-agent", "Find papers"));
        for (String catalog : List.of("[]", "{}", "null", "malformed", "[{\"agentId\":\"other\"}]"))
            assertThat(MERGER.validate(CALL, catalog).valid()).isFalse();
        assertThat(MERGER.validate(CALL, null).valid()).isFalse();
    }

    @Test void directResponseNeedsNoDelegationCatalog() {
        var result = MERGER.validate("{\"agentCalls\":[],\"directResponse\":\"Hello\"}", "[]");
        assertThat(result.kind()).isEqualTo(AgentCallValidationResult.Kind.DIRECT_RESPONSE);
        assertThat(result.directResponse()).isEqualTo("Hello");
        assertThat(MERGER.validate("{\"agentCalls\":[],\"directResponse\":null}", CATALOG).valid()).isFalse();
        assertThat(MERGER.validate(CALL.replace("null", "\"Both\""), CATALOG).valid()).isFalse();
    }

    @Test void rejectsLegacyToolPlannerFieldsEvenWhenEmpty() {
        var result = MERGER.validate("{\"toolCalls\":[],\"agentCalls\":[],\"directResponse\":\"Hello\"}", CATALOG);
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("toolCalls");
    }

    @Test void rejectsUnknownNestedFieldsDuplicateKeysAndTrailingDocuments() {
        for (String json : List.of(CALL.replace("\"message\"", "\"toolId\""),
                CALL.replace("\"Find papers\"", "42"),
                CALL.replace("\"agentCalls\":", "\"agentCalls\":[],\"agentCalls\":"),
                CALL + " {}", CALL.replace("\"directResponse\":null", "\"directResponse\":false")))
            assertThat(MERGER.validate(json, CATALOG).valid()).isFalse();
    }

    @Test void duplicateDelegationsAndMalformedCatalogsFailClosed() {
        String duplicate = "{\"agentCalls\":[{\"agentId\":\"literature-agent\"},{\"agentId\":\"literature-agent\"}],\"directResponse\":null}";
        assertThat(MERGER.validate(duplicate, CATALOG).valid()).isFalse();
        for (String catalog : List.of("[{}]", "[\"literature-agent\"]", "[{\"agentId\":\"literature-agent\"},{\"agentId\":\"literature-agent\"}]"))
            assertThat(MERGER.validate(CALL, catalog).valid()).isFalse();
    }

    @Test void invalidJsonDiagnosticsDoNotIncludePrivateModelText() {
        var result = MERGER.validate("{ private-model-output-secret", CATALOG);
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).doesNotContain("private-model-output-secret");
        var unknown = MERGER.validate("{\"private-model-output-secret\":true}", CATALOG);
        assertThat(unknown.message()).doesNotContain("private-model-output-secret");
    }

    @Test void delegatesSequentialChildWorkflowsWithReplayStableNodeIds() {
        var base = AgentCallSubgraphMergerTestWorkflows.orchestratorWithChildAgents();
        var calls = List.of(new ParsedAgentCall("literature-agent", "Find papers"), new ParsedAgentCall("synthesis-agent", "Summarize"));
        var first = MERGER.merge(base, "agent", "end", calls);
        var replay = MERGER.merge(base, "agent", "end", calls);
        var nodes = first.graph().getNodes().stream().filter(n -> n.getId().startsWith("agent-dyn-")).toList();
        assertThat(nodes).hasSize(2);
        assertThat(nodes).allMatch(n -> n.getType().equals("AGENT") && n.getExecutionModel() == ExecutionModel.CHILD_WORKFLOW);
        assertThat(nodes.stream().map(n -> n.getWorkflow().getWorkflowId())).containsExactly("literature-agent", "synthesis-agent");
        assertThat(first.entryNodeId()).isEqualTo(nodes.getFirst().getId());
        assertThat(first.graph().getEdges()).anyMatch(e -> e.getSourceNodeId().equals(nodes.getFirst().getId()) && e.getTargetNodeId().equals(nodes.getLast().getId()));
        assertThat(first.graph().getEdges()).anyMatch(e -> e.getSourceNodeId().equals(nodes.getLast().getId()) && e.getTargetNodeId().equals("end"));
        assertThat(first.graph().getNodes().stream().map(n -> n.getId())).containsExactlyElementsOf(replay.graph().getNodes().stream().map(n -> n.getId()).toList());
    }
}
