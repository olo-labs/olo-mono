package org.olo.annotation.processor.model;

import org.olo.annotation.catalog.DesignerDescriptor;
import org.olo.annotation.catalog.ParameterDescriptor;
import org.olo.annotation.catalog.PortDescriptor;

import java.util.ArrayList;
import java.util.List;

public final class WorkflowPresetCatalogDocument {

    public String schemaVersion = "1.0";
    public String moduleId;
    public String catalogType = "workflow-presets";
    public String generatedAt;
    public List<WorkflowPresetEntry> presets;

    public static final class WorkflowPresetEntry {
        public String id;
        public DesignerDescriptor designer;
        public List<PortDescriptor> inputs = new ArrayList<>();
        public List<PortDescriptor> outputs = new ArrayList<>();
        public List<ParameterDescriptor> parameters = new ArrayList<>();
    }
}
