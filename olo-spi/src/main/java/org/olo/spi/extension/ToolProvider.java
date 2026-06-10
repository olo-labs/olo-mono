package org.olo.spi.extension;

import org.olo.spi.tool.Tool;

import java.util.Optional;

/**
 * Supplies {@link Tool} implementations by registry id.
 */
public interface ToolProvider extends ExtensionPoint {

    /**
     * Tool or runtime binding id this provider handles.
     */
    String toolId();

    /**
     * Returns the tool implementation, if supported.
     */
    Optional<Tool> getTool();
}
