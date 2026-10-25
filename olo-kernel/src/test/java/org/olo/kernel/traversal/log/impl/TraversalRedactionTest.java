// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.kernel.traversal.log.impl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TraversalRedactionTest {
    @Test
    void privateValuesAreNeverStringifiedForDiagnostics() {
        var privateValue = new Object() {
            @Override public String toString() {
                throw new AssertionError("Private context must not be inspected by logging");
            }
        };
        assertThat(TraversalCompletionLogger.formatValue(privateValue)).isEqualTo("[redacted]");
        assertThat(TraversalCompletionLogger.formatValue(null)).isEqualTo("null");
    }
}
