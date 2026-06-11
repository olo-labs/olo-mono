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

    OloWorkflowParameter[] parameters() default {};
}
