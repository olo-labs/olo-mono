package org.olo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares workflow preset parameters for Studio and {@code olo-configuration} presets.
 * Processed at compile time into {@link OloCatalogLocations#WORKFLOW_PRESETS_CATALOG}.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OloWorkflowPreset {

    /** Workflow preset id (e.g. {@code agent} for {@code agent.json}). */
    String id();

    /** Studio palette, search, and canvas defaults for this workflow preset. */
    OloDesigner designer() default @OloDesigner;

    /**
     * Ports for delegate-agent graph nodes created from this preset.
     * Defaults to {@link OloCanvasPorts#AGENT_PLUGIN}.
     */
    OloCanvasPorts canvasPorts() default OloCanvasPorts.AGENT_PLUGIN;

    /** Explicit canvas inputs; overrides {@link #canvasPorts()} when non-empty. */
    OloPort[] inputs() default {};

    /** Explicit canvas outputs; overrides {@link #canvasPorts()} when non-empty. */
    OloPort[] outputs() default {};

    OloWorkflowParameter[] parameters() default {};
}
