package org.olo.kernel.traversal.request;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.spi.node.NodeRequest;

/**
 * Builds {@link NodeRequest} instances from graph nodes and runtime variables.
 */
public interface NodeRequestFactory {

    NodeRequest create(KernelRuntimeContext context, NodeDefinition node);
}
