/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.catalog;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Studio control widget for workflow {@code parameters.*.ui.widget} and related property forms.
 * Serialized as the enum name ({@code SLIDER}, not {@code slider}).
 */
public enum ParameterWidget {

    STRING,
    TEXTAREA,
    NUMBER,
    SLIDER,
    BOOLEAN,
    /** Yes/No button pair for human approval gates in chat and Studio. */
    APPROVAL_TOGGLE,
    SELECT,
    MULTI_SELECT,
    JSON,
    CODE,
    MODEL_SELECTOR,
    SECRET,
    CRON;

    private static final Map<String, ParameterWidget> LEGACY_ALIASES = Map.ofEntries(
            Map.entry("text", STRING),
            Map.entry("textarea", TEXTAREA),
            Map.entry("number", NUMBER),
            Map.entry("slider", SLIDER),
            Map.entry("switch", BOOLEAN),
            Map.entry("boolean", BOOLEAN),
            Map.entry("approval_toggle", APPROVAL_TOGGLE),
            Map.entry("yes_no", APPROVAL_TOGGLE),
            Map.entry("yes-no", APPROVAL_TOGGLE),
            Map.entry("select", SELECT),
            Map.entry("enum", SELECT),
            Map.entry("multi_select", MULTI_SELECT),
            Map.entry("json", JSON),
            Map.entry("code", CODE),
            Map.entry("model_selector", MODEL_SELECTOR),
            Map.entry("secret", SECRET),
            Map.entry("cron", CRON));

    /** Canonical catalog / JSON value ({@code name()}). */
    public String catalogValue() {
        return name();
    }

    /** All widgets for catalog {@code catalogMetadata.parameterWidgets}. */
    public static List<String> catalogValues() {
        return List.of(values()).stream().map(ParameterWidget::catalogValue).toList();
    }

    /**
     * Parses catalog or legacy lowercase widget strings. Returns {@code null} when unrecognized.
     */
    public static ParameterWidget parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            return valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return LEGACY_ALIASES.get(trimmed.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Parses and returns the canonical catalog value, or the original string when unknown.
     */
    public static String normalizeCatalogValue(String raw) {
        ParameterWidget widget = parse(raw);
        return widget != null ? widget.catalogValue() : raw;
    }
}
