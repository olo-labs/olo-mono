/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.input;

import org.olo.kernel.context.KernelRuntimeContext;

/**
 * Seeds workflow variables from the invocation input before traversal begins.
 */
public interface WorkflowInputBinder {

    void bind(KernelRuntimeContext context);
}
