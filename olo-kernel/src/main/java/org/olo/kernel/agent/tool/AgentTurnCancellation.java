/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.agent.tool;

import org.olo.kernel.KernelRuntimeHolder;
import org.olo.kernel.agent.impl.DefaultLlmInvocationService;
import org.olo.kernel.traversal.KernelExecutionSnapshot;

/** Runs only in a cleanup activity: obtains fresh caller credentials and cancels recorded operations. */
public final class AgentTurnCancellation {
    private AgentTurnCancellation() {}
    public static void cancel(KernelExecutionSnapshot snapshot) {
        var access = KernelRuntimeHolder.toolAccess();
        if (!access.isEnabled()) return;
        var context = snapshot.toContext();
        for (var node : context.getGraph().getNodes()) {
            if (!(context.getVariables().get(AgentToolLoop.TURN_VARIABLE_PREFIX + node.getId()) != null)) continue;
            var settings = AgentToolSettings.resolve(context.getGraph(), node, access.defaultMaxToolRounds());
            String tenant = context.getInput().getContext().getTenantId();
            access.identityPolicy().authorize(tenant, context.getGraph().getId(), settings.agentName());
            AgentToolLoop.cancelPending(context, node, DefaultLlmInvocationService.caller(context, access, settings), settings.agentName());
        }
    }
}
