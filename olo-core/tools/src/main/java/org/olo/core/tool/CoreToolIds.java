package org.olo.core.tool;

/**
 * Registry ids for built-in {@link org.olo.spi.tool.Tool} implementations.
 * <p>
 * Format: {@code {provider}:{localId}} — globally unique across community plugins.
 */
public final class CoreToolIds {

    public static final String PROVIDER = "olo-core";

    public static final String HTTP = "olo-core:http-tool";
    public static final String CALCULATOR = "olo-core:calculator";
    public static final String WEB_SEARCH = "olo-core:web-search";
    public static final String LOG_READER = "olo-core:log-reader";
    public static final String CPU_USAGE = "olo-core:cpu-usage";
    public static final String MEMORY_USAGE = "olo-core:memory-usage";
    public static final String NUMERIC_METRIC = "olo-core:numeric-metric";
    public static final String RECENTLY_CHANGED_CODE = "olo-core:recently-changed-code";

    private CoreToolIds() {
    }
}
