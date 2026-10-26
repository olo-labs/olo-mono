// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.packages;

import io.ololabs.toolgate.sdk.annotations.*;
import io.ololabs.toolgate.sdk.context.*;

import java.util.Map;

@ToolPackage(
        id = "olo.rag",
        name = "OLO rag",
        publisher = "OLO Labs",
        description = "User-governed platform rag operations.")
@Destination(id = "olo-rag", hostSetting = "ServiceHost", port = 8443)
@HttpRoute(
        id = "query",
        destination = "olo-rag",
        method = HttpMethod.POST,
        path = "/v1/rag.query",
        maxRequestBytes = 33554432,
        maxResponseBytes = 1048576,
        timeoutMs = 14000)
@HttpRoute(
        id = "ingest",
        destination = "olo-rag",
        method = HttpMethod.POST,
        path = "/v1/rag.ingest",
        maxRequestBytes = 33554432,
        maxResponseBytes = 1048576,
        timeoutMs = 14000)
@HttpRoute(
        id = "update",
        destination = "olo-rag",
        method = HttpMethod.POST,
        path = "/v1/rag.update",
        maxRequestBytes = 33554432,
        maxResponseBytes = 1048576,
        timeoutMs = 14000)
@HttpRoute(
        id = "delete",
        destination = "olo-rag",
        method = HttpMethod.POST,
        path = "/v1/rag.delete",
        maxRequestBytes = 33554432,
        maxResponseBytes = 1048576,
        timeoutMs = 14000)
@HttpRoute(
        id = "share",
        destination = "olo-rag",
        method = HttpMethod.POST,
        path = "/v1/rag.share",
        maxRequestBytes = 33554432,
        maxResponseBytes = 1048576,
        timeoutMs = 14000)
public final class RagPackage {
    public record Result(@Param(description = "Service result JSON") String json) {}

    @Authorization(action = "rag.query", resources = "olo://{tenant}/kb/{kbId}")
    @ServerTool(
            name = "query",
            title = "Query rag",
            description = "Deterministic platform rag.query operation.",
            openWorld = OpenWorld.FALSE,
            stateless = true,
            businessKeyNamespace = KeyNamespace.TENANT,
            effect = EffectSafety.READ_ONLY,
            routes = "query")
    public Result query(
            @Param(description = "Opaque resource UUID", minLength = 36, maxLength = 36)
                    String kbId,
            @Param(description = "Operation request JSON; no credentials or identity fields")
                    String request,
            ToolContext context) {
        var principal = context.invocation();
        if (principal.tenantId().isEmpty()
                || principal.operation().filter("rag.query"::equals).isEmpty()
                || !principal.delegationChain().isEmpty())
            throw new SecurityException("Verified platform principal required");
        principal.requireUser();
        var response =
                context.http()
                        .route("query")
                        .body(Map.of("kbId", kbId, "request", request))
                        .sendRaw();
        if (response.status() != 200)
            throw new IllegalStateException(
                    "Governed service refused operation; reconcile before retry");
        return new Result(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Authorization(action = "rag.ingest", resources = "olo://{tenant}/kb/{kbId}")
    @ServerTool(
            name = "ingest",
            title = "Ingest rag",
            description = "Deterministic platform rag.ingest operation.",
            openWorld = OpenWorld.FALSE,
            stateless = true,
            businessKeyNamespace = KeyNamespace.TENANT,
            effect = EffectSafety.IDEMPOTENT,
            routes = "ingest",
            reconciliation = Reconciliation.MANUAL)
    public Result ingest(
            @Param(description = "Opaque resource UUID", minLength = 36, maxLength = 36)
                    String kbId,
            @Param(description = "Operation request JSON; no credentials or identity fields")
                    String request,
            ToolContext context) {
        var principal = context.invocation();
        if (principal.tenantId().isEmpty()
                || principal.operation().filter("rag.ingest"::equals).isEmpty()
                || !principal.delegationChain().isEmpty())
            throw new SecurityException("Verified platform principal required");
        principal.requireUser();
        var response =
                context.http()
                        .route("ingest")
                        .body(Map.of("kbId", kbId, "request", request))
                        .sendRaw();
        if (response.status() != 200)
            throw new IllegalStateException(
                    "Governed service refused operation; reconcile before retry");
        return new Result(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Authorization(action = "rag.update", resources = "olo://{tenant}/kb/{kbId}")
    @ServerTool(
            name = "update",
            title = "Update rag",
            description = "Deterministic platform rag.update operation.",
            openWorld = OpenWorld.FALSE,
            stateless = true,
            businessKeyNamespace = KeyNamespace.TENANT,
            effect = EffectSafety.IDEMPOTENT,
            routes = "update",
            reconciliation = Reconciliation.MANUAL)
    public Result update(
            @Param(description = "Opaque resource UUID", minLength = 36, maxLength = 36)
                    String kbId,
            @Param(description = "Operation request JSON; no credentials or identity fields")
                    String request,
            ToolContext context) {
        var principal = context.invocation();
        if (principal.tenantId().isEmpty()
                || principal.operation().filter("rag.update"::equals).isEmpty()
                || !principal.delegationChain().isEmpty())
            throw new SecurityException("Verified platform principal required");
        var response =
                context.http()
                        .route("update")
                        .body(Map.of("kbId", kbId, "request", request))
                        .sendRaw();
        if (response.status() != 200)
            throw new IllegalStateException(
                    "Governed service refused operation; reconcile before retry");
        return new Result(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Authorization(action = "rag.delete", resources = "olo://{tenant}/kb/{kbId}")
    @ServerTool(
            name = "delete",
            destructive = true,
            title = "Delete rag",
            description = "Deterministic platform rag.delete operation.",
            openWorld = OpenWorld.FALSE,
            stateless = true,
            businessKeyNamespace = KeyNamespace.TENANT,
            effect = EffectSafety.IDEMPOTENT,
            routes = "delete",
            reconciliation = Reconciliation.MANUAL)
    public Result delete(
            @Param(description = "Opaque resource UUID", minLength = 36, maxLength = 36)
                    String kbId,
            @Param(description = "Operation request JSON; no credentials or identity fields")
                    String request,
            ToolContext context) {
        var principal = context.invocation();
        if (principal.tenantId().isEmpty()
                || principal.operation().filter("rag.delete"::equals).isEmpty()
                || !principal.delegationChain().isEmpty())
            throw new SecurityException("Verified platform principal required");
        principal.requireUser();
        var response =
                context.http()
                        .route("delete")
                        .body(Map.of("kbId", kbId, "request", request))
                        .sendRaw();
        if (response.status() != 200)
            throw new IllegalStateException(
                    "Governed service refused operation; reconcile before retry");
        return new Result(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Authorization(action = "rag.share", resources = "olo://{tenant}/kb/{kbId}")
    @ServerTool(
            name = "share",
            title = "Share rag",
            description = "Deterministic platform rag.share operation.",
            openWorld = OpenWorld.FALSE,
            stateless = true,
            businessKeyNamespace = KeyNamespace.TENANT,
            effect = EffectSafety.IDEMPOTENT,
            routes = "share",
            reconciliation = Reconciliation.MANUAL)
    public Result share(
            @Param(description = "Opaque resource UUID", minLength = 36, maxLength = 36)
                    String kbId,
            @Param(description = "Operation request JSON; no credentials or identity fields")
                    String request,
            ToolContext context) {
        var principal = context.invocation();
        if (principal.tenantId().isEmpty()
                || principal.operation().filter("rag.share"::equals).isEmpty()
                || !principal.delegationChain().isEmpty())
            throw new SecurityException("Verified platform principal required");
        principal.requireUser();
        var response =
                context.http()
                        .route("share")
                        .body(Map.of("kbId", kbId, "request", request))
                        .sendRaw();
        if (response.status() != 200)
            throw new IllegalStateException(
                    "Governed service refused operation; reconcile before retry");
        return new Result(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
