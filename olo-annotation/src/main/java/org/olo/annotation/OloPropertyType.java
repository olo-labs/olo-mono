package org.olo.annotation;

/**
 * Editor control type for {@link OloProperty} fields.
 * <p>
 * Serialized in extension catalogs as the enum name (e.g. {@code "STRING"}).
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
