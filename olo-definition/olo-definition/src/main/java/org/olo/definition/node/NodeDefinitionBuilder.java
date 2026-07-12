/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.node;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.error.OnFailureDefinition;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.hook.NodeHooksDefinition;
import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.parallel.JoinDefinition;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Jackson builder and fluent factory for {@link NodeDefinition}.
 */
@JsonPOJOBuilder(withPrefix = "")
public final class NodeDefinitionBuilder {

    String id;
    String type;
    String label;
    CapabilityDefinition capability;
    List<PortDefinition> ports;
    NodeExecutionDefinition execution;
    List<String> reads;
    List<String> writes;
    Map<String, Object> configuration;
    NodeHooksDefinition hooks;
    NodeExecutionDefinition.Builder executionBuilder;

    public NodeDefinitionBuilder id(String id) {
        this.id = id;
        return this;
    }

    public NodeDefinitionBuilder type(String type) {
        this.type = type;
        return this;
    }

    public NodeDefinitionBuilder type(NodeType type) {
        this.type = type == null ? null : type.value();
        return this;
    }

    public NodeDefinitionBuilder label(String label) {
        this.label = label;
        return this;
    }

    public NodeDefinitionBuilder capability(CapabilityDefinition capability) {
        this.capability = capability;
        return this;
    }

    public NodeDefinitionBuilder execution(NodeExecutionDefinition execution) {
        this.execution = execution;
        return this;
    }

    public NodeDefinitionBuilder reads(List<String> reads) {
        this.reads = reads;
        return this;
    }

    public NodeDefinitionBuilder addRead(String read) {
        if (this.reads == null) {
            this.reads = new ArrayList<>();
        }
        this.reads.add(read);
        return this;
    }

    public NodeDefinitionBuilder writes(List<String> writes) {
        this.writes = writes;
        return this;
    }

    public NodeDefinitionBuilder addWrite(String write) {
        if (this.writes == null) {
            this.writes = new ArrayList<>();
        }
        this.writes.add(write);
        return this;
    }

    public NodeDefinitionBuilder ports(List<PortDefinition> ports) {
        this.ports = ports;
        return this;
    }

    public NodeDefinitionBuilder addPort(PortDefinition port) {
        if (this.ports == null) {
            this.ports = new ArrayList<>();
        }
        this.ports.add(port);
        return this;
    }

    public NodeDefinitionBuilder configuration(Map<String, Object> configuration) {
        this.configuration = configuration;
        return this;
    }

    public NodeDefinitionBuilder putConfiguration(String key, Object value) {
        if (this.configuration == null) {
            this.configuration = new LinkedHashMap<>();
        }
        this.configuration.put(key, value);
        return this;
    }

    public NodeDefinitionBuilder executionKind(ExecutionKind executionKind) {
        ensureExecutionBuilder().executionKind(executionKind);
        return this;
    }

    public NodeDefinitionBuilder executionModel(ExecutionModel executionModel) {
        ensureExecutionBuilder().executionModel(executionModel);
        return this;
    }

    public NodeDefinitionBuilder workflow(WorkflowReferenceDefinition workflow) {
        ensureExecutionBuilder().workflow(workflow);
        return this;
    }

    public NodeDefinitionBuilder runtimeBinding(RuntimeBindingDefinition runtimeBinding) {
        ensureExecutionBuilder().runtimeBinding(runtimeBinding);
        return this;
    }

    public NodeDefinitionBuilder join(JoinDefinition join) {
        ensureExecutionBuilder().join(join);
        return this;
    }

    public NodeDefinitionBuilder subtype(String subtype) {
        ensureExecutionBuilder().subtype(subtype);
        return this;
    }

    public NodeDefinitionBuilder version(String version) {
        ensureExecutionBuilder().version(version);
        return this;
    }

    public NodeDefinitionBuilder routers(List<NodeRouterDefinition> routers) {
        ensureExecutionBuilder().routers(routers);
        return this;
    }

    public NodeDefinitionBuilder addRouter(NodeRouterDefinition router) {
        ensureExecutionBuilder().addRouter(router);
        return this;
    }

    public NodeDefinitionBuilder onFailure(OnFailureDefinition onFailure) {
        ensureExecutionBuilder().onFailure(onFailure);
        return this;
    }

    public NodeDefinitionBuilder approval(HumanApprovalDefinition approval) {
        ensureExecutionBuilder().approval(approval);
        return this;
    }

    public NodeDefinitionBuilder hooks(NodeHooksDefinition hooks) {
        this.hooks = hooks;
        return this;
    }

    public NodeDefinition build() {
        Objects.requireNonNull(id, "node id is required");
        Objects.requireNonNull(type, "node type is required");
        if (execution == null && executionBuilder != null) {
            execution = executionBuilder.build();
        }
        return new NodeDefinition(this);
    }

    private NodeExecutionDefinition.Builder ensureExecutionBuilder() {
        if (executionBuilder == null) {
            executionBuilder = NodeExecutionDefinition.builder();
        }
        return executionBuilder;
    }
}
