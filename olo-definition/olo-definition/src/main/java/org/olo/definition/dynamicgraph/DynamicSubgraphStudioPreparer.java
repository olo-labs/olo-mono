/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.dynamicgraph;

import org.olo.definition.designer.DesignerDefinition;
import org.olo.definition.designer.DesignerDefinitionBuilder;
import org.olo.definition.designer.InlinePropertyDefinition;
import org.olo.definition.designer.NodePaletteDesignerDefinition;
import org.olo.definition.designer.NodeTypeDesignerDefinition;
import org.olo.definition.designer.StudioDesignerDefaults;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Makes runtime-merged workflow graphs Studio-ready for olo-ui builder (canvas positions, designer metadata, ids).
 */
public final class DynamicSubgraphStudioPreparer {

    private DynamicSubgraphStudioPreparer() {
    }

    public static WorkflowDefinition prepareForBuilder(
            WorkflowDefinition merged, String builderId, String kind, String injectedAt) {
        WorkflowBuilder builder = WorkflowBuilder.from(merged)
                .id(builderId)
                .queue(builderId)
                .enabled(true)
                .isDefault(false)
                .label(merged.getLabel() == null ? builderId : merged.getLabel() + " (injected)")
                .designer(studioDesigner(merged))
                .metadata(
                        DynamicSubgraphInjectionSupport.METADATA_INJECTED_SUBGRAPH,
                        Map.of(
                                "kind", kind == null ? "unknown" : kind,
                                "sourceWorkflowId", merged.getId() == null ? builderId : merged.getId(),
                                "injectedAt", injectedAt == null ? "" : injectedAt));

        Map<String, Map<String, Object>> positions = layoutPositions(merged);
        for (NodeDefinition node : merged.getNodes()) {
            Map<String, Object> configuration = new LinkedHashMap<>();
            if (node.getConfiguration() != null) {
                configuration.putAll(node.getConfiguration());
            }
            if (isLikelyToolSynthesis(node)) {
                configuration.put(ToolSynthesisSupport.CONFIG_TOOL_SYNTHESIS, true);
            }
            Map<String, Object> layout = positions.get(node.getId());
            if (layout != null) {
                configuration.putAll(layout);
            }
            if (!configuration.isEmpty()) {
                builder.putNodeConfiguration(node.getId(), configuration);
            }
        }
        return builder.build();
    }

    private static boolean isLikelyToolSynthesis(NodeDefinition node) {
        if (!"AGENT".equalsIgnoreCase(node.getType())) {
            return false;
        }
        if (node.getConfiguration() != null
                && Boolean.TRUE.equals(node.getConfiguration().get(ToolSynthesisSupport.CONFIG_TOOL_SYNTHESIS))) {
            return true;
        }
        if (node.getId() != null && node.getId().endsWith(ToolCallPlannerSupport.DEFAULT_SYNTHESIS_NODE_SUFFIX)) {
            return true;
        }
        return "Tool synthesis".equalsIgnoreCase(node.getLabel());
    }

    private static DesignerDefinition studioDesigner(WorkflowDefinition workflow) {
        DesignerDefinition existing = workflow.getDesigner();
        Map<String, NodeTypeDesignerDefinition> nodeTypes = new LinkedHashMap<>();
        if (existing != null && existing.getNodeTypes() != null) {
            nodeTypes.putAll(existing.getNodeTypes());
        }
        if (nodeTypes.isEmpty()) {
            nodeTypes.putAll(StudioDesignerDefaults.agentPipelineNodeTypes(
                    workflow.getEmoji() == null ? "🤖" : workflow.getEmoji()));
        }
        if (containsNodeType(workflow, "TOOL") && !nodeTypes.containsKey("TOOL")) {
            nodeTypes.put("TOOL", toolNodeType());
        }
        DesignerDefinitionBuilder designer = DesignerDefinition.builder()
                .paletteGroup(existing == null || existing.getPaletteGroup() == null
                        ? "Injected"
                        : existing.getPaletteGroup())
                .nodeSize(StudioDesignerDefaults.NODE_WIDTH, StudioDesignerDefaults.NODE_HEIGHT)
                .resizable(true)
                .draggable(true)
                .layout(StudioDesignerDefaults.layout())
                .canvas(StudioDesignerDefaults.canvas())
                .portColors(StudioDesignerDefaults.portColors())
                .nodeTypes(nodeTypes);
        if (existing != null && existing.getSearchKeywords() != null) {
            existing.getSearchKeywords().forEach(designer::searchKeyword);
        }
        designer.searchKeyword("injected");
        return designer.build();
    }

    private static NodeTypeDesignerDefinition toolNodeType() {
        return NodeTypeDesignerDefinition.builder()
                .emoji("🔧")
                .typeLabel("Tool")
                .description("Registered tool invocation")
                .palette(NodePaletteDesignerDefinition.builder()
                        .name("Tool")
                        .category("tools")
                        .build())
                .inlineProperties(List.of(
                        InlinePropertyDefinition.builder()
                                .id("toolId")
                                .widget("TEXT")
                                .label("Tool id")
                                .binding("toolId")
                                .build()))
                .build();
    }

    private static boolean containsNodeType(WorkflowDefinition workflow, String type) {
        return workflow.getNodes().stream().anyMatch(node -> type.equalsIgnoreCase(node.getType()));
    }

    static Map<String, Map<String, Object>> layoutPositions(WorkflowDefinition workflow) {
        List<String> mainPath = mainMessagePath(workflow);
        Set<String> mainPathSet = new HashSet<>(mainPath);
        Map<String, Map<String, Object>> layouts = new LinkedHashMap<>();

        for (int index = 0; index < mainPath.size(); index++) {
            layouts.put(mainPath.get(index), designerPosition(columnX(index), StudioDesignerDefaults.LAYOUT_ORIGIN_Y));
        }

        int pluginColumn = 0;
        for (NodeDefinition node : workflow.getNodes()) {
            if (mainPathSet.contains(node.getId())) {
                continue;
            }
            if ("TOOL".equalsIgnoreCase(node.getType()) && isCapabilityOnlyTool(node, workflow)) {
                layouts.put(
                        node.getId(),
                        designerPosition(
                                columnX(pluginColumn),
                                StudioDesignerDefaults.LAYOUT_ORIGIN_Y + StudioDesignerDefaults.LAYOUT_ROW_GAP));
                pluginColumn++;
            } else if (!layouts.containsKey(node.getId())) {
                layouts.put(
                        node.getId(),
                        designerPosition(columnX(mainPath.size() + layouts.size()), StudioDesignerDefaults.LAYOUT_ORIGIN_Y));
            }
        }
        return layouts;
    }

    private static List<String> mainMessagePath(WorkflowDefinition workflow) {
        Map<String, String> nextByMessageEdge = new LinkedHashMap<>();
        for (EdgeDefinition edge : workflow.getEdges()) {
            if ("out".equals(edge.getSourcePortId()) && "in".equals(edge.getTargetPortId())) {
                nextByMessageEdge.putIfAbsent(edge.getSourceNodeId(), edge.getTargetNodeId());
            }
        }
        List<String> path = new ArrayList<>();
        String current = workflow.getNodes().stream()
                .filter(node -> "START".equalsIgnoreCase(node.getType()))
                .map(NodeDefinition::getId)
                .findFirst()
                .orElse("start");
        Set<String> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
            path.add(current);
            current = nextByMessageEdge.get(current);
        }
        return path;
    }

    private static boolean isCapabilityOnlyTool(NodeDefinition node, WorkflowDefinition workflow) {
        boolean hasCapabilityOut = workflow.getEdges().stream()
                .anyMatch(edge -> node.getId().equals(edge.getSourceNodeId()) && "capabilities".equals(edge.getSourcePortId()));
        boolean hasMessageIn = workflow.getEdges().stream()
                .anyMatch(edge -> node.getId().equals(edge.getTargetNodeId()) && "in".equals(edge.getTargetPortId()));
        return hasCapabilityOut && !hasMessageIn;
    }

    private static int columnX(int columnIndex) {
        return StudioDesignerDefaults.LAYOUT_ORIGIN_X + columnIndex * StudioDesignerDefaults.LAYOUT_COLUMN_GAP;
    }

    private static Map<String, Object> designerPosition(int x, int y) {
        return Map.of("designer", Map.of("position", Map.of("x", x, "y", y)));
    }
}
