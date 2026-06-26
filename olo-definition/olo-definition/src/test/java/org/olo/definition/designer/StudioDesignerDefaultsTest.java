package org.olo.definition.designer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudioDesignerDefaultsTest {

    @Test
    void studioAgentDesignerIncludesBuilderMetadata() {
        DesignerDefinition designer = StudioDesignerDefaults.studioAgentDesigner("🤖", "planning", "agent");

        assertThat(designer.getPaletteGroup()).isEqualTo("Agents");
        assertThat(designer.getSearchKeywords()).containsExactly("planning", "agent");
        assertThat(designer.getNodeSize().getWidth()).isEqualTo(300);
        assertThat(designer.getNodeSize().getHeight()).isEqualTo(120);
        assertThat(designer.getLayout()).isEqualTo(StudioDesignerDefaults.layout());
        assertThat(designer.getCanvas()).isEqualTo(StudioDesignerDefaults.canvas());
        assertThat(designer.getPortColors()).containsEntry("message", "#ef4444");
        assertThat(designer.getNodeTypes()).containsKeys("START", "AGENT", "END");
        assertThat(designer.getNodeTypes().get("AGENT").getEmoji()).isEqualTo("🤖");
        assertThat(designer.getNodeTypes().get("START").getPalette().getName()).isEqualTo("Start");
        assertThat(designer.getNodeTypes().get("END").getPalette().getName()).isEqualTo("End");
    }
}
