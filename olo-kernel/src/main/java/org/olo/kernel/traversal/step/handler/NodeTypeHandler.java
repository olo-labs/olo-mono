package org.olo.kernel.traversal.step.handler;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.spi.node.NodeResult;

/**
 * Executes a single graph node during traversal.
 */
public interface NodeTypeHandler {

    boolean supports(String nodeType);

    NodeResult execute(KernelRuntimeContext context, NodeDefinition node);
}
