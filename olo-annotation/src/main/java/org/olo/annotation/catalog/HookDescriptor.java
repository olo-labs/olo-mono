package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Merged hook extension entry from an extension catalog.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class HookDescriptor {

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
    public boolean deprecated;
    public String implementationClass;
    public String spiInterface;
    public List<String> phases;

    public String id() {
        return id;
    }
}
