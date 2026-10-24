/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.temporal;

import io.temporal.activity.Activity;
import io.temporal.activity.DynamicActivity;
import io.temporal.common.converter.DataConverterException;
import io.temporal.common.converter.EncodedValues;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.KernelEntryPoint;
import org.olo.kernel.KernelRuntimeHolder;
import org.olo.kernel.human.HumanResumeInput;
import org.olo.kernel.traversal.KernelExecutionSnapshot;

/**
 * Handles dynamically named Temporal activities ({@code id:label}) for kernel execution.
 */
public final class OloKernelDynamicActivity implements DynamicActivity {

    @Override
    public Object execute(EncodedValues args) {
        if("knowledge-document".equals(Activity.getExecutionContext().getInfo().getActivityType())) return org.olo.kernel.knowledge.KnowledgeRuntime.document(args.get(0,org.olo.kernel.knowledge.DocumentOperation.class));
        return switch (args.getSize()) {
            case 1 -> KernelEntryPoint.reportWorkflowResult(args.get(0, KernelExecutionSnapshot.class));
            case 2 -> dispatchTwoArgumentActivity(args);
            case 3 -> dispatchThreeArgumentActivity(args);
            default -> throw unsupportedArgs(args.getSize());
        };
    }

    private static Object dispatchThreeArgumentActivity(EncodedValues args) {
        KernelExecutionSnapshot snapshot = args.get(0, KernelExecutionSnapshot.class);
        String operation = args.get(1, String.class);
        if (KernelActivityOperations.RESUME_HUMAN_INPUT.equals(operation)) {
            HumanResumeInput input = args.get(2, HumanResumeInput.class);
            return KernelEntryPoint.resumeHumanInput(snapshot, input);
        }
        throw unsupportedArgs(3);
    }

    private static Object dispatchTwoArgumentActivity(EncodedValues args) {
        try {
            String operation = args.get(1, String.class);
            if(operation.startsWith("knowledge-check:")) return org.olo.kernel.knowledge.KnowledgeCheckpoint.check(args.get(0,KernelExecutionSnapshot.class),operation.substring("knowledge-check:".length()));
            if(operation.startsWith("knowledge-outcome:")) {
                if(org.olo.kernel.knowledge.KnowledgeRuntime.lifecycle()!=null)org.olo.kernel.knowledge.KnowledgeRuntime.lifecycle().marker(args.get(0, WorkflowInput.class),operation.substring("knowledge-outcome:".length()));
                return null;
            }
            if (KernelActivityOperations.CANCEL_TOOLS.equals(operation)) {
                var cancelled=args.get(0, KernelExecutionSnapshot.class);
                if(org.olo.kernel.knowledge.KnowledgeRuntime.lifecycle()!=null)org.olo.kernel.knowledge.KnowledgeRuntime.lifecycle().marker(cancelled.toContext(),"cancellation");
                org.olo.kernel.agent.tool.AgentTurnCancellation.cancel(cancelled);
                return null;
            }
            if (KernelActivityOperations.STEP.equals(operation)) {
                return KernelEntryPoint.executeTraversalStep(args.get(0, KernelExecutionSnapshot.class));
            }
            if (KernelActivityOperations.REPORT_HUMAN_WAITING.equals(operation)) {
                KernelEntryPoint.reportHumanWaiting(args.get(0, KernelExecutionSnapshot.class));
                return null;
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
