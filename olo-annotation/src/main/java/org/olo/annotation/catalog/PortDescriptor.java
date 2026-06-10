package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Input or output port on a node extension.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PortDescriptor {

    public String id;
    public String name;
    public String schema;
    public boolean required;
    public String description;
}
