package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Configuration or argument field on a node or tool extension.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PropertyDescriptor {

    public String name;
    public String label;
    public String type;
    public String description;
    public String help;
    public String placeholder;
    public String group;
    public Integer order;
    public boolean required;
    public String defaultValue;
    public List<String> enumValues;
    public boolean secret;
    public List<String> examples;
}
