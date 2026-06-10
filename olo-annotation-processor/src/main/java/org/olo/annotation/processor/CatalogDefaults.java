package org.olo.annotation.processor;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloStability;
import org.olo.annotation.processor.model.ExtensionCatalogDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Materializes annotation defaults into final catalog values. */
final class CatalogDefaults {

    static final String GENERATED_BY = "olo-annotation-processor";
    static final String GENERATED_BY_VERSION = "1.0.0";
    static final String DEFAULT_PROPERTY_GROUP = "General";

    private CatalogDefaults() {
    }

    static List<String> stringArray(String[] values) {
        return values == null || values.length == 0 ? List.of() : List.of(values);
    }

    /** Omits property-only collections from JSON when unset (see {@link CatalogJsonMapper}). */
    static List<String> optionalStringArray(String[] values) {
        return values == null || values.length == 0 ? null : List.of(values);
    }

    static String materializePortName(String id, String name) {
        String value = blankToNull(name);
        return value != null ? value : id;
    }

    static String materializePropertyLabel(OloProperty property) {
        String label = blankToNull(property.label());
        return label != null ? label : humanizeIdentifier(property.name());
    }

    /** Omitted from JSON when blank or the annotation default ({@code "General"}). */
    static String optionalPropertyGroup(String group) {
        String value = blankToNull(group);
        if (value == null || DEFAULT_PROPERTY_GROUP.equals(value)) {
            return null;
        }
        return value;
    }

    /** Omitted from JSON when the annotation default ({@link Integer#MAX_VALUE}) is used. */
    static Integer materializePropertyOrder(int order) {
        return order == Integer.MAX_VALUE ? null : order;
    }

    /**
     * Converts {@code maxIterations} → {@code Max Iterations}, {@code prompt} → {@code Prompt}.
     */
    static String humanizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String spaced =
                value.replace('_', ' ').replaceAll("([a-z])([A-Z])", "$1 $2").replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        String[] words = spaced.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(titleWord(words[i]));
        }
        return out.toString();
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    static String materializeVersion(String version) {
        String value = blankToNull(version);
        return value != null ? value : "1.0.0";
    }

    static String materializeProvider(String annotationProvider, String catalogProvider, String catalogModule) {
        String value = blankToNull(annotationProvider);
        if (value != null) {
            return value;
        }
        String provider = blankToNull(catalogProvider);
        if (provider != null) {
            return provider;
        }
        return blankToNull(catalogModule) != null ? catalogModule : "extensions";
    }

    static String serializeStability(OloStability stability, boolean legacyExperimental) {
        OloStability effective = stability;
        if (legacyExperimental && stability == OloStability.STABLE) {
            effective = OloStability.EXPERIMENTAL;
        }
        return effective.name().toLowerCase(Locale.ROOT);
    }

    static void applyDocumentHeader(
            ExtensionCatalogDocument document, String module, String catalogType, String generatedAt) {
        document.schemaVersion = "1.0";
        document.moduleId = module;
        document.catalogType = catalogType;
        document.generatedAt = generatedAt;
        document.generatedBy = GENERATED_BY;
        document.generatedByVersion = GENERATED_BY_VERSION;
    }

    static void applyMergedHeader(Map<String, Object> merged, String module, String generatedAt) {
        merged.put("schemaVersion", "1.0");
        merged.put("moduleId", module);
        merged.put("generatedAt", generatedAt);
        merged.put("generatedBy", GENERATED_BY);
        merged.put("generatedByVersion", GENERATED_BY_VERSION);
    }

    private static String titleWord(String word) {
        if (word.isEmpty()) {
            return word;
        }
        if (word.length() == 1) {
            return word.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase(Locale.ROOT);
    }
}
