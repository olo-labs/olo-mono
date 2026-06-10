package org.olo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

/**
 * Port contract for node inputs/outputs in the workflow editor.
 */
@Documented
@Target(ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OloPort {

    String id();

    String name() default "";

    String schema() default "any";

    boolean required() default false;

    /** Help text shown under the port label in the workflow editor. */
    String description() default "";
}
