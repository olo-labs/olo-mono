package org.olo.definition.configuration.travelplanner;

import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves the on-disk {@code olo-configuration/travel-planner/} root. */
final class TravelPlannerPaths {

    private TravelPlannerPaths() {
    }

    static Path resolveConfigurationRoot() {
        String property = System.getProperty("olo.travelPlanner.configuration.dir");
        if (property != null && !property.isBlank()) {
            return Path.of(property).normalize().toAbsolutePath();
        }
        for (String candidate :
                new String[] {"olo-configuration/travel-planner", "../olo-configuration/travel-planner"}) {
            Path path = Path.of(candidate).normalize().toAbsolutePath();
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return Path.of("olo-configuration/travel-planner").toAbsolutePath();
    }
}
