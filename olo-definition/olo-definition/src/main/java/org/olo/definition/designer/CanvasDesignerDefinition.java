/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.designer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Studio canvas chrome ({@code designer.canvas}).
 */
@JsonDeserialize(builder = CanvasDesignerDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CanvasDesignerDefinition {

    private final String backgroundColor;
    private final Integer gridGap;
    private final String edgeStroke;
    private final String selectionBorder;
    private final String minimapNodeColor;

    private CanvasDesignerDefinition(Builder builder) {
        this.backgroundColor = builder.backgroundColor;
        this.gridGap = builder.gridGap;
        this.edgeStroke = builder.edgeStroke;
        this.selectionBorder = builder.selectionBorder;
        this.minimapNodeColor = builder.minimapNodeColor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public Integer getGridGap() {
        return gridGap;
    }

    public String getEdgeStroke() {
        return edgeStroke;
    }

    public String getSelectionBorder() {
        return selectionBorder;
    }

    public String getMinimapNodeColor() {
        return minimapNodeColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CanvasDesignerDefinition that)) {
            return false;
        }
        return Objects.equals(backgroundColor, that.backgroundColor)
                && Objects.equals(gridGap, that.gridGap)
                && Objects.equals(edgeStroke, that.edgeStroke)
                && Objects.equals(selectionBorder, that.selectionBorder)
                && Objects.equals(minimapNodeColor, that.minimapNodeColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backgroundColor, gridGap, edgeStroke, selectionBorder, minimapNodeColor);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String backgroundColor;
        private Integer gridGap;
        private String edgeStroke;
        private String selectionBorder;
        private String minimapNodeColor;

        public Builder backgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder gridGap(Integer gridGap) {
            this.gridGap = gridGap;
            return this;
        }

        public Builder edgeStroke(String edgeStroke) {
            this.edgeStroke = edgeStroke;
            return this;
        }

        public Builder selectionBorder(String selectionBorder) {
            this.selectionBorder = selectionBorder;
            return this;
        }

        public Builder minimapNodeColor(String minimapNodeColor) {
            this.minimapNodeColor = minimapNodeColor;
            return this;
        }

        public CanvasDesignerDefinition build() {
            return new CanvasDesignerDefinition(this);
        }
    }
}
