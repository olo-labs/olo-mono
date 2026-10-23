# M2.1 — ToolGate capability gap matrix

The evidence comes from reading the ToolGate source at commit `fc4f6955` (`D:\git\olo-toolgate`), not from its
design documents. "Present" means code implements the capability. It does **not** mean it is qualified for OLO:
the qualification tests are M3.4, M3.5 and M4.5.

| Status | Meaning |
|---|---|
| Present | Implemented in runtime code |
| Partial | Some of it is implemented; the gap is named |
| Missing | Defined only in a contract or design document, or not at all |

## Summary

| # | Capability needed by OLO | Status | Owner milestone |
|---|---|---|---|
| G1 | User-bound Gateway authentication (path A) | **Missing** | M3.4 |
| G2 | Service-principal authentication (path C) | Partial | M3.5 |
| G3 | Delegated user + agent authentication (path B) | Partial | M3.10 |
| G4 | Credential issuance, renewal and revocation for OLO | Partial | M3.4, D4 |
| G5 | User and team permission resolution independent of OLO agents | Partial | M3.6 |
| G6 | Object-level resource authorization | Partial | M3.7 |
| G7 | Trusted executor context (tenant, user, session, resource) | **Missing** | M4.1–M4.3 |
| G8 | Signed, request-bound authorization evidence | Present for executors; **Missing** for downstream services | M4.6 |
| G9 | Current authority re-checked at the effect | Present for Tool Host and devices | M4.6 (extend to services) |
| G10 | Invocation states, including `OUTCOME_UNKNOWN` | Present (states); Partial (reconciliation) | M5.4 |
| G11 | Dedup with a stored fingerprint and conflict on mismatch | Partial | M5.3 |
| G12 | Approval resume and result retrieval for Gateway callers | Partial | M5.5 |
| G13 | Revocation within a measured bound | Partial | M2.4, M8.2 |
| G14 | Audit records with subject, actor, resource, decision and policy version | Partial | M4.4 |

## Details

### G1 — User-bound Gateway authentication: Missing

- **What exists:** the Gateway accepts only static workload credentials.
  - `apps/gateway/src/auth.rs` (credential checks near line 172) rejects `context.mode == RequestMode::Human`.
  - It also requires `workload_binding_id`, `agent_id` and `credential_epoch` on every credential.
  - The doc comment states "Static tokens cannot impersonate a HUMAN session".
- **What is only a contract:** `CredentialKind` in `packages/contracts/schemas/v2/common.schema.json` lists `JWT` and
  `INTROSPECTION`, but `apps/gateway/src` has no JWT, JWKS or introspection validator.
- **Where human mode does exist:** Control console sessions, through `HumanRuntime.java`
  (`apps/control-plane/.../application/`). It builds a `RequestMode.HUMAN` context from a console actor. That is a
  management path, not a Gateway runtime path for OLO.
- **Gap:** a Gateway credential that represents the signed-in OLO user, with no OLO agent involved. Built in M3.4,
  with the mechanism chosen by D4.

### G2 — Service-principal authentication: Partial

- `auth.rs` accepts `SERVICE` mode with no `user_id` and no `session_epoch`, but still requires an `agent_id` and a
  workload binding.
- **Gap:** a path C principal is therefore a ToolGate agent record, which needs a rule that keeps it apart from OLO
  workflow agents: a reserved naming, ownership or type flag, so OLO's agent mapping (M3.9) can never select it.
  It also needs explicit, resource-scoped grants (`EnterpriseGrantPurpose.SERVICE` exists).

### G3 — Delegated user + agent: Partial

- `auth.rs` requires `user_id` and `session_epoch` for `DELEGATED` mode. The `RequestContext` carries `chain`
  (actor hops), `agentId` and `delegatedSessionEpoch`.
- **Gap:** with static tokens, the delegated user is fixed in the credential file at provisioning. OLO needs a
  per-user delegated credential minted at request time (D4).

### G4 — Credential lifecycle: Partial

- **Present:** static credentials have an expiry and a `credentialEpoch`. The Gateway reloads its credential file on
  `SIGHUP`, and invalid files are rejected atomically (`docs/api/gateway-api.md`).
- **Not inbound:** `TOKEN_EXCHANGE` appears only in `AuthType` (`packages/contracts/java/.../v2/AuthType.java`), which
  is the outbound authentication a tool uses for its HTTP routes.
- **Gap:** no issuance API for short-lived user-bound credentials, and no renewal or revocation endpoint for them.

### G5 — User and team permission resolution: Partial

- **Present:** grants have a `purpose` that includes `HUMAN`. The target design describes team-derived human scopes
  (`docs/security/enterprise-access-control.md`, "Human direct").
- **Gap:** this is reachable only from the console path (G1), so it isn't usable by OLO until G1 is closed.

### G6 — Object-level resource authorization: Partial

- **Present:**
  - `ControlResourceExtractor` and `ResourceExtraction.java` extract resources from tool arguments by JSON pointer,
    into `ResourceDescriptor{kind, locator}`, and feed them to evaluation.
  - Grants carry `EnterpriseResourceRule{kind, locator, match: EXACT | PREFIX | ANY}`.
  - `ResourceKind` is `FILE | URL | DATABASE | DEVICE | CUSTOM`.
- **Gaps:**
  1. Package tools use the fixed `tool://<tool-id>` resource (`gateway-api.md`), so extractors must also apply to
     the `olo.memory` and `olo.rag` packages.
  2. `ControlAccessGrant` is documented as "never an individual ACL", and grants are group-sourced. Per-conversation
     or per-document ownership is application data, not ToolGate directory data. See
     [resources-and-permissions.md](resources-and-permissions.md) for the proposed split.
  3. There is no locator template bound to the caller (for example "the caller's own namespace"). Without one, a
     user-scoped PREFIX rule needs a grant per user.

### G7 — Trusted executor context: Missing

- `tool-sdk/java-sdk/api/.../context/Invocation.java` exposes `invocationId`, `toolId`, optional `agentId`,
  optional `userId`, `catalogRevision`, `deadline`, `businessKey`, `attempt` and `mode`.
- There is **no tenant, session, resource or operation** accessor.
- `runtime-mcp/.../StandaloneContext.java:303` returns `Optional.empty()` for `userId()`.
- **Gap:** M4.1 adds the full principal contract, and M4.3 refuses scoped tools when it is absent.

### G8 — Signed, request-bound evidence: Present for executors, Missing for services

- **Present:** `EnterpriseSignedPermit` is an RS256 JWS with header `typ: toolgate-effect-permit+jws`. Its claims are
  `issuer, audience, nonce, invocationId, requestDigest, evaluationDigest, authorizationEpoch, directoryRevision,
  issuedAtUnixMs, expiresAtUnixMs, toolDigest, packageDigest, bindingId, deviceId, approvalIds`
  (`schemas/v1/enterprise.schema.json`).
- **Gap for OLO:**
  - The permit's audience is the executor (a Tool Host or device), not the memory or RAG service behind it.
  - Tenant, subject and resource are bound only indirectly, through `evaluationDigest`.

  Downstream services need their own evidence. See [authorization-evidence.md](authorization-evidence.md).

### G9 — Current authority at the effect: Present for executors

- Tool Host consumes the permit's nonce with Control before executing (`apps/tool-host/src/control.rs`,
  `consume` → `POST /permits/consume`). `EnterprisePermitConsumption` is certificate-bound and single-use.
- On consumption Control's `fresh()` (`EnterpriseOperations.java`) refuses if the invocation has expired or if
  `authorizationEpoch` changed, then **re-runs the decision** (`decide(..., true)`).
- **Gap:** this protects the executor's effect. The service's effect (the database write) happens after it, so
  M2.10 extends the same idea to the service.

### G10 — Invocation states and reconciliation: Partial

- **Present:** `EnterpriseInvocationState` is `PENDING_APPROVAL, QUEUED, RESERVED, DISPATCHED, EXECUTING, SUCCEEDED,
  FAILED, CANCELLED, EXPIRED, PARTIAL, OUTCOME_UNKNOWN`. Invocations carry a `downstreamIdempotencyKey`
  (`EnterpriseOperations.java`).
- **Gap:** there is no per-executor reconciliation procedure, and no rule that forbids automatic redispatch while the
  outcome is `OUTCOME_UNKNOWN`.

### G11 — Dedup: Partial

- **`/mcp`:** "a retry with the same JSON-RPC `id` and the same caller maps to the same invocation"
  (`docs/api/api-reference.md`, tools/call).
- **`/access/invocations`:** requires an `Idempotency-Key` header.
- **Gap:** not yet verified:
  - that a reused id with *different* arguments is a conflict that never executes;
  - that concurrent submissions create exactly one invocation;
  - how long dedup records are retained.

  The contract is in [invocation-lifecycle.md](invocation-lifecycle.md); verification is in M5.3.

### G12 — Approval resume for Gateway callers: Partial

- **Present:** `EnterpriseApproval` binds `invocationId`, `requestDigest`, `authorizationEpoch`, `directoryRevision`
  and obligations. Its states are `PENDING, APPROVED, DENIED, CANCELLED, EXPIRED, REVOKED`. Console callers can fetch
  a result (`HumanRuntime.result`).
- **Gap:** a Gateway MCP caller gets `PENDING_APPROVAL` plus an `invocationId`, but there is no documented
  authenticated way to wait for, resume or fetch that invocation's result.

### G13 — Revocation bound: Partial

- **Present:** the `authorizationEpoch` check at consumption (G9), `credentialEpoch` and `sessionEpoch` on the context,
  and the kill switch (`docs/adr/017-kill-switch.md`).
- **Gap:**
  - No stated or measured bound.
  - Whether every relevant change bumps `authorizationEpoch` (a membership, share or session revocation) is
    unverified.
  - Cached results are not covered.

### G14 — Audit: Partial

- **Present:** invocation audit through
  `tx.audit(actorId, operation, "invocation:" + id, revision, requestId, requestDigest)` (`EnterpriseOperations.java`).
- **Gap:** subject versus actor, resource, decision, and policy version are not separate audit fields. They may be
  recoverable from the stored evaluation; M4.4 makes them explicit.

## What this means for the plan

- **Two capabilities are missing:** G1 (user-bound Gateway authentication) and G7 (trusted executor context). They are
  the critical path, and no memory or RAG code should start before both are qualified.
- **G8, G9 and G6 are a strong base.** Signed request-bound permits, online consumption with an epoch re-check, and
  argument-based resource extraction already exist. The OLO work extends them to downstream services and to
  package tools, rather than inventing new mechanisms.
