package org.olo.annotation.processor;

import org.olo.annotation.OloConnectionPolicy;
import org.olo.annotation.catalog.ConnectionPolicyDefaults;
import org.olo.annotation.catalog.ConnectionPolicyDescriptor;

/** Materializes {@link ConnectionPolicyDescriptor} from {@link OloConnectionPolicy}. */
final class ConnectionPolicyPopulator {

    private ConnectionPolicyPopulator() {}

    static ConnectionPolicyDescriptor from(OloConnectionPolicy policy) {
        if (policy == null) {
            return null;
        }
        int maxInputs = policy.maxInputs();
        int maxOutputs = policy.maxOutputs();
        if (maxInputs == ConnectionPolicyDefaults.DEFAULT_MAX_INPUTS
                && maxOutputs == ConnectionPolicyDefaults.DEFAULT_MAX_OUTPUTS) {
            return null;
        }
        ConnectionPolicyDescriptor descriptor = new ConnectionPolicyDescriptor();
        descriptor.maxInputs = maxInputs;
        descriptor.maxOutputs = maxOutputs;
        return descriptor;
    }
}
