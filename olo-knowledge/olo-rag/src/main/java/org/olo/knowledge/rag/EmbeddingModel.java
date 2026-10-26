// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.rag;

import java.util.List;

public interface EmbeddingModel {
    /** Immutable model artifact identity, including the operator-pinned digest. */
    String version();

    List<List<Double>> embed(List<String> texts) throws Exception;
}
