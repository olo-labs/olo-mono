/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a Temporal task queue bound to a workflow type for Studio catalog generation.
 */
@Documented
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface OloQueueBinding {

    /** Temporal task queue name (e.g. {@code oloQueue1}). */
    String name();

    /** Display label in Studio and run dialogs. */
    String label();

    /** Optional description shown in tooling. */
    String description() default "";
}
