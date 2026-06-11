package org.olo.annotation.processor;

import org.junit.jupiter.api.Test;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.catalog.ParameterValidationDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterValidationPopulatorTest {

    @OloProperty(
            name = "url",
            type = OloPropertyType.STRING,
            required = true,
            minLength = 8,
            maxLength = 2048)
    private @interface UrlProperty {}

    @Test
    void materializesStringValidation() throws Exception {
        OloProperty property = UrlProperty.class.getAnnotation(OloProperty.class);
        ParameterValidationDescriptor validation = ParameterValidationPopulator.from(property);
        assertThat(validation.minLength).isEqualTo(8);
        assertThat(validation.maxLength).isEqualTo(2048);
        assertThat(validation.minimum).isNull();
    }
}
