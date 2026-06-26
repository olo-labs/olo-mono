package org.olo.definition.workflow;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.DesignerDefinition;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.extension.ExtensionDefinition;
import org.olo.definition.hook.HookDefinition;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.spi.runtime.RuntimeCapability;
import org.olo.definition.runtime.WorkflowRuntimeDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.model.ModelRoutingDefinition;
import org.olo.definition.human.HumanApprovalDefinition;
import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.port.PortUiDefinition;
import org.olo.definition.port.PortUiPosition;
import org.olo.definition.port.PortWireType;
import org.olo.definition.parameter.AgentWorkflowParameters;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.runtime.AgentDelegationPolicy;
import org.olo.definition.runtime.RuntimeDelegationDefinition;
import org.olo.definition.planner.AgentAvailableAgents;
import org.olo.definition.planner.AgentReferenceDefinition;
import org.olo.definition.planner.WorkflowPlannerMetadata;
import org.olo.definition.state.StateFieldDefinition;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.variable.VariableDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent builder for {@link WorkflowDefinition}, including dynamic extension of existing workflows.
 */
public final class WorkflowBuilder {

    private static final String MESSAGE_PORT_COLOR = "#ef4444";
    private static final String CAPABILITIES_PORT_COLOR = "#22c55e";
    private static final String AGENT_PLUG_PORT_COLOR = "#a855f7";
    private static final String MESSAGE_INPUT_PORT_LABEL = "message in";
    private static final String MESSAGE_OUTPUT_PORT_LABEL = "message out";
    private static final String MESSAGE_INPUT_PORT_DESCRIPTION = "Incoming workflow message";
    private static final String MESSAGE_OUTPUT_PORT_DESCRIPTION = "Outgoing workflow message";
    private static final int CANVAS_LAYOUT_X = 80;
    private static final int CANVAS_LAYOUT_Y = 80;
    private static final int CANVAS_LAYOUT_COL_WIDTH = 360;

    public static final String RETURN_VARIABLE_METADATA_KEY = "returnVariable";
    public static final String RETURN_VARIABLE_NAME = "ReturnValue";
    public static final String RETURN_VARIABLE_ROLE = "return";

    private final WorkflowDefinition.Builder delegate = WorkflowDefinition.builder();
    private final List<NodeDefinition> nodes = new ArrayList<>();
    private final List<EdgeDefinition> edges = new ArrayList<>();
    private final Map<String, WorkflowInputDefinition> inputs = new LinkedHashMap<>();
    private final Map<String, StateFieldDefinition> state = new LinkedHashMap<>();
    private final Map<String, WorkflowParameterDefinition> parameters = new LinkedHashMap<>();
    private final List<VariableDefinition> variables = new ArrayList<>();
    private final List<ModelProviderDefinition> modelProviders = new ArrayList<>();
    private final List<ModelRoutingDefinition> modelRouting = new ArrayList<>();
    private final List<ExtensionDefinition> extensions = new ArrayList<>();
    private final List<ToolDefinition> tools = new ArrayList<>();
    private final List<AgentDefinition> agents = new ArrayList<>();
    private final List<HookDefinition> hooks = new ArrayList<>();
    private final List<ChildWorkflowDefinition> childWorkflows = new ArrayList<>();
    private final List<AgentReferenceDefinition> availableAgents = new ArrayList<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();
    private WorkflowRuntimeDefinition.Builder workflowRuntimeBuilder;

    private WorkflowBuilder() {
    }

    /**
     * Starts a new workflow; {@code label} is the display name and seeds {@code id} when not set explicitly.
     */
    public static WorkflowBuilder create(String label) {
        Objects.requireNonNull(label, "label is required");
        WorkflowBuilder builder = new WorkflowBuilder();
        builder.delegate.label(label);
        builder.delegate.id(slugify(label));
        return builder;
    }

    /**
     * Creates a builder seeded from an existing workflow (copy-on-write for nodes, edges, and related lists).
     */
    public static WorkflowBuilder from(WorkflowDefinition existing) {
        Objects.requireNonNull(existing, "existing workflow is required");
        WorkflowBuilder builder = new WorkflowBuilder();
        builder.delegate.id(existing.getId());
        builder.delegate.enabled(existing.isEnabled());
        builder.delegate.isDefault(existing.isDefault());
        builder.delegate.label(existing.getLabel());
        builder.delegate.role(existing.getRole());
        builder.delegate.shortDescription(existing.getShortDescription());
        builder.delegate.emoji(existing.getEmoji());
        if (existing.getDesigner() != null) {
            builder.delegate.designer(existing.getDesigner());
        }
        builder.delegate.queue(existing.getQueue());
        builder.delegate.workflowType(existing.getWorkflowType());
        builder.delegate.runAgain(existing.isRunAgain());
        builder.delegate.longDescription(existing.getLongDescription());
        builder.delegate.isExternalWorkflow(existing.isExternalWorkflow());
        builder.delegate.isChildWorkflow(existing.isChildWorkflow());
        builder.childWorkflows.addAll(existing.getChildWorkflows());
        builder.availableAgents.addAll(existing.getAvailableAgents());
        builder.delegate.version(existing.getVersion());
        builder.nodes.addAll(existing.getNodes());
        builder.edges.addAll(existing.getEdges());
        builder.inputs.putAll(existing.getInputs());
        builder.state.putAll(existing.getState());
        builder.parameters.putAll(existing.getParameters());
        builder.variables.addAll(existing.getVariables());
        builder.modelProviders.addAll(existing.getModelProviders());
        builder.modelRouting.addAll(existing.getModelRouting());
        builder.extensions.addAll(existing.getExtensions());
        builder.tools.addAll(existing.getTools());
        builder.agents.addAll(existing.getAgents());
        builder.hooks.addAll(existing.getHooks());
        builder.metadata.putAll(existing.getMetadata());
        if (existing.getCapability() != null) {
            builder.delegate.capability(existing.getCapability());
        }
        if (existing.getRuntimeBinding() != null) {
            builder.delegate.runtimeBinding(existing.getRuntimeBinding());
        }
        if (existing.getRuntime() != null) {
            builder.workflowRuntimeBuilder = WorkflowRuntimeDefinition.builder()
                    .contractVersion(existing.getRuntime().getContractVersion())
                    .executionModel(existing.getRuntime().getExecutionModel())
                    .capabilities(existing.getRuntime().getCapabilities())
                    .defaultTimeout(existing.getRuntime().getDefaultTimeout())
                    .delegation(existing.getRuntime().getDelegation());
        }
        return builder;
    }

    public WorkflowBuilder id(String id) {
        delegate.id(id);
        return this;
    }

    public WorkflowBuilder enabled(Boolean enabled) {
        delegate.enabled(enabled);
        return this;
    }

    public WorkflowBuilder isDefault(Boolean isDefault) {
        delegate.isDefault(isDefault);
        return this;
    }

    public WorkflowBuilder label(String label) {
        delegate.label(label);
        return this;
    }

    public WorkflowBuilder role(String role) {
        delegate.role(role);
        return this;
    }

    public WorkflowBuilder shortDescription(String shortDescription) {
        delegate.shortDescription(shortDescription);
        return this;
    }

    public WorkflowBuilder emoji(String emoji) {
        delegate.emoji(emoji);
        return this;
    }

    public WorkflowBuilder queue(String queue) {
        delegate.queue(queue);
        return this;
    }

    public WorkflowBuilder workflowType(String workflowType) {
        delegate.workflowType(workflowType);
        return this;
    }

    public WorkflowBuilder runAgain(Boolean runAgain) {
        delegate.runAgain(runAgain);
        return this;
    }

    public WorkflowBuilder longDescription(String longDescription) {
        delegate.longDescription(longDescription);
        return this;
    }

    public WorkflowBuilder isExternalWorkflow(Boolean isExternalWorkflow) {
        delegate.isExternalWorkflow(isExternalWorkflow);
        return this;
    }

    public WorkflowBuilder isChildWorkflow(Boolean isChildWorkflow) {
        delegate.isChildWorkflow(isChildWorkflow);
        return this;
    }

    public WorkflowBuilder childWorkflow(ChildWorkflowDefinition childWorkflow) {
        Objects.requireNonNull(childWorkflow, "childWorkflow is required");
        childWorkflows.add(childWorkflow);
        return this;
    }

    public WorkflowBuilder childWorkflowRef(String workflowId) {
        return childWorkflow(ChildWorkflowDefinition.builder().workflowId(workflowId).build());
    }

    public WorkflowBuilder version(String version) {
        delegate.version(version);
        return this;
    }

    public WorkflowBuilder startNode(String id) {
        return addNode(NodeDefinition.builder()
                .id(id)
                .type(NodeType.START)
                .addPort(defaultPort("out", "out", PortDirection.OUTPUT))
                .build());
    }

    /** START node that maps external {@code message} input into the workflow graph. */
    public WorkflowBuilder startNodeWithMessageInput(String id) {
        return addNode(NodeDefinition.builder()
                .id(id)
                .type(NodeType.START)
                .addRead("input." + WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                .addPort(defaultPort("out", "out", PortDirection.OUTPUT))
                .putConfiguration(
                        "inputVariableMappings",
                        List.of(WorkflowPresetInfrastructure.MESSAGE_VARIABLE))
                .build());
    }

    public WorkflowBuilder endNode(String id) {
        return addNode(NodeDefinition.builder()
                .id(id)
                .type(NodeType.END)
                .addPort(defaultPort("in", "in", PortDirection.INPUT))
                .build());
    }

    /** @deprecated use {@link #startNode(String)} */
    @Deprecated
    public WorkflowBuilder inputNode(String id) {
        return startNode(id);
    }

    /** @deprecated use {@link #endNode(String)} */
    @Deprecated
    public WorkflowBuilder outputNode(String id) {
        return endNode(id);
    }

    public WorkflowBuilder modelNode(String id) {
        return modelNode(id, null);
    }

    public WorkflowBuilder modelNode(String id, String subtype) {
        NodeDefinition.Builder node = NodeDefinition.builder()
                .id(id)
                .type(NodeType.MODEL)
                .addPort(defaultPort("in", "in", PortDirection.INPUT))
                .addPort(defaultPort("out", "out", PortDirection.OUTPUT));
        if (subtype != null) {
            node.subtype(subtype);
        }
        return addNode(node.build());
    }

    public WorkflowBuilder toolNode(String id) {
        return addNode(NodeDefinition.builder()
                .id(id)
                .type(NodeType.TOOL)
                .addPort(defaultPort("in", "in", PortDirection.INPUT))
                .addPort(defaultPort("out", "out", PortDirection.OUTPUT))
                .addPort(pluginPort("capabilities", PortWireType.CAPABILITIES, PortDirection.OUTPUT))
                .build());
    }

    public WorkflowBuilder vectorSearchNode(String id) {
        return addNode(NodeDefinition.builder()
                .id(id)
                .type(NodeType.VECTOR_SEARCH)
                .addPort(defaultPort("in", "in", PortDirection.INPUT))
                .addPort(defaultPort("out", "out", PortDirection.OUTPUT))
                .build());
    }

    public WorkflowBuilder agentNode(String id, WorkflowReferenceDefinition workflow) {
        return agentNode(id, null, workflow);
    }

    /**
     * Agent node: requires {@code workflow} (agent = workflow artifact). Optional subtype.
     */
    public WorkflowBuilder agentNode(String id, String subtype, WorkflowReferenceDefinition workflow) {
        Objects.requireNonNull(workflow, "workflow is required for AGENT nodes");
        NodeDefinition.Builder node = NodeDefinition.builder()
                .id(id)
                .type(NodeType.AGENT)
                .workflow(workflow)
                .executionKind(ExecutionKind.SUBWORKFLOW)
                .executionModel(ExecutionModel.CHILD_WORKFLOW)
                .addPort(defaultPort("in", "in", PortDirection.INPUT))
                .addPort(pluginPort("capabilities", PortWireType.CAPABILITIES, PortDirection.INPUT))
                .addPort(pluginPort("agentPlug", PortWireType.AGENT_PLUG, PortDirection.INPUT))
                .addPort(defaultPort("out", "out", PortDirection.OUTPUT));
        if (subtype != null) {
            node.subtype(subtype);
        }
        return addNode(node.build());
    }

    public WorkflowBuilder humanNode(String id, HumanApprovalDefinition approval) {
        return humanNode(id, "APPROVAL", approval);
    }

    /**
     * Human-in-the-loop node with structured approval metadata (e.g. trading desk sign-off).
     */
    public WorkflowBuilder humanNode(String id, String subtype, HumanApprovalDefinition approval) {
        Objects.requireNonNull(approval, "approval is required");
        NodeDefinition.Builder node = NodeDefinition.builder()
                .id(id)
                .type(NodeType.HUMAN)
                .approval(approval)
                .addPort(defaultPort("in", "in", PortDirection.INPUT))
                .addPort(defaultPort("out", "out", PortDirection.OUTPUT));
        if (subtype != null) {
            node.subtype(subtype);
        }
        return addNode(node.build());
    }

    public WorkflowBuilder addNode(NodeDefinition node) {
        Objects.requireNonNull(node, "node is required");
        ensureUniqueNodeId(node.getId());
        nodes.add(node);
        return this;
    }

    public WorkflowBuilder addEdge(EdgeDefinition edge) {
        Objects.requireNonNull(edge, "edge is required");
        edges.add(edge);
        return this;
    }

    /** Replaces the working edge list (used when dynamically rewriting graph connectivity). */
    public WorkflowBuilder replaceEdges(List<EdgeDefinition> replacementEdges) {
        edges.clear();
        if (replacementEdges != null) {
            edges.addAll(replacementEdges);
        }
        return this;
    }

    public WorkflowBuilder connect(String sourceNodeId, String targetNodeId) {
        return connect(sourceNodeId, null, targetNodeId, null);
    }

    public WorkflowBuilder connect(
            String sourceNodeId,
            String sourcePortId,
            String targetNodeId,
            String targetPortId) {
        return addEdge(EdgeDefinition.builder()
                .sourceNodeId(sourceNodeId)
                .sourcePortId(sourcePortId)
                .targetNodeId(targetNodeId)
                .targetPortId(targetPortId)
                .build());
    }

    public WorkflowBuilder input(String name, WorkflowInputDefinition input) {
        Objects.requireNonNull(name, "input name is required");
        Objects.requireNonNull(input, "input is required");
        inputs.put(name, input);
        return this;
    }

    public WorkflowBuilder stateField(String name, StateFieldDefinition field) {
        Objects.requireNonNull(name, "state field name is required");
        Objects.requireNonNull(field, "state field is required");
        state.put(name, field);
        return this;
    }

    public WorkflowBuilder parameter(String name, WorkflowParameterDefinition parameter) {
        Objects.requireNonNull(name, "parameter name is required");
        Objects.requireNonNull(parameter, "parameter is required");
        parameters.put(name, parameter);
        return this;
    }

    /** @deprecated use {@link #input(String, WorkflowInputDefinition)} */
    @Deprecated
    public WorkflowBuilder variable(VariableDefinition variable) {
        Objects.requireNonNull(variable, "variable is required");
        variables.add(variable);
        return this;
    }

    public WorkflowBuilder modelProvider(ModelProviderDefinition provider) {
        Objects.requireNonNull(provider, "provider is required");
        modelProviders.add(provider);
        return this;
    }

    public WorkflowBuilder modelRouting(ModelRoutingDefinition routing) {
        Objects.requireNonNull(routing, "routing is required");
        modelRouting.add(routing);
        return this;
    }

    public WorkflowBuilder withMessageInput() {
        if (!inputs.containsKey(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)) {
            input(
                    WorkflowPresetInfrastructure.MESSAGE_VARIABLE,
                    WorkflowInputDefinition.builder()
                            .schema("string")
                            .required(true)
                            .build());
        }
        return this;
    }

    public WorkflowBuilder withMessageVariable() {
        boolean hasMessage = variables.stream()
                .anyMatch(variable -> WorkflowPresetInfrastructure.MESSAGE_VARIABLE.equals(variable.getName()));
        if (!hasMessage) {
            variable(WorkflowPresetInfrastructure.messageVariable());
        }
        return this;
    }

    public WorkflowBuilder withMessageContract() {
        return withMessageInput().withMessageVariable();
    }

    public WorkflowBuilder defaultLocalModelInfrastructure() {
        if (modelProviders.isEmpty()) {
            modelProvider(WorkflowPresetInfrastructure.defaultLocalModelProvider());
        }
        if (modelRouting.isEmpty()) {
            modelRouting(WorkflowPresetInfrastructure.defaultModelRouting());
        }
        return this;
    }

    public WorkflowBuilder presetPlannerContext(String presetId) {
        metadata(
                org.olo.definition.planner.PlannerContextDefinition.METADATA_KEY,
                org.olo.definition.planner.PlannerContextDefinition.presetDefaults(presetId));
        return this;
    }

    /**
     * Canonical Studio canvas: START → AGENT (self workflow) → END with message input mapping.
     */
    public WorkflowBuilder agentCanvasPipeline(String workflowId) {
        return startNodeWithMessageInput("start")
                .agentNode(
                        "agent",
                        WorkflowReferenceDefinition.builder()
                                .workflowId(workflowId)
                                .version("1.0.0")
                                .build())
                .endNode("end")
                .connect("start", "out", "agent", "in")
                .connect("agent", "out", "end", "in")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout("agent", 1)
                .nodeCanvasLayout("end", 2);
    }

    /**
     * Studio canvas position for a node already added to this builder ({@code configuration.designer.position}).
     */
    public WorkflowBuilder nodeCanvasLayout(String nodeId, int columnIndex) {
        Objects.requireNonNull(nodeId, "nodeId is required");
        for (int i = 0; i < nodes.size(); i++) {
            NodeDefinition node = nodes.get(i);
            if (!node.getId().equals(nodeId)) {
                continue;
            }
            Map<String, Object> configuration = new LinkedHashMap<>(node.getConfiguration());
            configuration.put("designer", designerLayout(columnIndex));
            nodes.set(i, nodeWithConfiguration(node, configuration));
            return this;
        }
        throw new IllegalArgumentException("unknown node id for canvas layout: " + nodeId);
    }

    /** Message wire port used by Studio presets (labels, color, cardinality). */
    public static PortDefinition messagePort(String id, PortDirection direction) {
        return defaultPort(id, id, direction);
    }

    public WorkflowBuilder extension(ExtensionDefinition extension) {
        Objects.requireNonNull(extension, "extension is required");
        extensions.add(extension);
        return this;
    }

    public WorkflowBuilder metadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    public WorkflowBuilder metadata(Map<String, Object> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    /**
     * Declares the standard workflow return variable and {@code metadata.returnVariable = ReturnValue}.
     */
    public WorkflowBuilder withStandardReturnVariable() {
        metadata.putIfAbsent(RETURN_VARIABLE_METADATA_KEY, RETURN_VARIABLE_NAME);
        boolean hasReturnVariable = variables.stream()
                .anyMatch(variable -> RETURN_VARIABLE_NAME.equals(variable.getName()));
        if (!hasReturnVariable) {
            variable(VariableDefinition.builder()
                    .name(RETURN_VARIABLE_NAME)
                    .type("string")
                    .description("Workflow return message returned to the caller")
                    .required(false)
                    .scope(org.olo.definition.variable.VariableScope.LOCAL)
                    .defaultValue(null)
                    .metadata(Map.of("role", RETURN_VARIABLE_ROLE))
                    .build());
        }
        return this;
    }

    /**
     * Planner-readable contract for this workflow (required for valid workflows).
     */
    public WorkflowBuilder capability(CapabilityDefinition capability) {
        Objects.requireNonNull(capability, "capability is required");
        delegate.capability(capability);
        return this;
    }

    public WorkflowBuilder runtime(WorkflowRuntimeDefinition runtime) {
        delegate.runtime(runtime);
        this.workflowRuntimeBuilder = null;
        return this;
    }

    /**
     * Sets {@code runtime.executionModel} (catalog-aligned: {@code INLINE}, {@code ACTIVITY},
     * {@code CHILD_WORKFLOW}, {@code EXTERNAL}).
     */
    public WorkflowBuilder executionModel(ExecutionModel executionModel) {
        workflowRuntimeBuilder().executionModel(executionModel);
        return this;
    }

    public WorkflowBuilder defaultTimeout(String defaultTimeout) {
        workflowRuntimeBuilder().defaultTimeout(defaultTimeout);
        return this;
    }

    /** {@code CHILD_WORKFLOW} agent runtime with debug, replay, timeout, and a 10-minute default. */
    public WorkflowBuilder agentWorkflowRuntime() {
        return executionModel(ExecutionModel.CHILD_WORKFLOW)
                .debuggable()
                .replayable()
                .addRuntimeCapability(RuntimeCapability.TIMEOUT)
                .defaultTimeout("PT10M");
    }

    /** Agent tuning parameters with catalog defaults ({@code systemPrompt} = {@code {message}}). */
    public WorkflowBuilder baselineAgentParameters() {
        AgentWorkflowParameters.defaults().forEach(this::parameter);
        return this;
    }

    /** Agent tuning parameters for the {@code agent} preset. */
    public WorkflowBuilder agentParameters() {
        return agentParameters("agent");
    }

    /** Agent tuning parameters with a preset-specific system prompt default. */
    public WorkflowBuilder agentParameters(String presetId) {
        AgentWorkflowParameters.forPreset(presetId).forEach(this::parameter);
        return this;
    }

    /** Planner routing metadata ({@link WorkflowPlannerMetadata}). */
    public WorkflowBuilder agentPlannerMetadata() {
        WorkflowPlannerMetadata.agentDefaults().forEach(this::metadata);
        return this;
    }

    /** Default planner context ({@link org.olo.definition.planner.PlannerContextDefinition}). */
    public WorkflowBuilder agentPlannerContext() {
        metadata(
                org.olo.definition.planner.PlannerContextDefinition.METADATA_KEY,
                org.olo.definition.planner.PlannerContextDefinition.agentDefaults());
        return this;
    }

    public WorkflowBuilder availableAgent(String agentWorkflowId) {
        return availableAgent(AgentReferenceDefinition.of(agentWorkflowId));
    }

    public WorkflowBuilder availableAgent(AgentReferenceDefinition agentReference) {
        availableAgents.add(agentReference);
        return this;
    }

    /** Default {@code availableAgents} for the agent preset ({@link AgentAvailableAgents}). */
    public WorkflowBuilder agentAvailableAgents() {
        AgentAvailableAgents.agentPresetDefaults().forEach(this::availableAgent);
        return this;
    }

    public WorkflowBuilder delegation(RuntimeDelegationDefinition delegation) {
        workflowRuntimeBuilder().delegation(delegation);
        return this;
    }

    /** Delegation guardrails for the agent preset ({@link AgentDelegationPolicy}). */
    public WorkflowBuilder agentDelegation() {
        return delegation(AgentDelegationPolicy.agentPresetDefaults());
    }

    public WorkflowBuilder debuggable() {
        workflowRuntimeBuilder().debuggable(true);
        return this;
    }

    public WorkflowBuilder replayable() {
        workflowRuntimeBuilder().replayable(true);
        return this;
    }

    public WorkflowBuilder addRuntimeCapability(RuntimeCapability capability) {
        workflowRuntimeBuilder().addCapability(capability);
        return this;
    }

    public WorkflowBuilder runtimeBinding(RuntimeBindingDefinition runtimeBinding) {
        delegate.runtimeBinding(runtimeBinding);
        return this;
    }

    public WorkflowBuilder tool(ToolDefinition tool) {
        Objects.requireNonNull(tool, "tool is required");
        tools.add(tool);
        return this;
    }

    public WorkflowBuilder agent(AgentDefinition agent) {
        Objects.requireNonNull(agent, "agent is required");
        agents.add(agent);
        return this;
    }

    public WorkflowBuilder hook(HookDefinition hook) {
        Objects.requireNonNull(hook, "hook is required");
        hooks.add(hook);
        return this;
    }

    public WorkflowBuilder designer(DesignerDefinition designer) {
        delegate.designer(designer);
        return this;
    }

    public WorkflowDefinition build() {
        delegate.nodes(List.copyOf(nodes));
        delegate.edges(List.copyOf(edges));
        delegate.inputs(Map.copyOf(inputs));
        delegate.state(Map.copyOf(state));
        delegate.parameters(Map.copyOf(parameters));
        delegate.variables(List.copyOf(variables));
        delegate.modelProviders(List.copyOf(modelProviders));
        delegate.modelRouting(List.copyOf(modelRouting));
        delegate.extensions(List.copyOf(extensions));
        delegate.tools(List.copyOf(tools));
        delegate.agents(List.copyOf(agents));
        delegate.hooks(List.copyOf(hooks));
        delegate.childWorkflows(List.copyOf(childWorkflows));
        delegate.availableAgents(List.copyOf(availableAgents));
        delegate.metadata(Map.copyOf(metadata));
        if (workflowRuntimeBuilder != null) {
            delegate.runtime(workflowRuntimeBuilder.build());
        }
        return delegate.build();
    }

    private WorkflowRuntimeDefinition.Builder workflowRuntimeBuilder() {
        if (workflowRuntimeBuilder == null) {
            workflowRuntimeBuilder = WorkflowRuntimeDefinition.builder();
        }
        return workflowRuntimeBuilder;
    }

    private void ensureUniqueNodeId(String id) {
        for (NodeDefinition existing : nodes) {
            if (existing.getId().equals(id)) {
                throw new IllegalArgumentException("duplicate node id: " + id);
            }
        }
    }

    private static String slugify(String name) {
        return name.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private static PortDefinition defaultPort(String id, String name, PortDirection direction) {
        String label = direction == PortDirection.INPUT ? MESSAGE_INPUT_PORT_LABEL : MESSAGE_OUTPUT_PORT_LABEL;
        String wireType = PortWireType.MESSAGE.wireName();
        PortDefinition.Builder builder = PortDefinition.builder()
                .id(id)
                .label(label)
                .shortDescription(direction == PortDirection.INPUT
                        ? MESSAGE_INPUT_PORT_DESCRIPTION
                        : MESSAGE_OUTPUT_PORT_DESCRIPTION)
                .schema(wireType)
                .type(wireType)
                .direction(direction)
                .ui(PortUiDefinition.builder()
                        .position(PortUiPosition.defaultFor(direction))
                        .color(MESSAGE_PORT_COLOR)
                        .build());
        if (direction == PortDirection.INPUT) {
            builder.acceptType(wireType)
                    .required(true)
                    .minConnections(1)
                    .maxConnections(1);
        } else {
            builder.required(false).minConnections(0);
        }
        return builder.build();
    }

    private static PortDefinition pluginPort(String id, PortWireType wireType, PortDirection direction) {
        PortUiPosition position = switch (id) {
            case "capabilities" -> direction == PortDirection.INPUT ? PortUiPosition.BOTTOM : PortUiPosition.TOP;
            case "agentPlug" -> direction == PortDirection.INPUT ? PortUiPosition.BOTTOM : PortUiPosition.TOP;
            default -> direction == PortDirection.INPUT ? PortUiPosition.BOTTOM : PortUiPosition.TOP;
        };
        String color = wireType == PortWireType.CAPABILITIES ? CAPABILITIES_PORT_COLOR : AGENT_PLUG_PORT_COLOR;
        String label = wireType == PortWireType.CAPABILITIES ? "available tools" : "available agents";
        String description = direction == PortDirection.INPUT
                ? (wireType == PortWireType.CAPABILITIES
                        ? "Tools and hooks registered for runtime prompt assembly (0 or more)"
                        : "Child workflows registered for runtime prompt assembly (0 or more)")
                : (wireType == PortWireType.CAPABILITIES
                        ? "Capability indicator for runtime prompt assembly on a connected agent"
                        : "Child workflow indicator for runtime prompt assembly on a connected agent");
        PortDefinition.Builder builder = PortDefinition.builder()
                .id(id)
                .label(label)
                .shortDescription(description)
                .schema(wireType.wireName())
                .type(wireType.wireName())
                .direction(direction)
                .ui(PortUiDefinition.builder().position(position).color(color).build())
                .required(false)
                .minConnections(0);
        if (direction == PortDirection.INPUT) {
            builder.acceptType(wireType.wireName());
        }
        return builder.build();
    }

    private static Map<String, Object> designerLayout(int columnIndex) {
        return Map.of(
                "position",
                Map.of("x", CANVAS_LAYOUT_X + columnIndex * CANVAS_LAYOUT_COL_WIDTH, "y", CANVAS_LAYOUT_Y));
    }

    private static NodeDefinition nodeWithConfiguration(
            NodeDefinition node, Map<String, Object> configuration) {
        NodeDefinition.Builder builder = NodeDefinition.builder()
                .id(node.getId())
                .type(node.getType())
                .label(node.getLabel())
                .capability(node.getCapability())
                .ports(node.getPorts())
                .reads(node.getReads())
                .writes(node.getWrites())
                .configuration(configuration)
                .hooks(node.getHooks());
        if (node.getExecution() != null) {
            builder.execution(node.getExecution());
        }
        return builder.build();
    }
}
