/*

 * Copyright (c) 2026 Olo Labs

 * SPDX-License-Identifier: Apache-2.0

 */

package org.olo.definition.configuration.scenario;



import org.olo.definition.preset.WorkflowConversationPluginSupport;

import org.olo.definition.tool.ToolDefinition;

import org.olo.definition.workflow.WorkflowBuilder;



/** @deprecated Use {@link WorkflowConversationPluginSupport} from main sources. */

@Deprecated

public final class ScenarioConversationPluginSupport {



    public static final String CONVERSATION_LOAD_TOOL_ID = WorkflowConversationPluginSupport.CONVERSATION_LOAD_TOOL_ID;

    public static final String CONVERSATION_STORE_TOOL_ID = WorkflowConversationPluginSupport.CONVERSATION_STORE_TOOL_ID;

    public static final String CONVERSATION_LOAD_NODE_ID = WorkflowConversationPluginSupport.CONVERSATION_LOAD_NODE_ID;

    public static final String CONVERSATION_STORE_NODE_ID = WorkflowConversationPluginSupport.CONVERSATION_STORE_NODE_ID;

    public static final String CONVERSATION_SUMMARY_VARIABLE = WorkflowConversationPluginSupport.CONVERSATION_SUMMARY_VARIABLE;

    public static final String CONVERSATION_HISTORY_VARIABLE = WorkflowConversationPluginSupport.CONVERSATION_HISTORY_VARIABLE;



    private ScenarioConversationPluginSupport() {

    }



    public static ToolDefinition conversationLoadTool() {

        return WorkflowConversationPluginSupport.conversationLoadTool();

    }



    public static ToolDefinition conversationStoreTool() {

        return WorkflowConversationPluginSupport.conversationStoreTool();

    }



    public static WorkflowBuilder wireConversationPlugins(WorkflowBuilder builder, String continueNodeId) {

        return WorkflowConversationPluginSupport.wireConversationToolNodes(builder);

    }



    public static String conversationContextPromptBlock() {

        return WorkflowConversationPluginSupport.conversationContextPromptBlock();

    }

}

