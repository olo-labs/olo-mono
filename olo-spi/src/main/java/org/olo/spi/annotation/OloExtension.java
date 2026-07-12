/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an OLO runtime extension for discovery (classpath scanning or documentation).
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OloExtension {

    /**
     * Optional human-readable name.
     */
    String name() default "";

    /**
     * Optional description for tooling and catalogs.
     */
    String description() default "";
}
