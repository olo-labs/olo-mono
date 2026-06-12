package org.olo.kernel.traversal.context.impl;

import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.context.ExecutionContextFactory;
import org.olo.spi.context.ExecutionContext;

public final class KernelExecutionContextFactory implements ExecutionContextFactory {

    @Override
    public ExecutionContext create(KernelRuntimeContext context, String nodeId) {
        return VariableScopeBridge.toExecutionContext(context, nodeId);
    }
}
