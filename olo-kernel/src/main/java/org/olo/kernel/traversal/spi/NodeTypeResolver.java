/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.spi;

/**
 * Maps workflow definition node types to SPI {@link org.olo.spi.node.Node} type tokens.
 */
public interface NodeTypeResolver {

    String resolve(String definitionNodeType);
}
