/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.travelplanner;

import org.olo.definition.OloProductTerminology;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioAgentSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioToolSpec;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/** Programmatic builders for the {@code travel-planner} scenario collection. */
public final class TravelPlannerDefinitions {

    public static final String QUEUE = "oloQueue2";
    public static final String ORCHESTRATOR_ID = "travel-orchestrator";
    public static final String DESTINATION_AGENT_ID = "destination-agent";
    public static final String ITINERARY_AGENT_ID = "itinerary-agent";
    public static final String DESTINATIONS_NODE_ID = "travel-destinations";
    public static final String OFFERS_NODE_ID = "travel-offers";
    public static final String DESTINATIONS_TOOL_ID = "olo-core:travel-destinations";
    public static final String OFFERS_TOOL_ID = "olo-core:travel-offers";

    static final String JSON_ONLY_PROMPT_TEMPLATE =
            """
            """
                    + OloProductTerminology.agentRolePrompt("travel planning")
                    + """

            User request:
            {message}

            Available tools (strict allow-list — use only these toolId values):
            {availableToolsJson}

            Available agents (strict allow-list — delegate only to these agentId values):
            {availableAgentsJson}

            Output rules (strict):
            1. Return ONLY a single JSON object — no markdown, no code fences, no commentary, no trailing text.
            2. Schema:
            {
              "toolCalls": [
                { "toolId": "olo-core:travel-destinations", "arguments": { "region": "europe" } },
                { "toolId": "olo-core:travel-offers", "arguments": { "origin": "London", "destination": "Paris" } }
              ],
              "directResponse": null
            }
            3. Use olo-core:travel-destinations for destination ideas and olo-core:travel-offers for packages.
            4. Delegate only to agents listed in availableAgentsJson when specialist help is needed.
            5. If no tools are needed, set "toolCalls": [] and put the final answer in "directResponse".

            If a previous attempt failed validation, fix it:
            {toolCallSequenceJsonValidationError}

            Respond with JSON only.""";

    private TravelPlannerDefinitions() {
    }

    public static WorkflowDefinition orchestrator() {
        WorkflowDefinition workflow = ScenarioPlannerSupport.orchestratorBuilder(
                        ORCHESTRATOR_ID,
                        QUEUE,
                        "Travel Orchestrator",
                        "Plans trips by querying destination guides and travel offers, with specialist agents",
                        "✈️",
                        JSON_ONLY_PROMPT_TEMPLATE,
                        List.of(
                                new ScenarioAgentSpec(DESTINATION_AGENT_ID, "Destination Agent"),
                                new ScenarioAgentSpec(ITINERARY_AGENT_ID, "Itinerary Agent")),
                        List.of(travelDestinationsTool(), travelOffersTool()))
                .build();
        WorkflowValidator.validateOrThrow(workflow);
        return workflow;
    }

    public static WorkflowDefinition destinationAgent() {
        return ScenarioPlannerSupport.childAgentPreset(
                DESTINATION_AGENT_ID,
                QUEUE,
                "Destination Agent",
                "Recommends cities and highlights for a travel region",
                "🌍",
                "destination",
                "travel",
                "city");
    }

    public static WorkflowDefinition itineraryAgent() {
        return ScenarioPlannerSupport.childAgentPreset(
                ITINERARY_AGENT_ID,
                QUEUE,
                "Itinerary Agent",
                "Builds day-by-day itineraries and compares package offers",
                "🗺️",
                "itinerary",
                "travel",
                "schedule");
    }

    private static ScenarioToolSpec travelDestinationsTool() {
        return new ScenarioToolSpec(
                DESTINATIONS_NODE_ID,
                DESTINATIONS_TOOL_ID,
                "Travel Destinations",
                "Returns mock destination guides for travel planner scenarios",
                "Suggest destinations in Europe for summer");
    }

    private static ScenarioToolSpec travelOffersTool() {
        return new ScenarioToolSpec(
                OFFERS_NODE_ID,
                OFFERS_TOOL_ID,
                "Travel Offers",
                "Returns mock flight and hotel offers for travel planner scenarios",
                "Find weekend offers from London to Paris");
    }
}
