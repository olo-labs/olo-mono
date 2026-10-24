// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.kernel.knowledge;

public final class OloDocumentWorkflowImpl implements OloDocumentWorkflow {
    @Override
    public String execute(DocumentOperation operation) {
        return GovernedActivity.execute(
                java.time.Duration.ofMinutes(2), "knowledge-document", String.class, operation);
    }
}
