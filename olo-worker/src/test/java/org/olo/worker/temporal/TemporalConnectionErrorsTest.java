/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.temporal;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;

class TemporalConnectionErrorsTest {

    @Test
    void detectsUnavailableStatus() {
        StatusRuntimeException error = Status.UNAVAILABLE.withDescription("io exception").asRuntimeException();
        assertThat(TemporalConnectionErrors.isServerUnavailable(error)).isTrue();
    }

    @Test
    void detectsConnectionRefusedCause() {
        RuntimeException error = new RuntimeException(
                "failed",
                new ConnectException("Connection refused: getsockopt"));
        assertThat(TemporalConnectionErrors.isServerUnavailable(error)).isTrue();
    }

    @Test
    void ignoresUnrelatedErrors() {
        assertThat(TemporalConnectionErrors.isServerUnavailable(new IllegalStateException("bad config"))).isFalse();
    }
}
