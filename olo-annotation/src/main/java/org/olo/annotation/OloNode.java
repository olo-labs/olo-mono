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

    OloPort[] inputs() default {};

    OloPort[] outputs() default {};

    OloProperty[] configuration() default {};

    /** Semantic capability required inputs for planners. */
    String[] capabilityInputs() default {};

    /** Semantic capability required outputs for planners. */
    String[] capabilityOutputs() default {};
}
