/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation;

/**
 * Authoring token for {@link OloProperty} fields. Catalog emission maps to JSON {@code type} +
 * {@code ui.widget} via {@link org.olo.spi.catalog.ParameterSchemaMapping} (same model as workflow parameters).
 */
public enum OloPropertyType {
    STRING,
    TEXTAREA,
    NUMBER,
    BOOLEAN,
    ENUM,
    JSON,
    /** Masked credential or API key field. */
    SECRET,
    /** Ordered list of values (JSON array in configuration). */
    ARRAY,
    /** Structured object (JSON object in configuration). */
    OBJECT,
    /** Source code or expression editor. */
    CODE,
    /** Cron schedule expression. */
    CRON,
    /** LLM / embedding model picker (Studio-resolved). */
    MODEL_SELECTOR
}
