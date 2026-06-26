package org.olo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a Temporal workflow type for catalog generation and Studio selection.
 * <p>
 * Annotate the {@code @WorkflowInterface} type. Processed at compile time into
 * {@link OloCatalogLocations#WORKFLOW_TYPES_CATALOG}.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OloWorkflowType {

    /**
     * Workflow type id used in configuration JSON ({@code workflowType}) and Temporal client stubs.
     * Defaults to {@link #temporalMethod()} when blank.
     */
    String id();

    /** Display label in Studio. */
    String label();

    /** Optional description for tooling. */
    String description() default "";

    /**
     * Temporal {@code @WorkflowMethod} name. When blank, the processor reads it from the annotated type.
     */
    String temporalMethod() default "";

    /** Task queues that execute this workflow type. */
    OloQueueBinding[] queues() default {};
}
