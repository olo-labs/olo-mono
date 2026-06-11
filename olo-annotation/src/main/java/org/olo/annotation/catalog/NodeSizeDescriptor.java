package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Canvas rendering size hint for a catalog entry ({@code designer.nodeSize}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class NodeSizeDescriptor {

    public Integer width;
    public Integer height;
}
