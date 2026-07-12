/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.designer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Studio designer metadata on workflow artifacts ({@code designer} on workflows, agents, tools, hooks).
 */
@JsonDeserialize(builder = DesignerDefinitionBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DesignerDefinition {

    private final String paletteGroup;
    private final List<String> searchKeywords;
    private final NodeSizeDefinition nodeSize;
    private final Boolean resizable;
    private final Boolean draggable;
    private final LayoutDesignerDefinition layout;
    private final CanvasDesignerDefinition canvas;
    private final Map<String, String> portColors;
    private final Map<String, NodeTypeDesignerDefinition> nodeTypes;

    DesignerDefinition(DesignerDefinitionBuilder builder) {
        this.paletteGroup = builder.paletteGroup;
        this.searchKeywords = builder.searchKeywords == null
                ? List.of()
                : List.copyOf(builder.searchKeywords);
        this.nodeSize = builder.nodeSize;
        this.resizable = builder.resizable;
        this.draggable = builder.draggable;
        this.layout = builder.layout;
        this.canvas = builder.canvas;
        this.portColors = builder.portColors == null ? Map.of() : Map.copyOf(builder.portColors);
        this.nodeTypes = builder.nodeTypes == null ? Map.of() : Map.copyOf(builder.nodeTypes);
    }

    public static DesignerDefinitionBuilder builder() {
        return new DesignerDefinitionBuilder();
    }

    public String getPaletteGroup() {
        return paletteGroup;
    }

    public List<String> getSearchKeywords() {
        return searchKeywords;
    }

    public NodeSizeDefinition getNodeSize() {
        return nodeSize;
    }

    public Boolean getResizable() {
        return resizable;
    }

    public Boolean getDraggable() {
        return draggable;
    }

    public LayoutDesignerDefinition getLayout() {
        return layout;
    }

    public CanvasDesignerDefinition getCanvas() {
        return canvas;
    }

    public Map<String, String> getPortColors() {
        return portColors;
    }

    public Map<String, NodeTypeDesignerDefinition> getNodeTypes() {
        return nodeTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DesignerDefinition that)) {
            return false;
        }
        return Objects.equals(paletteGroup, that.paletteGroup)
                && Objects.equals(searchKeywords, that.searchKeywords)
                && Objects.equals(nodeSize, that.nodeSize)
                && Objects.equals(resizable, that.resizable)
                && Objects.equals(draggable, that.draggable)
                && Objects.equals(layout, that.layout)
                && Objects.equals(canvas, that.canvas)
                && Objects.equals(portColors, that.portColors)
                && Objects.equals(nodeTypes, that.nodeTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                paletteGroup,
                searchKeywords,
                nodeSize,
                resizable,
                draggable,
                layout,
                canvas,
                portColors,
                nodeTypes);
    }
}
