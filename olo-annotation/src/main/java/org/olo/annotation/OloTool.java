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

    /**
     * @deprecated use {@link #stability()} = {@link OloStability#EXPERIMENTAL}
     */
    @Deprecated
    boolean experimental() default false;

    OloProperty[] arguments() default {};

    OloProperty[] configuration() default {};

    String[] capabilityInputs() default {};

    String[] capabilityOutputs() default {};
}
