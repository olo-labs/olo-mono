/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.designer;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.port.PortWireType;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared assertions for Studio-ready workflow presets (olo-ui builder).
 */
public final class StudioDesignerAssertions {

    private StudioDesignerAssertions() {
    }

    public static void assertStudioBuildReady(WorkflowDefinition workflow) {
        assertStudioBuildReady(workflow, "START", "AGENT", "END");
    }

    public static void assertStudioToolPipelineBuildReady(WorkflowDefinition workflow) {
        assertStudioBuildReady(workflow, "START", "TOOL", "END");
    }

    private static void assertStudioBuildReady(WorkflowDefinition workflow, String... expectedNodeTypes) {
        DesignerDefinition designer = workflow.getDesigner();
        assertThat(designer).isNotNull();
        assertThat(designer.getNodeSize()).isNotNull();
        assertThat(designer.getNodeSize().getWidth()).isEqualTo(StudioDesignerDefaults.NODE_WIDTH);
        assertThat(designer.getNodeSize().getHeight()).isEqualTo(StudioDesignerDefaults.NODE_HEIGHT);
        assertThat(designer.getLayout()).isEqualTo(StudioDesignerDefaults.layout());
        assertThat(designer.getCanvas()).isEqualTo(StudioDesignerDefaults.canvas());
        assertThat(designer.getPortColors()).isEqualTo(StudioDesignerDefaults.portColors());
        assertThat(designer.getNodeTypes().keySet()).containsExactlyInAnyOrder(expectedNodeTypes);
        assertThat(designer.getNodeTypes().get("START").getInlineProperties()).isNotEmpty();
        assertThat(designer.getNodeTypes().get("END").getInlineProperties()).isNotEmpty();
        if (java.util.Arrays.asList(expectedNodeTypes).contains("AGENT")) {
            assertThat(designer.getNodeTypes().get("AGENT").getInlineProperties()).isNotEmpty();
            assertThat(designer.getNodeTypes().get("AGENT").getEmoji()).isEqualTo(workflow.getEmoji());
        }
        if (java.util.Arrays.asList(expectedNodeTypes).contains("TOOL")) {
            assertThat(designer.getNodeTypes().get("TOOL").getInlineProperties()).isNotEmpty();
        }

        List<NodeDefinition> nodes = workflow.getNodes();
        assertThat(nodes).isNotEmpty();
        for (NodeDefinition node : nodes) {
            @SuppressWarnings("unchecked")
            Map<String, Object> configuration = node.getConfiguration();
            assertThat(configuration)
                    .as("node %s must expose configuration.designer.position", node.getId())
                    .containsKey("designer");
            @SuppressWarnings("unchecked")
            Map<String, Object> nodeDesigner = (Map<String, Object>) configuration.get("designer");
            assertThat(nodeDesigner).containsKey("position");
            @SuppressWarnings("unchecked")
            Map<String, Object> position = (Map<String, Object>) nodeDesigner.get("position");
            assertThat(position.get("x")).isInstanceOf(Number.class);
            assertThat(position.get("y")).isInstanceOf(Number.class);
            if ("AGENT".equalsIgnoreCase(node.getType())) {
                if (isAgentPlugin(node)) {
                    assertAgentPluginPorts(node);
                } else {
                    assertAgentHostPorts(node);
                }
            }
            if ("TOOL".equalsIgnoreCase(node.getType()) || "HOOK".equalsIgnoreCase(node.getType())) {
                assertCapabilityPluginPorts(node);
            }
        }
    }

    public static void assertCapabilityPluginPorts(NodeDefinition pluginNode) {
        List<PortDefinition> ports = pluginNode.getPorts();
        assertThat(ports).anyMatch(port -> isMessagePort(port, PortDirection.INPUT));
        assertThat(ports).anyMatch(port -> isMessagePort(port, PortDirection.OUTPUT));
        assertThat(ports).anyMatch(port -> isPluginPort(
                port, PortDirection.OUTPUT, PortWireType.CAPABILITIES.wireName(), "capabilities"));
    }

    public static void assertAgentPluginPorts(NodeDefinition pluginNode) {
        List<PortDefinition> ports = pluginNode.getPorts();
        assertThat(ports).anyMatch(port -> isMessagePort(port, PortDirection.INPUT));
        assertThat(ports).anyMatch(port -> isMessagePort(port, PortDirection.OUTPUT));
        assertThat(ports).anyMatch(port -> isPluginPort(
                port, PortDirection.OUTPUT, PortWireType.AGENT_PLUG.wireName(), "agentPlug"));
    }

    public static void assertAgentHostPorts(NodeDefinition agentNode) {
        List<PortDefinition> ports = agentNode.getPorts();
        assertThat(ports).anyMatch(port -> isMessagePort(port, PortDirection.INPUT));
        assertThat(ports).anyMatch(port -> isMessagePort(port, PortDirection.OUTPUT));
        assertThat(ports).anyMatch(port -> isPluginPort(port, PortDirection.INPUT, PortWireType.CAPABILITIES.wireName(), "capabilities"));
        assertThat(ports).anyMatch(port -> isPluginPort(port, PortDirection.INPUT, PortWireType.AGENT_PLUG.wireName(), "agentPlug"));
    }

    private static boolean isAgentPlugin(NodeDefinition node) {
        if (node.getConfiguration() == null) {
            return false;
        }
        Object delegateAgentId = node.getConfiguration().get("delegateAgentId");
        return delegateAgentId instanceof String id && !id.isBlank();
    }

    private static boolean isMessagePort(PortDefinition port, PortDirection direction) {
        return port.getDirection() == direction
                && PortWireType.MESSAGE.wireName().equals(port.getType())
                && "#ef4444".equals(port.getUi() == null ? null : port.getUi().getColor());
    }

    private static boolean isPluginPort(
            PortDefinition port, PortDirection direction, String wireType, String portId) {
        return portId.equals(port.getId())
                && port.getDirection() == direction
                && wireType.equals(port.getType());
    }
}
