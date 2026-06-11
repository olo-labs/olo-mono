package org.olo.annotation.processor;

import org.olo.annotation.OloExecutionModel;
import org.olo.annotation.OloRetryPolicy;
import org.olo.annotation.catalog.CatalogRuntimeDescriptor;
import org.olo.spi.runtime.RuntimeCapability;
import org.olo.spi.runtime.RuntimeCapabilities;

import java.util.ArrayList;
import java.util.List;

/** Materializes {@link CatalogRuntimeDescriptor} from extension annotations. */
final class CatalogRuntimePopulator {

    private CatalogRuntimePopulator() {
    }

    static CatalogRuntimeDescriptor create(
            String runtimeContractVersion,
            OloExecutionModel executionModel,
            boolean retryable,
            boolean timeoutAware,
            String defaultTimeout,
            OloRetryPolicy defaultRetryPolicy,
            boolean supportsAsyncCompletion,
            boolean supportsHeartbeat,
            boolean supportsDebugging,
            boolean supportsReplay,
            boolean supportsCheckpointing) {
        CatalogRuntimeDescriptor runtime = new CatalogRuntimeDescriptor();
        runtime.contractVersion = CatalogDefaults.materializeRuntimeContractVersion(runtimeContractVersion);
        runtime.executionModel = executionModel.name();
        runtime.capabilities =
                RuntimeCapabilities.materializeDeviations(
                        materializeCapabilities(
                                supportsDebugging,
                                supportsReplay,
                                supportsCheckpointing,
                                retryable,
                                timeoutAware,
                                supportsAsyncCompletion,
                                supportsHeartbeat));
        runtime.defaultTimeout = CatalogDefaults.blankToNull(defaultTimeout);
        runtime.defaultRetryPolicy =
                defaultRetryPolicy == null || defaultRetryPolicy == OloRetryPolicy.NONE
                        ? null
                        : defaultRetryPolicy.name();
        return runtime;
    }

    private static List<RuntimeCapability> materializeCapabilities(
            boolean supportsDebugging,
            boolean supportsReplay,
            boolean supportsCheckpointing,
            boolean retryable,
            boolean timeoutAware,
            boolean supportsAsyncCompletion,
            boolean supportsHeartbeat) {
        List<RuntimeCapability> capabilities = new ArrayList<>();
        if (supportsDebugging) {
            capabilities.add(RuntimeCapability.DEBUG);
        }
        if (supportsReplay) {
            capabilities.add(RuntimeCapability.REPLAY);
        }
        if (supportsCheckpointing) {
            capabilities.add(RuntimeCapability.CHECKPOINT);
        }
        if (retryable) {
            capabilities.add(RuntimeCapability.RETRY);
        }
        if (timeoutAware) {
            capabilities.add(RuntimeCapability.TIMEOUT);
        }
        if (supportsAsyncCompletion) {
            capabilities.add(RuntimeCapability.ASYNC_COMPLETION);
        }
        if (supportsHeartbeat) {
            capabilities.add(RuntimeCapability.HEARTBEAT);
        }
        return List.copyOf(capabilities);
    }
}
