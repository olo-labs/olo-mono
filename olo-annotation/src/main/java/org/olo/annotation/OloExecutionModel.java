package org.olo.annotation;

/**
 * How an extension is scheduled by the workflow orchestrator (catalog metadata).
 * <p>
 * Distinct from JVM bindings in {@code runtime.json} and from workflow-graph
 * {@code executionKind} in {@code olo-definition}.
 */
public enum OloExecutionModel {

    /** Execute inside the current workflow (in-process, deterministic step). */
    INLINE,

    /** Execute as a Temporal Activity (or equivalent schedulable unit). */
    ACTIVITY,

    /** Execute as a child workflow boundary. */
    CHILD_WORKFLOW,

    /** Delegate to an external system outside the workflow sandbox. */
    EXTERNAL
}
