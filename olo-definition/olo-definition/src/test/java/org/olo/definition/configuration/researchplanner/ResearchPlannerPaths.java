package org.olo.definition.configuration.researchplanner;

import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves the on-disk {@code olo-configuration/research-planner/} root. */
final class ResearchPlannerPaths {

    private ResearchPlannerPaths() {
    }

    static Path resolveConfigurationRoot() {
        String property = System.getProperty("olo.researchPlanner.configuration.dir");
        if (property != null && !property.isBlank()) {
            return Path.of(property).normalize().toAbsolutePath();
        }
        for (String candidate :
                new String[] {"olo-configuration/research-planner", "../olo-configuration/research-planner"}) {
            Path path = Path.of(candidate).normalize().toAbsolutePath();
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return Path.of("olo-configuration/research-planner").toAbsolutePath();
    }
}
