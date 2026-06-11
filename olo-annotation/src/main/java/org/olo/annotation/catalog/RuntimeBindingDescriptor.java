package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps a globally unique extension id to its JVM implementation.
 * <p>
 * Emitted in {@code META-INF/olo/catalog/runtime.json} — not merged into Studio-facing catalog JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuntimeBindingDescriptor {

    public String kind;
    public String id;
    public String implementationClass;
    public String spiInterface;

    public String id() {
        return id;
    }
}
