/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.agent.tool;

import org.junit.jupiter.api.Test;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.kernel.traversal.engine.impl.DefaultGraphTraversalEngine;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRegistry;
import org.olo.kernel.traversal.strategy.impl.*;
import org.olo.spi.node.NodeResult;
import org.olo.toolgate.client.ToolGateCallResult;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.olo.kernel.agent.tool.AgentToolTestSupport.*;

class ParallelAgentRecoveryTest {
    @Test void aHumanResumeAtABranchJoinStillSchedulesTheRemainingAgentAsAnActivity() {
        var builder=WorkflowBuilder.create("human-fork").queue("oloQueue2");
        for(var id:List.of("start","fork","ahuman","b","join"))builder.addNode(NodeDefinition.builder().id(id)
            .type(id.equals("start")?"START":id.equals("fork")?"PARALLEL":id.equals("ahuman")?"HUMAN":id.equals("join")?"END":"AGENT")
            .executionModel(ExecutionModel.INLINE).build());
        for(var pair:List.of(List.of("start","fork"),List.of("fork","ahuman"),List.of("fork","b"),List.of("ahuman","join"),List.of("b","join")))
            builder.addEdge(EdgeDefinition.builder().sourceNodeId(pair.get(0)).targetNodeId(pair.get(1)).build());
        var visits=new ArrayList<String>();
        var engine=new DefaultGraphTraversalEngine(index->index.findNode("start"),index->true,(context,node)->{
            visits.add(node.getId());return node.getType().equals("HUMAN")?NodeResult.waiting("approve",Map.of()):NodeResult.completed(Map.of());
        },new ExecutionStrategyRegistry(List.of(new ParallelExecutionStrategy(),new LinearExecutionStrategy())));
        var snapshot=KernelExecutionSnapshot.fromContext(context(builder.build(),"run-human"));
        for(int i=0;i<3;i++)snapshot=engine.executeSingleStep(snapshot);
        assertThat(snapshot.isWaiting()).isTrue();
        snapshot=org.olo.kernel.human.HumanInputResumeSupport.resume(snapshot,org.olo.kernel.human.HumanResumeInput.of("yes","operator"));
        assertThat(snapshot.getNextNodeId()).isEqualTo("b");assertThat(snapshot.isNextRequiresDedicatedActivity()).isTrue();
        snapshot=engine.executeSingleStep(snapshot);snapshot=engine.executeSingleStep(snapshot);
        assertThat(snapshot.getStatus()).isEqualTo(KernelExecutionSnapshot.Status.COMPLETED);
        assertThat(visits).containsExactly("start","fork","ahuman","b","join");
    }

    @Test void inlineAgentsInForksRecordEachDecisionAcrossSerializedBoundariesBeforeDispatch()throws Exception {
        var builder=WorkflowBuilder.create("forked").queue("oloQueue2");
        for(var id:List.of("start","fork","a","b","join"))builder.addNode(NodeDefinition.builder().id(id)
            .type(id.equals("start")?"START":id.equals("fork")?"PARALLEL":id.equals("join")?"END":"AGENT")
            .executionModel(ExecutionModel.INLINE).build());
        for(var pair:List.of(List.of("start","fork"),List.of("fork","a"),List.of("fork","b"),List.of("a","join"),List.of("b","join")))
            builder.addEdge(EdgeDefinition.builder().sourceNodeId(pair.get(0)).targetNodeId(pair.get(1)).build());
        var graph=builder.build();
        var llm=new ScriptedLlm().reply(callTool("incidents_restart","{\"host\":\"a\"}"),answer("a done"),
            callTool("incidents_restart","{\"host\":\"b\"}"),answer("b done"));
        var gateway=new FakeToolGate(Map.of("triage-agent",List.of(tool("incidents.restart"))));
        var effects=new HashSet<String>();
        gateway.results=c->{effects.add(c.requestId());return new ToolGateCallResult(false,"ok",null,null,"inv");};
        var loop=new AgentToolLoop(llm);var visits=new ArrayList<String>();
        var engine=new DefaultGraphTraversalEngine(index->index.findNode("start"),index->true,(context,node)->{
            visits.add(node.getId());
            if(!node.getType().equals("AGENT"))return NodeResult.completed(Map.of());
            var step=loop.step(context,node,MODEL,"test",gateway,new AgentToolSettings("triage-agent",8));
            return step.done()?NodeResult.completed(Map.of()):NodeResult.continuing(step.progress());
        },new ExecutionStrategyRegistry(List.of(new ParallelExecutionStrategy(),new LinearExecutionStrategy())));
        KernelExecutionSnapshot snapshot=KernelExecutionSnapshot.fromContext(context(graph,"run-fork"));
        snapshot=engine.executeSingleStep(snapshot); // start
        snapshot=engine.executeSingleStep(snapshot); // fork only; no model or effect
        assertThat(snapshot.getNextNodeId()).isEqualTo("a");assertThat(llm.chatMessages).isEmpty();
        assertThat(snapshot.isNextRequiresDedicatedActivity()).isTrue();
        snapshot=engine.executeSingleStep(snapshot); // record a's decision
        assertThat(gateway.calls).isEmpty();assertThat(llm.chatMessages).hasSize(1);
        var recorded=MAPPER.readValue(MAPPER.writeValueAsBytes(snapshot),KernelExecutionSnapshot.class);
        engine.executeSingleStep(recorded); // dispatch result lost
        snapshot=engine.executeSingleStep(MAPPER.readValue(MAPPER.writeValueAsBytes(recorded),KernelExecutionSnapshot.class));
        assertThat(effects).hasSize(1);assertThat(llm.chatMessages).hasSize(1);
        for(int i=0;i<12&&!snapshot.isTerminal();i++)snapshot=engine.executeSingleStep(
            MAPPER.readValue(MAPPER.writeValueAsBytes(snapshot),KernelExecutionSnapshot.class));
        assertThat(snapshot.getStatus()).isEqualTo(KernelExecutionSnapshot.Status.COMPLETED);
        assertThat(effects).hasSize(2);assertThat(llm.chatMessages).hasSize(4);
        assertThat(visits).containsSubsequence("start","fork","a","a","a","a","b","b","b","join");
        assertThat(snapshot.getVariables().get("__olo.kernel.branchStack")).isNull();
    }
}
