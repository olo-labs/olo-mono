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

    private CoreToolIds() {
    }
}
