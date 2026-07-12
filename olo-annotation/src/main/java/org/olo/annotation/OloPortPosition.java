/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation;

/**
 * Canvas side for a node port handle. {@link #DEFAULT} infers from input vs output in catalog generation.
 */
public enum OloPortPosition {

    DEFAULT,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM
}
