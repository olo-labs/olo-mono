package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Machine-readable semantic contract for AI planners and strict validators.
 * <p>
 * Omitted from catalog when neither schema is set.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ContractDescriptor {

    public JsonNode inputSchema;
    public JsonNode outputSchema;
}
