# Plan: ToolGate-governed tools, memory and RAG for OLO

**Goal.** OLO stops shipping tool code. Everything that reaches data or causes an effect goes through
[OLO ToolGate](../../git/olo-toolgate), under the identity that should be authorized for it:

- **Conversation memory and RAG** are platform capabilities. OLO runs them deterministically. ToolGate authorizes
  them for the **signed-in user** and the specific resource. Separate memory and RAG services store the data and
  enforce the authorized scope.
- **Business tools chosen by a model** are authorized for the user *plus* the delegated agent actor. The agent's
  grants are a ceiling, never a substitute for the user's.
- All local tool code, annotations, call schemas and TOOL nodes are removed from olo-mono.
- A multi-agent pipeline on ToolGate's mocked tools proves the design end to end.

**Scope: fresh installation only.** The target is a new installation, with new pipelines and new workflows, and
empty application stores (conversations, memory, documents, ToolGate state).

| In scope | Out of scope |
|---|---|
| New identities, grants, services, pipelines and workflows, provisioned from documented steps | Migrating any existing conversations, documents, embeddings or ToolGate state |
| Retry safety, authorization, deletion consistency and failure handling for new workflows | Compatibility readers for old workflow JSON, `tools[]` or TOOL nodes |
| | Supporting workflow executions started before the new code |

Fresh installation simplifies removal (M9) and deployment (M10). It does not relax the authorization, retry or
deletion requirements.

**Status (2026-10-23): 3% counted, 13% implemented.**
- **Counted:** M0 is complete.
- **Implemented, not yet counted:** M1 is in review as [olo-labs/olo-mono#2](https://github.com/olo-labs/olo-mono/pull/2)
  on branch `feature/toolgate-integration`. It counts once the PR is merged.
- **Plan revisions:** three security reviews are folded in. They set the user/resource authorization model and the
  deterministic memory/RAG flow, and cover fresh-install scope, service-job identity, payload protection,
  unknown-outcome reconciliation, revocation of derived context, authorization evidence for services, and
  credential renewal.
- **M1 remediation:** tracked as M1-R1 to M1-R6.
- **Next:** M2. The architecture is cleared to proceed. Three contracts must be agreed in M2 before anything that
  depends on them is built: derived-context revocation (M2.4), authorization evidence (M2.10) and credential renewal
  (M2.5).

**The installation boundary is fixed.** We ship one new installation contract: new pipelines, workflows and
identities, and empty application stores. No migration tools, legacy readers, compatibility modes, or support for
executions started under the old implementation will be built. A change to this boundary needs a new decision
that replaces D8.

## Progress

| # | Milestone | Repos | Weight | Implemented | Counted | Status |
|---|---|---|---:|---:|---:|---|
| M0 | Discovery, plan and security reviews | all | 3 | 100% | 100% | ✅ Done |
| M1 | ToolGate client and agent tool loop (historical) | olo-mono | 10 | 100% | 0% | 🟨 In review (PR #2); remediation in M3, M5 |
| M2 | Capability gaps, contracts and open decisions | all | 7 | 0% | 0% | ⬜ Next |
| M3 | Verified user and service identity; resource authorization | olo, ToolGate, olo-mono | 15 | 0% | 0% | ⬜ |
| M4 | Trusted SDK, executor context and executor-to-service calls | ToolGate, services | 8 | 0% | 0% | ⬜ |
| M5 | Durable invocation, payload protection, retries and approvals | olo-mono, ToolGate | 12 | 0% | 0% | ⬜ |
| M6 | Memory and RAG services and ToolGate packages | ToolGate, services | 12 | 0% | 0% | ⬜ |
| M7 | OLO conversation and document lifecycles | olo-mono, olo | 10 | 0% | 0% | ⬜ |
| M8 | Isolation, revocation, recovery and real-stack tests | all | 8 | 0% | 0% | ⬜ (runs twice: before and after M9) |
| M9 | Remove legacy local tool code; coordinate backend and UI | olo-mono, olo, olo-ui, olo-chat, olo-docker | 10 | 0% | 0% | ⬜ |
| M10 | Deployment, clean install and operational qualification | all | 5 | 0% | 0% | ⬜ |
| | **Overall** | | **100** | **13%** | **3%** | |

**How the percentages work.**
- A milestone's weight is its share of the total effort, and inside a milestone every step has an equal share.
- **Implemented** counts steps whose code or document is written and has passed its local checks.
- **Counted** counts only steps whose PR is merged and whose acceptance check passes. Counted is the progress figure.
- Overall % = Σ (weight × milestone %) / 100. Update both columns when a step changes.

**Order.** M2 → M3 → M4 → M5 → M6 → M7 → M8 (first run) → M9 → M8 (re-run) → M10.
- No memory or RAG code (M6) is written before the identity, authorization and executor contracts (M2 to M4) exist
  and are qualified.
- Local tool code is deleted (M9) only after the governed paths have passed M8.
- M8 then runs again, because tests passed before M9 don't prove the final stack works without the local tool code.

**Where the earlier milestones went.** Old M2 (split agent delegation) and old M3 (remove tool code) → M9. Old M4
(memory/RAG as agent tools) is replaced by M6 + M7. Old M5 (incident-recovery demo) → M8.4. Old M6 (debug stack,
docs, cross-repo) → M8.3, M9.6–M9.7 and M10.8.

---

## Target design

### Three call paths

| Path | What | Who decides to call | Authorization subject | Role of `agentName` |
|---|---|---|---|---|
| **A. Governed platform calls** | Load conversation, append turn, RAG query, ingest, delete | OLO runtime, deterministically | The signed-in user (direct-human mode) | None |
| **B. Agent tool calls** | Business tools in the agent's catalog | The model, inside the M1 tool loop | User **and** delegated agent actor | Capability ceiling and audit |
| **C. Service jobs** | Scheduled indexing, maintenance, retention sweeps | OLO runtime | A ToolGate service principal with explicit resource-scoped grants | None |

- **Path A** never goes through an LLM decision, and it is not an AGENT node. Removing TOOL nodes does not mean
  making every operation an agent: OLO keeps an internal **governed-call** abstraction for paths A and C (M7.1).
- **Path C** authenticates with an internal ToolGate workload identity. That identity provides service
  accountability and nothing more.
  - It is a dedicated platform principal, never an OLO workflow agent.
  - A workflow's `agentName` can neither select it nor widen it.
  - It needs its own explicit, resource-scoped grants (for example "index knowledge base X"). It has no blanket
    access to memory or RAG, and it never acts as a made-up user.

### Memory and RAG flow (path A)

```
Authenticated user
  → OLO backend verifies identity, tenant membership and session                  [olo, M3]
  → OLO runtime calls ToolGate with a user-bound credential                       [olo-mono, M3/M7]
  → ToolGate authorizes the user, the operation and the resource                  [ToolGate, M3/M4]
  → Memory/RAG executor gets that scope through a protected context               [ToolGate, M4]
  → Executor calls the memory/RAG service over an authenticated channel           [ToolGate + services, M4]
  → Service enforces the scope in storage                                         [services, M6]
  → Authorized context is handed to the agents, as untrusted data                 [olo-mono, M7]
  → OLO appends the accepted user message and the finalized response              [olo-mono, M7]
```

### Identity model

| Flow | Authorization subject | Role of the agent identity |
|---|---|---|
| Conversation memory | Signed-in user | Not involved |
| User document retrieval | Signed-in user | Not involved |
| User ingest or delete request | Signed-in user | Not involved |
| Other tools the agent chooses | User plus delegated actor | Capability ceiling and audit |
| Scheduled indexing and maintenance | ToolGate service principal with resource-scoped grants | Not involved; no fabricated user identity |

- **Subject and actor stay separate** for delegated calls (RFC 8693 token exchange semantics).
- **`agentName` never selects** a user's memory, never determines document permissions, and never selects the path C
  service principal.
- **Direct-user Gateway authentication is implementation work, not configuration** (M3.4).
  - Today the Gateway's static credentials reject `HUMAN` mode, and they require an `agentId` and a workload binding
    (`apps/gateway/src/auth.rs`).
  - Direct-human execution exists only for Control console sessions (`HumanRuntime`).
  - Path A needs a supported user-bound Gateway path that doesn't make an OLO agent an authorization dependency.

### Identity context contract (agreed in M2.2)

| Field | Requirement |
|---|---|
| Tenant | Derived from the verified identity and trusted tenant membership. Never a fallback to `sub`. |
| User | A stable internal user id, mapped from the verified issuer and subject. Absent for path C. |
| Session | A validated authenticated session, with revocation handling |
| Authentication expiry and audience | Present and checked. Credentials are issued for ToolGate's audience only. |
| Resource | Conversation, memory namespace, knowledge base or document id. A locator, not a permission; ownership is checked server-side. |
| Operation | An explicit permission (see *Permissions*) |
| Correlation | Run, turn, invocation and logical-operation ids, for tracing and idempotency |

- Hiding `userId` or `tenantId` from the model's schema does **not** make them trusted. ToolGate authenticates where
  they came from, and the executor receives them only through a protected context.
- Credentials are short-lived and audience-restricted, or an equivalent authenticated server binding.
  Frontend tokens are never forwarded downstream: no token passthrough, per the MCP security guidance and the
  OAuth 2.0 Security Best Current Practice (RFC 9700).
- **Bearer and refresh tokens are never persisted**: not in workflow state, Temporal history, logs or service storage.
- **Protection is about misuse, not reuse.** A legitimate retry may present the same bearer credential again.
  Credentials must resist:
  - impersonation
  - use outside their intended audience, tenant or session
  - duplicate effects, which dedup (M5.3) prevents, not the credential

  Sender binding (for example mTLS-bound or DPoP-style tokens) is required only if M2.5 decides so.

### Credential renewal in long-running work

Workflows and approvals can outlive a user credential. The rules (contract in M2.5):

- The run stores only a **non-secret binding reference**: the verified session binding, tenant and user, never a token.
- Before each user-scoped call, OLO gets a fresh short-lived credential through a trusted mechanism, either the OLO
  backend's credential service or ToolGate token exchange (D4). It then rechecks that the user's current authority
  still covers the operation and resource.
- If the session has expired or been revoked, the user-scoped operation **pauses for re-authentication** or is
  **terminated**. Which one is set per operation type in M2.5.
- A user-scoped operation is **never silently switched** to the path C service principal, or to any other identity.

### Permissions

Each operation is its own permission, and none implies another.

| Area | Permissions |
|---|---|
| Conversation memory | `memory.read`, `memory.append`; separately `memory.delete`, `memory.share`, `memory.export` |
| Long-term user memory | `ltm.read`, `ltm.write`, `ltm.delete` (if D6 puts it in scope) |
| RAG | `rag.query`; separately `rag.ingest`, `rag.update`, `rag.delete`, `rag.share` |

Policies grant exactly the operations a role needs. There is no blanket `memory.*` or `rag.*` grant. Ordinary chat
needs only `memory.read` and `memory.append` on the user's own conversations, granted as ALLOW so a turn never waits
for approval. Delete, share, export and long-term memory changes each have their own policy.

### Responsibilities

| Party | Owns |
|---|---|
| ToolGate | Policy decisions, execution authorization, permits, approvals, audit |
| ToolGate memory/RAG executors | Translating an authorized invocation into a service call that carries the verified scope. They hold only service-call credentials. |
| Memory and RAG services | Storage, and enforcing the authorized scope on every read and write. **They own the database credentials.** |
| Databases | Extra isolation where they support it (for example tenant-scoped collections or row security) |
| OLO | Authenticated identity context, and deterministic orchestration of when memory loads, retrieval runs and turns persist |
| Agents | Consuming the authorized context. They cannot widen it. |

**No bypassing ToolGate.** The services accept calls only from ToolGate executors, and are not network-reachable
from the OLO worker, the backend or the model. Two separate checks apply (contract in M2.10, built in M4.6):

1. **Who is calling.** mTLS or audience-restricted service credentials prove the caller is a ToolGate executor.
   That alone does not prove the user and resource scope it sends were authorized.
2. **What was authorized.** Each request carries authorization evidence that the service verifies itself. Either:
   - a permit **cryptographically signed by ToolGate Control**, checked against Control's published keys, or
   - an authenticated verification call to ToolGate for the permit reference.

   The evidence binds the audience (that specific service), tenant, subject or service principal, operation,
   resource, a request fingerprint (a hash of the exact request), and an expiry. The service refuses the request if
   any of them is missing, expired, or doesn't match the request it received.

### Revocation timing

Revoking a grant, membership, share or session takes effect within a **defined bound**. M2.4 sets the number, and
M8 measures it. Authorization is checked again at three points:

1. **Before context reaches the model.** Already-loaded history or retrieved chunks are re-checked against current
   grants before each LLM call that would include them. Revoked items are dropped.
2. **Before an effect executes.** In-flight and approved-but-not-run operations are re-authorized at dispatch.
3. **Before a result is released.** Results and cached content are re-checked when they are fetched or returned.

**Derived context counts too.** Dropping the original chunk or message isn't enough. An earlier model response, a
summary, or a child agent's output may repeat revoked information. So:

- Every piece of context the run builds carries its **source dependencies**: the message, document version and chunk
  ids it was derived from. This covers model responses, summaries, child-agent outputs and tool results that
  quoted protected content.
- At check points 1 and 3, anything that depends on a revoked source is discarded or regenerated without it, before
  the next model call and before any result is released.
- Where dependencies can't be tracked precisely, the derived item is treated as depending on everything that was in
  its prompt.
- Stored conversation summaries are versioned with their dependencies (M6.1), and are rebuilt when a source is
  revoked or deleted.

### Memory types

| Type | Scope and behavior |
|---|---|
| Conversation history | Private conversation records. Sharing is explicit. Default access requires tenant plus owner or membership. |
| Working state | Temporary run state with bounded retention |
| Long-term user memory | Separate, controlled facts and preferences, with provenance, correction, deletion and retention (in scope per D6) |

### RAG controls

- Every chunk stores tenant, document id, ownership and sharing metadata, document version and authorization metadata.
- The authorized scope is a **mandatory filter** on query, fetch, update and delete. It is applied in vector search,
  and checked again before content is released. The model never builds or overrides the filter.
- **Versions publish atomically.** A new document version becomes searchable all at once, and the old version
  disappears in the same step, never half and half.
- **Deletion takes effect immediately.** Deleting a document writes a tombstone that excludes it from search at once,
  then removes its source content, chunks, embeddings and caches. An indexing job that started before the deletion
  checks the tombstone and version before writing, so it can't bring deleted content back.
- ACL changes propagate to the chunks and invalidate cached results. **Caches are keyed by tenant and authorization
  context**, so a cached result is never served to a caller with a different scope.
- Results carry source and version citations. Retrieved text is untrusted data, never instructions.
- Ingestion is controlled: file limits, parser protections, and restricted outbound access.
- The embedding model is versioned, with a reindexing process. The demo hash vectors are replaced.

These follow the OWASP guidance for RAG applications. Qdrant supports tenant filtering, but OLO's authorization
still has to supply that filter and enforce it.

### Workflow payloads

- Sensitive arguments, prompts, retrieved content and tool results are protected in Temporal from the moment
  durable workflow state is introduced (M5), not later.
- D7 (decided in M2) chooses between an encryption payload codec and storing references instead of content.
- Tokens are never part of any payload.

---

## M0 — Discovery, plan and security reviews (weight 3) ✅

- [x] Survey the ToolGate Gateway API, MCP surface, agent and workload-binding model, and mocked tools
- [x] Inventory tool code across olo-mono (8 modules) and the workflow JSON (67 files)
- [x] Confirm that agent delegation (`agentCalls`) shares the planner schema with `toolCalls`
- [x] Write the plan
- [x] Fold in the first security review: user authorization, resource permissions, durable invocations, deterministic
      platform calls. Its references were checked against the code.
- [x] Fold in the second review: fresh-install scope, direct-user Gateway gap, service-job identity, payload
      protection timing, unknown-outcome reconciliation, explicit memory permissions, revocation timing, document
      consistency, executor-to-service calls
- [x] Fold in the third review: revocation of derived context, authorization evidence that services verify,
      credential renewal, dedup scope and retention, turn assembly and completion, RAG quality and performance
      targets, a fixed installation boundary

## M1 — ToolGate client and agent tool loop (weight 10) 🟨 implemented, in review

Implemented in [olo-labs/olo-mono#2](https://github.com/olo-labs/olo-mono/pull/2), which isn't merged yet, so it
isn't counted. It remains the basis for path B.

- [x] **1.1** `olo-toolgate-client`: MCP 2026-07-28 client (`McpToolGateClient`) for `tools/list` and `tools/call`.
      Mirrored headers, JSON/SSE replies, CA-pinned TLS, typed fail-closed `ToolGateException`s.
- [x] **1.2** `FileAndEnvCredentialResolver`: per-agent workload tokens from `OLO_TOOLGATE_TOKEN_<AGENT_NAME>` or
      `<credentialsDir>/<agentName>.token`, re-read on every call, with path-safe names
- [x] **1.3** Worker `toolgate` config section, validated at load and probed at bootstrap
- [x] **1.4** `agentName` and `maxToolRounds` on AGENT nodes (`AgentToolSettings`). The Studio property moved to M9.5.
- [x] **1.5** Native tool calling in `OllamaLlmClient.chat`
- [x] **1.6** `AgentToolLoop`: list, chat, call; model-safe names; deterministic JSON-RPC ids; a round limit
- [x] **1.7** `PENDING_APPROVAL` ends the turn as `WAITING_APPROVAL` (to be replaced by M5.5). Tool errors are
      shown to the model; outages fail the node.
- [x] **1.8** 53 new tests across four modules, all passing

The boxes mark the steps as implemented. They count toward progress once PR #2 is merged.

**M1 remediation.** These gaps were found by the reviews. Each is done in, and counted by, the milestone named.

| # | Gap in M1 | Fix | Done in |
|---|---|---|---|
| M1-R1 | An activity retry re-runs the whole LLM loop. The model may choose different calls or arguments, which produce new request ids and can repeat effects. | Persist each selected tool operation, with a stable logical-operation id, before dispatch. Recovery resumes it rather than regenerating it. | M5.1 |
| M1-R2 | `agentName` is editable, so a workflow author can select any token mounted on the worker | Only server-authorized workload mappings (tenant, workflow → allowed agent identities). The worker stops resolving arbitrary names to tokens. | M3.9 |
| M1-R3 | Path B calls ToolGate as the agent alone | Delegated mode: user subject plus agent actor | M3.10 |
| M1-R4 | `WAITING_APPROVAL` tells the user to run the request again | Resume the original invocation | M5.5 |
| M1-R5 | Transport dedup (the JSON-RPC id) is treated as if it were business idempotency | Separate the two; reconcile unknown outcomes per executor | M5.4 |
| M1-R6 | Prompts, tool results and outputs can land in Temporal history | Payload protection, decided in M2 and implemented in M5 | M5.2 |

**Known unrelated failures.** 7 existing `olo-kernel` tests fail the same way on a clean `origin/main`. Five need
`olo-workflow-input/samples/minimal-local/workflow-input.json`, which `.gitignore` (`*/samples/*/*.json`) excludes.
Two read conversation history that other tests leave in `olo-core/tools/demo-data/conversations`. M9 deletes that
data; the missing sample needs its own fix.

## M2 — Capability gaps, contracts and open decisions (weight 7)

The ToolGate documentation describes the identity modes we need, but its
[enterprise access-control design](../../git/olo-toolgate/docs/security/enterprise-access-control.md) calls itself a
"target design" and is not a claim that the features exist. Readiness has to be shown by code and tests.

- [ ] **2.1** Gap matrix, with evidence (code paths and tests, not docs), for each required ToolGate capability:
      user-bound Gateway authentication; service-principal authentication; credential lifecycle; user and team
      permission resolution independent of OLO agents; object-level resource authorization; trusted executor
      context; revocation during calls, approvals, jobs and result retrieval; durable invocation status, cancellation
      and reconciliation; audit fields. Known gaps so far:
      - Gateway static credentials reject `HUMAN` mode and require an `agentId` and workload binding (`auth.rs`).
        Direct-human execution exists only for Control console sessions (`HumanRuntime`).
      - The SDK's `Invocation.userId()` is optional, and there is no tenant accessor. `StandaloneContext.userId()`
        always returns empty.
      - Package tools have a fixed `tool://<tool-id>` resource, which can't tell one conversation or document from another.
- [ ] **2.2** Identity context contract (the table above), for user-bound paths A and B and for path C: fields,
      provenance, signing or binding, and the expiry and audience rules. Agreed across olo, olo-mono and ToolGate.
- [ ] **2.3** Resource and permission model: resource identifiers; owner, membership and sharing semantics; the
      permission set from *Permissions*, with no wildcard grants
- [ ] **2.4** Invocation lifecycle and revocation contract (**P1**):
      - **Dedup key:** tenant + subject or service principal + logical-operation id + request fingerprint (a hash of
        the operation, resource and exact arguments). Credential rotation doesn't change the key. Reusing the key
        with a different fingerprint is a conflict.
      - **Dedup retention:** at least the longest workflow lifetime plus the approval expiry. Once a record has
        expired, recovery treats the operation as `OUTCOME_UNKNOWN` and reconciles it (M5.4); it never re-sends blindly.
      - Statuses, including `OUTCOME_UNKNOWN`
      - Cancellation, approval resume, and result retrieval
      - **The revocation bound** (a number of seconds), the three check points, and **source-dependency tracking for
        derived context** (what is recorded, how a derived item is discarded or rebuilt), from *Revocation timing*
- [ ] **2.5** Credential lifecycle and renewal contract (**P1**), for user-bound and service credentials:
      - Issuance, audience, expiry, revocation and tenant binding
      - Renewal during long-running workflows and approvals: the non-secret binding reference the run stores, the
        trusted mechanism that issues fresh credentials, and rechecking current authority
      - Behavior when the session has expired or been revoked: pause for re-authentication or terminate, per
        operation type. Never fall back to the service principal.
      - Misuse protection: impersonation, use outside the audience, tenant or session, and whether sender binding is
        required
      - Decide D4
- [ ] **2.6** Decide D5: who owns the memory and RAG services, and where they are deployed
- [ ] **2.7** Decide D6: whether long-term user memory ships in the first release
- [ ] **2.8** Decide D7: payload codec or references, and key management for it
- [ ] **2.9** Threat-model update for paths A, B and C, in ToolGate's `docs/security` and in OLO
- [ ] **2.10** Authorization evidence contract (**P1**): how a memory or RAG service validates what was authorized.
      - The format: a Control-signed permit with key discovery and rotation, or authenticated verification against ToolGate
      - The required claims: audience, tenant, subject or service principal, operation, resource, request
        fingerprint, expiry
      - How replay within the expiry is bounded
- [ ] **2.11** Conversation turn contract:
      - How the accepted user message reaches the prompt exactly once
      - What happens when persisting the response fails after it was generated
      - When a turn counts as complete (M7.2)
- [ ] **2.12** RAG quality and performance targets, each with a measurement method and an evaluation set:
      - Retrieval relevance within the authorized scope (for example recall@k on a labelled set)
      - Citation correctness: every citation names an authorized source and version that contains the cited text
      - Indexing freshness: time from ingest to searchable
      - Context limits respected
      - Query and turn latency (p50 and p95)
      - Expected concurrent users and documents

**Acceptance.**
- All three repos sign off a written contract set (2.2–2.5, 2.10, 2.11).
- The three P1 contracts (2.4 derived-context revocation, 2.5 renewal, 2.10 authorization evidence) are agreed
  before M3–M7 implement them.
- D4–D7 are decided, and the targets in 2.12 are set.
- Every gap in 2.1 has an owner and a milestone.

## M3 — Verified user and service identity; resource authorization (weight 15)

- [ ] **3.1** [olo] Replace the demo JWT handling. `JwtTenantIdDecoder` and `DefaultJwtTenantExtractor` decode claims
      without checking signatures, and fall back from `tenantId` to `sub`. Verify issuer, signature, audience and
      expiry; map to a stable internal user id; resolve tenant from trusted membership; remove the `sub` fallback.
- [ ] **3.2** [olo] Session binding and revocation
- [ ] **3.3** [olo → olo-mono] Carry the verified identity context into the run as a **non-secret binding reference**
      (server-bound data). Never as client fields, never as stored tokens.
      - Before each user-scoped call, the kernel gets a fresh credential through the M2.5 mechanism and rechecks
        current authority.
      - On an expired or revoked session it pauses for re-authentication or terminates the operation. It never
        substitutes the service principal.
- [ ] **3.4** [ToolGate] **Implement a user-bound Gateway path.**
      - A short-lived, audience-restricted, tenant-bound credential for the signed-in user that the Gateway accepts in
        `HUMAN` mode.
      - No OLO agent identity is required or consulted.
      - Tests show the OLO worker can't mint the credential, can't impersonate another user with it, and can't use it
        outside its audience, tenant or session. Legitimate retries may present it again; duplicate effects are
        prevented by dedup (M5.3).
      - Sender binding is included if M2.5 requires it.
- [ ] **3.5** [ToolGate] Service-principal path for path C: a dedicated platform workload identity in `SERVICE` mode,
      separate from all OLO workflow agents, with explicit resource-scoped grants
- [ ] **3.6** [ToolGate] Resolve the user's and team's current permissions independently of OLO workflow agents
- [ ] **3.7** [ToolGate] Object-level authorization for conversations, memory namespaces, knowledge bases and documents
      that reaches execution, beyond the fixed `tool://<tool-id>` resource
- [ ] **3.8** [ToolGate] Separate operation permissions as defined in *Permissions*. Wildcard grants are refused.
- [ ] **3.9** [olo-mono] **M1-R2:** server-authorized workload mappings for path B. A workflow's `agentName` must be in
      the allowlist for that tenant and workflow, or the call fails. The path C principal can never be mapped.
- [ ] **3.10** [olo-mono, ToolGate] **M1-R3:** delegated mode for path B, with the user as subject and the agent as actor.
      Deny when the agent may use a tool but the user may not.
- [ ] **3.11** [olo] The same checks on every alternate endpoint: uploads, document downloads, knowledge-source
      listings, session APIs, exports and result retrieval

**Acceptance.**
- Requests without a verified identity are rejected.
- A known resource id with the wrong tenant or user is denied by ToolGate.
- A workflow can't pick an agent identity it isn't mapped to.
- A path A call succeeds with no OLO agent identity involved.

## M4 — Trusted SDK, executor context and executor-to-service calls (weight 8)

- [ ] **4.1** [ToolGate] Complete principal contract in the SDK `Invocation`: tenant, user (or service principal),
      session, resource, operation and correlation, required for scoped tools (including a tenant accessor)
- [ ] **4.2** [ToolGate] Propagate it from Gateway → Control → executor. Permits bind identity, operation, resource,
      exact arguments and expiry.
- [ ] **4.3** [ToolGate] Scoped tools refuse to run without a trusted identity. This includes standalone runtimes, where
      `userId()` is empty today.
- [ ] **4.4** [ToolGate] Audit records: subject, actor, workload, resource, operation, decision, policy version,
      invocation correlation and outcome
- [ ] **4.5** [ToolGate] Qualification tests proving identity survives Gateway → Control → executor → service → storage
- [ ] **4.6** [ToolGate, services] Executor-to-service calls, to the M2.10 contract:
      - **Caller authentication:** mTLS or audience-restricted service credentials prove the caller is a ToolGate
        executor
      - **Authorization evidence:** every request carries a Control-signed permit, or a reference the service
        verifies with ToolGate. The service checks audience, tenant, subject or service principal, operation,
        resource, request fingerprint and expiry against the request it actually received.
      - Network policy keeps the services unreachable from the OLO worker, the backend and the model

**Acceptance.**
- A test package sees exactly the verified principal and resource. A forged argument can't change it, and a missing
  identity is refused.
- A direct call to a memory or RAG service that bypasses ToolGate is refused.
- A genuine executor is refused if it sends a scope, resource, operation or request body that doesn't match its
  authorization evidence, or evidence that is expired or meant for another service.

## M5 — Durable invocation, payload protection, retries and approvals (weight 12)

Temporal can run a failed activity again, so every effect has to be safe to replay.

- [ ] **5.1** [olo-mono] **M1-R1:** split the agent turn. One activity gets the model's decision and records each selected
      operation, with a stable logical-operation id, in workflow state. A separate activity dispatches each recorded
      operation. On recovery, recorded operations are resumed, never regenerated.
- [ ] **5.2** [olo-mono] **M1-R6:** payload protection (D7) ships together with 5.1. Sensitive arguments, prompts,
      retrieved content and results are encrypted or stored by reference. Bearer and refresh tokens are never persisted.
- [ ] **5.3** [ToolGate, olo-mono] Dedup on the M2.4 key: tenant + subject or service principal + logical-operation id
      + request fingerprint.
      - Credential rotation and renewal don't change the key.
      - Reusing the key with a different fingerprint is a conflict, not a silent replay.
      - Records are kept for the M2.4 retention period. After expiry, a recovering operation becomes `OUTCOME_UNKNOWN`
        and goes through reconciliation (5.4).
- [ ] **5.4** [olo-mono, ToolGate] **M1-R5: unknown outcomes.** Transport dedup is separate from executor and business
      idempotency. Polling the invocation status alone can't settle a timeout after an external effect, so:
      - Each side-effecting executor declares its reconciliation method: a downstream idempotency key where the
        target system supports one, or an executor-specific status lookup.
      - While the outcome is unknown, the operation is marked `OUTCOME_UNKNOWN` and is **never redispatched
        automatically**. It is resolved by reconciliation or by an operator.
- [ ] **5.5** [olo-mono, ToolGate] **M1-R4:** approvals resume the original invocation. The run waits on an
      authenticated notification or bounded polling, then retrieves the result. Covers expiry, denial, cancellation,
      and re-authorizing at dispatch (*Revocation timing*, point 2).

      Resuming after a long wait follows *Credential renewal*: a fresh credential comes from the stored binding
      reference, and current authority is rechecked. An expired or revoked session means re-authentication or
      termination, never the service principal.
- [ ] **5.6** [olo-mono] Bounded retries, timeouts and cancellation propagated to ToolGate
- [ ] **5.7** Tests:
      - a crash after dispatch but before recording
      - an activity retry
      - an unknown outcome that stays undispatched until reconciled
      - a dedup record that has expired, then recovery
      - a credential rotated mid-operation, keeping the same dedup key
      - approve, deny and expire
      - a grant revoked while waiting
      - a session that expires during an approval
      - no tokens or cleartext sensitive payloads in Temporal history

**Acceptance.**
- No completed side effect repeats.
- An unknown outcome is never redispatched automatically.
- An approval resumes the original operation.
- Temporal history holds no tokens and no unprotected sensitive content.

## M6 — Memory and RAG services and ToolGate packages (weight 12)

ToolGate governs access; it is not the storage engine. Memory and RAG are separate services (D5), exposed through
ToolGate packages. The existing demo code is not copied:
- `ConversationPluginSupport` is file storage keyed by session.
- `RagVectorStoreSupport` filters Qdrant only by `capabilitySource`.

The stores start empty (fresh install), so no import or migration is built.

- [ ] **6.1** Conversation history service:
      - Tenant, owner and membership on every record, with explicit sharing
      - Transactional, idempotent writes keyed by stable message ids, with concurrency control
      - Defined ordering for simultaneous turns
      - Versioned summaries that record their source dependencies and are rebuilt when a source is revoked or deleted
      - Context budgets
      - Deletion that also removes derived summaries and embeddings
- [ ] **6.2** Working-state store with bounded retention
- [ ] **6.3** Long-term user memory as a separate store, with provenance, correction, deletion and retention, if D6
      puts it in the first release
- [ ] **6.4** RAG service:
      - Chunk metadata, mandatory authorized filters on query/fetch/update/delete, and a release-time re-check
      - Atomic version publication
      - Tombstones that exclude a deleted document from search at once, and that stop in-flight indexing from
        recreating it
      - Complete deletion of content, chunks, embeddings and caches
      - ACL propagation; caches keyed by tenant and authorization context
      - Citations; a versioned, real embedding model and a reindex process
- [ ] **6.5** Ingestion controls: file size and type limits, parser hardening, restricted outbound access.
      Retrieved text is marked as untrusted data.
- [ ] **6.6** ToolGate packages `olo.memory` and `olo.rag`, built with the Java Tool SDK on the M4 principal contract.
      Executors hold only service-call credentials (M4.6). Database credentials stay in the services.
- [ ] **6.7** Service tests: isolation (two users, two tenants), ACL change, revocation within the M2.4 bound, version
      publication, deletion versus a concurrent indexing job, concurrent appends

**Acceptance.** The services refuse any request outside the authorized scope, even with a valid resource id. A deleted
document never reappears in search.

## M7 — OLO conversation and document lifecycles (weight 10)

- [ ] **7.1** [olo-mono] Governed-call abstraction for deterministic path A and path C operations. It is neither an
      AGENT node nor model-selected, and it runs on M5's durable dispatch.
- [ ] **7.2** [olo-mono] Conversation lifecycle, replacing per-agent memory hooks (contract in M2.11):
      1. Authorize the conversation.
      2. **Persist each accepted user message** idempotently, under its stable message id, once it is accepted.
      3. Load permitted history once for that turn. Assemble the prompt so the accepted user message appears
         **exactly once**: history is loaded up to, but not including, the current message id, and the current message
         is added as the turn's input.
      4. Pass the relevant context, with its source dependencies, to the workflow's agents.
      5. **Persist the finalized response** idempotently, under its stable message id.
      6. **Mark the turn complete only after the response is durably recorded.** The response is released to the user
         as final only then.
         - If persisting fails after the response was generated, the append is retried with the same message id.
         - If it still fails within the retry bound, the turn ends as a failure. The unrecorded response is not
           released as final.

      Only the conversation lifecycle writes history; child agents never do. Retries may repeat idempotent persistence
      calls, but must never create duplicate records. Lifecycle rules by outcome:

      | Turn outcome | What is persisted |
      |---|---|
      | Success | The accepted user message and the finalized response |
      | Failure | The accepted user message, plus a failure marker linked to it. No partial response. |
      | Cancellation | The accepted user message, plus a cancellation marker. No partial response. |
      | Streaming | Nothing while streaming. The response is persisted once, when finalized. A broken stream follows the failure rule. |
      | Pending approval | The accepted user message, plus a pending marker. The response is persisted when the resumed invocation (M5.5) finishes. |

- [ ] **7.3** [olo-mono] Context re-check before each LLM call and before a result is released (*Revocation timing*,
      points 1 and 3).
      - Already-loaded history and retrieved chunks are re-authorized, and revoked items are dropped.
      - **Derived context** is checked through its source dependencies: earlier model responses, summaries,
        child-agent outputs and tool results. Anything that depends on a revoked source is discarded, or regenerated
        without it, before the next model call and before release.
- [ ] **7.4** [olo-mono] Document lifecycle: ingest and delete are governed calls under the user. Scheduled indexing
      runs on path C under the service principal, with resource-scoped grants.
- [ ] **7.5** [olo-mono] RAG retrieval as a governed call. Results reach agents as untrusted, cited context.
- [ ] **7.6** [olo] Backend endpoints (chat, uploads, knowledge sources, sessions, exports) go through the same identity
      and authorization path

**Acceptance.**
- Each turn persists exactly the records the lifecycle table requires, with no duplicates under retries or
  concurrent turns.
- The accepted user message appears in the prompt exactly once.
- A turn is never reported complete before its response is durably recorded.
- Changing the workflow's agent doesn't change what history or documents the user can reach.
- A response or summary derived from a revoked source is not reused in a later prompt or released.

## M8 — Isolation, revocation, recovery and real-stack tests (weight 8)

M8 runs twice: once to qualify the governed paths before M9, and again after M9 on the final stack. Both runs must
pass for M8 to count.

- [ ] **8.1** Mandatory acceptance cases, automated against the real stack:
      1. Two users in one tenant cannot access each other's private resources.
      2. Cross-tenant access fails, even with a known resource id.
      3. Changing the workflow agent does not change memory or RAG entitlement.
      4. Forged identity arguments and substituted resource ids fail.
      5. Revoked grants block execution, cached content access and result retrieval.
      6. Concurrent turns and activity retries create no duplicate memory records.
      7. Crash recovery does not repeat completed side effects, and an unknown outcome is not redispatched.
      8. An approval resumes the original operation.
      9. Document ACL changes and deletion affect retrieval within the M2.4 bound, and a deleted document never reappears.
- [ ] **8.2** Revocation timing, measured at all three check points against the M2.4 bound. Covers already-loaded
      context, **derived context** (an earlier response, summary or child-agent output that quoted the revoked
      source), and in-flight operations.
- [ ] **8.2a** RAG quality and performance, measured against the M2.12 targets on the evaluation set:
      - retrieval relevance within the authorized scope
      - citation correctness
      - indexing freshness
      - context limits
      - query and turn latency (p50 and p95) at the expected concurrent load
- [ ] **8.3** Debug stack `debug.ps1 -ToolGate`: start or attach ToolGate and the memory/RAG services, add readiness
      probes, provision identities (old 6.1)
- [ ] **8.4** Multi-agent `incident-recovery` pipeline on ToolGate's `server-recovery` mocks (old M5), on path B with
      the user as subject and each agent as actor:

      | OLO agent (`agentName`, server-mapped) | Agent capability ceiling | Role |
      |---|---|---|
      | `incident-commander` (orchestrator) | none | Delegates and writes the summary |
      | `triage-agent` | `incidents.incidents`, `incidents.host-status`, `incidents.crash-report` | Finds the incident and its root cause |
      | `remediation-agent` | `incidents.runbook`, `incidents.remediate` (**ASK**), `incidents.verify` | Rolls back after approval, then verifies |
      | `incident-report-agent` | `incidents.update-incident` | Records the outcome and resolves INC-5001 |

      Tests:
      - `triage-agent` never sees `incidents.remediate`.
      - A user without `incidents.remediate` is denied, even through `remediation-agent`.
      - After approval, the original `incidents.remediate` invocation resumes (M5.5).
- [ ] **8.5** Evidence kept: run transcripts, the ToolGate audit log, the test reports
- [ ] **8.6** Re-run 8.1–8.5 after M9, on the stack without local tool code

**Acceptance.**
- In both runs: all nine cases pass, revocation (including derived context) stays within the bound, and the RAG
  quality and performance targets are met.
- The incident-recovery run completes after approval, and every call is audited with the right subject, actor,
  resource and decision.

## M9 — Remove legacy local tool code; coordinate backend and UI (weight 10)

Fresh installation only: no deprecations, shims, compatibility readers or migration of existing workflows or data. A
workflow that still contains `tools[]` or a TOOL node fails validation. The ToolGate client and the path B tool loop
(`olo-toolgate-client`, `org.olo.kernel.agent.tool`) legitimately keep tool schemas and tool-call concepts, and they
stay.

- [ ] **9.1** Separate agent delegation from local tool calls (old M2):
      - Rename `kernel/toolcall` to `kernel/agentcall`, and `ToolCallExpansionExecutionStrategy` to
        `AgentCallExpansionExecutionStrategy`
      - The planner schema keeps only `agentCalls`
- [ ] **9.2** `olo-spi`, `olo-annotation`, `olo-annotation-processor`: delete the local tool SPI (`org.olo.spi.tool.*`,
      `@ToolId`, `ToolProvider`), `@OloTool`, `ToolDescriptor`, `CatalogToolCollector`, the tool validator branches,
      and `tools.json`
- [ ] **9.3** `olo-core`: delete the `tools` module, its demo data and the tool human-input plugins, plus
      `ToolRegistry` and the tool branch in `ExecutionEngine`
- [ ] **9.4** `olo-definition` and `olo-kernel`:
      - Delete `ToolDefinition`, `ToolCallPlannerSupport`, `NodeType.TOOL`, `WorkflowDefinition.tools` and
        `capability.tool_requirements`
      - Remove the local tool validator rules, `ToolNodeTypeHandler`, `ToolChainMerger`, `AvailableToolsJsonResolver`,
        `ToolLabelResolver`, `ParsedToolCall` and `ToolCallRetryVariables`
- [ ] **9.5** Replace the shipped workflow JSON with new workflows generated from the definitions (no conversion of
      old files):
      - No `tools[]`, no TOOL nodes, no `{availableToolsJson}`
      - The AGENT Studio property `agentName`, limited to server-mapped identities (M3.9)
      - Rewrite the planner end-to-end tests against the fake Gateway
- [ ] **9.6** olo-docker: remove the `OLO_SHARED_SOURCE` and tools demo-data mounts and `tools.json`; add ToolGate
      and memory/RAG service configuration for a fresh install
- [ ] **9.7** olo, olo-ui, olo-chat: remove TOOL nodes from the Studio palette and the catalog readers. Each is a PR in
      its own repo, landed in the same window as 9.2–9.5.
- [ ] **9.8** Guard, limited to the **legacy local tool APIs**:
      `grep -rnE "org\.olo\.spi\.tool|@OloTool|ToolDescriptor|NodeType\.TOOL|ToolNodeTypeHandler|ToolCallPlannerSupport|availableToolsJson|\"toolCalls\""`
      finds nothing outside the ToolGate client and the path B tool loop. Every module builds and tests green.

**Acceptance.** No legacy local tool type, annotation, node, schema or JSON field remains. Every repo is green. M8 is
then re-run (8.6).

## M10 — Deployment, clean install and operational qualification (weight 5)

- [ ] **10.1** Encrypted transport everywhere, and encryption at rest for memory, RAG and ToolGate state
- [ ] **10.2** Credential rotation for user-bound, service, executor-to-service and database credentials, with tested
      rollover. Also rotation of the payload-protection keys (M5.2).
- [ ] **10.3** Redacted diagnostics: no tokens, prompts or retrieved content in logs or errors
- [ ] **10.4** Bounded retries, timeouts, cancellation, and per-user quotas. A load test at the M2.12 concurrency on
      staging confirms the latency and freshness targets hold with quotas on.
- [ ] **10.5** Backups, restore tests, and retention policies for conversations, working state, long-term memory and documents
- [ ] **10.6** **Clean-install acceptance test.** On empty infrastructure, follow only the documented steps to provision
      identities, grants (including the path C service principal), the memory and RAG services, ToolGate packages and
      the new workflows. Then check:
      - M8's smoke subset passes.
      - A **newly created pipeline** runs end to end with all local tool execution removed. No local tool module is
        on the classpath, and nothing falls back to one.
      - Workflow JSON with legacy fields (`tools[]`, a TOOL node, `{availableToolsJson}`, `toolCalls`) is **rejected
        with a clear validation error** naming the field. It is not accepted, converted or ignored.

      This tests the new contract without supporting the old one.
- [ ] **10.7** Grant guide: explicit `memory.read` and `memory.append` (ALLOW) for chat on the user's own conversations,
      plus separate policies for `memory.delete`, `memory.share`, `memory.export`, long-term memory changes and each
      RAG operation. No wildcard grants.
- [ ] **10.8** Docs: architecture, the identity and permission model, an operations runbook (including
      `OUTCOME_UNKNOWN` handling), and the incident-recovery walkthrough (old 6.4)

**Acceptance.** The clean-install test passes on a staging environment from documented steps alone, along with a
restore test and a credential rotation.

---

## Decisions

| # | Question | Decision or recommendation | Status |
|---|---|---|---|
| D1 | Where do conversation memory and RAG go? | ToolGate *governs* them, as user-authorized platform capabilities. Separate memory and RAG services *store* the data and enforce scope. OLO orchestrates them deterministically (path A). They are not agent-selected tools, and agent identities don't authorize them. | Decided 2026-10-23, revised after review |
| D2 | What happens after an ASK approval? | Resume the original invocation (M5.5), with expiry, denial, cancellation and re-authorization at dispatch. M1's "run it again" is temporary and is removed in M5. | Decided; implementation in M5 |
| D3 | Is end-user identity required? | **Required scope.** Memory and RAG are authorized for the signed-in user. Agent-selected tools are authorized for user plus delegated actor. Scheduled jobs run as a ToolGate service principal with resource-scoped grants. | Decided |
| D4 | How does OLO obtain user-bound, audience-restricted ToolGate credentials? | Token exchange if ToolGate supports it, otherwise an authenticated server binding. Never token passthrough, never persisted. | Open, **decided in M2.5** |
| D5 | Who owns the memory and RAG services, and where do they run? | Separate services next to ToolGate's executors, never in the OLO worker. They own the database credentials. | Open, **decided in M2.6** |
| D6 | Is long-term user memory in the first release? | The design covers it. Building it can follow conversation history and RAG. | Open, **decided in M2.7** |
| D7 | How are sensitive Temporal payloads protected? | An encryption payload codec, or keeping only references in history. Implemented with durable state in M5.2. | Open, **decided in M2.8** |
| D8 | Existing data and executions? | **Out of scope, and the boundary is fixed.** One new installation contract: no migration tools, legacy readers, compatibility modes, or support for executions started under the old implementation. Changing this needs a new decision that replaces D8. | Decided |

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| ToolGate's identity and resource features are a target design, and its Gateway currently rejects `HUMAN` mode for static credentials | M3–M6 depend on ToolGate work of unknown size | M2.1's evidence-based gap matrix comes first; user-bound Gateway auth is explicit work (M3.4). No memory or RAG code is written before M4 qualifies. |
| OLO's current JWT handling is demo code (no signature check, `sub` fallback) | Any identity-based control would be bypassable | M3.1 is a hard prerequisite for path A |
| Tool reach depends on agents chosen by workflow authors | Privilege escalation through `agentName` | Server-authorized mappings (M3.9), a user-plus-actor check (M3.10), and the path C principal can never be mapped |
| A retry or crash repeats a side effect, or an unknown outcome is retried blindly | Double remediation, duplicate memory records | Durable recorded operations (M5.1), executor-specific reconciliation, no automatic redispatch (M5.4), tests (M8.1 cases 6–7) |
| Sensitive data or tokens reach Temporal history before protection exists | Data exposure | Payload protection ships with durable state (M5.2), tokens are never persisted, redaction (M10.3) |
| Revocation lags behind already-loaded context or in-flight work | A user keeps seeing or affecting data after losing access | A defined bound (M2.4), three check points (M5.5, M7.3, M6.4), measured in M8.2 |
| Revoked information survives in derived context (an earlier response, summary or child-agent output) | Revoked content keeps reaching the model or the user | Source-dependency tracking, discard or rebuild before the next model call and before release (M2.4, M6.1, M7.3), tested in M8.2 |
| A genuine executor sends a scope it wasn't authorized for | Services trust a forged user or resource scope | Services verify signed or ToolGate-verified authorization evidence bound to the request, not just mTLS (M2.10, M4.6) |
| A long workflow or approval outlives the user's credential | Calls fail mid-run, or worse, fall back to a broader identity | Non-secret binding reference, fresh credentials and an authority recheck; re-authenticate or terminate, never fall back (M2.5, M3.3, M5.5) |
| Dedup records expire before a slow recovery | A duplicate effect after the window | Retention covers the longest workflow plus approval expiry; after expiry → `OUTCOME_UNKNOWN` and reconciliation (M2.4, M5.3) |
| Secure RAG that retrieves poorly or slowly | The platform is safe but not usable | Quality and performance targets set in M2.12, measured in M8.2a and load-tested in M10.4 |
| A deletion races an indexing job | Deleted content reappears | Tombstones and version checks (M6.4), tested in M6.7 and M8.1 case 9 |
| Services reachable without ToolGate | Authorization bypassed | Authenticated executor-to-service calls and network policy (M4.6) |
| Memory and RAG depend on ToolGate and the services being up | No chat history or retrieval during an outage | Fail closed with a clear error, readiness probes (M8.3), timeouts and quotas (M10.4) |
| Retrieved documents carry prompt injection | The model is steered by data | Retrieved text is marked untrusted, the model can't touch the filters, citations (M6.4, M7.5) |
| `llama3.2` (3B) is unreliable on long tool chains | Flaky M8.4 demo | 1–3 tools per agent, and a larger model through `-Model` |
| olo, olo-ui and olo-chat break while TOOL nodes still exist there | Stack regressions | Land M9.7 in the same window as M9.2–M9.5; pin images in `debug.ps1` until then |

## References

- ToolGate: [enterprise access-control target design](../../git/olo-toolgate/docs/security/enterprise-access-control.md),
  [Gateway API](../../git/olo-toolgate/docs/api/gateway-api.md), [threat model](../../git/olo-toolgate/docs/security/threat-model.md),
  Gateway credential checks in `apps/gateway/src/auth.rs`, console human runtime in
  `apps/control-plane/.../application/HumanRuntime.java`
- [RFC 8693, OAuth 2.0 Token Exchange](https://www.rfc-editor.org/rfc/rfc8693) (subject and actor separation)
- [RFC 9700, OAuth 2.0 Security Best Current Practice](https://www.rfc-editor.org/rfc/rfc9700)
- MCP specification: authorization, and security best practices (audience validation, no token passthrough)
- OWASP guidance on RAG and LLM application security
- Temporal documentation: activity retries, and data encryption with payload codecs
