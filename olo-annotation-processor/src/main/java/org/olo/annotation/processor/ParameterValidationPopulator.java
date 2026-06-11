package org.olo.annotation.processor;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloWorkflowParameter;
import org.olo.annotation.catalog.ParameterValidationDescriptor;

/** Materializes {@link ParameterValidationDescriptor} from extension annotations. */
final class ParameterValidationPopulator {

    private ParameterValidationPopulator() {}

    static ParameterValidationDescriptor from(OloProperty property) {
        return materialize(
                property.minLength(),
                property.maxLength(),
                property.minimum(),
                property.maximum(),
                property.step());
    }

    static ParameterValidationDescriptor from(OloWorkflowParameter parameter) {
        return materialize(
                parameter.minLength(),
                parameter.maxLength(),
                parameter.minimum(),
                parameter.maximum(),
                parameter.step());
    }

    private static ParameterValidationDescriptor materialize(
            int minLength, int maxLength, double minimum, double maximum, double step) {
        ParameterValidationDescriptor validation = new ParameterValidationDescriptor();
        boolean any = false;
        if (minLength >= 0) {
            validation.minLength = minLength;
            any = true;
        }
        if (maxLength >= 0) {
            validation.maxLength = maxLength;
            any = true;
        }
        if (!Double.isNaN(minimum)) {
            validation.minimum = minimum;
            any = true;
        }
        if (!Double.isNaN(maximum)) {
            validation.maximum = maximum;
            any = true;
        }
        if (!Double.isNaN(step)) {
            validation.step = step;
            any = true;
        }
        return any ? validation : null;
    }
}
