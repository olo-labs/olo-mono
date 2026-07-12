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
 * Default canvas grid for new nodes ({@code designer.layout}).
 */
@JsonDeserialize(builder = LayoutDesignerDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LayoutDesignerDefinition {

    private final Integer originX;
    private final Integer originY;
    private final Integer columnGap;
    private final Integer rowGap;
    private final Integer columns;

    private LayoutDesignerDefinition(Builder builder) {
        this.originX = builder.originX;
        this.originY = builder.originY;
        this.columnGap = builder.columnGap;
        this.rowGap = builder.rowGap;
        this.columns = builder.columns;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getOriginX() {
        return originX;
    }

    public Integer getOriginY() {
        return originY;
    }

    public Integer getColumnGap() {
        return columnGap;
    }

    public Integer getRowGap() {
        return rowGap;
    }

    public Integer getColumns() {
        return columns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LayoutDesignerDefinition that)) {
            return false;
        }
        return Objects.equals(originX, that.originX)
                && Objects.equals(originY, that.originY)
                && Objects.equals(columnGap, that.columnGap)
                && Objects.equals(rowGap, that.rowGap)
                && Objects.equals(columns, that.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originX, originY, columnGap, rowGap, columns);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private Integer originX;
        private Integer originY;
        private Integer columnGap;
        private Integer rowGap;
        private Integer columns;

        public Builder originX(Integer originX) {
            this.originX = originX;
            return this;
        }

        public Builder originY(Integer originY) {
            this.originY = originY;
            return this;
        }

        public Builder columnGap(Integer columnGap) {
            this.columnGap = columnGap;
            return this;
        }

        public Builder rowGap(Integer rowGap) {
            this.rowGap = rowGap;
            return this;
        }

        public Builder columns(Integer columns) {
            this.columns = columns;
            return this;
        }

        public LayoutDesignerDefinition build() {
            return new LayoutDesignerDefinition(this);
        }
    }
}
