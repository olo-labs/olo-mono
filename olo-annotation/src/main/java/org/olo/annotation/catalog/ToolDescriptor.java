package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Catalog entry for a {@link org.olo.spi.tool.Tool} implementation. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ToolDescriptor extends CatalogComponent {

    public List<PortDescriptor> inputs;
    public List<PortDescriptor> outputs;
    public List<ParameterDescriptor> parameters;

    /** Machine-readable JSON Schema contract. Omitted when unset. */
    public ContractDescriptor contract;
}
