package org.olo.annotation.processor.model;

import java.util.List;
public class ToolExtensionDescriptor {

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
    public List<PropertyDescriptor> arguments;
    public List<PropertyDescriptor> configuration;
    public CapabilityDescriptor capability;
}
