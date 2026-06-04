package io.olo.definition.validation;

import io.olo.definition.edge.EdgeDefinition;
import io.olo.definition.error.ErrorRoute;
import io.olo.definition.error.OnFailureDefinition;
import io.olo.definition.error.RetryPolicy;
import io.olo.definition.human.HumanApprovalDefinition;
import io.olo.definition.execution.ExecutionKind;
import io.olo.definition.node.NodeType;
import io.olo.definition.parallel.JoinDefinition;
import io.olo.definition.parallel.JoinStrategy;
import io.olo.definition.workflow.WorkflowReferenceDefinition;
import io.olo.definition.extension.ExtensionDefinition;
import io.olo.definition.model.ModelProviderDefinition;
import io.olo.definition.model.ModelRoutingDefinition;
import io.olo.definition.node.NodeDefinition;
import io.olo.definition.node.NodeRouterDefinition;
import io.olo.definition.port.PortDefinition;
import io.olo.definition.input.WorkflowInputDefinition;
import io.olo.definition.path.DataPath;
import io.olo.definition.path.DataPathParseResult;
import io.olo.definition.path.DataPathParser;
import io.olo.definition.path.PathRoot;
import io.olo.definition.state.EffectiveStateFields;
import io.olo.definition.state.StateFieldDefinition;
import io.olo.definition.variable.VariableDefinition;
import io.olo.definition.agent.AgentDefinition;
import io.olo.definition.capability.CapabilityDefinition;
import io.olo.definition.capability.CapabilityValidator;
import io.olo.definition.runtime.RuntimeBindingValidator;
import io.olo.definition.tool.ToolDefinition;
import io.olo.definition.workflow.WorkflowDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Structural validation for workflow definitions (no runtime or execution checks).
 */
public final class WorkflowValidator {

    private WorkflowValidator() {
    }

    public static ValidationResult validate(WorkflowDefinition workflow) {
        List<String> errors = new ArrayList<>();
        if (workflow == null) {
            return ValidationResult.failure(List.of("workflow must not be null"));
        }
        if (isBlank(workflow.getId())) {
            errors.add("workflow id is required");
        }
        validateCapability(workflow, errors);
        RuntimeBindingValidator.validate("workflow " + workflow.getId(), workflow.getRuntimeBinding(), errors);

        Set<String> nodeIds = new HashSet<>();
        Map<String, NodeDefinition> nodesById = new HashMap<>();
        for (NodeDefinition node : workflow.getNodes()) {
            if (node == null) {
                errors.add("node entry must not be null");
                continue;
            }
            if (isBlank(node.getId())) {
                errors.add("node id is required");
            } else if (!nodeIds.add(node.getId())) {
                errors.add("duplicate node id: " + node.getId());
            } else {
                nodesById.put(node.getId(), node);
            }
            if (isBlank(node.getType())) {
                errors.add("node type is required for node: " + node.getId());
            }
            if (!isBlank(node.getId())) {
                validatePorts(node, errors);
                validateNodeCapability(node, errors);
                validateNodeRuntimeBinding(node, errors);
            }
        }
        Set<String> stateFieldNames = EffectiveStateFields.names(workflow);
        Set<String> inputFieldNames = new HashSet<>(workflow.getInputs().keySet());
        Set<String> parameterFieldNames = new HashSet<>(workflow.getParameters().keySet());
        EffectiveStateFields.validateDeclarations(workflow, errors);

        for (NodeDefinition node : workflow.getNodes()) {
            if (node != null) {
                validateNodeRouters(node, nodeIds, errors);
                validateOnFailure(node, nodeIds, errors);
                validateHumanApproval(node, errors);
                validateExecutionMapping(node, errors);
                validateWorkflowReference(node, errors);
                validateParallelJoin(node, errors);
                validateDataPathAccess(node, stateFieldNames, inputFieldNames, parameterFieldNames, errors);
            }
        }

        for (Map.Entry<String, WorkflowInputDefinition> entry : workflow.getInputs().entrySet()) {
            if (isBlank(entry.getKey())) {
                errors.add("input name is required");
            } else if (entry.getValue() == null) {
                errors.add("input definition must not be null for: " + entry.getKey());
            }
        }

        for (Map.Entry<String, StateFieldDefinition> entry : workflow.getState().entrySet()) {
            if (isBlank(entry.getKey())) {
                errors.add("state field name is required");
            } else if (entry.getValue() == null) {
                errors.add("state field definition must not be null for: " + entry.getKey());
            }
        }

        Set<String> variableNames = new HashSet<>();
        for (VariableDefinition variable : workflow.getVariables()) {
            if (variable == null) {
                errors.add("variable entry must not be null");
                continue;
            }
            if (isBlank(variable.getName())) {
                errors.add("variable name is required");
            } else if (!variableNames.add(variable.getName())) {
                errors.add("duplicate variable name: " + variable.getName());
            }
        }

        Set<String> providerIds = new HashSet<>();
        for (ModelProviderDefinition provider : workflow.getModelProviders()) {
            if (provider == null) {
                errors.add("model provider entry must not be null");
                continue;
            }
            if (isBlank(provider.getId())) {
                errors.add("model provider id is required");
            } else if (!providerIds.add(provider.getId())) {
                errors.add("duplicate model provider id: " + provider.getId());
            }
        }

        for (ModelRoutingDefinition routing : workflow.getModelRouting()) {
            if (routing != null && !isBlank(routing.getDefaultProviderId())) {
                String defaultId = routing.getDefaultProviderId();
                if (!providerIds.isEmpty() && !providerIds.contains(defaultId)) {
                    errors.add("model routing references unknown provider: " + defaultId);
                }
            }
        }

        Set<String> registryIds = new HashSet<>();
        for (ToolDefinition tool : workflow.getTools()) {
            if (tool == null) {
                errors.add("tool entry must not be null");
                continue;
            }
            if (isBlank(tool.getId())) {
                errors.add("tool id is required");
            } else if (!registryIds.add(tool.getId())) {
                errors.add("duplicate registry id: " + tool.getId());
            } else {
                validateTool(tool, errors);
            }
        }
        for (AgentDefinition agent : workflow.getAgents()) {
            if (agent == null) {
                errors.add("agent entry must not be null");
                continue;
            }
            if (isBlank(agent.getId())) {
                errors.add("agent id is required");
            } else if (!registryIds.add(agent.getId())) {
                errors.add("duplicate registry id: " + agent.getId());
            } else {
                validateAgent(agent, errors);
            }
        }

        Set<String> extensionIds = new HashSet<>();
        for (ExtensionDefinition extension : workflow.getExtensions()) {
            if (extension == null) {
                errors.add("extension entry must not be null");
                continue;
            }
            if (isBlank(extension.getId())) {
                errors.add("extension id is required");
            } else if (!extensionIds.add(extension.getId())) {
                errors.add("duplicate extension id: " + extension.getId());
            }
        }

        int edgeIndex = 0;
        for (EdgeDefinition edge : workflow.getEdges()) {
            if (edge == null) {
                errors.add("edge entry must not be null");
                edgeIndex++;
                continue;
            }
            String prefix = "edge[" + edgeIndex + "]: ";
            if (isBlank(edge.getSourceNodeId())) {
                errors.add(prefix + "sourceNodeId is required");
            } else if (!nodeIds.contains(edge.getSourceNodeId())) {
                errors.add(prefix + "unknown source node: " + edge.getSourceNodeId());
            }
            if (isBlank(edge.getTargetNodeId())) {
                errors.add(prefix + "targetNodeId is required");
            } else if (!nodeIds.contains(edge.getTargetNodeId())) {
                errors.add(prefix + "unknown target node: " + edge.getTargetNodeId());
            }
            if (edge.getSourceNodeId() != null
                    && edge.getSourceNodeId().equals(edge.getTargetNodeId())) {
                errors.add(prefix + "self-loop on node: " + edge.getSourceNodeId());
            }
            validateEdgePorts(edge, nodesById, prefix, errors);
            edgeIndex++;
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.failure(errors);
    }

    public static void validateOrThrow(WorkflowDefinition workflow) {
        ValidationResult result = validate(workflow);
        if (!result.valid()) {
            throw new WorkflowValidationException(result.errors());
        }
    }

    private static void validateHumanApproval(NodeDefinition node, List<String> errors) {
        String nodeId = node.getId();
        boolean isHuman = NodeType.HUMAN.value().equals(node.getType());
        HumanApprovalDefinition approval = node.getApproval();
        if (approval != null && !isHuman) {
            errors.add("approval is only valid on HUMAN nodes, found on: " + nodeId);
            return;
        }
        if (!isHuman) {
            return;
        }
        if (approval == null) {
            errors.add("HUMAN node " + nodeId + " requires an approval block");
            return;
        }
        if (isBlank(approval.getTitle())) {
            errors.add("approval title is required on HUMAN node: " + nodeId);
        }
        if (approval.getApprovers().isEmpty()) {
            errors.add("approval approvers must not be empty on HUMAN node: " + nodeId);
        }
        if (approval.getTimeoutSeconds() != null && approval.getTimeoutSeconds() < 0) {
            errors.add("approval timeoutSeconds must be >= 0 on HUMAN node: " + nodeId);
        }
    }

    private static void validateWorkflowReference(NodeDefinition node, List<String> errors) {
        String nodeId = node.getId();
        String type = node.getType();
        WorkflowReferenceDefinition workflow = node.getWorkflow();

        if (NodeType.AGENT.value().equals(type) && workflow == null) {
            errors.add("AGENT node " + nodeId + " requires a workflow reference (agent = workflow)");
        }
        if (NodeType.WORKFLOW_REF.value().equals(type) && workflow == null) {
            errors.add("WORKFLOW_REF node " + nodeId + " requires a workflow reference");
        }
        if (workflow != null
                && !NodeType.WORKFLOW_REF.value().equals(type)
                && !NodeType.AGENT.value().equals(type)) {
            errors.add("workflow is only valid on AGENT or WORKFLOW_REF nodes, found on: " + nodeId);
        }
        if ("SUBGRAPH".equals(type) || "SUBWORKFLOW".equals(type)) {
            errors.add(
                    "type "
                            + type
                            + " on node "
                            + nodeId
                            + " is not supported; use WORKFLOW_REF with workflow instead");
        }
    }

    private static void validateParallelJoin(NodeDefinition node, List<String> errors) {
        String nodeId = node.getId();
        JoinDefinition join = node.getJoin();
        if (NodeType.PARALLEL.value().equals(node.getType())) {
            if (join == null) {
                errors.add("PARALLEL node " + nodeId + " requires a join definition");
                return;
            }
            if (join.getStrategy() == null) {
                errors.add("join strategy is required on PARALLEL node: " + nodeId);
            }
            if (join.getStrategy() == JoinStrategy.QUORUM
                    && (join.getQuorumCount() == null || join.getQuorumCount() < 1)) {
                errors.add("join quorumCount must be >= 1 when strategy is QUORUM on node: " + nodeId);
            }
        } else if (join != null) {
            errors.add("join is only valid on PARALLEL nodes, found on: " + nodeId);
        }
    }

    private static void validateExecutionMapping(NodeDefinition node, List<String> errors) {
        String nodeId = node.getId();
        String type = node.getType();
        ExecutionKind kind = node.getExecutionKind();
        if (kind == ExecutionKind.HUMAN_WAIT && !NodeType.HUMAN.value().equals(type)) {
            errors.add("executionKind HUMAN_WAIT requires type HUMAN on node: " + nodeId);
        }
        if (NodeType.HUMAN.value().equals(type)
                && kind != null
                && kind != ExecutionKind.HUMAN_WAIT
                && kind != ExecutionKind.EVENT) {
            errors.add("executionKind on HUMAN node " + nodeId + " should be HUMAN_WAIT or EVENT");
        }
    }

    private static void validateOnFailure(NodeDefinition node, Set<String> nodeIds, List<String> errors) {
        OnFailureDefinition onFailure = node.getOnFailure();
        if (onFailure == null) {
            return;
        }
        String nodeId = node.getId();
        if (onFailure.getRetry() == null && onFailure.getRoute() == null) {
            errors.add("onFailure on node " + nodeId + " must declare retry and/or route");
            return;
        }
        RetryPolicy retry = onFailure.getRetry();
        if (retry != null) {
            if (retry.getAttempts() < 1) {
                errors.add("onFailure retry attempts must be >= 1 on node: " + nodeId);
            }
            if (retry.getInitialDelayMs() != null && retry.getInitialDelayMs() < 0) {
                errors.add("onFailure retry initialDelayMs must be >= 0 on node: " + nodeId);
            }
            if (retry.getMaxDelayMs() != null && retry.getMaxDelayMs() < 0) {
                errors.add("onFailure retry maxDelayMs must be >= 0 on node: " + nodeId);
            }
        }
        ErrorRoute route = onFailure.getRoute();
        if (route != null) {
            if (isBlank(route.getTargetNodeId())) {
                errors.add("onFailure route targetNodeId is required on node: " + nodeId);
            } else if (!nodeIds.contains(route.getTargetNodeId())) {
                errors.add(
                        "onFailure route on node "
                                + nodeId
                                + " references unknown target node: "
                                + route.getTargetNodeId());
            } else if (route.getTargetNodeId().equals(nodeId)) {
                errors.add("onFailure route on node " + nodeId + " must not target the same node");
            }
        }
    }

    private static void validateNodeRouters(
            NodeDefinition node, Set<String> nodeIds, List<String> errors) {
        Set<String> routerIds = new HashSet<>();
        for (NodeRouterDefinition router : node.getRouters()) {
            if (router == null) {
                errors.add("router entry must not be null on node: " + node.getId());
                continue;
            }
            if (!isBlank(router.getId()) && !routerIds.add(router.getId())) {
                errors.add("duplicate router id '" + router.getId() + "' on node: " + node.getId());
            }
            if (!isBlank(router.getTargetNodeId()) && !nodeIds.contains(router.getTargetNodeId())) {
                errors.add(
                        "router on node "
                                + node.getId()
                                + " references unknown target node: "
                                + router.getTargetNodeId());
            }
        }
    }

    private static void validatePorts(NodeDefinition node, List<String> errors) {
        validatePortList(node.getId(), "input", node.getInputs(), errors);
        validatePortList(node.getId(), "output", node.getOutputs(), errors);
    }

    private static void validatePortList(
            String nodeId, String kind, List<PortDefinition> ports, List<String> errors) {
        Set<String> names = new HashSet<>();
        for (PortDefinition port : ports) {
            if (port == null) {
                errors.add("null " + kind + " port on node: " + nodeId);
                continue;
            }
            if (isBlank(port.getName())) {
                errors.add(kind + " port name is required on node: " + nodeId);
            } else if (!names.add(port.getName())) {
                errors.add("duplicate " + kind + " port '" + port.getName() + "' on node: " + nodeId);
            }
            if (isBlank(port.getSchema())) {
                errors.add(
                        kind
                                + " port '"
                                + port.getName()
                                + "' on node "
                                + nodeId
                                + " requires a schema");
            }
        }
    }

    private static void validateEdgePorts(
            EdgeDefinition edge,
            Map<String, NodeDefinition> nodesById,
            String prefix,
            List<String> errors) {
        NodeDefinition source = nodesById.get(edge.getSourceNodeId());
        NodeDefinition target = nodesById.get(edge.getTargetNodeId());
        if (source == null || target == null) {
            return;
        }

        PortResolution sourcePort =
                resolvePort(edge.getSourcePort(), source.getOutputs(), "output", source.getId(), prefix, errors);
        PortResolution targetPort =
                resolvePort(edge.getTargetPort(), target.getInputs(), "input", target.getId(), prefix, errors);

        if (sourcePort.port != null && targetPort.port != null) {
            if (!SchemaCompatibility.compatible(sourcePort.port.getSchema(), targetPort.port.getSchema())) {
                errors.add(
                        prefix
                                + "schema mismatch: output port '"
                                + sourcePort.port.getName()
                                + "' on node "
                                + source.getId()
                                + " ("
                                + sourcePort.port.getSchema()
                                + ") is not compatible with input port '"
                                + targetPort.port.getName()
                                + "' on node "
                                + target.getId()
                                + " ("
                                + targetPort.port.getSchema()
                                + ")");
            }
        }
    }

    private static PortResolution resolvePort(
            String portName,
            List<PortDefinition> ports,
            String kind,
            String nodeId,
            String prefix,
            List<String> errors) {
        if (ports.isEmpty()) {
            return PortResolution.unresolved(portName);
        }

        String resolvedName = portName;
        if (isBlank(resolvedName)) {
            if (ports.size() == 1) {
                resolvedName = ports.get(0).getName();
            } else {
                errors.add(
                        prefix
                                + kind
                                + "Port is required on node "
                                + nodeId
                                + " (declares "
                                + ports.size()
                                + " "
                                + kind
                                + " ports)");
                return PortResolution.unresolved(null);
            }
        }

        for (PortDefinition port : ports) {
            if (port != null && resolvedName.equals(port.getName())) {
                return new PortResolution(resolvedName, port);
            }
        }
        errors.add(
                prefix
                        + "unknown "
                        + kind
                        + " port '"
                        + resolvedName
                        + "' on node "
                        + nodeId);
        return PortResolution.unresolved(resolvedName);
    }

    private record PortResolution(String name, PortDefinition port) {

        static PortResolution unresolved(String name) {
            return new PortResolution(name, null);
        }
    }

    private static void validateCapability(WorkflowDefinition workflow, List<String> errors) {
        CapabilityValidator.validate(
                workflow.getId(),
                workflow.getCapability(),
                CapabilityValidator.Context.WORKFLOW,
                errors);
    }

    private static void validateTool(ToolDefinition tool, List<String> errors) {
        if (!ToolDefinition.TYPE.equals(tool.getType())) {
            errors.add("tool " + tool.getId() + ": type must be " + ToolDefinition.TYPE);
        }
        CapabilityValidator.validate(tool.getId(), tool.getCapability(), CapabilityValidator.Context.TOOL, errors);
        RuntimeBindingValidator.validate("tool " + tool.getId(), tool.getRuntimeBinding(), errors);
    }

    private static void validateAgent(AgentDefinition agent, List<String> errors) {
        if (!AgentDefinition.TYPE.equals(agent.getType())) {
            errors.add("agent " + agent.getId() + ": type must be " + AgentDefinition.TYPE);
        }
        CapabilityValidator.validate(agent.getId(), agent.getCapability(), CapabilityValidator.Context.AGENT, errors);
        if (agent.getWorkflow() == null) {
            errors.add("agent " + agent.getId() + ": workflow reference is required");
        } else if (isBlank(agent.getWorkflow().getWorkflowId())) {
            errors.add("agent " + agent.getId() + ": workflow.workflowId is required");
        }
        RuntimeBindingValidator.validate("agent " + agent.getId(), agent.getRuntimeBinding(), errors);
    }

    private static void validateNodeRuntimeBinding(NodeDefinition node, List<String> errors) {
        if (node.getRuntimeBinding() == null) {
            return;
        }
        String type = node.getType();
        boolean allowed =
                NodeType.AGENT.value().equals(type)
                        || NodeType.WORKFLOW_REF.value().equals(type)
                        || NodeType.TOOL.value().equals(type);
        if (!allowed) {
            errors.add("runtimeBinding is only valid on AGENT, WORKFLOW_REF, or TOOL nodes, found on: " + node.getId());
            return;
        }
        RuntimeBindingValidator.validate("node " + node.getId(), node.getRuntimeBinding(), errors);
    }

    private static void validateNodeCapability(NodeDefinition node, List<String> errors) {
        if (node.getCapability() != null) {
            CapabilityValidator.validate(
                    node.getId(), node.getCapability(), CapabilityValidator.Context.NODE, errors);
        }
    }

    private static void validateDataPathAccess(
            NodeDefinition node,
            Set<String> stateFieldNames,
            Set<String> inputFieldNames,
            Set<String> parameterFieldNames,
            List<String> errors) {
        String nodeId = node.getId();
        for (String read : node.getReads()) {
            validateDataPath(nodeId, "read", read, false, stateFieldNames, inputFieldNames, parameterFieldNames, errors);
        }
        for (String write : node.getWrites()) {
            validateDataPath(nodeId, "write", write, true, stateFieldNames, inputFieldNames, parameterFieldNames, errors);
        }
    }

    private static void validateDataPath(
            String nodeId,
            String accessKind,
            String pathLiteral,
            boolean write,
            Set<String> stateFieldNames,
            Set<String> inputFieldNames,
            Set<String> parameterFieldNames,
            List<String> errors) {
        DataPathParseResult parsed = DataPathParser.parse(pathLiteral);
        if (!parsed.isSuccess()) {
            errors.add("node " + nodeId + ": invalid " + accessKind + " path '" + pathLiteral + "': "
                    + parsed.error().orElse("parse error"));
            return;
        }
        DataPath path = parsed.path().orElseThrow();
        if (write && path.root() != PathRoot.STATE) {
            errors.add("node " + nodeId + ": write path must use state. namespace, found: " + path.literal());
            return;
        }
        String topLevel = path.topLevelName();
        boolean declared = switch (path.root()) {
            case STATE -> stateFieldNames.contains(topLevel);
            case INPUT -> inputFieldNames.contains(topLevel);
            case PARAMETER -> parameterFieldNames.contains(topLevel);
        };
        if (!declared) {
            errors.add("node " + nodeId + " " + accessKind + "s unknown " + path.root().prefix() + " field: "
                    + path.literal() + " (no " + path.root().prefix() + " field '" + topLevel + "')");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
