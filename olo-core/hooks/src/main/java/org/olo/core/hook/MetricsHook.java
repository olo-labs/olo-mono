package org.olo.core.hook;

import org.olo.annotation.OloHook;
import org.olo.annotation.OloHookPhase;
import org.olo.spi.annotation.ImplementationId;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.hook.Hook;
import org.olo.spi.hook.HookRequest;
import org.olo.spi.hook.HookResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@OloHook(
        implementationId = CoreHookIds.METRICS,
        name = "Metrics",
        description = "Lightweight metrics events (log-based stub)",
        category = "observability",
        emoji = "📊",
        phases = {OloHookPhase.PRE, OloHookPhase.FINALLY},
        tags = {"metrics", "core"})
@ImplementationId(CoreHookIds.METRICS)
public final class MetricsHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger("org.olo.metrics");

    @Override
    public String implementationId() {
        return CoreHookIds.METRICS;
    }

    @Override
    public HookResult run(HookRequest request, ExecutionContext context) {
        log.info(
                "metric event=hook phase={} hookId={} workflowId={} runId={}",
                request.phase(),
                request.hookId(),
                context.getWorkflowId(),
                context.getRunId());
        return HookResult.ok(Map.of("metricRecorded", true, "phase", request.phase().name()));
    }
}
