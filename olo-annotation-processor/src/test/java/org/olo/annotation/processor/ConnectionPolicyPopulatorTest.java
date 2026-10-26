/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor;

import org.junit.jupiter.api.Test;
import org.olo.annotation.OloConnectionPolicy;
import org.olo.annotation.catalog.ConnectionPolicyDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionPolicyPopulatorTest {

    @OloConnectionPolicy(maxInputs = 1, maxOutputs = -1)
    private @interface SwitchPolicy {}

    @Test
    void emitsCardinalityWhenNonDefault() throws Exception {
        OloConnectionPolicy policy = AnnotationTestValues.of(OloConnectionPolicy.class,
                java.util.Map.of("maxInputs", 1, "maxOutputs", -1));
        ConnectionPolicyDescriptor descriptor = ConnectionPolicyPopulator.from(policy);
        assertThat(descriptor.maxInputs).isEqualTo(1);
        assertThat(descriptor.maxOutputs).isEqualTo(-1);
    }
}
