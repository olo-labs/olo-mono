package org.olo.annotation.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloStability;
import org.olo.annotation.OloWorkflowParameter;
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

    private static final ObjectMapper JSON = new ObjectMapper();

    private CatalogDefaults() {
    }

    /** Parses a JSON Schema string; returns {@code null} when blank. */
    static JsonNode parseJsonSchema(String json) throws IllegalArgumentException {
        String value = blankToNull(json);
        if (value == null) {
            return null;
        }
        try {
            JsonNode node = JSON.readTree(value);
            if (!node.isObject()) {
                throw new IllegalArgumentException("JSON Schema must be a JSON object");
            }
            return node;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON Schema: " + e.getMessage(), e);
        }
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

    static String materializeWorkflowParameterLabel(OloWorkflowParameter parameter) {
        String label = blankToNull(parameter.label());
        return label != null ? label : humanizeIdentifier(parameter.name());
    }

    static Double optionalDouble(double value) {
        return Double.isNaN(value) ? null : value;
    }

    static Object parseWorkflowParameterDefault(OloWorkflowParameter parameter) {
        return parseParameterDefault(parameter.type(), parameter.defaultValue());
    }

    static Object parsePropertyDefault(String jsonType, String rawDefault) {
        return parseParameterDefault(jsonType, rawDefault);
    }

    private static Object parseParameterDefault(String jsonType, String rawDefault) {
        String raw = blankToNull(rawDefault);
        if (raw == null || jsonType == null || jsonType.isBlank()) {
            return null;
        }
        return switch (jsonType) {
            case "integer" -> Integer.parseInt(raw);
            case "number" -> parseNumberDefault(raw);
            case "boolean" -> Boolean.parseBoolean(raw);
            default -> raw;
        };
    }

    private static Number parseNumberDefault(String raw) {
        if (raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0) {
            return Double.parseDouble(raw);
        }
        return Integer.parseInt(raw);
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

    /** Materializes {@code runtime.contractVersion}; defaults to {@code "1.0"} when blank. */
    static String materializeRuntimeContractVersion(String contractVersion) {
        String value = blankToNull(contractVersion);
        return value != null ? value : "1.0";
    }

    /** Parses ISO-8601 duration (e.g. {@code PT30S}); returns {@code null} when blank. */
    static String materializeIsoDuration(String duration) throws IllegalArgumentException {
        String value = blankToNull(duration);
        if (value == null) {
            return null;
        }
        try {
            java.time.Duration.parse(value);
            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ISO-8601 duration: " + e.getMessage(), e);
        }
    }

    /**
     * Prefixes {@code localId} with {@code provider} when the id is not already namespaced.
     * Convention: {@code olo-core:http-tool}, {@code olo-core:PROMPT}.
     */
    static String materializeGlobalId(String localId, String provider) {
        if (localId == null || localId.isBlank()) {
            return localId;
        }
        if (localId.contains(":")) {
            return localId;
        }
        String namespace = blankToNull(provider);
        return namespace != null ? namespace + ":" + localId : localId;
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
