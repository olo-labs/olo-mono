package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * Merged node extension entry from an extension catalog.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NodeDescriptor {

    public String kind;
    public String id;
    public String version;
    public String provider;
    public String stability;
    public String name;
    public String description;
    public String category;
    public String emoji;
    public List<String> tags;
    public List<String> examples;
    public boolean featured;
    public boolean deprecated;
    public String implementationClass;
    public String spiInterface;
    public List<PortDescriptor> inputs;
    public List<PortDescriptor> outputs;
    public List<PropertyDescriptor> configuration;
    public Map<String, Object> capability;

    public String id() {
        return id;
    }
}
