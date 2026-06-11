package org.olo.spi.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeCapabilitiesTest {

    @Test
    void omitsDeviationsWhenEffectiveMatchesInheritedDefaults() {
        assertThat(RuntimeCapabilities.materializeDeviations(
                        List.of(RuntimeCapability.DEBUG, RuntimeCapability.REPLAY)))
                .isNull();
    }

    @Test
    void materializesExtrasOnly() {
        assertThat(RuntimeCapabilities.materializeDeviations(List.of(
                        RuntimeCapability.DEBUG,
                        RuntimeCapability.REPLAY,
                        RuntimeCapability.CHECKPOINT,
                        RuntimeCapability.TIMEOUT,
                        RuntimeCapability.RETRY)))
                .containsExactly("CHECKPOINT", "TIMEOUT", "RETRY");
    }

    @Test
    void resolvesInheritedDefaultsWhenCapabilitiesOmitted() {
        assertThat(RuntimeCapabilities.resolveEffectiveNames(null))
                .containsExactly("DEBUG", "REPLAY");
    }

    @Test
    void resolvesDeviationsIntoEffectiveCapabilities() {
        assertThat(RuntimeCapabilities.resolveEffectiveNames(
                        List.of("CHECKPOINT", "TIMEOUT", "RETRY")))
                .containsExactly("DEBUG", "REPLAY", "CHECKPOINT", "TIMEOUT", "RETRY");
    }

    @Test
    void resolvesReplacementWhenOptingOutOfDefaults() {
        assertThat(RuntimeCapabilities.resolveEffectiveNames(List.of("REPLAY")))
                .containsExactly("REPLAY");
    }
}
