# Plan: ToolGate-governed tools, memory and RAG for OLO

**Goal.** OLO stops shipping tool code. Everything that reaches data or causes an effect goes through
[OLO ToolGate](../../git/olo-toolgate), under the identity that should be authorized for it:

- **Conversation memory and RAG** are platform capabilities. OLO runs them deterministically. ToolGate authorizes
  them for the **signed-in user** and the specific resource. Separate memory and RAG services store the data and
  enforce the authorized scope.
- **Business tools chosen by a model** are authorized for the user *plus* the delegated agent actor. The agent's
  grants are a ceiling, never a substitute for the user's.
- All tool code, annotations, call schemas and TOOL nodes are removed from olo-mono, with no backward compatibility.
- A multi-agent pipeline on ToolGate's mocked tools proves the design end to end.

**Status (2026-10-23): 13% of target work done.** A security review (folded into this revision) found that the
previous plan authorized memory and RAG through agent identities. The plan now requires user authorization,
resource-level permissions, durable invocations and deterministic platform orchestration, and it adds the
ToolGate work this needs. The scope is much larger, so progress was recalculated: M0 and M1 are done, everything
else is open. M1 is in review as [olo-labs/olo-mono#2](https://github.com/olo-labs/olo-mono/pull/2) on branch
`feature/toolgate-integration`. The remediation it needs is tracked below (M1-R1 to M1-R6). **Next:** M2.

## Progress

| # | Milestone | Repos | Weight | Done | Status |
|---|---|---|---:|---:|---|
| M0 | Discovery, plan and security review | all | 3 | 100% | ✅ Done |
| M1 | ToolGate client and agent tool loop (historical) | olo-mono | 10 | 100% | ✅ Done; remediation in M3, M5, M10 |
| M2 | Capability gaps and cross-repo contracts | all | 7 | 0% | ⬜ Next |
| M3 | Verified user identity and resource authorization | olo, ToolGate, olo-mono | 15 | 0% | ⬜ |
| M4 | Trusted SDK and executor context | ToolGate | 8 | 0% | ⬜ |
| M5 | Durable invocation, retries and approvals | olo-mono, ToolGate | 12 | 0% | ⬜ |
| M6 | Memory and RAG services and ToolGate packages | ToolGate, services | 12 | 0% | ⬜ |
| M7 | OLO conversation and document lifecycles | olo-mono, olo | 10 | 0% | ⬜ |
| M8 | Isolation, recovery and real-stack tests | all | 8 | 0% | ⬜ |
| M9 | Remove old tool code; coordinate backend and UI | olo-mono, olo, olo-ui, olo-chat, olo-docker | 10 | 0% | ⬜ |
| M10 | Deployment and operational qualification | all | 5 | 0% | ⬜ |
| | **Overall** | | **100** | **13%** | |

**How the percentages work.** A milestone's weight is its share of the total effort. Inside a milestone, every
step has the same share. Overall % = Σ (weight × milestone done %) / 100. Tick a step when its PR is merged and its
acceptance check passes, then update this table.

**Order.** M2 → M3 → M4 → M5 → M6 → M7 → M8 → M9 → M10. No memory or RAG code (M6) is written before the identity,
authorization and executor-context contracts (M2 to M4) exist and are qualified. The old tool code is deleted (M9)
only once the governed paths have passed M8, so every PR keeps the build green.

**Where the earlier milestones went.** Old M2 (split agent delegation) and old M3 (remove tool code) → M9. Old M4
(memory/RAG as agent tools) is replaced by M6 + M7. Old M5 (incident-recovery demo) → M8.3. Old M6 (debug stack,
docs, cross-repo) → M8.2, M9.6–M9.8 and M10.7.

---

## Target design

### Two call paths

| Path | What | Who decides to call | Authorization subject | Role of `agentName` |
|---|---|---|---|---|
| **A. Governed platform calls** | Load conversation, append turn, RAG query, ingest, delete | OLO runtime, deterministically | The signed-in user (direct-human mode) | None |
| **B. Agent tool calls** | Business tools in the agent's catalog | The model, inside the M1 tool loop | User **and** delegated agent actor | Capability ceiling and audit |
| **C. Service jobs** | Scheduled indexing, maintenance | OLO runtime | An explicit service principal | None; no made-up user |

Path A never goes through an LLM decision, and it is not an AGENT node. Removing TOOL nodes does not mean making
every operation an agent: OLO keeps an internal **governed-call** abstraction for path A and path C (M7.1).

### Memory and RAG flow (path A)

```
Authenticated user
  → OLO backend verifies identity, tenant membership and session                  [olo, M3]
  → OLO runtime calls ToolGate with verified user context                         [olo-mono, M3/M7]
  → ToolGate authorizes the user, the operation and the resource                  [ToolGate, M3/M4]
  → Memory/RAG executor gets that authorized scope through a protected context    [ToolGate, M4]
  → Memory/RAG service enforces the scope in storage                              [services, M6]
  → Authorized context is handed to the workflow's agents, as untrusted data      [olo-mono, M7]
  → OLO appends the final conversation turn through ToolGate                      [olo-mono, M7]
```

### Identity model

| Flow | Authorization subject | Role of the agent identity |
|---|---|---|
| Conversation memory | Signed-in user | Not involved |
| User document retrieval | Signed-in user | Not involved |
| User ingest or delete request | Signed-in user | Not involved |
| Other tools the agent chooses | User plus delegated actor | Capability ceiling and audit |
| Scheduled indexing | Explicit service principal | Not involved; no fabricated user identity |

- **OLO's workload identity** still authenticates transport and gives service accountability. It never stands in
  for the requesting user's permissions.
- **Subject and actor stay separate** for delegated calls (RFC 8693 token exchange semantics).
- **`agentName` never selects** a user's memory and never determines document permissions.

### Identity context contract (agreed in M2.2)

| Field | Requirement |
|---|---|
| Tenant | Derived from the verified identity and trusted tenant membership. Never a fallback to `sub`. |
| User | A stable internal user id, mapped from the verified issuer and subject |
| Session | A validated authenticated session, with revocation handling |
| Authentication expiry and audience | Present and checked. Credentials are issued for ToolGate's audience only. |
| Resource | Conversation, memory namespace, knowledge base or document id. A locator, not a permission; ownership is checked server-side. |
| Operation | An explicit permission, for example `memory.read`, `memory.append`, `rag.query`, `rag.ingest`, `rag.delete` |
| Correlation | Run, turn, invocation and logical-operation ids, for tracing and idempotency |

- Hiding `userId` or `tenantId` from the model's schema does **not** make them trusted. ToolGate authenticates
  where they came from, and the executor receives them only through a protected context.
- Credentials are short-lived and audience-restricted, or an equivalent authenticated server binding.
  Frontend tokens are never forwarded downstream: no token passthrough, per the MCP security guidance and the
  OAuth 2.0 Security Best Current Practice (RFC 9700).

### Responsibilities

| Party | Owns |
|---|---|
| ToolGate | Policy decisions, execution authorization, permits, approvals, audit |
| Memory and RAG services | Storage, and enforcing the authorized resource scope on every read and write |
| Databases | Extra isolation where they support it (for example tenant-scoped collections or row security) |
| OLO | Authenticated identity context, and deterministic orchestration of when memory loads, retrieval runs and turns persist |
| Agents | Consuming the authorized context. They cannot widen it. |

Database credentials exist only in the trusted memory and RAG executors and services, never in the OLO worker or the model.

### Memory types

| Type | Scope and behavior |
|---|---|
| Conversation history | Private conversation records. Sharing is explicit. Default access requires tenant plus owner or membership. |
| Working state | Temporary run state with bounded retention |
| Long-term user memory | Separate, controlled facts and preferences, with provenance, correction, deletion and retention |

### RAG controls

- Every chunk stores tenant, document id, ownership and sharing metadata, document version and authorization metadata.
- The authorized scope is a **mandatory filter** on query, fetch, update and delete. It is applied in vector search,
  and checked again before content is released where needed. The model never builds or overrides the filter.
- ACL changes propagate to the chunks and invalidate cached results. Deleting a document removes its source content,
  chunks, embeddings and relevant caches.
- Results carry source and version citations. Retrieved text is untrusted data, never instructions.
- Ingestion is controlled: file limits, parser protections, and restricted outbound access.
- The embedding model is versioned, with a reindexing process. The demo hash vectors are replaced.

These follow the OWASP guidance for RAG applications. Qdrant supports tenant filtering, but OLO's authorization
still has to supply that filter and enforce it.

---

## M0 — Discovery, plan and security review (weight 3) ✅

- [x] Survey the ToolGate Gateway API, MCP surface, agent and workload-binding model, and mocked tools
- [x] Inventory tool code across olo-mono (8 modules) and the workflow JSON (67 files)
- [x] Confirm that agent delegation (`agentCalls`) shares the planner schema with `toolCalls`
- [x] Write the plan
- [x] Fold in the security review: user authorization, resource permissions, durable invocations, deterministic
      platform calls. The review's references were checked against the code (M2.1 records them as evidence).

## M1 — ToolGate client and agent tool loop (weight 10) ✅ historical

Delivered in [olo-labs/olo-mono#2](https://github.com/olo-labs/olo-mono/pull/2). It remains the basis for path B.

- [x] **1.1** `olo-toolgate-client`: MCP 2026-07-28 client (`McpToolGateClient`) for `tools/list` and `tools/call`.
      Mirrored headers, JSON/SSE replies, CA-pinned TLS, typed fail-closed `ToolGateException`s.
- [x] **1.2** `FileAndEnvCredentialResolver`: per-agent workload tokens from `OLO_TOOLGATE_TOKEN_<AGENT_NAME>` or
      `<credentialsDir>/<agentName>.token`, re-read on every call, with path-safe names
- [x] **1.3** Worker `toolgate` config section, validated at load and probed at bootstrap
- [x] **1.4** `agentName` and `maxToolRounds` on AGENT nodes (`AgentToolSettings`). The Studio property moved to M9.5.
- [x] **1.5** Native tool calling in `OllamaLlmClient.chat`
- [x] **1.6** `AgentToolLoop`: list, chat, call; model-safe names; deterministic JSON-RPC ids; a round limit
- [x] **1.7** `PENDING_APPROVAL` ends the turn as `WAITING_APPROVAL` (to be replaced by M5.4). Tool errors are
      shown to the model; outages fail the node.
- [x] **1.8** 53 new tests across four modules, all passing

**M1 remediation.** These gaps were found by the review. Each is done in, and counted by, the milestone named.

| # | Gap in M1 | Fix | Done in |
|---|---|---|---|
| M1-R1 | An activity retry re-runs the whole LLM loop. The model may choose different calls or arguments, which produce new request ids and can repeat effects. | Persist each selected tool operation, with a stable logical-operation id, before dispatch. Recovery resumes it rather than regenerating it. | M5.1 |
| M1-R2 | `agentName` is editable, so a workflow author can select any token mounted on the worker | Only server-authorized workload mappings (tenant, workflow → allowed agent identities). The worker stops resolving arbitrary names to tokens. | M3.8 |
| M1-R3 | Path B calls ToolGate as the agent alone | Delegated mode: user subject plus agent actor | M3.9 |
| M1-R4 | `WAITING_APPROVAL` tells the user to run the request again | Resume the original invocation | M5.4 |
| M1-R5 | Transport dedup (the JSON-RPC id) is treated as if it were business idempotency | Separate the two; reconcile unknown outcomes | M5.3 |
| M1-R6 | Prompts, tool results and outputs can land in Temporal history | Payload encryption or references | M10.5 |

**Known unrelated failures.** 7 existing `olo-kernel` tests fail the same way on a clean `origin/main`. Five need
`olo-workflow-input/samples/minimal-local/workflow-input.json`, which `.gitignore` (`*/samples/*/*.json`) excludes.
Two read conversation history that other tests leave in `olo-core/tools/demo-data/conversations`. M9 deletes that
data; the missing sample needs its own fix.

## M2 — Capability gaps and cross-repo contracts (weight 7)

The ToolGate documentation describes the identity modes we need, but its
[enterprise access-control design](../../git/olo-toolgate/docs/security/enterprise-access-control.md) calls itself a
"target design" and is not a claim that the features exist. Readiness has to be shown by code and tests.

- [ ] **2.1** Gap matrix, with evidence (code paths and tests, not docs), for each required ToolGate capability:
      direct-human runtime authentication; credential lifecycle; user and team permission resolution independent of
      OLO agents; object-level resource authorization; trusted executor context; enforcing revocation during calls,
      approvals, jobs and result retrieval; durable invocation status, cancellation and reconciliation; audit fields.
      Known gaps so far:
      - The SDK's `Invocation.userId()` is optional, and there is no tenant accessor.
      - `StandaloneContext.userId()` always returns empty.
      - Package tools have a fixed `tool://<tool-id>` resource, which can't tell one conversation or document from another.
- [ ] **2.2** Identity context contract (the table above): fields, provenance, signing or binding, and the expiry and
      audience rules. Agreed across olo, olo-mono and ToolGate.
- [ ] **2.3** Resource and permission model:
      - Resource identifiers for conversations, memory namespaces, knowledge bases and documents
      - Owner, membership and sharing semantics
      - Separate permissions: memory read/append/delete; long-term memory read/write/delete; RAG
        query/ingest/update/delete/share. Query never implies ingest or delete.
- [ ] **2.4** Invocation lifecycle contract:
      - Logical-operation ids and the idempotency scope (subject + request; reusing an id with changed arguments is a conflict)
      - Statuses, cancellation, reconciliation after an unknown outcome
      - Approval resume, and result retrieval
- [ ] **2.5** Credential lifecycle: issuance, audience, expiry, renewal, revocation and tenant binding. Choose the
      mechanism (D4): token exchange if ToolGate supports it, otherwise an authenticated server binding.
- [ ] **2.6** Threat-model update for paths A, B and C, in both ToolGate's `docs/security` and OLO

**Acceptance.** All three repos sign off a written contract set (2.2–2.5). Every gap in 2.1 has an owner and a milestone.

## M3 — Verified user identity and resource authorization (weight 15)

- [ ] **3.1** [olo] Replace the demo JWT handling. `JwtTenantIdDecoder` and `DefaultJwtTenantExtractor` decode claims
      without checking signatures, and fall back from `tenantId` to `sub`. Verify issuer, signature, audience and
      expiry; map to a stable internal user id; resolve tenant from trusted membership; remove the `sub` fallback.
- [ ] **3.2** [olo] Session binding and revocation
- [ ] **3.3** [olo → olo-mono] Carry the verified identity context into the run as server-bound data, never as client
      fields. The kernel refuses user-scoped calls when the context is absent or expired.
- [ ] **3.4** [ToolGate] Qualify, or implement, a direct-human runtime credential path for OLO: short-lived,
      audience-restricted and tenant-bound, with tests
- [ ] **3.5** [ToolGate] Resolve the user's and team's current permissions independently of OLO workflow agents
- [ ] **3.6** [ToolGate] Object-level authorization for conversations, memory namespaces, knowledge bases and documents
      that reaches execution, beyond the fixed `tool://<tool-id>` resource
- [ ] **3.7** [ToolGate] Separate operation permissions as defined in M2.3
- [ ] **3.8** [olo-mono] **M1-R2:** server-authorized workload mappings for path B. A workflow's `agentName` must be in
      the allowlist for that tenant and workflow, or the call fails.
- [ ] **3.9** [olo-mono, ToolGate] **M1-R3:** delegated mode for path B, with the user as subject and the agent as actor.
      Deny when the agent may use a tool but the user may not.
- [ ] **3.10** [olo] The same checks on every alternate endpoint: uploads, document downloads, knowledge-source
      listings, session APIs, exports and result retrieval

**Acceptance.** Requests without a verified identity are rejected. A known resource id with the wrong tenant or user is
denied by ToolGate. A workflow can't pick an agent identity it isn't mapped to.

## M4 — Trusted SDK and executor context (weight 8)

- [ ] **4.1** [ToolGate] Complete principal contract in the SDK `Invocation`: tenant, user, session, resource, operation
      and correlation, required for user-scoped tools (including a tenant accessor)
- [ ] **4.2** [ToolGate] Propagate it from Gateway → Control → executor. Permits bind identity, operation, resource,
      exact arguments and expiry.
- [ ] **4.3** [ToolGate] User-scoped tools refuse to run without a trusted identity. This includes standalone runtimes,
      where `userId()` is empty today.
- [ ] **4.4** [ToolGate] Audit records: subject, workload, resource, operation, decision, policy version, invocation
      correlation and outcome
- [ ] **4.5** [ToolGate] Qualification tests proving identity survives Gateway → Control → executor → storage

**Acceptance.** A test package sees exactly the verified principal and resource. A forged argument can't change it, and
a missing identity is refused.

## M5 — Durable invocation, retries and approvals (weight 12)

Temporal can run a failed activity again, so every effect has to be safe to replay.

- [ ] **5.1** [olo-mono] **M1-R1:** split the agent turn. One activity gets the model's decision and records each selected
      operation, with a stable logical-operation id, in workflow state. A separate activity dispatches each recorded
      operation. On recovery, recorded operations are resumed, never regenerated.
- [ ] **5.2** [ToolGate] Dedup is bound to the authenticated subject and the request. Reusing an id with changed
      arguments is a conflict, not a silent replay.
- [ ] **5.3** [olo-mono, ToolGate] **M1-R5:** transport dedup is separate from executor and business idempotency. A
      timeout after an external effect triggers reconciliation (query the invocation status) before any retry.
- [ ] **5.4** [olo-mono, ToolGate] **M1-R4:** approvals resume the original invocation. The run waits on an
      authenticated notification or bounded polling, then retrieves the result. Covers expiry, denial, cancellation,
      and re-checking revocation before execution.
- [ ] **5.5** [olo-mono] Bounded retries, timeouts and cancellation propagated to ToolGate
- [ ] **5.6** Tests: a crash after dispatch but before recording; an activity retry; approve, deny and expire; a
      grant revoked while waiting

**Acceptance.** Crash and retry tests show that no completed side effect repeats, and that an approval resumes the
original operation.

## M6 — Memory and RAG services and ToolGate packages (weight 12)

ToolGate governs access; it is not the storage engine. Memory and RAG are separate services exposed through
ToolGate packages. The existing demo code is not copied unchanged:
- `ConversationPluginSupport` is file storage keyed by session.
- `RagVectorStoreSupport` filters Qdrant only by `capabilitySource`.

- [ ] **6.1** Conversation history service:
      - Tenant, owner and membership on every record, with explicit sharing
      - Transactional, idempotent append keyed by a stable turn id, plus concurrency control
      - Defined ordering for simultaneous turns
      - Versioned summaries and context budgets
      - Deletion that also removes derived summaries and embeddings
- [ ] **6.2** Working-state store with bounded retention
- [ ] **6.3** Long-term user memory as a separate store, with provenance, correction, deletion and retention (scope: D6)
- [ ] **6.4** RAG service:
      - Chunk metadata as in *RAG controls*, and mandatory authorized filters on query/fetch/update/delete
      - A release-time re-check
      - ACL propagation and cache invalidation; complete deletion
      - Citations
      - A versioned, real embedding model and a reindex process
- [ ] **6.5** Ingestion controls: file size and type limits, parser hardening, restricted outbound access.
      Retrieved text is marked as untrusted data.
- [ ] **6.6** ToolGate packages `olo.memory` and `olo.rag`, built with the Java Tool SDK on the M4 principal contract.
      Database credentials live only in their executors.
- [ ] **6.7** Service tests: isolation (two users, two tenants), ACL change, deletion, concurrent appends

**Acceptance.** The services refuse any request outside the authorized scope, even when called with a valid resource id.

## M7 — OLO conversation and document lifecycles (weight 10)

- [ ] **7.1** [olo-mono] Governed-call abstraction for deterministic path A and path C operations. It is neither an
      AGENT node nor model-selected, and it runs on M5's durable dispatch.
- [ ] **7.2** [olo-mono] Conversation lifecycle, replacing per-agent memory hooks:
      1. Authorize the conversation.
      2. Load permitted history once per incoming user turn.
      3. Pass the relevant context to the workflow's agents.
      4. Append the user turn and the final response with stable turn ids.

      Child agents and retries never write history.
- [ ] **7.3** [olo-mono] Document lifecycle: the index and delete pipelines become governed calls under the user, or
      under an explicit service principal for scheduled indexing
- [ ] **7.4** [olo-mono] RAG retrieval as a governed call. Results reach agents as untrusted, cited context.
- [ ] **7.5** [olo] Backend endpoints (chat, uploads, knowledge sources, sessions, exports) go through the same identity
      and authorization path

**Acceptance.** A chat turn loads history once and appends exactly two records. Changing the workflow's agent doesn't
change what history or documents the user can reach.

## M8 — Isolation, recovery and real-stack tests (weight 8)

- [ ] **8.1** Mandatory acceptance cases, automated against the real stack:
      1. Two users in one tenant cannot access each other's private resources.
      2. Cross-tenant access fails, even with a known resource id.
      3. Changing the workflow agent does not change memory or RAG entitlement.
      4. Forged identity arguments and substituted resource ids fail.
      5. Revoked grants block execution, cached content access and result retrieval.
      6. Concurrent turns and activity retries create no duplicate memory records.
      7. Crash recovery does not repeat completed side effects.
      8. An approval resumes the original operation.
      9. Document ACL changes and deletion affect retrieval immediately, within a defined bound.
- [ ] **8.2** Debug stack `debug.ps1 -ToolGate`: start or attach ToolGate and the memory/RAG services, add readiness
      probes, provision identities (old 6.1)
- [ ] **8.3** Multi-agent `incident-recovery` pipeline on ToolGate's `server-recovery` mocks (old M5), on path B with
      the user as subject and each agent as actor:

      | OLO agent (`agentName`, server-mapped) | Agent capability ceiling | Role |
      |---|---|---|
      | `incident-commander` (orchestrator) | none | Delegates and writes the summary |
      | `triage-agent` | `incidents.incidents`, `incidents.host-status`, `incidents.crash-report` | Finds the incident and its root cause |
      | `remediation-agent` | `incidents.runbook`, `incidents.remediate` (**ASK**), `incidents.verify` | Rolls back after approval, then verifies |
      | `incident-report-agent` | `incidents.update-incident` | Records the outcome and resolves INC-5001 |

      Tests: triage never sees `remediate`; a user without `remediate` is denied even through `remediation-agent`;
      after approval the original `remediate` invocation resumes (M5.4).
- [ ] **8.4** Evidence kept: run transcripts, the ToolGate audit log, the test reports

**Acceptance.** All nine cases pass on the real stack. The incident-recovery run completes after approval, and each
call is audited with the right user, actor, resource and decision.

## M9 — Remove old tool code; coordinate backend and UI (weight 10)

No deprecations, shims or compatibility readers. A workflow that still contains `tools` or `TOOL` fails validation.

- [ ] **9.1** Separate agent delegation from tool calls (old M2):
      - Rename `kernel/toolcall` to `kernel/agentcall`, and `ToolCallExpansionExecutionStrategy` to
        `AgentCallExpansionExecutionStrategy`
      - The planner schema keeps only `agentCalls`
- [ ] **9.2** `olo-spi`, `olo-annotation`, `olo-annotation-processor`: delete the tool SPI (`Tool`, `ToolRequest`,
      `ToolResult`, `ToolStatus`, `@ToolId`, `ToolProvider`), `@OloTool`, `ToolDescriptor`, `CatalogToolCollector`,
      the tool validator branches, and `tools.json`
- [ ] **9.3** `olo-core`: delete the `tools` module, its demo data and the tool human-input plugins, plus
      `ToolRegistry` and the tool branch in `ExecutionEngine`
- [ ] **9.4** `olo-definition` and `olo-kernel`:
      - Delete `ToolDefinition`, `ToolCallPlannerSupport`, `NodeType.TOOL`, `WorkflowDefinition.tools` and
        `capability.tool_requirements`
      - Remove the tool validator rules, `ToolNodeTypeHandler`, `ToolChainMerger`, `AvailableToolsJsonResolver`,
        `ToolLabelResolver`, `ParsedToolCall` and `ToolCallRetryVariables`
- [ ] **9.5** Regenerate all workflow JSON. This removes `tools[]`, TOOL nodes and `{availableToolsJson}`. It adds the
      AGENT Studio property `agentName`, limited to server-mapped identities (M3.8). Rewrite the planner end-to-end
      tests against the fake Gateway.
- [ ] **9.6** olo-docker: remove the `OLO_SHARED_SOURCE` and tools demo-data mounts and `tools.json`; add ToolGate
      and memory/RAG service configuration
- [ ] **9.7** olo, olo-ui, olo-chat: remove TOOL nodes from the Studio palette and the catalog readers. Each is a PR in
      its own repo, landed in the same window as 9.2–9.5.
- [ ] **9.8** Guard: `grep -riE "OloTool|ToolRequest|NodeType.TOOL|availableToolsJson|toolCalls"` finds nothing, and
      every module builds and tests green

**Acceptance.** No tool type, annotation, node, schema or JSON field is left in olo-mono, and every repo is green.

## M10 — Deployment and operational qualification (weight 5)

- [ ] **10.1** Encrypted transport everywhere, and encryption at rest for memory, RAG and ToolGate state
- [ ] **10.2** Credential rotation for workload, user-bound and database credentials, with tested rollover
- [ ] **10.3** Redacted diagnostics: no tokens, prompts or retrieved content in logs or errors
- [ ] **10.4** Bounded retries, timeouts, cancellation, and per-user quotas
- [ ] **10.5** **M1-R6:** protect sensitive Temporal payloads with a payload codec (encryption) or references (D7),
      because prompts and outputs can enter history
- [ ] **10.6** Backups, restore tests, and retention policies for conversations, working state, long-term memory and documents
- [ ] **10.7** Docs: architecture, the identity and permission model, the grant guide (ALLOW for `memory.*`, so chat
      never stalls on approval), operations runbook, and the incident-recovery walkthrough (old 6.4)

**Acceptance.** A deployment checklist passes on a staging environment, including a restore test and a credential rotation.

---

## Decisions

| # | Question | Decision or recommendation | Status |
|---|---|---|---|
| D1 | Where do conversation memory and RAG go? | **Revised.** ToolGate *governs* them, as user-authorized platform capabilities. Separate memory and RAG services *store* the data and enforce scope. OLO orchestrates them deterministically (path A). They are not agent-selected tools, and agent identities don't authorize them. | Decided 2026-10-23, revised after review |
| D2 | What happens after an ASK approval? | **Revised.** Resume the original invocation (M5.4), with expiry, denial, cancellation and a revocation re-check. M1's "run it again" is temporary and is removed in M5. | Decided; implementation in M5 |
| D3 | Is end-user identity required? | **Moved into required scope.** Memory and RAG are authorized for the signed-in user. Agent-selected tools are authorized for user plus delegated actor. Scheduled jobs run as an explicit service principal. | Decided |
| D4 | How does OLO obtain user-bound, audience-restricted ToolGate credentials? | Token exchange if ToolGate supports it, otherwise an authenticated server binding. Never token passthrough. | Open, decided in M2.5 |
| D5 | Where do the memory and RAG services live and run? | A separate repo or service, deployed next to ToolGate's executors. Not the OLO worker. | Open, M2 |
| D6 | Is long-term user memory in the first release? | The design covers it. Building it can follow conversation history and RAG. | Open, M2 |
| D7 | How are sensitive Temporal payloads protected? | An encryption payload codec, or keeping only references in history | Open, M10.5 |

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| ToolGate's identity and resource features are a target design, not shipped | M3–M6 depend on ToolGate work of unknown size | M2.1's evidence-based gap matrix comes first. No memory or RAG code is written before M4 qualifies. |
| OLO's current JWT handling is demo code (no signature check, `sub` fallback) | Any identity-based control would be bypassable | M3.1 is a hard prerequisite for path A |
| Tool reach depends on agents chosen by workflow authors | Privilege escalation through `agentName` | Server-authorized mappings (M3.8), plus a user-plus-actor check (M3.9) |
| A retry or crash repeats a side effect | Double remediation, duplicate memory records | Durable recorded operations (M5.1), reconciliation (M5.3), recovery tests (M8.1 cases 6–7) |
| Memory and RAG depend on ToolGate and the services being up | No chat history or retrieval during an outage | Fail closed with a clear error, readiness probes (M8.2), timeouts and quotas (M10.4) |
| Retrieved documents carry prompt injection | The model is steered by data | Retrieved text is marked untrusted, the model can't touch the filters, citations (M6.4, M7.4) |
| Sensitive data in Temporal history and logs | Data exposure | Payload protection (M10.5), redaction (M10.3) |
| `llama3.2` (3B) is unreliable on long tool chains | Flaky M8.3 demo | 1–3 tools per agent, and a larger model through `-Model` |
| olo, olo-ui and olo-chat break while TOOL nodes still exist there | Stack regressions | Land M9.7 in the same window as M9.2–M9.5; pin images in `debug.ps1` until then |

## References

- ToolGate: [enterprise access-control target design](../../git/olo-toolgate/docs/security/enterprise-access-control.md),
  [Gateway API](../../git/olo-toolgate/docs/api/gateway-api.md), [threat model](../../git/olo-toolgate/docs/security/threat-model.md)
- [RFC 8693, OAuth 2.0 Token Exchange](https://www.rfc-editor.org/rfc/rfc8693) (subject and actor separation)
- [RFC 9700, OAuth 2.0 Security Best Current Practice](https://www.rfc-editor.org/rfc/rfc9700)
- MCP specification: authorization, and security best practices (audience validation, no token passthrough)
- OWASP guidance on RAG and LLM application security
- Temporal documentation: activity retries, and data encryption with payload codecs
