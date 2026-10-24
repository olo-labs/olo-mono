// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.kernel.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;

import io.temporal.activity.DynamicActivity;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;

import org.junit.jupiter.api.*;
import org.olo.toolgate.client.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class DocumentWorkflowTest {
    @AfterEach
    void reset() {
        KnowledgeRuntime.reset();
    }

    @Test
    void scheduledReindexUsesOnlyTenantBoundServiceIdentityAndReferencePayloads() {
        var calls = new AtomicInteger();
        var json = new ObjectMapper();
        ToolGateClient client =
                new ToolGateClient() {
                    public ToolGateClient forService(String tenant, String principal) {
                        assertEquals("t1", tenant);
                        assertEquals("olo-platform-service", principal);
                        return this;
                    }

                    public ToolGateClient forUser(UserBinding user) {
                        throw new AssertionError("No user fallback");
                    }

                    public List<ToolGateTool> listTools(String agent) {
                        throw new AssertionError("No catalog");
                    }

                    public void probe() {}

                    public ToolGateCallResult callTool(
                            String route, String id, String tool, JsonNode args) {
                        assertEquals("olo-platform-service", route);
                        assertEquals("olo.rag/update", tool);
                        assertFalse(args.path("request").asText().contains("content"));
                        calls.incrementAndGet();
                        return new ToolGateCallResult(
                                false, "", json.createObjectNode().put("version", 2), null, null);
                    }
                };
        KnowledgeRuntime.installDocuments(new GovernedCalls(client));
        try (var env = TestWorkflowEnvironment.newInstance()) {
            var worker = env.newWorker("documents");
            worker.registerWorkflowImplementationTypes(OloDocumentWorkflowImpl.class);
            worker.registerActivitiesImplementations(
                    (DynamicActivity)
                            args ->
                                    KnowledgeRuntime.document(
                                            args.get(0, DocumentOperation.class)));
            env.start();
            var workflow =
                    env.getWorkflowClient()
                            .newWorkflowStub(
                                    OloDocumentWorkflow.class,
                                    WorkflowOptions.newBuilder()
                                            .setWorkflowId(
                                                    org.olo.input.codec.TenantWorkflowIds.run(
                                                            "t1", UUID.randomUUID().toString()))
                                            .setTaskQueue("documents")
                                            .build());
            var result =
                    workflow.execute(
                            new DocumentOperation(
                                    "t1",
                                    null,
                                    "olo-platform-service",
                                    "reindex",
                                    "rag.update",
                                    UUID.randomUUID().toString(),
                                    Map.of(
                                            "type",
                                            "reindex",
                                            "docId",
                                            UUID.randomUUID().toString())));
            assertTrue(result.contains("version"));
            assertEquals(1, calls.get());
        }
    }
}
