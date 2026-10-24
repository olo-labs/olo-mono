// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.kernel.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import io.temporal.activity.DynamicActivity;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;

import org.junit.jupiter.api.*;
import org.olo.kernel.temporal.*;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.toolgate.client.*;

import java.util.*;
import java.util.concurrent.atomic.*;

class GovernedLifecycleRecoveryTest {
    @AfterEach
    void reset() {
        KnowledgeRuntime.reset();
    }

    @Test
    void modelActivityRetryGetsFreshScopeAndFinalAppendKeepsItsId() {
        verify(false, false);
    }

    @Test
    void checkpointApprovalResumesTheSameInvocationUsingADurableTimer() {
        verify(true, false);
    }

    @Test
    void revocationAfterLostModelResultPreventsNextModelAndFinalRelease() {
        verify(false, true);
    }

    void verify(boolean ask, boolean revoke) {
        var support = new ConversationLifecycleTest();
        var gateway = support.gateway;
        var lifecycle = support.lifecycle;
        KnowledgeRuntime.install(lifecycle);
        var modelAttempts = new AtomicInteger();
        var reads = new ArrayList<String>();
        var approval = new AtomicBoolean(ask);
        ToolGateClient wrapped =
                new ToolGateClient() {
                    public ToolGateClient forUser(UserBinding binding) {
                        return this;
                    }

                    public List<ToolGateTool> listTools(String route) {
                        throw new AssertionError("No agent catalog");
                    }

                    public void probe() {}

                    public ToolGateCallResult callTool(
                            String route,
                            String id,
                            String tool,
                            com.fasterxml.jackson.databind.JsonNode args) {
                        if (tool.equals("olo.memory/read") && gateway.messages.size() > 0) {
                            reads.add(id);
                            if (reads.size() > 1 && approval.compareAndSet(true, false))
                                return new ToolGateCallResult(
                                        true, "", null, "PENDING_APPROVAL", "original-approval");
                        }
                        return gateway.callTool(route, id, tool, args);
                    }
                };
        lifecycle = new ConversationLifecycle(new GovernedCalls(wrapped));
        KnowledgeRuntime.install(lifecycle);
        final var activeLifecycle = lifecycle;
        try (var env = TestWorkflowEnvironment.newInstance()) {
            var worker = env.newWorker("queue");
            worker.registerWorkflowImplementationTypes(OloKernelWorkflowImpl.class);
            worker.registerActivitiesImplementations(
                    (DynamicActivity)
                            args -> {
                                if (args.getSize() == 2
                                        && args.get(1, Object.class) instanceof String operation) {
                                    if (operation.startsWith("knowledge-outcome:")) {
                                        activeLifecycle.marker(
                                                args.get(
                                                        0, org.olo.input.model.WorkflowInput.class),
                                                operation.substring("knowledge-outcome:".length()));
                                        return null;
                                    }
                                    if (operation.startsWith("knowledge-check:"))
                                        return KnowledgeCheckpoint.check(
                                                args.get(0, KernelExecutionSnapshot.class),
                                                operation.substring("knowledge-check:".length()));
                                    var snapshot = args.get(0, KernelExecutionSnapshot.class);
                                    var ctx = snapshot.toContext();
                                    KnowledgeRuntime.beforeModel(ctx, "model");
                                    int attempt = modelAttempts.incrementAndGet();
                                    if (attempt == 1) {
                                        gateway.revoked = revoke;
                                        throw ApplicationFailure.newFailure(
                                                "Lost model activity result", "LOST_RESULT");
                                    }
                                    return KernelExecutionSnapshot.fromContext(
                                            ctx,
                                            null,
                                            2,
                                            KernelExecutionSnapshot.Status.COMPLETED,
                                            "model",
                                            null,
                                            "final answer");
                                }
                                if (args.getSize() == 1) {
                                    var snapshot = args.get(0, KernelExecutionSnapshot.class);
                                    activeLifecycle.finish(snapshot.toContext(), "final answer");
                                    return "final answer";
                                }
                                var ctx = support.context();
                                activeLifecycle.begin(ctx);
                                // Dedicated model step: the workflow invokes the checkpoint before
                                // executing it.
                                var snapshot =
                                        KernelExecutionSnapshot.fromContext(
                                                ctx,
                                                "model",
                                                0,
                                                KernelExecutionSnapshot.Status.RUNNING,
                                                null,
                                                null,
                                                null);
                                return new KernelExecutionSnapshot(
                                        snapshot.getQueue(),
                                        snapshot.getInput(),
                                        snapshot.getGraphJson(),
                                        snapshot.getVariables(),
                                        snapshot.getOutputs(),
                                        "model",
                                        0,
                                        snapshot.getStatus(),
                                        null,
                                        null,
                                        null,
                                        true,
                                        snapshot.getWorkflowActivityName(),
                                        "model:Model");
                            });
            env.start();
            var workflow =
                    env.getWorkflowClient()
                            .newWorkflowStub(
                                    OloKernelWorkflow.class,
                                    WorkflowOptions.newBuilder().setTaskQueue("queue").build());
            if (revoke) {
                assertThrows(
                        io.temporal.client.WorkflowFailedException.class,
                        () -> workflow.execute(support.context().getInput()));
                assertEquals(1, modelAttempts.get());
                assertEquals(1, gateway.messages.size());
            } else {
                assertEquals("final answer", workflow.execute(support.context().getInput()));
                assertEquals(2, modelAttempts.get());
                assertEquals(2, gateway.messages.size());
                assertTrue(new HashSet<>(reads).size() >= 3);
            }
            if (ask)
                assertTrue(
                        reads.stream().anyMatch(id -> Collections.frequency(reads, id) > 1),
                        "Pending checkpoint must keep the original operation id");
        }
    }
}
