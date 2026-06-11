package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.olo.spi.runtime.RuntimeCapability;
import org.olo.spi.runtime.RuntimeCapabilities;

import java.util.List;

/**
 * Orchestration hints for planners and workflow engines (Studio catalog).
 * <p>
 * Not JVM classpath bindings — those live in {@code META-INF/olo/catalog/runtime.json}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CatalogRuntimeDescriptor {

    /**
     * Runtime execution contract version (e.g. {@code "1.0"}) — distinct from descriptor {@code version}
     * (extension implementation semver).
     */
    @JsonAlias("apiVersion")
    public String contractVersion;

    /**
     * Primary scheduling primitive — Studio uses this to choose execution, replay, debugger, visualization,
     * and observability surfaces ({@code INLINE}, {@code ACTIVITY}, {@code CHILD_WORKFLOW}, {@code EXTERNAL}).
     */
    public String executionModel;

    /**
     * Capability deviations from {@link RuntimeCapabilities#INHERITED_CATALOG_DEFAULTS} — omitted when
     * effective matches defaults. Values are {@link RuntimeCapability} names.
     */
    public List<String> capabilities;

    /** Effective capability names after merging {@link #capabilities} with catalog defaults. */
    public List<String> effectiveCapabilityNames() {
        return RuntimeCapabilities.resolveEffectiveNames(capabilities);
    }

    /** Effective capabilities after merging {@link #capabilities} with catalog defaults. */
    public List<RuntimeCapability> effectiveCapabilities() {
        return RuntimeCapabilities.resolveEffective(capabilities);
    }

    /** ISO-8601 duration (e.g. {@code PT30S}). Omitted when unset. */
    public String defaultTimeout;

    /** {@link org.olo.annotation.OloRetryPolicy} name. Omitted when unset ({@code NONE}). */
    public String defaultRetryPolicy;
}
