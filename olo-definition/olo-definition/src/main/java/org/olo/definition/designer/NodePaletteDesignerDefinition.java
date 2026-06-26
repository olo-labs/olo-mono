package org.olo.definition.designer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Palette entry metadata for boundary nodes ({@code designer.nodeTypes.*.palette}).
 */
@JsonDeserialize(builder = NodePaletteDesignerDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NodePaletteDesignerDefinition {

    private final String name;
    private final String category;
    private final Boolean enabled;

    private NodePaletteDesignerDefinition(Builder builder) {
        this.name = builder.name;
        this.category = builder.category;
        this.enabled = builder.enabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodePaletteDesignerDefinition that)) {
            return false;
        }
        return Objects.equals(name, that.name)
                && Objects.equals(category, that.category)
                && Objects.equals(enabled, that.enabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, category, enabled);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String name;
        private String category;
        private Boolean enabled;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public NodePaletteDesignerDefinition build() {
            return new NodePaletteDesignerDefinition(this);
        }
    }
}
