package org.olo.core.node;

/**
 * Node type tokens for built-in {@link org.olo.spi.node.Node} implementations.
 * <p>
 * Format: {@code {provider}:{localType}} — globally unique across community plugins.
 */
public final class CoreNodeTypes {

    public static final String PROVIDER = "olo-core";

    public static final String PROMPT = "olo-core:PROMPT";
    public static final String AGENT = "olo-core:AGENT";
    public static final String PARALLEL = "olo-core:PARALLEL";
    public static final String LOOP = "olo-core:LOOP";
    public static final String SWITCH = "olo-core:SWITCH";
    public static final String APPROVAL = "olo-core:APPROVAL";

    private CoreNodeTypes() {
    }
}
