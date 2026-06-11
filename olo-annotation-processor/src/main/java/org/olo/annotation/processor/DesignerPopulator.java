package org.olo.annotation.processor;

import org.olo.annotation.OloDesigner;
import org.olo.annotation.OloNodeShape;
import org.olo.annotation.catalog.DesignerDefaults;
import org.olo.annotation.catalog.DesignerDescriptor;
import org.olo.annotation.catalog.NodeSizeDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Materializes {@link DesignerDescriptor} from {@link OloDesigner} and legacy node layout fields. */
final class DesignerPopulator {

    private DesignerPopulator() {}

    static DesignerDescriptor from(
            OloDesigner designer,
            String category,
            String[] tags,
            OloNodeShape legacyShape,
            int legacyWidth,
            int legacyHeight) {
        OloNodeShape shape = legacyShape != null ? legacyShape : designer.canvasShape();

        DesignerDescriptor descriptor = new DesignerDescriptor();
        descriptor.paletteGroup = materializePaletteGroup(designer.paletteGroup(), category);
        descriptor.searchKeywords = materializeSearchKeywords(designer.searchKeywords(), tags);
        NodeSizeDescriptor nodeSize = new NodeSizeDescriptor();
        nodeSize.width = resolveWidth(designer.width(), legacyWidth, shape);
        nodeSize.height = resolveHeight(designer.height(), legacyHeight, shape);
        descriptor.nodeSize = nodeSize;
        descriptor.resizable = designer.resizable();
        descriptor.draggable = designer.draggable();
        return DesignerDefaults.stripInherited(descriptor);
    }

    static DesignerDescriptor from(OloDesigner designer, String category, String[] tags) {
        return from(designer, category, tags, null, 0, 0);
    }

    private static String materializePaletteGroup(String paletteGroup, String category) {
        if (paletteGroup != null && !paletteGroup.isBlank()) {
            return paletteGroup.trim();
        }
        if (category == null || category.isBlank()) {
            return "General";
        }
        return titleCase(category.trim());
    }

    private static List<String> materializeSearchKeywords(String[] searchKeywords, String[] tags) {
        Set<String> merged = new LinkedHashSet<>();
        if (searchKeywords != null) {
            for (String keyword : searchKeywords) {
                if (keyword != null && !keyword.isBlank()) {
                    merged.add(keyword.trim());
                }
            }
        }
        if (tags != null) {
            for (String tag : tags) {
                if (tag != null && !tag.isBlank()) {
                    merged.add(tag.trim());
                }
            }
        }
        return merged.isEmpty() ? null : List.copyOf(merged);
    }

    private static int resolveWidth(int annotatedWidth, int legacyWidth, OloNodeShape shape) {
        if (annotatedWidth > 0) {
            return annotatedWidth;
        }
        if (legacyWidth > 0) {
            return legacyWidth;
        }
        return defaultWidth(shape);
    }

    private static int resolveHeight(int annotatedHeight, int legacyHeight, OloNodeShape shape) {
        if (annotatedHeight > 0) {
            return annotatedHeight;
        }
        if (legacyHeight > 0) {
            return legacyHeight;
        }
        return defaultHeight(shape);
    }

    static int defaultWidth(OloNodeShape shape) {
        return switch (shape) {
            case AGENT -> 300;
            case TOOL -> 160;
            default -> DesignerDefaults.STANDARD_WIDTH;
        };
    }

    static int defaultHeight(OloNodeShape shape) {
        return switch (shape) {
            case AGENT -> 120;
            case TOOL -> 72;
            default -> DesignerDefaults.STANDARD_HEIGHT;
        };
    }

    private static String titleCase(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
