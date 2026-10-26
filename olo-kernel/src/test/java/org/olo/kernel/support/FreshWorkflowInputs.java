/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.support;

import org.olo.input.model.WorkflowInput;

/** Self-contained input for clean-checkout kernel tests; no generated sample files. */
public final class FreshWorkflowInputs {
    private FreshWorkflowInputs() {}

    public static WorkflowInput minimal() throws java.io.IOException {
        return WorkflowInput.fromJson("""
                {"version":"1.0","inputs":[{"name":"query","displayName":"Query",
                  "type":"STRING","storage":{"mode":"LOCAL"},"value":"Hello, OLO"}],
                  "routing":{"pipeline":"default-pipeline","transactionType":"QUESTION_ANSWER",
                  "transactionId":"fresh-kernel-entry"}}
                """);
    }
}
