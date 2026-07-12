/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor;

import com.fasterxml.jackson.databind.JsonNode;
import org.olo.annotation.catalog.ContractDescriptor;

/** Materializes {@link ContractDescriptor} from extension annotations. */
public final class CatalogContractPopulator {

    private CatalogContractPopulator() {
    }

    public static ContractDescriptor create(String inputSchema, String outputSchema) {
        JsonNode input = CatalogDefaults.parseJsonSchema(inputSchema);
        JsonNode output = CatalogDefaults.parseJsonSchema(outputSchema);
        if (input == null && output == null) {
            return null;
        }
        ContractDescriptor contract = new ContractDescriptor();
        contract.inputSchema = input;
        contract.outputSchema = output;
        return contract;
    }
}
