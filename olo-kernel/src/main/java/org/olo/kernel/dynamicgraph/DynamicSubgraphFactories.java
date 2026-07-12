/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.dynamicgraph;

import org.olo.kernel.dynamicgraph.impl.DefaultDynamicSubgraphMerger;

public final class DynamicSubgraphFactories {

    private DynamicSubgraphFactories() {
    }

    public static DynamicSubgraphMerger defaultMerger() {
        return new DefaultDynamicSubgraphMerger();
    }
}
