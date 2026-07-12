/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.step.handler;

/**
 * Resolves a {@link NodeTypeHandler} for a graph node type.
 */
public interface NodeTypeHandlerRegistry {

    NodeTypeHandler resolve(String nodeType);
}
