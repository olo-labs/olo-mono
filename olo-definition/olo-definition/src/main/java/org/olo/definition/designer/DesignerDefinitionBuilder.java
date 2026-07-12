/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.designer;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Jackson builder and fluent factory for {@link DesignerDefinition}.
 */
@JsonPOJOBuilder(withPrefix = "")
public final class DesignerDefinitionBuilder {

    String paletteGroup;
    List<String> searchKeywords;
    NodeSizeDefinition nodeSize;
    Boolean resizable;
    Boolean draggable;
    LayoutDesignerDefinition layout;
    CanvasDesignerDefinition canvas;
    Map<String, String> portColors;
    Map<String, NodeTypeDesignerDefinition> nodeTypes;

    public DesignerDefinitionBuilder paletteGroup(String paletteGroup) {
        this.paletteGroup = paletteGroup;
        return this;
    }

    public DesignerDefinitionBuilder searchKeywords(List<String> searchKeywords) {
        this.searchKeywords = searchKeywords;
        return this;
    }

    public DesignerDefinitionBuilder searchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return this;
        }
        if (this.searchKeywords == null) {
            this.searchKeywords = new ArrayList<>();
        }
        this.searchKeywords.add(keyword);
        return this;
    }

    public DesignerDefinitionBuilder nodeSize(NodeSizeDefinition nodeSize) {
        this.nodeSize = nodeSize;
        return this;
    }

    public DesignerDefinitionBuilder nodeSize(int width, int height) {
        this.nodeSize = NodeSizeDefinition.builder().width(width).height(height).build();
        return this;
    }

    /** Legacy flat field — migrated to {@link #nodeSize}. */
    @JsonSetter("defaultWidth")
    public DesignerDefinitionBuilder legacyDefaultWidth(Integer width) {
        return nodeSizeWidth(width);
    }

    /** Legacy flat field — migrated to {@link #nodeSize}. */
    @JsonSetter("defaultHeight")
    public DesignerDefinitionBuilder legacyDefaultHeight(Integer height) {
        return nodeSizeHeight(height);
    }

    private DesignerDefinitionBuilder nodeSizeWidth(Integer width) {
        if (width == null) {
            return this;
        }
        NodeSizeDefinition.Builder size = nodeSize == null
                ? NodeSizeDefinition.builder()
                : NodeSizeDefinition.builder().width(nodeSize.getWidth()).height(nodeSize.getHeight());
        this.nodeSize = size.width(width).build();
        return this;
    }

    private DesignerDefinitionBuilder nodeSizeHeight(Integer height) {
        if (height == null) {
            return this;
        }
        NodeSizeDefinition.Builder size = nodeSize == null
                ? NodeSizeDefinition.builder()
                : NodeSizeDefinition.builder().width(nodeSize.getWidth()).height(nodeSize.getHeight());
        this.nodeSize = size.height(height).build();
        return this;
    }

    public DesignerDefinitionBuilder resizable(Boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    public DesignerDefinitionBuilder draggable(Boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public DesignerDefinitionBuilder layout(LayoutDesignerDefinition layout) {
        this.layout = layout;
        return this;
    }

    public DesignerDefinitionBuilder canvas(CanvasDesignerDefinition canvas) {
        this.canvas = canvas;
        return this;
    }

    public DesignerDefinitionBuilder portColors(Map<String, String> portColors) {
        this.portColors = portColors == null ? null : new LinkedHashMap<>(portColors);
        return this;
    }

    public DesignerDefinitionBuilder nodeTypes(Map<String, NodeTypeDesignerDefinition> nodeTypes) {
        this.nodeTypes = nodeTypes == null ? null : new LinkedHashMap<>(nodeTypes);
        return this;
    }

    public DesignerDefinition build() {
        return new DesignerDefinition(this);
    }
}
