package org.olo.definition.edge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Directed connection between two node ports in a workflow graph.
 */
@JsonDeserialize(builder = EdgeDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class EdgeDefinition {

    private final String sourceNodeId;
    private final String sourcePort;
    private final String targetNodeId;
    private final String targetPort;

    private EdgeDefinition(Builder builder) {
        this.sourceNodeId = builder.sourceNodeId;
        this.sourcePort = builder.sourcePort;
        this.targetNodeId = builder.targetNodeId;
        this.targetPort = builder.targetPort;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public String getSourcePort() {
        return sourcePort;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public String getTargetPort() {
        return targetPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EdgeDefinition that)) {
            return false;
        }
        return Objects.equals(sourceNodeId, that.sourceNodeId)
                && Objects.equals(sourcePort, that.sourcePort)
                && Objects.equals(targetNodeId, that.targetNodeId)
                && Objects.equals(targetPort, that.targetPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceNodeId, sourcePort, targetNodeId, targetPort);
    }

    @Override
    public String toString() {
        return "EdgeDefinition{"
                + sourceNodeId
                + (sourcePort != null ? ":" + sourcePort : "")
                + " -> "
                + targetNodeId
                + (targetPort != null ? ":" + targetPort : "")
                + "}";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String sourceNodeId;
        private String sourcePort;
        private String targetNodeId;
        private String targetPort;

        public Builder sourceNodeId(String sourceNodeId) {
            this.sourceNodeId = sourceNodeId;
            return this;
        }

        public Builder sourcePort(String sourcePort) {
            this.sourcePort = sourcePort;
            return this;
        }

        public Builder targetNodeId(String targetNodeId) {
            this.targetNodeId = targetNodeId;
            return this;
        }

        public Builder targetPort(String targetPort) {
            this.targetPort = targetPort;
            return this;
        }

        public EdgeDefinition build() {
            Objects.requireNonNull(sourceNodeId, "sourceNodeId is required");
            Objects.requireNonNull(targetNodeId, "targetNodeId is required");
            return new EdgeDefinition(this);
        }
    }
}
