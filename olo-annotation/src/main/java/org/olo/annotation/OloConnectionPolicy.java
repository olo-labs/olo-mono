/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation;

import org.olo.annotation.catalog.ConnectionPolicyDefaults;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

/**
 * Canvas edge attachment cardinality for a node type in Studio.
 * <p>
 * Omitted from catalog when both values match platform defaults ({@code -1} / {@code -1}).
 */
@Documented
@Target(ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OloConnectionPolicy {

    /** Maximum incoming edges; {@link ConnectionPolicyDefaults#UNLIMITED} ({@code -1}) for no limit. */
    int maxInputs() default ConnectionPolicyDefaults.DEFAULT_MAX_INPUTS;

    /** Maximum outgoing edges; {@link ConnectionPolicyDefaults#UNLIMITED} ({@code -1}) for no limit. */
    int maxOutputs() default ConnectionPolicyDefaults.DEFAULT_MAX_OUTPUTS;
}
