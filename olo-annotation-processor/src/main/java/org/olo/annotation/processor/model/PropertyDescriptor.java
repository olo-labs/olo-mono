package org.olo.annotation.processor.model;

import java.util.List;

public class PropertyDescriptor {

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
