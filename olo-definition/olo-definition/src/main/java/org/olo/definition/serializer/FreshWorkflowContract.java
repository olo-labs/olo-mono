/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.definition.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowDefinitionBuilder;
import java.io.IOException;
import java.util.Set;

/** The fresh installation contract rejects retired execution fields rather than ignoring them. */
public final class FreshWorkflowContract {
    private static final Set<String> RETIRED = Set.of(
            "tools", "toolCalls", "tool_requirements", "toolRequirements", "toolCallPlanner",
            "selectedTools", "injectCapabilities", "availableToolsVariable", "dynamicToolExecution",
            "aggregateToolResult", "toolSynthesis", "orchestration");
    private FreshWorkflowContract() {}

    /** Contains only a known contract field name, never submitted values. */
    public static final class Rejected extends IllegalArgumentException {
        private Rejected(String message) { super(message); }
    }

    public static void validate(JsonNode value) {
        if (value == null) return;
        if (value.isObject()) {
            var fields = value.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                if (RETIRED.contains(field.getKey()))
                    throw new Rejected("Unsupported workflow field: " + field.getKey());
                if (field.getKey().equals("type") && field.getValue().isTextual()
                        && field.getValue().asText().strip().equalsIgnoreCase("TOOL"))
                    throw new Rejected("Unsupported workflow node type: TOOL");
                validate(field.getValue());
            }
        } else if (value.isArray()) {
            for (var child : value) validate(child);
        } else if (value.isTextual() && value.asText().contains("{availableToolsJson}")) {
            throw new Rejected("Unsupported workflow placeholder: availableToolsJson");
        }
    }

    static BeanDeserializerModifier deserializers() {
        return new BeanDeserializerModifier() {
            @Override public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                    BeanDescription bean, JsonDeserializer<?> delegate) {
                return bean.getBeanClass() == WorkflowDefinitionBuilder.class
                        || bean.getBeanClass() == WorkflowDefinition.class ? new Checked(delegate) : delegate;
            }
        };
    }

    private static final class Checked extends DelegatingDeserializer {
        Checked(JsonDeserializer<?> delegate) { super(delegate); }
        @Override protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> delegate) {
            return new Checked(delegate);
        }
        @Override public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode value = parser.readValueAsTree();
            validate(value);
            try (var checked = value.traverse(parser.getCodec())) {
                checked.nextToken();
                return _delegatee.deserialize(checked, context);
            }
        }
    }
}
