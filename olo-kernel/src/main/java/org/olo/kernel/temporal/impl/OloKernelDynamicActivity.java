/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.temporal.impl;

import io.temporal.activity.Activity;
import io.temporal.activity.DynamicActivity;
import io.temporal.common.converter.DataConverterException;
import io.temporal.common.converter.EncodedValues;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.KernelEntryPoint;
import org.olo.kernel.KernelRuntimeHolder;
import org.olo.kernel.temporal.KernelActivityOperations;
import org.olo.kernel.traversal.KernelExecutionSnapshot;

/**
 * Handles dynamically named Temporal activities ({@code id:label}) for kernel execution.
 */
public final class OloKernelDynamicActivity implements DynamicActivity {

    @Override
    public Object execute(EncodedValues args) {
        return switch (args.getSize()) {
            case 1 -> KernelEntryPoint.reportWorkflowResult(args.get(0, KernelExecutionSnapshot.class));
            case 2 -> dispatchTwoArgumentActivity(args);
            default -> throw unsupportedArgs(args.getSize());
        };
    }

    private static Object dispatchTwoArgumentActivity(EncodedValues args) {
        try {
            String operation = args.get(1, String.class);
            if (KernelActivityOperations.STEP.equals(operation)) {
                return KernelEntryPoint.executeTraversalStep(args.get(0, KernelExecutionSnapshot.class));
            }
        } catch (DataConverterException ignored) {
            // Second argument is not a traversal operation code (e.g. WorkflowInput for context build).
        }

        String queue = args.get(0, String.class);
        WorkflowInput input = args.get(1, WorkflowInput.class);
        if (queue == null || input == null) {
            throw unsupportedArgs(2);
        }
        return KernelEntryPoint.buildContextAndNotifyUi(queue, input, KernelRuntimeHolder.registry());
    }

    private static IllegalArgumentException unsupportedArgs(int argCount) {
        return new IllegalArgumentException(
                "unsupported dynamic kernel activity with " + argCount + " argument(s) for type "
                        + Activity.getExecutionContext().getInfo().getActivityType());
    }
}
