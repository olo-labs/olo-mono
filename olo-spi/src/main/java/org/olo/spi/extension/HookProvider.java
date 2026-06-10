package org.olo.spi.extension;

import org.olo.spi.hook.Hook;

import java.util.Optional;

/**
 * Supplies {@link Hook} implementations by {@code implementationId}.
 */
public interface HookProvider extends ExtensionPoint {

    /**
     * Hook implementation id from the workflow graph.
     */
    String implementationId();

    /**
     * Returns the hook implementation, if supported.
     */
    Optional<Hook> getHook();
}
