package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * User-configurable parameter on a node, tool, or workflow preset.
 * Catalog entries use {@code parameters: [...]} with stable {@code id} and display {@code label}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ParameterDescriptor {

    /** Stable parameter key — never rename in saved workflows or Studio state. */
    public String id;
    /** Display label in the property panel. */
    @JsonAlias("name")
    public String label;
    /** JSON Schema value type ({@code string}, {@code number}, {@code enum}, …). */
    public String type;
    public String description;
    /** Always emitted ({@code true} or {@code false}) — see catalog parameter rules. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public boolean required;
    public ParameterValidationDescriptor validation;
    public Object defaultValue;
    /** Allowed values when {@code type} is {@code enum}. */
    @JsonAlias("enumValues")
    public List<String> values;
    public List<String> examples;
    /** Show when sibling field values match — keys are parameter {@code id} values. */
    public Map<String, String> visibleWhen;
    public ParameterUiDescriptor ui;
}
