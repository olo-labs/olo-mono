package org.olo.definition.configuration;

import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves committed paths under {@code olo-configuration/} for tests. */
public final class ConfigurationFixturePaths {

    private ConfigurationFixturePaths() {
    }

    public static Path resolveConfigurationRoot() {
        String property = System.getProperty("olo.configuration.dir");
        if (property != null && !property.isBlank()) {
            return Path.of(property).normalize().toAbsolutePath();
        }
        for (String candidate :
                new String[] {"olo-configuration", "../olo-configuration", "../olo-definition/olo-configuration"}) {
            Path path = Path.of(candidate).normalize().toAbsolutePath();
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return Path.of("olo-configuration").toAbsolutePath();
    }

    public static Path resolveFixture(String fileName) {
        Path fixture = resolveConfigurationRoot().resolve("fixtures").resolve(fileName);
        if (!Files.isRegularFile(fixture)) {
            throw new org.opentest4j.TestAbortedException("configuration fixture not found: " + fixture);
        }
        return fixture;
    }

    public static Path resolveCollectionFolder(String folderName) {
        Path root = resolveConfigurationRoot();
        for (String candidate : new String[] {folderName, folderName.replace("travel-planner", "travel-Planner")}) {
            Path path = root.resolve(candidate);
            if (Files.isDirectory(path)) {
                return path.toAbsolutePath().normalize();
            }
        }
        throw new org.opentest4j.TestAbortedException("configuration collection not found: " + folderName);
    }
}
