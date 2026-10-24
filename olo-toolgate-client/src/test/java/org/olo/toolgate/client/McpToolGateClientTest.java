/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.toolgate.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.olo.toolgate.client.impl.FileAndEnvCredentialResolver;
import org.olo.toolgate.client.impl.McpToolGateClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpToolGateClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String META = "\"_meta\":{\"io.modelcontextprotocol/serverInfo\":{\"name\":\"olo-toolgate-gateway\",\"version\":\"test\"}}";

    private FakeGateway gateway;
    private ToolGateClient client;

    @BeforeEach
    void setUp() throws Exception {
        gateway = new FakeGateway();
        ToolGateCredentialResolver credentials = new FileAndEnvCredentialResolver(null, Map.of(
                "OLO_TOOLGATE_TOKEN_TRIAGE_AGENT", "triage-token",
                "OLO_TOOLGATE_TOKEN_REPORT_AGENT", "report-token"));
        client = new McpToolGateClient(
                new ToolGateSettings(gateway.mcpUrl(), null, null, Duration.ofSeconds(5)), credentials);
    }

    @AfterEach
    void tearDown() {
        gateway.close();
    }

    @Test
    void cancellationUsesTheSameUserAndActorAndCarriesOnlyTheOperationReference() {
        gateway.respond(r -> FakeGateway.Response.json(200,"{\"jsonrpc\":\"2.0\",\"id\":\"x\",\"result\":{\"cancelled\":true}}"));
        var users = new McpToolGateClient(new ToolGateSettings(gateway.mcpUrl(),null,null,Duration.ofSeconds(5)),
                agent -> "actor-token", binding -> "user-token");
        users.forUserViaAgent(new UserBinding("t1","u1","issuer","sid",1)).cancelOperation("triage-agent","olo-op-original");
        var request=gateway.received().getFirst();
        assertThat(request.header("Authorization")).isEqualTo("Bearer user-token");
        assertThat(request.header("X-ToolGate-Actor-Credential")).isEqualTo("actor-token");
        assertThat(request.body().path("method").asText()).isEqualTo("olo/invocations/cancel");
        assertThat(request.body().path("params").path("operationId").asText()).isEqualTo("olo-op-original");
    }

    @Test
    void listToolsSendsTheAgentTokenAndTheExactMcpFrame() {
        gateway.respond(r -> FakeGateway.Response.json(200, "{\"jsonrpc\":\"2.0\",\"id\":\"x\",\"result\":{\"tools\":["
                + "{\"name\":\"incidents.host-status\",\"description\":\"Host status\","
                + "\"inputSchema\":{\"type\":\"object\",\"properties\":{\"host\":{\"type\":\"string\"}},\"required\":[\"host\"]}}"
                + "],\"ttlMs\":0,\"cacheScope\":\"private\",\"resultType\":\"complete\"," + META + "}}"));

        List<ToolGateTool> tools = client.listTools("triage-agent");

        assertThat(tools).extracting(ToolGateTool::name).containsExactly("incidents.host-status");
        assertThat(tools.get(0).inputSchema().path("required").get(0).asText()).isEqualTo("host");

        FakeGateway.Received request = gateway.received().get(0);
        assertThat(request.header("Authorization")).isEqualTo("Bearer triage-token");
        assertThat(request.header("MCP-Protocol-Version")).isEqualTo("2026-07-28");
        assertThat(request.header("Mcp-Method")).isEqualTo("tools/list");
        assertThat(request.header("Mcp-Name")).isNull();
        assertThat(request.header("Accept")).contains("application/json").contains("text/event-stream");
        assertThat(request.body().path("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(request.body().path("method").asText()).isEqualTo("tools/list");
        assertThat(request.body().path("params").path("_meta")
                .path("io.modelcontextprotocol/protocolVersion").asText()).isEqualTo("2026-07-28");
        assertThat(request.body().path("params").path("_meta")
                .path("io.modelcontextprotocol/clientCapabilities").isObject()).isTrue();
        assertThat(request.body().fieldNames()).toIterable()
                .containsExactlyInAnyOrder("jsonrpc", "id", "method", "params");
    }

    @Test
    void eachAgentNameUsesItsOwnIdentity() {
        gateway.respond(r -> FakeGateway.Response.json(200, "{\"jsonrpc\":\"2.0\",\"id\":\"x\",\"result\":{\"tools\":[]}}"));

        client.listTools("triage-agent");
        client.listTools("report-agent");

        assertThat(gateway.received()).extracting(r -> r.header("Authorization"))
                .containsExactly("Bearer triage-token", "Bearer report-token");
    }

    @Test
    void callToolMirrorsTheToolNameAndUsesTheRequestIdAsJsonRpcId() throws Exception {
        gateway.respond(r -> FakeGateway.Response.json(200, "{\"jsonrpc\":\"2.0\",\"id\":\"olo-1\",\"result\":{"
                + "\"content\":[{\"type\":\"text\",\"text\":\"payments-api OOM\"}],\"isError\":false,"
                + "\"structuredContent\":{\"host\":\"app-02\",\"healthy\":false},\"resultType\":\"complete\"," + META + "}}"));
        ObjectNode arguments = (ObjectNode) MAPPER.readTree("{\"host\":\"app-02\"}");

        ToolGateCallResult result = client.callTool("triage-agent", "olo-1", "incidents.host-status", arguments);

        assertThat(result.error()).isFalse();
        assertThat(result.text()).isEqualTo("payments-api OOM");
        assertThat(result.structuredContent().path("host").asText()).isEqualTo("app-02");
        assertThat(result.pendingApproval()).isFalse();

        FakeGateway.Received request = gateway.received().get(0);
        assertThat(request.header("Mcp-Method")).isEqualTo("tools/call");
        assertThat(request.header("Mcp-Name")).isEqualTo("incidents.host-status");
        assertThat(request.body().path("id").asText()).isEqualTo("olo-1");
        assertThat(request.body().path("params").fieldNames()).toIterable()
                .containsExactlyInAnyOrder("name", "arguments", "_meta");
        assertThat(request.body().path("params").path("arguments").path("host").asText()).isEqualTo("app-02");
    }

    @Test
    void askPolicyReturnsAPendingApprovalResult() {
        gateway.respond(r -> FakeGateway.Response.json(200, "{\"jsonrpc\":\"2.0\",\"id\":\"olo-2\",\"result\":{"
                + "\"content\":[{\"type\":\"text\",\"text\":\"Approval required\"}],"
                + "\"structuredContent\":{\"invocationId\":\"inv-123\",\"state\":\"PENDING_APPROVAL\"},"
                + "\"isError\":true,\"resultType\":\"complete\"}}"));

        ToolGateCallResult result = client.callTool("triage-agent", "olo-2", "incidents.remediate", null);

        assertThat(result.error()).isTrue();
        assertThat(result.pendingApproval()).isTrue();
        assertThat(result.invocationId()).isEqualTo("inv-123");
    }

    @Test
    void toolOutsideTheCatalogComesBackAsAToolError() {
        gateway.respond(r -> FakeGateway.Response.json(403, "{\"jsonrpc\":\"2.0\",\"id\":\"olo-3\",\"error\":"
                + "{\"code\":-32602,\"message\":\"Tool unavailable for this actor and device\",\"data\":null}}"));

        ToolGateCallResult result = client.callTool("triage-agent", "olo-3", "incidents.remediate", null);

        assertThat(result.error()).isTrue();
        assertThat(result.text()).contains("not available to agent 'triage-agent'");
    }

    @Test
    void executorFailureComesBackAsAToolError() {
        gateway.respond(r -> FakeGateway.Response.json(200, "{\"jsonrpc\":\"2.0\",\"id\":\"olo-4\",\"error\":"
                + "{\"code\":-32000,\"message\":\"Tool execution failed\",\"data\":{\"code\":\"NOT_COVERED\"}}}"));

        ToolGateCallResult result = client.callTool("triage-agent", "olo-4", "claims.assess", null);

        assertThat(result.error()).isTrue();
        assertThat(result.text()).contains("Tool execution failed").contains("NOT_COVERED");
    }

    @Test
    void firstDispatchUnknownOutcomeRetainsTheInvocationInsteadOfBecomingAnOrdinaryError() {
        gateway.respond(r -> FakeGateway.Response.json(200, """
                {"jsonrpc":"2.0","id":"op-1","error":{"code":-32000,"message":"Tool execution failed",
                "data":{"code":"DEPENDENCY_UNAVAILABLE","outcome":"OUTCOME_UNKNOWN","requestId":"inv-42",
                "error":{"code":"OUTCOME_UNKNOWN","retryable":false}}}}
                """));
        ToolGateCallResult result = client.callTool("triage-agent", "op-1", "incidents.remediate", null);
        assertThat(result.outcomeUnknown()).isTrue();
        assertThat(result.invocationId()).isEqualTo("inv-42");
        assertThat(result.structuredContent().path("state").asText()).isEqualTo("OUTCOME_UNKNOWN");
    }

    @Test
    void rejectedTokenFailsClosed() {
        gateway.respond(r -> FakeGateway.Response.json(401, "{\"error\":{\"code\":\"UNAUTHORIZED\"}}"));

        assertThatThrownBy(() -> client.listTools("triage-agent"))
                .isInstanceOf(ToolGateException.class)
                .satisfies(e -> assertThat(((ToolGateException) e).kind()).isEqualTo(ToolGateException.Kind.UNAUTHORIZED));
    }

    @Test
    void catalogDiscoveryFailureFailsClosed() {
        gateway.respond(r -> FakeGateway.Response.json(200, "{\"jsonrpc\":\"2.0\",\"id\":\"x\",\"error\":"
                + "{\"code\":-32000,\"message\":\"Local tool discovery failed\",\"data\":{\"code\":\"DEPENDENCY_UNAVAILABLE\"}}}"));

        assertThatThrownBy(() -> client.listTools("triage-agent"))
                .isInstanceOf(ToolGateException.class)
                .satisfies(e -> assertThat(((ToolGateException) e).kind()).isEqualTo(ToolGateException.Kind.UNAVAILABLE));
    }

    @Test
    void overloadedGatewayIsUnavailable() {
        gateway.respond(r -> FakeGateway.Response.json(503, "{\"error\":{\"code\":\"DEPENDENCY_UNAVAILABLE\"}}"));

        assertThatThrownBy(() -> client.callTool("triage-agent", "olo-5", "incidents.incidents", null))
                .isInstanceOf(ToolGateException.class)
                .satisfies(e -> assertThat(((ToolGateException) e).kind()).isEqualTo(ToolGateException.Kind.UNAVAILABLE));
    }

    @Test
    void readsServerSentEventReplies() {
        gateway.respond(r -> new FakeGateway.Response(200, "text/event-stream",
                "event: message\ndata: {\"jsonrpc\":\"2.0\",\"id\":\"x\",\"result\":{\"tools\":[{\"name\":\"a.b\"}]}}\n\n"));

        assertThat(client.listTools("triage-agent")).extracting(ToolGateTool::name).containsExactly("a.b");
    }

    @Test
    void unreachableGatewayFailsClosed() {
        gateway.close();

        assertThatThrownBy(() -> client.listTools("triage-agent"))
                .isInstanceOf(ToolGateException.class)
                .satisfies(e -> assertThat(((ToolGateException) e).kind()).isEqualTo(ToolGateException.Kind.UNAVAILABLE));
    }

    @Test
    void probeAcceptsAnyHttpAnswerWithoutSendingAToken() {
        gateway.respond(r -> FakeGateway.Response.json(401, "{}"));

        client.probe();

        assertThat(gateway.received().get(0).header("Authorization")).isNull();
    }

    @Test
    void rejectsOverlongRequestIds() {
        assertThatThrownBy(() -> client.callTool("triage-agent", "x".repeat(129), "a.b", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
