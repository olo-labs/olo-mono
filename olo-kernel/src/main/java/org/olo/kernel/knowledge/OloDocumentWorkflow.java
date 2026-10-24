// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.kernel.knowledge;

@io.temporal.workflow.WorkflowInterface
public interface OloDocumentWorkflow {
    @io.temporal.workflow.WorkflowMethod
    String execute(DocumentOperation operation);
}
