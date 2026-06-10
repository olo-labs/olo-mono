package org.olo.kernel;

import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelContextBuildRequest;
import org.olo.kernel.context.KernelContextBuilder;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.callback.UiCallbackReporter;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.input.WorkflowReturnResolution;
import org.olo.kernel.input.WorkflowReturnResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Synchronous kernel entry point for queue execution: builds runtime context and notifies the UI.
 */
public final class KernelEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(KernelEntryPoint.class);

    private KernelEntryPoint() {
    }

    /**
     * Builds kernel runtime context for {@code queue} from a deserialized {@link WorkflowInput}.
     *
     * @return the primary user message from the workflow input
     */
    public static String execute(String queue, WorkflowInput input, WorkflowDefinitionRegistry registry) {
        Objects.requireNonNull(queue, "queue");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(registry, "registry");

        log.info("Kernel entry: queue={}, transactionId={}", queue, input.getRouting() != null
                ? input.getRouting().getTransactionId()
                : null);

        WorkflowDefinition sourceGraph = registry.findByQueue(queue)
                .orElseThrow(() -> new KernelException("no workflow definition registered for queue: " + queue));

        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of(queue, input, sourceGraph));
        if (!context.isGraphReady()) {
            throw new KernelException("workflow graph is not ready for queue: " + queue);
        }

        UiCallbackReporter.reportContextReady(context);
        WorkflowReturnResolution resolution = WorkflowReturnResolver.resolveDetails(context);
        logReturnBeforeCallback(queue, context, resolution);
        UiCallbackReporter.reportWorkflowResult(
                context,
                resolution.returnVariableName(),
                resolution.returnVariableValue(),
                resolution.message(),
                resolution.usedAdminFallback());
        log.info(
                "Kernel entry complete: queue={}, returnVariable={}, returnValue={}, messageLen={}, message={}",
                queue,
                resolution.returnVariableName(),
                formatLogValue(resolution.returnVariableValue()),
                resolution.message().length(),
                resolution.message());
        return resolution.message();
    }

    /**
     * Builds kernel runtime context from a JSON payload string (file-based tests and legacy callers).
     *
     * @return the primary user message from the workflow input
     */
    public static String execute(String queue, String inputPayload, WorkflowDefinitionRegistry registry) {
        Objects.requireNonNull(queue, "queue");
        Objects.requireNonNull(inputPayload, "inputPayload");
        Objects.requireNonNull(registry, "registry");

        WorkflowDefinition sourceGraph = registry.findByQueue(queue)
                .orElseThrow(() -> new KernelException("no workflow definition registered for queue: " + queue));

        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of(queue, inputPayload, sourceGraph));
        if (!context.isGraphReady()) {
            throw new KernelException("workflow graph is not ready for queue: " + queue);
        }

        UiCallbackReporter.reportContextReady(context);
        WorkflowReturnResolution resolution = WorkflowReturnResolver.resolveDetails(context);
        logReturnBeforeCallback(queue, context, resolution);
        UiCallbackReporter.reportWorkflowResult(
                context,
                resolution.returnVariableName(),
                resolution.returnVariableValue(),
                resolution.message(),
                resolution.usedAdminFallback());
        log.info(
                "Kernel entry complete: queue={}, returnVariable={}, returnValue={}, messageLen={}, message={}",
                queue,
                resolution.returnVariableName(),
                formatLogValue(resolution.returnVariableValue()),
                resolution.message().length(),
                resolution.message());
        return resolution.message();
    }

    private static void logReturnBeforeCallback(
            String queue, KernelRuntimeContext context, WorkflowReturnResolution resolution) {
        log.info(
                "Workflow return before callback: queue={}, returnVariable={}, returnValue={}, variables={}, message={}",
                queue,
                resolution.returnVariableName(),
                formatLogValue(resolution.returnVariableValue()),
                context.getVariableMap(),
                resolution.message());
    }

    private static String formatLogValue(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value);
        if (text.length() <= 120) {
            return text;
        }
        return text.substring(0, 120) + "...";
    }
}
