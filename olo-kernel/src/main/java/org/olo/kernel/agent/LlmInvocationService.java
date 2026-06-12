package org.olo.kernel.agent;

import org.olo.kernel.context.KernelRuntimeContext;

/**
 * Renders the workflow default prompt and invokes the routed model provider.
 */
public interface LlmInvocationService {

    LlmInvocationResult invoke(KernelRuntimeContext context);
}
