package org.olo.kernel.traversal.input;

import org.olo.kernel.context.KernelRuntimeContext;

/**
 * Seeds workflow variables from the invocation input before traversal begins.
 */
public interface WorkflowInputBinder {

    void bind(KernelRuntimeContext context);
}
