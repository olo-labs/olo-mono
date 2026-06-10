package org.olo.core.node;

import java.util.Map;

/**
 * Small helpers for reading node configuration and input maps.
 */
final class NodeConfig {

    private NodeConfig() {
    }

    static String string(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || map.isEmpty()) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text;
    }

    static int integer(Map<String, Object> map, String key, int defaultValue) {
        if (map == null || map.isEmpty()) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
