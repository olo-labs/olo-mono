package org.olo.kernel.dynamicgraph;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves {@code olo-definition/olo-configuration/log/} for dynamic subgraph injection audit files.
 */
public final class DynamicSubgraphInjectionLogPaths {

    public static final String LOG_DIR_PROPERTY = "olo.configuration.log.dir";
    public static final String CURRENT_ACTIVE_DIR_PROPERTY = "olo.configuration.currentActive.dir";

    private DynamicSubgraphInjectionLogPaths() {
    }

    public static Path resolveCurrentActiveDirectory() {
        String property = System.getProperty(CURRENT_ACTIVE_DIR_PROPERTY);
        if (property != null && !property.isBlank()) {
            return Path.of(property).normalize().toAbsolutePath();
        }
        Path logDirectory = resolveLogDirectory();
        Path sibling = logDirectory.getParent().resolve("current-active");
        if (Files.isDirectory(sibling)) {
            return sibling;
        }
        for (String candidate :
                new String[] {
                    "olo-configuration/current-active",
                    "../olo-configuration/current-active",
                    "../olo-definition/olo-configuration/current-active",
                    "olo-definition/olo-configuration/current-active"
                }) {
            Path path = Path.of(candidate).normalize().toAbsolutePath();
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return sibling.toAbsolutePath();
    }

    public static Path resolveLogDirectory() {
        String property = System.getProperty(LOG_DIR_PROPERTY);
        if (property != null && !property.isBlank()) {
            return Path.of(property).normalize().toAbsolutePath();
        }
        for (String candidate :
                new String[] {
                    "olo-configuration/log",
                    "../olo-configuration/log",
                    "../olo-definition/olo-configuration/log",
                    "olo-definition/olo-configuration/log"
                }) {
            Path path = Path.of(candidate).normalize().toAbsolutePath();
            if (Files.isDirectory(path.getParent()) || Files.isDirectory(path)) {
                return path;
            }
        }
        return Path.of("olo-definition/olo-configuration/log").toAbsolutePath();
    }
}
