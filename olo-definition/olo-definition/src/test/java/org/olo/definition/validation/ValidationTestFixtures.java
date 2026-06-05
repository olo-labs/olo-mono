package org.olo.definition.validation;

import org.olo.definition.capability.CapabilityDefinition;

/**
 * Shared capability blocks for workflow validation tests.
 */
public final class ValidationTestFixtures {

    private ValidationTestFixtures() {
    }

    public static CapabilityDefinition minimalCapability() {
        return CapabilityDefinition.builder()
                .name("Test Workflow")
                .description("Test workflow for structural validation")
                .addInput("input")
                .addOutput("output")
                .build();
    }
}
