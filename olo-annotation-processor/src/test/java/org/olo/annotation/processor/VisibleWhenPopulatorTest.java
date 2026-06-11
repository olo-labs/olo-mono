package org.olo.annotation.processor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VisibleWhenPopulatorTest {

    @Test
    void parsesKeyValuePairs() {
        assertThat(VisibleWhenPopulator.parse(new String[] {"method=POST"}))
                .containsEntry("method", "POST");
        assertThat(VisibleWhenPopulator.parse(new String[] {"method=POST", "enabled=true"}))
                .containsEntry("method", "POST")
                .containsEntry("enabled", "true");
    }

    @Test
    void returnsNullWhenUnset() {
        assertThat(VisibleWhenPopulator.parse(null)).isNull();
        assertThat(VisibleWhenPopulator.parse(new String[0])).isNull();
    }

    @Test
    void rejectsMalformedEntries() {
        assertThatThrownBy(() -> VisibleWhenPopulator.parse(new String[] {"method"}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VisibleWhenPopulator.parse(new String[] {"=POST"}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
