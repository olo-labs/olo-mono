package org.olo.spi.catalog;

/**
 * Maps legacy {@code OloPropertyType} authoring tokens to the unified parameter schema:
 * JSON Schema-style {@code type} + {@link ParameterWidget} in {@code ui.widget}.
 */
public final class ParameterSchemaMapping {

    public record MappedParameter(String jsonType, ParameterWidget widget) {}

    private ParameterSchemaMapping() {}

    public static MappedParameter fromPropertyType(String propertyTypeName) {
        return fromPropertyType(propertyTypeName, false);
    }

    public static MappedParameter fromPropertyType(String propertyTypeName, boolean secret) {
        if (secret) {
            return new MappedParameter("string", ParameterWidget.SECRET);
        }
        if (propertyTypeName == null || propertyTypeName.isBlank()) {
            return new MappedParameter("string", ParameterWidget.STRING);
        }
        return switch (propertyTypeName.trim().toUpperCase()) {
            case "STRING" -> new MappedParameter("string", ParameterWidget.STRING);
            case "TEXTAREA" -> new MappedParameter("string", ParameterWidget.TEXTAREA);
            case "NUMBER" -> new MappedParameter("number", ParameterWidget.NUMBER);
            case "BOOLEAN" -> new MappedParameter("boolean", ParameterWidget.BOOLEAN);
            case "ENUM" -> new MappedParameter("enum", ParameterWidget.SELECT);
            case "JSON" -> new MappedParameter("object", ParameterWidget.JSON);
            case "SECRET" -> new MappedParameter("string", ParameterWidget.SECRET);
            case "ARRAY" -> new MappedParameter("array", ParameterWidget.MULTI_SELECT);
            case "OBJECT" -> new MappedParameter("object", ParameterWidget.JSON);
            case "CODE" -> new MappedParameter("string", ParameterWidget.CODE);
            case "CRON" -> new MappedParameter("string", ParameterWidget.CRON);
            case "MODEL_SELECTOR" -> new MappedParameter("string", ParameterWidget.MODEL_SELECTOR);
            default -> new MappedParameter("string", ParameterWidget.STRING);
        };
    }
}
