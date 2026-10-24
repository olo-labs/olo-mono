/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.traversal.engine.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.dynamicgraph.MutableGraphSession;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.kernel.traversal.strategy.ExecutionDecision;
import java.util.ArrayList;
import java.util.List;

/** Sequential branch scheduling with a persisted continuation stack, one node phase per activity. */
final class ParallelBranchExecutor {
    private static final String STACK = "__olo.kernel.branchStack";
    private static final ObjectMapper JSON = new ObjectMapper();
    record Frame(List<String> entries, int index, String join, String fork, String message) {}
    @FunctionalInterface
    interface StepRunner {
        TraversalCursor executeSingleStep(KernelRuntimeContext context, MutableGraphSession session, TraversalCursor cursor);
    }
    private ParallelBranchExecutor() {}

    static TraversalCursor execute(KernelRuntimeContext context, MutableGraphSession session,
            ExecutionDecision decision, int step, String fork, String message, StepRunner unused) {
        if (decision.branchEntryNodeIds().isEmpty()) throw new KernelException("PARALLEL fork has no outgoing branches");
        String join = decision.joinNodeId().orElseThrow(() -> new KernelException("PARALLEL fork has no common join"));
        List<Frame> frames = load(context);
        frames.add(new Frame(decision.branchEntryNodeIds(), 0, join, fork, message));
        save(context, frames);
        return TraversalCursor.running(decision.branchEntryNodeIds().get(0), step, fork, message);
    }

    static TraversalCursor advance(KernelRuntimeContext context, TraversalCursor cursor) {
        if (cursor.status() != KernelExecutionSnapshot.Status.RUNNING) return cursor;
        List<Frame> frames = load(context);
        while (!frames.isEmpty()) {
            Frame frame = frames.getLast();
            if (!frame.join().equals(cursor.nextNodeId())) break;
            int next = frame.index() + 1;
            if (next < frame.entries().size()) {
                frames.set(frames.size() - 1, new Frame(frame.entries(), next, frame.join(), frame.fork(), frame.message()));
                cursor = TraversalCursor.running(frame.entries().get(next), cursor.step(), frame.fork(), frame.message());
                break;
            }
            frames.removeLast();
        }
        save(context, frames);
        return cursor;
    }
    private static List<Frame> load(KernelRuntimeContext context) {
        String stored = context.getVariables().getString(STACK);
        if (stored == null) return new ArrayList<>();
        try { return JSON.readValue(stored, new TypeReference<ArrayList<Frame>>() {}); }
        catch (Exception invalid) { throw new KernelException("unreadable parallel continuation"); }
    }
    private static void save(KernelRuntimeContext context, List<Frame> frames) {
        try { context.getVariables().set(STACK, frames.isEmpty() ? null : JSON.writeValueAsString(frames)); }
        catch (Exception invalid) { throw new KernelException("parallel continuation is not serializable"); }
    }
}
