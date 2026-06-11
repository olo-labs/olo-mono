package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Studio canvas edge attachment cardinality for a node type.
 * {@link ConnectionPolicyDefaults#UNLIMITED} ({@code -1}) means no limit.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public final class ConnectionPolicyDescriptor {

    public Integer maxInputs;
    public Integer maxOutputs;

    public static Map<String, Object> catalogDefaults() {
        return Map.of(
                "maxInputs", ConnectionPolicyDefaults.DEFAULT_MAX_INPUTS,
                "maxOutputs", ConnectionPolicyDefaults.DEFAULT_MAX_OUTPUTS);
    }
}
