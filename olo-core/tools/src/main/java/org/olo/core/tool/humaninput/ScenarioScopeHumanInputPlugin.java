/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.humaninput;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloTool;
import org.olo.spi.annotation.ImplementationId;
import org.olo.spi.annotation.ToolId;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.Tool;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

@OloTool(
        id = CoreHumanInputPluginIds.SCENARIO_SCOPE,
        name = "Scenario Scope Human Input",
        description = "Collects operator scope notes and constraints before a scenario planner runs",
        category = "human-input",
        emoji = "📝",
        tags = {"human-input", "plugin", "scope", "intake"},
        examples = {"Confirm investigation scope before performance triage runs"},
        arguments = {
            @OloProperty(
                    name = "confirmScope",
                    label = "Continue with this scope?",
                    type = OloPropertyType.APPROVAL_TOGGLE,
                    required = true,
                    description = "Select Yes to proceed with the scope below",
                    group = "Approval",
                    order = 0),
            @OloProperty(
                    name = "scopeNotes",
                    label = "Scope notes",
                    type = OloPropertyType.TEXTAREA,
                    required = true,
                    description = "Add context, constraints, or priorities for this run",
                    placeholder = "Focus on payment-api latency between 14:00-15:00 UTC…",
                    group = "Scope",
                    order = 1)
        })
@ToolId(CoreHumanInputPluginIds.SCENARIO_SCOPE)
@ImplementationId(CoreHumanInputPluginIds.SCENARIO_SCOPE)
public final class ScenarioScopeHumanInputPlugin implements Tool {

    @Override
    public String toolId() {
        return CoreHumanInputPluginIds.SCENARIO_SCOPE;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        return HumanInputPluginSupport.schemaOnlyInvoke(toolId(), request, context);
    }
}
