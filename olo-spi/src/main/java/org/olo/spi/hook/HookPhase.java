package org.olo.spi.hook;

/**
 * Lifecycle phase for a {@link Hook}, aligned with workflow hook definitions.
 */
public enum HookPhase {
    PRE,
    ON_ERROR,
    FINALLY
}
