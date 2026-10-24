/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.agent.tool;

import io.temporal.activity.DynamicActivity;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.Test;
import org.olo.input.codec.OloPayloadCodec;
import org.olo.input.codec.PayloadKeyRing;
import org.olo.input.codec.TenantWorkflowIds;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.temporal.KernelActivityOperations;
import org.olo.kernel.temporal.OloKernelWorkflow;
import org.olo.kernel.temporal.OloKernelWorkflowImpl;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.toolgate.client.ToolGateCallResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.*;

/** Exercises the production Temporal workflow and agent loop across actual serialized activity boundaries. */
class TemporalAgentRecoveryTest {
    @Test
    void aLostDispatchResultIsRetriedWithoutRegeneratingTheDecisionOrRepeatingTheEffect() {
        verify(false);
    }

    @Test
    void approvalWaitUsesATemporalTimerAndResumesTheOriginalOperation() {
        verify(true);
    }

    private void verify(boolean approval) {
        var ring = new PayloadKeyRing(Map.of("t1-key", new byte[32], "platform", new byte[32]),
                Map.of("t1", "t1-key"), "platform");
        var converter = new CodecDataConverter(DefaultDataConverter.newDefaultInstance(),
                List.of(new OloPayloadCodec(ring)), true);
        var options = TestEnvironmentOptions.newBuilder().setWorkflowClientOptions(
                WorkflowClientOptions.newBuilder().setDataConverter(converter).build()).build();
        try (var environment = TestWorkflowEnvironment.newInstance(options)) {
            var node = agentNode("agent", Map.of());
            var graph = graph("triage-agent", node);
            var llm = new ScriptedLlm().reply(callTool("incidents_restart", "{\"host\":\"private-app-02\"}"),
                    answer("private recovery result"));
            var gateway = new FakeToolGate(Map.of("triage-agent", List.of(tool("incidents.restart"))));
            var submissions = new AtomicInteger();
            var effects = new AtomicInteger();
            var committed = new ConcurrentHashMap<String, ToolGateCallResult>();
            gateway.results = call -> {
                int attempt = submissions.incrementAndGet();
                if (approval && attempt == 1) {
                    return new ToolGateCallResult(true, "approval required", null, "PENDING_APPROVAL", "inv-original");
                }
                return committed.computeIfAbsent(call.requestId(), key -> {
                    effects.incrementAndGet();
                    return new ToolGateCallResult(false, "private effect result", null, null, "inv-original");
                });
            };
            Clock clock = new Clock() {
                @Override public ZoneId getZone() { return ZoneOffset.UTC; }
                @Override public Clock withZone(ZoneId zone) { return this; }
                @Override public Instant instant() { return Instant.ofEpochMilli(environment.currentTimeMillis()); }
            };
            var loop = new AgentToolLoop(llm, clock);
            var settings = new AgentToolSettings("triage-agent", 8);
            var crash = new AtomicBoolean(!approval);
            var worker = environment.newWorker("oloQueue2");
            worker.registerWorkflowImplementationTypes(OloKernelWorkflowImpl.class);
            // Replace external model/Gateway integrations, retaining the production workflow, loop, codec and timers.
            worker.registerActivitiesImplementations((DynamicActivity) args -> {
                if (args.getSize() == 2 && !KernelActivityOperations.STEP.equals(args.get(1, Object.class))) {
                    return KernelExecutionSnapshot.fromContext(context(graph, "run-1"), "agent", 0,
                            KernelExecutionSnapshot.Status.RUNNING, null, null, null);
                }
                KernelExecutionSnapshot snapshot = args.get(0, KernelExecutionSnapshot.class);
                if (args.getSize() == 1) return snapshot.getMessage();
                var context = snapshot.toContext();
                int before = submissions.get();
                var step = loop.step(context, node, MODEL, "private user prompt", gateway, settings);
                if (submissions.get() > before && crash.compareAndSet(true, false)) {
                    throw ApplicationFailure.newFailure("lost activity result containing private account", "LOST_RESULT");
                }
                return KernelExecutionSnapshot.fromContext(context, "agent", snapshot.getStep() + 1,
                        step.done() ? KernelExecutionSnapshot.Status.COMPLETED : KernelExecutionSnapshot.Status.RUNNING,
                        "agent", null, step.done() ? step.result().response() : step.progress());
            });
            environment.start();
            String workflowId = TenantWorkflowIds.run("t1", "recovery-" + approval);
            OloKernelWorkflow workflow = environment.getWorkflowClient().newWorkflowStub(OloKernelWorkflow.class,
                    WorkflowOptions.newBuilder().setTaskQueue("oloQueue2").setWorkflowId(workflowId).build());
            String result = workflow.execute(WorkflowInput.fromJson("{\"message\":\"private user prompt\"}"));
            assertThat(result).isEqualTo("private recovery result");
            assertThat(submissions.get()).isEqualTo(2);
            assertThat(effects.get()).isEqualTo(1);
            assertThat(gateway.calls).extracting(FakeToolGate.Call::requestId).containsOnly(gateway.calls.get(0).requestId());
            assertThat(llm.chatMessages).hasSize(2);
            String history = environment.getWorkflowClient().fetchHistory(workflowId).getHistory().toString();
            assertThat(history).doesNotContain("private user prompt", "private recovery result", "private-app-02",
                    "lost activity result containing private account");
            if (approval) assertThat(history).contains("EVENT_TYPE_TIMER_STARTED");
            else assertThat(history).contains("EVENT_TYPE_ACTIVITY_TASK_STARTED");
        }
    }
}
