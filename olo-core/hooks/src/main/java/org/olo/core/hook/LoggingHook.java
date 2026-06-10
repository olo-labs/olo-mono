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
        implementationId = CoreHookIds.LOGGING,
        name = "Logging",
        description = "Structured log lines for hook phases",
        category = "observability",
        emoji = "📋",
        phases = {OloHookPhase.PRE, OloHookPhase.ON_ERROR, OloHookPhase.FINALLY},
        tags = {"logging", "core"})
@ImplementationId(CoreHookIds.LOGGING)
public final class LoggingHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(LoggingHook.class);

    @Override
    public String implementationId() {
        return CoreHookIds.LOGGING;
    }

    @Override
    public HookResult run(HookRequest request, ExecutionContext context) {
        log.info(
                "hook phase={} hookId={} nodeId={} nodeType={} runId={} queue={}",
                request.phase(),
                request.hookId(),
                request.nodeId(),
                request.nodeType(),
                context.getRunId(),
                context.getQueue());
        return HookResult.ok(Map.of("logged", true));
    }
}
