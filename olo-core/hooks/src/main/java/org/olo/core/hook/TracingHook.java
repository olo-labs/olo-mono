package org.olo.core.hook;

import org.olo.annotation.OloHook;
import org.olo.annotation.OloHookPhase;
import org.olo.annotation.OloStability;
import org.olo.spi.annotation.ImplementationId;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.hook.Hook;
import org.olo.spi.hook.HookRequest;
import org.olo.spi.hook.HookResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

@OloHook(
        implementationId = CoreHookIds.TRACING,
        name = "Tracing",
        description = "Generates a trace span id per hook invocation",
        category = "observability",
        emoji = "🧵",
        phases = {OloHookPhase.PRE, OloHookPhase.ON_ERROR, OloHookPhase.FINALLY},
        tags = {"tracing", "core"},
        stability = OloStability.EXPERIMENTAL)
@ImplementationId(CoreHookIds.TRACING)
public final class TracingHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger("org.olo.tracing");

    @Override
    public String implementationId() {
        return CoreHookIds.TRACING;
    }

    @Override
    public HookResult run(HookRequest request, ExecutionContext context) {
        String spanId = UUID.randomUUID().toString();
        log.debug(
                "trace spanId={} phase={} hookId={} correlationId={}",
                spanId,
                request.phase(),
                request.hookId(),
                context.getCorrelationId().orElse(""));
        return HookResult.ok(Map.of("spanId", spanId, "phase", request.phase().name()));
    }
}
