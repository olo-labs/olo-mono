/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.samples.definitions;

import org.olo.definition.OloProductTerminology;
import org.olo.definition.extension.ExtensionDefinition;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.model.ModelRoutingDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;
import java.util.Map;

import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.buildSample;
import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.nodeWithDefaultPorts;
import static org.olo.definition.samples.definitions.SampleWorkflowBuilders.passThroughCapability;

/** Minimal echo, RAG, and analysis extension sample workflows. */
public final class SampleBasicWorkflows {

    private SampleBasicWorkflows() {
    }

    public static WorkflowDefinition minimalEcho() {
        return buildSample(WorkflowBuilder.create("Minimal Echo")
                .id("minimal-echo")
                .version("1.0.0")
                .capability(passThroughCapability(
                        "Minimal Echo", "Smallest valid " + OloProductTerminology.WORKFLOW + ": passes input through to output."))
                .inputNode("input")
                .outputNode("output")
                .connect("input", "output")
                .metadata("description", "Smallest valid " + OloProductTerminology.WORKFLOW + ": passes input through to output."));
    }


    public static WorkflowDefinition ragChat() {
        return buildSample(WorkflowBuilder.create("RAG Chat")
                .id("rag-chat")
                .version("1.0.0")
                .capability(passThroughCapability(
                        "RAG Chat", "Retrieve-augmented generation over a Qdrant vector store."))
                .inputNode("input")
                .addNode(nodeWithDefaultPorts("embed", NodeType.MODEL)
                        .subtype("EMBEDDING")
                        .putConfiguration("providerRef", "openai-embeddings")
                        .build())
                .addNode(nodeWithDefaultPorts("retrieve", NodeType.VECTOR_SEARCH)
                        .putConfiguration("extensionRef", "pgvector-store")
                        .putConfiguration("driver", "${env:OLO_VECTOR_STORE_DRIVER}")
                        .putConfiguration("connectionRef", "${env:OLO_VECTOR_STORE_URL}")
                        .putConfiguration("collection", "${env:OLO_VECTOR_STORE_COLLECTION}")
                        .putConfiguration("vectorSize", "${env:OLO_VECTOR_STORE_VECTOR_SIZE}")
                        .putConfiguration("distance", "${env:OLO_VECTOR_STORE_DISTANCE}")
                        .putConfiguration("topK", 5)
                        .putConfiguration("scoreThreshold", 0.25)
                        .build())
                .addNode(nodeWithDefaultPorts("llm1", NodeType.MODEL)
                        .subtype("CHAT")
                        .putConfiguration("providerRef", "openai-default")
                        .putConfiguration("systemPrompt", "Answer using only the provided context.")
                        .build())
                .outputNode("output")
                .connect("input", "embed")
                .connect("embed", "retrieve")
                .connect("retrieve", "llm1")
                .connect("llm1", "output")
                .modelProvider(ModelProviderDefinition.builder()
                        .id("openai-default")
                        .provider("openai")
                        .model("gpt-4o-mini")
                        .build())
                .modelProvider(ModelProviderDefinition.builder()
                        .id("openai-embeddings")
                        .provider("openai")
                        .model("text-embedding-3-small")
                        .build())
                .modelRouting(ModelRoutingDefinition.builder()
                        .id("default-routing")
                        .defaultProviderId("openai-default")
                        .rules(List.of(ModelRoutingDefinition.RoutingRule.builder()
                                .name("long-context")
                                .providerId("openai-default")
                                .match(Map.of("tokenEstimateGt", 8000))
                                .build()))
                        .build())
                .extension(ExtensionDefinition.builder()
                        .id("pgvector-store")
                        .type("VECTOR_STORE")
                        .putConfiguration("driver", "${env:OLO_VECTOR_STORE_DRIVER}")
                        .putConfiguration("connectionRef", "${env:OLO_VECTOR_STORE_URL}")
                        .putConfiguration("table", "${env:OLO_VECTOR_STORE_TABLE}")
                        .putConfiguration("collection", "${env:OLO_VECTOR_STORE_COLLECTION}")
                        .putConfiguration("vectorSize", "${env:OLO_VECTOR_STORE_VECTOR_SIZE}")
                        .putConfiguration("distance", "${env:OLO_VECTOR_STORE_DISTANCE}")
                        .build())
                .metadata(
                        "description",
                        "Retrieve-augmented generation over a Qdrant vector store."));
    }

    public static WorkflowDefinition analysisBase() {
        return buildSample(WorkflowBuilder.create("Analysis")
                .id("analysis")
                .version("1.0.0")
                .capability(passThroughCapability("Analysis", "Single-model analysis workflow."))
                .inputNode("input")
                .addNode(nodeWithDefaultPorts("llm1", NodeType.MODEL)
                        .subtype("CHAT")
                        .putConfiguration("providerRef", "ollama-local")
                        .build())
                .outputNode("output")
                .connect("input", "llm1")
                .connect("llm1", "output")
                .modelProvider(ModelProviderDefinition.builder()
                        .id("ollama-local")
                        .provider("ollama")
                        .model("llama3.2")
                        .putConfiguration("baseUrl", "http://localhost:11434")
                        .build())
                .metadata(
                        "description",
                        "Base workflow before RAG extension (see workflow-extended.json)."));
    }

    public static WorkflowDefinition analysisExtended() {
        return buildSample(WorkflowBuilder.create("Analysis with RAG")
                .id("analysis")
                .version("1.1.0")
                .capability(passThroughCapability(
                        "Analysis with RAG", "Analysis workflow extended with vector retrieval."))
                .inputNode("input")
                .addNode(nodeWithDefaultPorts("rag1", NodeType.VECTOR_SEARCH)
                        .putConfiguration("extensionRef", "local-chroma")
                        .putConfiguration("topK", 3)
                        .build())
                .addNode(nodeWithDefaultPorts("llm1", NodeType.MODEL)
                        .subtype("CHAT")
                        .putConfiguration("providerRef", "ollama-local")
                        .build())
                .outputNode("output")
                .connect("input", "rag1")
                .connect("rag1", "llm1")
                .connect("llm1", "output")
                .modelProvider(ModelProviderDefinition.builder()
                        .id("ollama-local")
                        .provider("ollama")
                        .model("llama3.2")
                        .putConfiguration("baseUrl", "http://localhost:11434")
                        .build())
                .extension(ExtensionDefinition.builder()
                        .id("local-chroma")
                        .type("VECTOR_STORE")
                        .putConfiguration("driver", "chroma")
                        .putConfiguration("collection", "docs")
                        .build())
                .metadata(
                        "description",
                        "Extended workflow: INPUT → VECTOR_SEARCH → MODEL → OUTPUT (git-branch style extension).")
                .metadata("derivedFrom", "workflow-base.json")
                .metadata("derivedFromVersion", "1.0.0"));
    }
}
