package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Studio presentation hints for catalog parameters ({@code parameters[].ui}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ParameterUiDescriptor {

    public String widget;
    public String group;
    public String help;
    public String placeholder;
    public Integer order;
}
