package org.olo.annotation.processor.model;

import java.util.List;

public class ExtensionCatalogDocument {

    public String schemaVersion;
    public String moduleId;
    public String catalogType;
    public String generatedAt;
    public String generatedBy;
    public String generatedByVersion;
    public List<NodeExtensionDescriptor> nodes;
    public List<ToolExtensionDescriptor> tools;
    public List<HookExtensionDescriptor> hooks;
}
