/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.designer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared Studio canvas defaults for agent-style workflow presets (olo-ui builder).
 */
public final class StudioDesignerDefaults {

    public static final int LAYOUT_ORIGIN_X = 80;
    public static final int LAYOUT_ORIGIN_Y = 80;
    public static final int LAYOUT_COLUMN_GAP = 360;
    public static final int LAYOUT_ROW_GAP = 200;
    public static final int LAYOUT_COLUMNS = 4;

    public static final int NODE_WIDTH = 300;
    public static final int NODE_HEIGHT = 120;

    private StudioDesignerDefaults() {
    }

    public static LayoutDesignerDefinition layout() {
        return LayoutDesignerDefinition.builder()
                .originX(LAYOUT_ORIGIN_X)
                .originY(LAYOUT_ORIGIN_Y)
                .columnGap(LAYOUT_COLUMN_GAP)
                .rowGap(LAYOUT_ROW_GAP)
                .columns(LAYOUT_COLUMNS)
                .build();
    }

    public static CanvasDesignerDefinition canvas() {
        return CanvasDesignerDefinition.builder()
                .backgroundColor("#3f3f46")
                .gridGap(16)
                .edgeStroke("#52525b")
                .selectionBorder("#3b82f6")
                .minimapNodeColor("#52525b")
                .build();
    }

    public static Map<String, String> portColors() {
        Map<String, String> colors = new LinkedHashMap<>();
        colors.put("message", "#ef4444");
        colors.put("capabilities", "#22c55e");
        colors.put("agent-plug", "#a855f7");
        colors.put("any", "#94a3b8");
        colors.put("string", "#a3e635");
        colors.put("number", "#38bdf8");
        colors.put("integer", "#38bdf8");
        colors.put("boolean", "#fb923c");
        colors.put("object", "#c084fc");
        colors.put("array", "#f472b6");
        return Map.copyOf(colors);
    }

    public static NodeTypeDesignerDefinition startNodeType() {
        return NodeTypeDesignerDefinition.builder()
                .emoji("▶")
                .typeLabel("Start")
                .description("Workflow entry — maps caller input to workflow variables")
                .palette(NodePaletteDesignerDefinition.builder()
                        .name("Start")
                        .category("control")
                        .build())
                .inlineProperties(List.of(
                        InlinePropertyDefinition.builder()
                                .id("inputVariables")
                                .widget("VARIABLE_CHECKLIST")
                                .label("Input variables")
                                .binding("inputVariableMappings")
                                .build()))
                .build();
    }

    public static NodeTypeDesignerDefinition agentNodeType(String emoji) {
        return NodeTypeDesignerDefinition.builder()
                .emoji(emoji)
                .typeLabel("Agent")
                .inlineProperties(List.of(
                        InlinePropertyDefinition.builder()
                                .id("parameters")
                                .widget("WORKFLOW_PARAMETERS")
                                .label("Parameters")
                                .build(),
                        InlinePropertyDefinition.builder()
                                .id("model")
                                .widget("MODEL_SELECTOR")
                                .label("Model")
                                .build()))
                .build();
    }

    public static NodeTypeDesignerDefinition endNodeType() {
        return NodeTypeDesignerDefinition.builder()
                .emoji("⏹")
                .typeLabel("End")
                .description("Workflow exit — maps a workflow variable to the caller result")
                .palette(NodePaletteDesignerDefinition.builder()
                        .name("End")
                        .category("control")
                        .build())
                .inlineProperties(List.of(
                        InlinePropertyDefinition.builder()
                                .id("outputVariable")
                                .widget("VARIABLE_SELECT")
                                .label("Output variable")
                                .binding("outputVariableMapping")
                                .build()))
                .build();
    }

    public static NodeTypeDesignerDefinition toolNodeType() {
        return NodeTypeDesignerDefinition.builder()
                .emoji("🔧")
                .typeLabel("Tool")
                .description("Executes a registered tool plugin (e.g. RAG ingest)")
                .palette(NodePaletteDesignerDefinition.builder()
                        .name("Tool")
                        .category("plugins")
                        .build())
                .inlineProperties(List.of(
                        InlinePropertyDefinition.builder()
                                .id("toolId")
                                .widget("STRING")
                                .label("Tool")
                                .binding("toolId")
                                .build(),
                        InlinePropertyDefinition.builder()
                                .id("extensionRef")
                                .widget("STRING")
                                .label("Vector store")
                                .binding("extensionRef")
                                .build(),
                        InlinePropertyDefinition.builder()
                                .id("vectorTable")
                                .widget("STRING")
                                .label("Collection table")
                                .binding("vectorTable")
                                .build(),
                        InlinePropertyDefinition.builder()
                                .id("chunkSize")
                                .widget("NUMBER")
                                .label("Chunk size")
                                .binding("chunkSize")
                                .build()))
                .build();
    }

    public static Map<String, NodeTypeDesignerDefinition> toolPipelineNodeTypes() {
        Map<String, NodeTypeDesignerDefinition> nodeTypes = new LinkedHashMap<>();
        nodeTypes.put("START", startNodeType());
        nodeTypes.put("TOOL", toolNodeType());
        nodeTypes.put("END", endNodeType());
        return Map.copyOf(nodeTypes);
    }

    public static Map<String, NodeTypeDesignerDefinition> agentPipelineNodeTypes(String agentEmoji) {
        Map<String, NodeTypeDesignerDefinition> nodeTypes = new LinkedHashMap<>();
        nodeTypes.put("START", startNodeType());
        nodeTypes.put("AGENT", agentNodeType(agentEmoji));
        nodeTypes.put("END", endNodeType());
        return Map.copyOf(nodeTypes);
    }

    public static DesignerDefinition studioAgentDesigner(String agentEmoji, String... searchKeywords) {
        DesignerDefinitionBuilder builder = DesignerDefinition.builder()
                .paletteGroup("Agents")
                .nodeSize(NODE_WIDTH, NODE_HEIGHT)
                .resizable(true)
                .draggable(true)
                .layout(layout())
                .canvas(canvas())
                .portColors(portColors())
                .nodeTypes(agentPipelineNodeTypes(agentEmoji));
        for (String keyword : searchKeywords) {
            builder.searchKeyword(keyword);
        }
        return builder.build();
    }

    public static DesignerDefinition studioToolPipelineDesigner(String emoji, String... searchKeywords) {
        DesignerDefinitionBuilder builder = DesignerDefinition.builder()
                .paletteGroup("Knowledge")
                .nodeSize(NODE_WIDTH, NODE_HEIGHT)
                .resizable(true)
                .draggable(true)
                .layout(layout())
                .canvas(canvas())
                .portColors(portColors())
                .nodeTypes(toolPipelineNodeTypes());
        for (String keyword : searchKeywords) {
            builder.searchKeyword(keyword);
        }
        return builder.build();
    }
}
