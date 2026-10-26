// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.packages;

import io.ololabs.toolgate.sdk.annotations.*;
import io.ololabs.toolgate.sdk.context.*;

import java.util.Map;

@ToolPackage(
        id = "olo.memory",
        name = "OLO memory",
        publisher = "OLO Labs",
        description = "User-governed platform memory operations.")
@Destination(id = "olo-memory", hostSetting = "ServiceHost", port = 8443)
@HttpRoute(
        id = "read",
        destination = "olo-memory",
        method = HttpMethod.POST,
        path = "/v1/memory.read",
        maxRequestBytes = 33554432,
        maxResponseBytes = 1048576,
        timeoutMs = 14000)
@HttpRoute(
        id = "append",
        destination = "olo-memory",
        method = HttpMethod.POST,
        path = "/v1/memory.append",
        maxRequestBytes = 33554432,
        maxResponseBytes = 1048576,
        timeoutMs = 14000)
@HttpRoute(
        id = "delete",
        destination = "olo-memory",
        method = HttpMethod.POST,
        path = "/v1/memory.delete",
        maxRequestBytes = 33554432,
        maxResponseBytes = 1048576,
        timeoutMs = 14000)
@HttpRoute(
        id = "share",
        destination = "olo-memory",
        method = HttpMethod.POST,
        path = "/v1/memory.share",
        maxRequestBytes = 33554432,
        maxResponseBytes = 1048576,
        timeoutMs = 14000)
@HttpRoute(
        id = "export",
        destination = "olo-memory",
        method = HttpMethod.POST,
        path = "/v1/memory.export",
        maxRequestBytes = 33554432,
        maxResponseBytes = 1048576,
        timeoutMs = 14000)
public final class MemoryPackage {
    public record Result(@Param(description = "Service result JSON") String json) {}

    @Authorization(
            action = "memory.read",
            resources = "olo://{tenant}/conversation/{conversationId}")
    @ServerTool(
            name = "read",
            title = "Read memory",
            description = "Deterministic platform memory.read operation.",
            openWorld = OpenWorld.FALSE,
            stateless = true,
            businessKeyNamespace = KeyNamespace.TENANT,
            effect = EffectSafety.READ_ONLY,
            routes = "read")
    public Result read(
            @Param(description = "Opaque resource UUID", minLength = 36, maxLength = 36)
                    String conversationId,
            @Param(description = "Operation request JSON; no credentials or identity fields")
                    String request,
            ToolContext context) {
        var principal = context.invocation();
        if (principal.tenantId().isEmpty()
                || principal.operation().filter("memory.read"::equals).isEmpty()
                || !principal.delegationChain().isEmpty())
            throw new SecurityException("Verified platform principal required");
        principal.requireUser();
        var response =
                context.http()
                        .route("read")
                        .body(Map.of("conversationId", conversationId, "request", request))
                        .sendRaw();
        if (response.status() != 200)
            throw new IllegalStateException(
                    "Governed service refused operation; reconcile before retry");
        return new Result(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Authorization(
            action = "memory.append",
            resources = "olo://{tenant}/conversation/{conversationId}")
    @ServerTool(
            name = "append",
            title = "Append memory",
            description = "Deterministic platform memory.append operation.",
            openWorld = OpenWorld.FALSE,
            stateless = true,
            businessKeyNamespace = KeyNamespace.TENANT,
            effect = EffectSafety.IDEMPOTENT,
            routes = "append",
            reconciliation = Reconciliation.MANUAL)
    public Result append(
            @Param(description = "Opaque resource UUID", minLength = 36, maxLength = 36)
                    String conversationId,
            @Param(description = "Operation request JSON; no credentials or identity fields")
                    String request,
            ToolContext context) {
        var principal = context.invocation();
        if (principal.tenantId().isEmpty()
                || principal.operation().filter("memory.append"::equals).isEmpty()
                || !principal.delegationChain().isEmpty())
            throw new SecurityException("Verified platform principal required");
        principal.requireUser();
        var response =
                context.http()
                        .route("append")
                        .body(Map.of("conversationId", conversationId, "request", request))
                        .sendRaw();
        if (response.status() != 200)
            throw new IllegalStateException(
                    "Governed service refused operation; reconcile before retry");
        return new Result(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Authorization(
            action = "memory.delete",
            resources = "olo://{tenant}/conversation/{conversationId}")
    @ServerTool(
            name = "delete",
            destructive = true,
            title = "Delete memory",
            description = "Deterministic platform memory.delete operation.",
            openWorld = OpenWorld.FALSE,
            stateless = true,
            businessKeyNamespace = KeyNamespace.TENANT,
            effect = EffectSafety.IDEMPOTENT,
            routes = "delete",
            reconciliation = Reconciliation.MANUAL)
    public Result delete(
            @Param(description = "Opaque resource UUID", minLength = 36, maxLength = 36)
                    String conversationId,
            @Param(description = "Operation request JSON; no credentials or identity fields")
                    String request,
            ToolContext context) {
        var principal = context.invocation();
        if (principal.tenantId().isEmpty()
                || principal.operation().filter("memory.delete"::equals).isEmpty()
                || !principal.delegationChain().isEmpty())
            throw new SecurityException("Verified platform principal required");
        principal.requireUser();
        var response =
                context.http()
                        .route("delete")
                        .body(Map.of("conversationId", conversationId, "request", request))
                        .sendRaw();
        if (response.status() != 200)
            throw new IllegalStateException(
                    "Governed service refused operation; reconcile before retry");
        return new Result(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Authorization(
            action = "memory.share",
            resources = "olo://{tenant}/conversation/{conversationId}")
    @ServerTool(
            name = "share",
            title = "Share memory",
            description = "Deterministic platform memory.share operation.",
            openWorld = OpenWorld.FALSE,
            stateless = true,
            businessKeyNamespace = KeyNamespace.TENANT,
            effect = EffectSafety.IDEMPOTENT,
            routes = "share",
            reconciliation = Reconciliation.MANUAL)
    public Result share(
            @Param(description = "Opaque resource UUID", minLength = 36, maxLength = 36)
                    String conversationId,
            @Param(description = "Operation request JSON; no credentials or identity fields")
                    String request,
            ToolContext context) {
        var principal = context.invocation();
        if (principal.tenantId().isEmpty()
                || principal.operation().filter("memory.share"::equals).isEmpty()
                || !principal.delegationChain().isEmpty())
            throw new SecurityException("Verified platform principal required");
        principal.requireUser();
        var response =
                context.http()
                        .route("share")
                        .body(Map.of("conversationId", conversationId, "request", request))
                        .sendRaw();
        if (response.status() != 200)
            throw new IllegalStateException(
                    "Governed service refused operation; reconcile before retry");
        return new Result(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Authorization(
            action = "memory.export",
            resources = "olo://{tenant}/conversation/{conversationId}")
    @ServerTool(
            name = "export",
            title = "Export memory",
            description = "Deterministic platform memory.export operation.",
            openWorld = OpenWorld.FALSE,
            stateless = true,
            businessKeyNamespace = KeyNamespace.TENANT,
            effect = EffectSafety.IDEMPOTENT,
            routes = "export",
            reconciliation = Reconciliation.MANUAL)
    public Result export(
            @Param(description = "Opaque resource UUID", minLength = 36, maxLength = 36)
                    String conversationId,
            @Param(description = "Operation request JSON; no credentials or identity fields")
                    String request,
            ToolContext context) {
        var principal = context.invocation();
        if (principal.tenantId().isEmpty()
                || principal.operation().filter("memory.export"::equals).isEmpty()
                || !principal.delegationChain().isEmpty())
            throw new SecurityException("Verified platform principal required");
        principal.requireUser();
        var response =
                context.http()
                        .route("export")
                        .body(Map.of("conversationId", conversationId, "request", request))
                        .sendRaw();
        if (response.status() != 200)
            throw new IllegalStateException(
                    "Governed service refused operation; reconcile before retry");
        return new Result(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
