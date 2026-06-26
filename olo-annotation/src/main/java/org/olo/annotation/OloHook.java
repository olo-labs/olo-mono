package org.olo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an OLO hook implementation and supplies UI/planner metadata.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OloHook {

    /** Implementation id (maps to {@code HookActionDefinition.implementationId}). */
    String implementationId();

    /** Extension implementation version (semver) for Studio upgrade notices and compatibility checks. */
    String version() default "1.0.0";

    /**
     * Owning plugin or distribution id. When blank, materialized from {@code -Aolo.catalog.provider}.
     */
    String provider() default "";

    String name();

    String description() default "";

    String category() default "observability";

    String emoji() default "";

    OloHookPhase[] phases() default {OloHookPhase.PRE, OloHookPhase.ON_ERROR, OloHookPhase.FINALLY};

    String[] tags() default {};

    /** Mark as deprecated; keep available but show "⚠ Deprecated" in the UI. */
    boolean deprecated() default false;

    OloStability stability() default OloStability.STABLE;

    /** Studio palette, search, and canvas defaults. */
    OloDesigner designer() default @OloDesigner;

    /**
     * @deprecated use {@link #stability()} = {@link OloStability#EXPERIMENTAL}
     */
    @Deprecated
    boolean experimental() default false;

    /** Graph canvas port profile; hooks default to {@link OloCanvasPorts#CAPABILITY_PLUGIN}. */
    OloCanvasPorts canvasPorts() default OloCanvasPorts.CAPABILITY_PLUGIN;

    /** Explicit canvas inputs; overrides {@link #canvasPorts()} when non-empty. */
    OloPort[] inputs() default {};

    /** Explicit canvas outputs; overrides {@link #canvasPorts()} when non-empty. */
    OloPort[] outputs() default {};
}
