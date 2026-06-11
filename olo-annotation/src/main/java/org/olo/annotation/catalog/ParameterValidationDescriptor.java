package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Studio validation hints for a parameter or property ({@code validation} on catalog entries).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ParameterValidationDescriptor {

    public Integer minLength;
    public Integer maxLength;
    public Double minimum;
    public Double maximum;
    public Double step;
}
