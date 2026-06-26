package org.olo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an OLO tool implementation and supplies UI/planner metadata.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OloTool {

    /** Registry id (maps to {@code ToolDefinition.id} / runtime binding {@code implementationId}). */
    String id();

    /** Extension implementation version (semver) for Studio upgrade notices and compatibility checks. */
    String version() default "1.0.0";

    /**
     * Owning plugin or distribution id. When blank, materialized from {@code -Aolo.catalog.provider}.
     */
    String provider() default "";

    String name();

    String description() default "";

    String category() default "tools";

    String emoji() default "";

    String[] tags() default {};

    /** Use-case lines for editor discovery — not configuration sample values. */
    String[] examples() default {};

    /**
     * Advisory hint for workflow editors. Not an OLO endorsement — editors may ignore
     * or combine with their own ranking.
     */
    boolean featured() default false;

    boolean deprecated() default false;

    OloStability stability() default OloStability.STABLE;

    /** Studio palette, search, and canvas defaults. */
    OloDesigner designer() default @OloDesigner;

    /**
     * @deprecated use {@link #stability()} = {@link OloStability#EXPERIMENTAL}
     */
    @Deprecated
    boolean experimental() default false;

    OloProperty[] arguments() default {};

    OloProperty[] configuration() default {};

    /**
     * Graph canvas port profile when {@link #inputs()} and {@link #outputs()} are empty.
     * Tools default to {@link OloCanvasPorts#CAPABILITY_PLUGIN}.
     */
    OloCanvasPorts canvasPorts() default OloCanvasPorts.CAPABILITY_PLUGIN;

    /** Explicit canvas inputs; overrides {@link #canvasPorts()} when non-empty. */
    OloPort[] inputs() default {};

    /** Explicit canvas outputs; overrides {@link #canvasPorts()} when non-empty. */
    OloPort[] outputs() default {};

    /**
     * JSON Schema object (as a string) describing semantic inputs for machine-readable planners.
     * Omitted from catalog when blank.
     */
    String capabilityInputSchema() default "";

    /**
     * JSON Schema object (as a string) describing semantic outputs for machine-readable planners.
     * Omitted from catalog when blank.
     */
    String capabilityOutputSchema() default "";

    /**
     * Runtime execution contract version emitted as {@code runtime.contractVersion} (e.g. {@code "1.0"}).
     * Distinct from {@link #version()} — extension implementation semver.
     */
    String runtimeContractVersion() default "1.0";

    /** How the orchestrator schedules this tool. */
    OloExecutionModel executionModel() default OloExecutionModel.ACTIVITY;

    /** Whether failed executions may be retried by the orchestrator. */
    boolean retryable() default false;

    /** Whether the orchestrator should apply timeout policies to this step. */
    boolean timeoutAware() default false;

    /** Default execution timeout as ISO-8601 duration (e.g. {@code PT30S}). Omitted when blank. */
    String defaultTimeout() default "";

    /** Default retry policy hint. Omitted from catalog when {@link OloRetryPolicy#NONE}. */
    OloRetryPolicy defaultRetryPolicy() default OloRetryPolicy.NONE;

    /** Whether the step may complete asynchronously (signal/callback). */
    boolean supportsAsyncCompletion() default false;

    /** Whether the orchestrator should expect activity heartbeats. */
    boolean supportsHeartbeat() default false;

    /** Studio debugger support for this tool type. Emits {@code DEBUG} on {@code runtime.capabilities}. */
    boolean supportsDebugging() default true;

    /** Workflow replay support for executions involving this tool type. Emits {@code REPLAY}. */
    boolean supportsReplay() default true;

    /** Checkpoint / time-travel support for this tool type. Emits {@code CHECKPOINT} when {@code true}. */
    boolean supportsCheckpointing() default false;
}
