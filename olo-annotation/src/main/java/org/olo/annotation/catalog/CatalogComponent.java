/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Shared extension metadata for every catalog entry (node, tool, hook).
 * <p>
 * Not to be confused with workflow graph types in {@code olo-definition} ({@code NodeDefinition}, etc.).
 * Catalog descriptors describe <em>implementations</em> for Studio and marketplace; workflow definitions
 * describe <em>graph instances</em>.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class CatalogComponent {

    /** Studio palette, search, and canvas defaults. */
    public DesignerDescriptor designer;

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

    public boolean featured;
    public List<String> examples;

    public boolean deprecated;

    /** Orchestration scheduling hints for planners and workflow engines. */
    public CatalogRuntimeDescriptor runtime;

    public String id() {
        return id;
    }
}
