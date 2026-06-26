package org.olo.definition.designer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Studio presentation for a workflow node type ({@code designer.nodeTypes.START}, etc.).
 */
@JsonDeserialize(builder = NodeTypeDesignerDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NodeTypeDesignerDefinition {

    private final String emoji;
    private final String typeLabel;
    private final String description;
    private final NodePaletteDesignerDefinition palette;
    private final List<InlinePropertyDefinition> inlineProperties;

    private NodeTypeDesignerDefinition(Builder builder) {
        this.emoji = builder.emoji;
        this.typeLabel = builder.typeLabel;
        this.description = builder.description;
        this.palette = builder.palette;
        this.inlineProperties = builder.inlineProperties == null
                ? List.of()
                : List.copyOf(builder.inlineProperties);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getEmoji() {
        return emoji;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public String getDescription() {
        return description;
    }

    public NodePaletteDesignerDefinition getPalette() {
        return palette;
    }

    public List<InlinePropertyDefinition> getInlineProperties() {
        return inlineProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodeTypeDesignerDefinition that)) {
            return false;
        }
        return Objects.equals(emoji, that.emoji)
                && Objects.equals(typeLabel, that.typeLabel)
                && Objects.equals(description, that.description)
                && Objects.equals(palette, that.palette)
                && Objects.equals(inlineProperties, that.inlineProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(emoji, typeLabel, description, palette, inlineProperties);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String emoji;
        private String typeLabel;
        private String description;
        private NodePaletteDesignerDefinition palette;
        private List<InlinePropertyDefinition> inlineProperties;

        public Builder emoji(String emoji) {
            this.emoji = emoji;
            return this;
        }

        public Builder typeLabel(String typeLabel) {
            this.typeLabel = typeLabel;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder palette(NodePaletteDesignerDefinition palette) {
            this.palette = palette;
            return this;
        }

        public Builder inlineProperties(List<InlinePropertyDefinition> inlineProperties) {
            this.inlineProperties = inlineProperties;
            return this;
        }

        public Builder inlineProperty(InlinePropertyDefinition inlineProperty) {
            if (inlineProperty == null) {
                return this;
            }
            if (this.inlineProperties == null) {
                this.inlineProperties = new ArrayList<>();
            }
            this.inlineProperties.add(inlineProperty);
            return this;
        }

        public NodeTypeDesignerDefinition build() {
            return new NodeTypeDesignerDefinition(this);
        }
    }
}
