package org.olo.kernel.agent.model.impl;

import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.model.ModelRoutingDefinition;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.agent.model.ModelProviderResolver;
import org.olo.kernel.agent.model.ResolvedModelCall;

import java.util.Map;

public final class WorkflowModelProviderResolver implements ModelProviderResolver {

    private static final String TEMPERATURE_PARAMETER = "temperature";
    private static final double DEFAULT_TEMPERATURE = 0.2;
    private static final String DEFAULT_BASE_URL = "http://localhost:51435";

    @Override
    public ResolvedModelCall resolve(WorkflowDefinition graph) {
        if (graph.getModelRouting() == null || graph.getModelRouting().isEmpty()) {
            throw new KernelException("workflow graph has no modelRouting for queue workflow: " + graph.getId());
        }
        ModelRoutingDefinition routing = graph.getModelRouting().getFirst();
        String providerId = routing.getDefaultProviderId();
        if (providerId == null || providerId.isBlank()) {
            throw new KernelException("workflow model routing has no defaultProviderId for: " + graph.getId());
        }

        ModelProviderDefinition provider = graph.getModelProviders().stream()
                .filter(candidate -> providerId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new KernelException(
                        "model provider '" + providerId + "' not found on workflow: " + graph.getId()));

        String baseUrl = readBaseUrl(provider);
        double temperature = readTemperature(graph.getParameters());
        return new ResolvedModelCall(
                provider.getId(),
                provider.getProvider(),
                provider.getModel(),
                baseUrl,
                temperature);
    }

    private static String readBaseUrl(ModelProviderDefinition provider) {
        Map<String, Object> configuration = provider.getConfiguration();
        if (configuration == null || configuration.isEmpty()) {
            return DEFAULT_BASE_URL;
        }
        Object baseUrl = configuration.get("baseUrl");
        if (baseUrl instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return DEFAULT_BASE_URL;
    }

    private static double readTemperature(Map<String, WorkflowParameterDefinition> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return DEFAULT_TEMPERATURE;
        }
        WorkflowParameterDefinition temperature = parameters.get(TEMPERATURE_PARAMETER);
        if (temperature == null || temperature.getDefaultValue() == null) {
            return DEFAULT_TEMPERATURE;
        }
        Object value = temperature.getDefaultValue();
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return DEFAULT_TEMPERATURE;
        }
    }
}
