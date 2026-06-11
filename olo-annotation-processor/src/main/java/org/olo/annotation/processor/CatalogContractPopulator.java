package org.olo.annotation.processor;

import com.fasterxml.jackson.databind.JsonNode;
import org.olo.annotation.catalog.ContractDescriptor;

/** Materializes {@link ContractDescriptor} from extension annotations. */
final class CatalogContractPopulator {

    private CatalogContractPopulator() {
    }

    static ContractDescriptor create(String inputSchema, String outputSchema) {
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
