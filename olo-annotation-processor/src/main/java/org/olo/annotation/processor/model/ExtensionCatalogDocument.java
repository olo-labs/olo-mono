/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor.model;

import java.util.List;
import org.olo.annotation.catalog.HookDescriptor;
import org.olo.annotation.catalog.NodeDescriptor;
import org.olo.annotation.catalog.ToolDescriptor;

public class ExtensionCatalogDocument {

    public String schemaVersion;
    public String moduleId;
    public String catalogType;
    public String generatedAt;
    public String generatedBy;
    public String generatedByVersion;
    public List<NodeDescriptor> nodes;
    public List<ToolDescriptor> tools;
    public List<HookDescriptor> hooks;
}
