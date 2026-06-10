package org.olo.worker.config.model;

/**
 * Identifies where worker configuration was loaded from.
 * Additional source types (database, Redis, GitHub) can be added as implementations of
 * {@link org.olo.worker.config.source.ConfigurationSource}.
 */
public enum ConfigurationSourceType {
    FILE,
    DATABASE,
    REDIS,
    GITHUB
}
