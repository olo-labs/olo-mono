package org.olo.definition.dynamicgraph;

/**
 * Configuration for inline agent-delegation synthesis nodes created after child workflow dispatch.
 */
public final class AgentSynthesisSupport {

    public static final String CONFIG_AGENT_SYNTHESIS = "agentSynthesis";

    private AgentSynthesisSupport() {
    }

    public static boolean isAgentSynthesis(org.olo.definition.node.NodeDefinition node) {
        return node != null
                && node.getConfiguration() != null
                && Boolean.TRUE.equals(node.getConfiguration().get(CONFIG_AGENT_SYNTHESIS));
    }
}
