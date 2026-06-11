package org.olo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an OLO node implementation and supplies UI/planner metadata.
 * <p>
 * Processed at compile time by {@code olo-annotation-processor} into
 * {@value org.olo.annotation.OloCatalogLocations#CATALOG_DIR} resources for plug-and-play workflow editing.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OloNode {

    /** Node type token (maps to {@code NodeDefinition.type}). */
    String type();

    /**
     * Extension implementation version (semver) for Studio upgrade notices and compatibility checks.
     */
    String version() default "1.0.0";

    /**
     * Owning plugin or distribution id (e.g. {@code olo-core}, {@code acme-aws}). When blank, the
     * annotation processor materializes from {@code -Aolo.catalog.provider}.
     */
    String provider() default "";

    String name();

    String description() default "";

    String category() default "general";

    String emoji() default "";

    String[] tags() default {};

    /**
     * Use-case lines for editor discovery (e.g. "Summarize a document").
     * Not configuration sample text — use {@link OloProperty#placeholder()} for that.
     */
    String[] examples() default {};

    /**
     * Advisory hint for workflow editors (e.g. a "⭐ Recommended" palette section).
     * Not an OLO endorsement — editors may ignore or combine with their own ranking.
     */
    boolean featured() default false;

    /** Mark as deprecated; keep available but show "⚠ Deprecated" in the UI. */
    boolean deprecated() default false;

    /** Extension maturity for marketplace and compatibility UX. */
    OloStability stability() default OloStability.STABLE;

    /**
     * @deprecated use {@link #stability()} = {@link OloStability#EXPERIMENTAL}
     */
    @Deprecated
    boolean experimental() default false;

    /** Studio palette, search, and canvas defaults. */
    OloDesigner designer() default @OloDesigner;

    /**
     * @deprecated use {@link #designer()}
     */
    @Deprecated
    OloNodeShape nodeShape() default OloNodeShape.STANDARD;

    /**
     * @deprecated use {@link #designer()}{@code .width()}
     */
    @Deprecated
    int uiWidth() default 0;

    /**
     * @deprecated use {@link #designer()}{@code .height()}
     */
    @Deprecated
    int uiHeight() default 0;

    /** Canvas edge attachment rules for Studio. Omitted when both match platform defaults. */
    OloConnectionPolicy connectionPolicy() default @OloConnectionPolicy;

    OloPort[] inputs() default {};

    OloPort[] outputs() default {};

    OloProperty[] configuration() default {};

    /**
     * JSON Schema object (as a string) describing semantic inputs for machine-readable planners.
     * Omitted from catalog when blank. Example:
     * {@code {"type":"object","properties":{"query":{"type":"string"}},"required":["query"]}}
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

    /** How the orchestrator schedules this node. */
    OloExecutionModel executionModel() default OloExecutionModel.INLINE;

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

    /** Studio debugger support for this node type. Emits {@code DEBUG} on {@code runtime.capabilities}. */
    boolean supportsDebugging() default true;

    /** Workflow replay support for executions involving this node type. Emits {@code REPLAY}. */
    boolean supportsReplay() default true;

    /** Checkpoint / time-travel support for this node type. Emits {@code CHECKPOINT} when {@code true}. */
    boolean supportsCheckpointing() default false;
}
