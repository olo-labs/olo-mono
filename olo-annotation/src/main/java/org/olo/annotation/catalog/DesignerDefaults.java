/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Inherited Studio designer baselines ({@code defaults.designer}) and deviation stripping for catalog entries.
 */
public final class DesignerDefaults {

    public static final int STANDARD_WIDTH = 200;
    public static final int STANDARD_HEIGHT = 80;
    public static final boolean RESIZABLE = true;
    public static final boolean DRAGGABLE = true;

    private DesignerDefaults() {}

    public static DesignerDescriptor baseline() {
        DesignerDescriptor designer = new DesignerDescriptor();
        NodeSizeDescriptor nodeSize = new NodeSizeDescriptor();
        nodeSize.width = STANDARD_WIDTH;
        nodeSize.height = STANDARD_HEIGHT;
        designer.nodeSize = nodeSize;
        designer.resizable = RESIZABLE;
        designer.draggable = DRAGGABLE;
        return designer;
    }

    public static Map<String, Object> catalogDefaults() {
        Map<String, Object> designer = new LinkedHashMap<>();
        designer.put("nodeSize", Map.of("width", STANDARD_WIDTH, "height", STANDARD_HEIGHT));
        designer.put("resizable", RESIZABLE);
        designer.put("draggable", DRAGGABLE);
        return designer;
    }

    /**
     * Returns a copy with fields matching {@link #baseline()} removed so Studio merges with {@code defaults.designer}.
     */
    public static DesignerDescriptor stripInherited(DesignerDescriptor designer) {
        if (designer == null) {
            return null;
        }
        DesignerDescriptor stripped = new DesignerDescriptor();
        stripped.paletteGroup = designer.paletteGroup;
        stripped.searchKeywords = designer.searchKeywords;

        if (designer.nodeSize != null) {
            boolean widthDiffers =
                    designer.nodeSize.width != null && designer.nodeSize.width != STANDARD_WIDTH;
            boolean heightDiffers =
                    designer.nodeSize.height != null && designer.nodeSize.height != STANDARD_HEIGHT;
            if (widthDiffers || heightDiffers) {
                NodeSizeDescriptor nodeSize = new NodeSizeDescriptor();
                if (widthDiffers) {
                    nodeSize.width = designer.nodeSize.width;
                }
                if (heightDiffers) {
                    nodeSize.height = designer.nodeSize.height;
                }
                stripped.nodeSize = nodeSize;
            }
        }

        if (designer.resizable != null && !Objects.equals(designer.resizable, RESIZABLE)) {
            stripped.resizable = designer.resizable;
        }
        if (designer.draggable != null && !Objects.equals(designer.draggable, DRAGGABLE)) {
            stripped.draggable = designer.draggable;
        }
        return stripped;
    }
}
