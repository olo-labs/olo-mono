package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Input or output port on a node extension.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PortDescriptor {

    public String id;
    public String name;
    public String schema;
    public boolean required;
    public String description;
    public PortUiDescriptor ui;
}
