package org.olo.spi.extension;

/**
 * Marker for discoverable runtime extension providers (nodes, tools, hooks).
 * <p>
 * Implementations are registered by the runtime engine or via {@link java.util.ServiceLoader}.
 */
public interface ExtensionPoint {

    /**
     * Stable provider id for logging and diagnostics.
     */
    String providerId();
}
