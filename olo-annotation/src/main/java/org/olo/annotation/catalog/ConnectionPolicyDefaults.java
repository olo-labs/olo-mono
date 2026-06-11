package org.olo.annotation.catalog;

/**
 * Cardinality baselines for {@code defaults.connectionPolicy} and node deviations.
 */
public final class ConnectionPolicyDefaults {

    /** No limit on incoming or outgoing edges. */
    public static final int UNLIMITED = -1;

    public static final int DEFAULT_MAX_INPUTS = UNLIMITED;
    public static final int DEFAULT_MAX_OUTPUTS = UNLIMITED;

    private ConnectionPolicyDefaults() {}
}
