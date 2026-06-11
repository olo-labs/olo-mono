package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Catalog entry for a {@link org.olo.spi.node.Node} implementation. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class NodeDescriptor extends CatalogComponent {

    public List<PortDescriptor> inputs;
    public List<PortDescriptor> outputs;
    /** Studio edge attachment rules. Omitted when platform defaults apply. */
    public ConnectionPolicyDescriptor connectionPolicy;
    public List<ParameterDescriptor> parameters;

    /** Machine-readable JSON Schema contract. Omitted when unset. */
    public ContractDescriptor contract;
}
