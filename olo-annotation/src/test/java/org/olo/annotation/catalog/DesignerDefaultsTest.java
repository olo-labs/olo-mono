/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DesignerDefaultsTest {

    @Test
    void stripsStandardCanvasDefaults() {
        DesignerDescriptor full = new DesignerDescriptor();
        full.paletteGroup = "Control";
        full.searchKeywords = java.util.List.of("branch");
        NodeSizeDescriptor nodeSize = new NodeSizeDescriptor();
        nodeSize.width = DesignerDefaults.STANDARD_WIDTH;
        nodeSize.height = DesignerDefaults.STANDARD_HEIGHT;
        full.nodeSize = nodeSize;
        full.resizable = true;
        full.draggable = true;

        DesignerDescriptor stripped = DesignerDefaults.stripInherited(full);
        assertThat(stripped.paletteGroup).isEqualTo("Control");
        assertThat(stripped.searchKeywords).containsExactly("branch");
        assertThat(stripped.nodeSize).isNull();
        assertThat(stripped.resizable).isNull();
        assertThat(stripped.draggable).isNull();
    }

    @Test
    void keepsNonStandardNodeSize() {
        DesignerDescriptor full = new DesignerDescriptor();
        full.paletteGroup = "Agents";
        NodeSizeDescriptor nodeSize = new NodeSizeDescriptor();
        nodeSize.width = 300;
        nodeSize.height = 120;
        full.nodeSize = nodeSize;
        full.resizable = true;
        full.draggable = true;

        DesignerDescriptor stripped = DesignerDefaults.stripInherited(full);
        assertThat(stripped.nodeSize.width).isEqualTo(300);
        assertThat(stripped.nodeSize.height).isEqualTo(120);
        assertThat(stripped.resizable).isNull();
    }
}
