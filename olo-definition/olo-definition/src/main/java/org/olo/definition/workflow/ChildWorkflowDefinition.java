package org.olo.definition.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Declares a child workflow artifact composed or invoked by a parent workflow.
 */
@JsonDeserialize(builder = ChildWorkflowDefinition.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ChildWorkflowDefinition {

    private final String workflowId;
    private final String workflowVersion;

    private ChildWorkflowDefinition(Builder builder) {
        this.workflowId = builder.workflowId;
        this.workflowVersion = builder.workflowVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getWorkflowId() {
        return workflowId;
    }

    @JsonProperty("workflowVersion")
    public String getWorkflowVersion() {
        return workflowVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChildWorkflowDefinition that)) {
            return false;
        }
        return Objects.equals(workflowId, that.workflowId)
                && Objects.equals(workflowVersion, that.workflowVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflowId, workflowVersion);
    }

    @Override
    public String toString() {
        return "ChildWorkflowDefinition{workflowId='"
                + workflowId
                + "', workflowVersion='"
                + workflowVersion
                + "'}";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String workflowId;
        @JsonProperty("workflowVersion")
        private String workflowVersion;

        public Builder workflowId(String workflowId) {
            this.workflowId = workflowId;
            return this;
        }

        public Builder workflowVersion(String workflowVersion) {
            this.workflowVersion = workflowVersion;
            return this;
        }

        public ChildWorkflowDefinition build() {
            Objects.requireNonNull(workflowId, "workflowId is required");
            return new ChildWorkflowDefinition(this);
        }
    }
}
