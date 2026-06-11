package org.olo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

/**
 * Configuration or argument field exposed in the workflow/tool editor UI.
 */
@Documented
@Target(ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OloProperty {

    String name();

    /** Display label in the editor (falls back to {@link #name()} when blank). */
    String label() default "";

    /** Editor control; emitted as JSON {@code type} + {@code ui.widget} in catalogs. */
    OloPropertyType type() default OloPropertyType.STRING;

    /** Developer/catalog-oriented field summary (tooling, docs, codegen). */
    String description() default "";

    /** End-user guidance shown in the property panel below the label. */
    String help() default "";

    /** Placeholder text inside the input control. */
    String placeholder() default "";

    /**
     * Property panel section. Free text; use recommended names exactly:
     * {@code General}, {@code Advanced}, {@code Security}, {@code Model Settings}.
     */
    String group() default "General";

    /** Sort order within a group; lower values appear first. */
    int order() default Integer.MAX_VALUE;

    boolean required() default false;

    /** Minimum string length; {@code -1} omits from catalog. */
    int minLength() default -1;

    /** Maximum string length; {@code -1} omits from catalog. */
    int maxLength() default -1;

    /** Minimum inclusive bound for numeric fields; {@code Double.NaN} omits from catalog. */
    double minimum() default Double.NaN;

    /** Maximum inclusive bound for numeric fields; {@code Double.NaN} omits from catalog. */
    double maximum() default Double.NaN;

    /** Step increment for numeric fields; {@code Double.NaN} omits from catalog. */
    double step() default Double.NaN;

    String defaultValue() default "";

    String[] enumValues() default {};

    /** Mask input in the UI (e.g. API keys). */
    boolean secret() default false;

    /**
     * Use-case bullets shown under the field (e.g. "Summarize document").
     * Not sample configuration text — use {@link #placeholder()} or {@link #defaultValue()}.
     */
    String[] examples() default {};

    /**
     * Show this field only when sibling configuration values match.
     * Each entry is {@code key=value} (e.g. {@code "method=POST"}).
     */
    String[] visibleWhen() default {};
}
