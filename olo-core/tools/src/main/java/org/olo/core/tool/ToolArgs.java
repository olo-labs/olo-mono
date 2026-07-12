/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool;

import java.util.Map;

final class ToolArgs {

    private ToolArgs() {
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
}
