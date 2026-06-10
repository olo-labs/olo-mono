package org.olo.bootstrap.model;

import org.olo.definition.workflow.WorkflowDefinition;

import java.util.Objects;

/**
 * A workflow definition loaded from disk together with provenance metadata.
 */
public final class CachedWorkflowDefinition {

    private final String sourcePath;
    private final WorkflowDefinition definition;

    public CachedWorkflowDefinition(String sourcePath, WorkflowDefinition definition) {
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public WorkflowDefinition getDefinition() {
        return definition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CachedWorkflowDefinition that)) {
            return false;
        }
        return sourcePath.equals(that.sourcePath) && definition.equals(that.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourcePath, definition);
    }
}
