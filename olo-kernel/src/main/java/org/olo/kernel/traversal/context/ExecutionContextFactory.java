package org.olo.kernel.traversal.context;

import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.spi.context.ExecutionContext;

/**
 * Creates SPI execution contexts backed by kernel runtime variables.
 */
public interface ExecutionContextFactory {

    ExecutionContext create(KernelRuntimeContext context, String nodeId);
}
