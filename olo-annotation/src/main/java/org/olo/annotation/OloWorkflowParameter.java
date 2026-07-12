/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation;

import org.olo.spi.catalog.ParameterWidget;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

/**
 * Workflow-level tuning parameter with Studio UI metadata ({@code parameters} on workflow presets).
 */
@Documented
@Target(ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OloWorkflowParameter {

    String name();

    /** Display label in the parameter panel (falls back to {@link #name()} when blank). */
    String label() default "";

    /**
     * JSON Schema-style value type written as {@code type} on the preset ({@code string}, {@code number},
     * {@code integer}, {@code boolean}).
     */
    String type() default "string";

    String description() default "";

    /** End-user guidance shown below the label. */
    String help() default "";

    String placeholder() default "";

    /**
     * Parameter panel section. Recommended: {@code General}, {@code Model Settings}, {@code Advanced}.
     */
    String group() default "Model Settings";

    int order() default Integer.MAX_VALUE;

    boolean required() default false;

    /** Minimum string length; {@code -1} omits from catalog. */
    int minLength() default -1;

    /** Maximum string length; {@code -1} omits from catalog. */
    int maxLength() default -1;

    /** Default value as a string literal (parsed per {@link #type()}). */
    String defaultValue() default "";

    /** Minimum inclusive bound for numeric parameters; omit when unset ({@code Double.NaN}). */
    double minimum() default Double.NaN;

    /** Maximum inclusive bound for numeric parameters; omit when unset ({@code Double.NaN}). */
    double maximum() default Double.NaN;

    /** Step increment for numeric inputs and sliders; omit when unset ({@code Double.NaN}). */
    double step() default Double.NaN;

    ParameterWidget widget() default ParameterWidget.STRING;

    /**
     * Show this parameter only when sibling values match.
     * Each entry is {@code key=value}.
     */
    String[] visibleWhen() default {};
}
