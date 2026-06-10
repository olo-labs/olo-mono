package org.olo.core.hook;

/**
 * Implementation ids for built-in {@link org.olo.spi.hook.Hook} implementations.
 */
public final class CoreHookIds {

    public static final String LOGGING = "logging-hook";
    public static final String METRICS = "metrics-hook";
    public static final String TRACING = "tracing-hook";

    private CoreHookIds() {
    }
}
