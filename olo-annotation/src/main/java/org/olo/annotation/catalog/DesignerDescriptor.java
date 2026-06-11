package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Reusable Studio designer metadata for catalog entries and workflow presets.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DesignerDescriptor {

    public String paletteGroup;
    public List<String> searchKeywords;
    /** Canvas rendering size hint — not a workflow default. */
    public NodeSizeDescriptor nodeSize;
    public Boolean resizable;
    public Boolean draggable;
}
