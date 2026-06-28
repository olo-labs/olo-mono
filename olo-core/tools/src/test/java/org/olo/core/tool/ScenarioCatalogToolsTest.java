package org.olo.core.tool;

import org.junit.jupiter.api.Test;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolStatus;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioCatalogToolsTest {

    private static final ExecutionContext CONTEXT = new ExecutionContext() {
        @Override
        public String getWorkflowId() {
            return "scenario-tools";
        }

        @Override
        public String getRunId() {
            return "run-scenario-tools";
        }

        @Override
        public String getQueue() {
            return "default";
        }

        @Override
        public Optional<String> getNodeId() {
            return Optional.of("tool-node");
        }

        @Override
        public Optional<String> getCorrelationId() {
            return Optional.of("corr-scenario-tools");
        }

        @Override
        public boolean hasVariable(String name) {
            return false;
        }

        @Override
        public Object getVariable(String name) {
            return null;
        }

        @Override
        public void setVariable(String name, Object value) {
            // no-op
        }

        @Override
        public Map<String, Object> getVariables() {
            return Collections.emptyMap();
        }
    };

    @Test
    void researchLiteratureMatchesTopicFixture() {
        ResearchLiteratureTool tool = new ResearchLiteratureTool();
        var result = tool.invoke(
                new ToolRequest(
                        CoreToolIds.RESEARCH_LITERATURE,
                        "inv-001",
                        Map.of("topic", "ai safety alignment"),
                        Map.of()),
                CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(String.valueOf(result.output().get("papers"))).contains("alignment evaluation");
    }

    @Test
    void researchLiteratureMatchesLongUserMessageTopic() {
        ResearchLiteratureTool tool = new ResearchLiteratureTool();
        var result = tool.invoke(
                new ToolRequest(
                        CoreToolIds.RESEARCH_LITERATURE,
                        "inv-001b",
                        Map.of(
                                "topic",
                                "Find papers on renewable energy storage and summarize the key trends"),
                        Map.of()),
                CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(String.valueOf(result.output().get("papers"))).contains("Lithium-ion degradation");
    }

    @Test
    void researchLiteratureMatchesQueryArgument() {
        ResearchLiteratureTool tool = new ResearchLiteratureTool();
        var result = tool.invoke(
                new ToolRequest(
                        CoreToolIds.RESEARCH_LITERATURE,
                        "inv-001c",
                        Map.of("query", "renewable energy storage literature review"),
                        Map.of()),
                CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(String.valueOf(result.output().get("papers"))).contains("Hybrid solar");
    }

    @Test
    void researchLiteratureMatchesOloKnowledgeSearchQuery() {
        ResearchLiteratureTool tool = new ResearchLiteratureTool();
        var result = tool.invoke(
                new ToolRequest(
                        CoreToolIds.RESEARCH_LITERATURE,
                        "inv-001d",
                        Map.of(
                                "topic", "what is olo",
                                "query", "perform Knowledge Search on topic what is olo?"),
                        Map.of()),
                CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(String.valueOf(result.output().get("papers"))).contains("Open LLM Orchestrator");
    }

    @Test
    void travelDestinationsMatchesRegionFixture() {
        TravelDestinationTool tool = new TravelDestinationTool();
        var result = tool.invoke(
                new ToolRequest(
                        CoreToolIds.TRAVEL_DESTINATIONS,
                        "inv-002",
                        Map.of("region", "europe"),
                        Map.of()),
                CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(String.valueOf(result.output().get("destinations"))).contains("Summer city breaks");
    }

    @Test
    void travelOffersMatchesOriginDestinationFixture() {
        TravelOffersTool tool = new TravelOffersTool();
        var result = tool.invoke(
                new ToolRequest(
                        CoreToolIds.TRAVEL_OFFERS,
                        "inv-003",
                        Map.of("origin", "London", "destination", "Paris"),
                        Map.of()),
                CONTEXT);

        assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(String.valueOf(result.output().get("offers"))).contains("Weekend packages London");
    }
}
