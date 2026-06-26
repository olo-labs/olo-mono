package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WorkflowPresetDescriptor {

    public String id;
    public DesignerDescriptor designer;
    public List<PortDescriptor> inputs = new ArrayList<>();
    public List<PortDescriptor> outputs = new ArrayList<>();
    public List<ParameterDescriptor> parameters = new ArrayList<>();
}
