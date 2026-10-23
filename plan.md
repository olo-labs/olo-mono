# Plan: ToolGate-governed tools for OLO agents

**Goal.** Every OLO agent gets its tools from [OLO ToolGate](../../git/olo-toolgate) instead of from Java code in
olo-mono. An agent asks ToolGate which tools it may use, and the LLM can call only those, through ToolGate.
All tool code, annotations, call schemas and tool nodes are removed from olo-mono, with no backward compatibility.
A new multi-agent pipeline proves the design end to end against ToolGate's mocked tools.

**Status (2026-10-23): 31% of target work done.** M0 and M1 are complete on branch `feature/toolgate-integration`.
D1 is decided (conversation memory and RAG move into ToolGate). Next up: M2.

## Progress

| # | Milestone | Weight | Done | Status |
|---|---|---:|---:|---|
| M0 | Discovery and plan | 5 | 100% | ✅ Done |
| M1 | ToolGate client and agent tool loop | 25 | 100% | ✅ Done (fake Gateway; real ToolGate run is M5) |
| M2 | Separate agent delegation from tool calls | 10 | 0% | ⬜ Not started |
| M3 | Remove all tool code from olo-mono | 25 | 0% | ⬜ Not started |
| M4 | Move conversation memory and RAG into ToolGate tool packages | 10 | 14% | 🟨 D1 decided (1 of 7 steps) |
| M5 | Multi-agent `incident-recovery` pipeline on ToolGate mocks | 15 | 0% | ⬜ Not started |
| M6 | Debug stack, docs and cross-repo follow-ups | 10 | 0% | ⬜ Not started |
| | **Overall** | **100** | **31%** | |

**How the percentages work.** A milestone's weight is its share of the total effort. Inside a milestone, every
step has the same share. Overall % = Σ (weight × milestone done %) / 100. Tick a step's box when its PR is merged
and its acceptance check passes, then update the table above.

Planned order: M1 → M2 → M4 → M3 → M5 → M6. M3 deletes the old path, so the new one (M1, M2, M4) must be in
place first. That way the build and the tests stay green on every PR.

---

## Target design

```
AGENT node  (agentName = "triage-agent"; defaults to the agent's id, user-editable)
  all of this runs inside the node's traversal-step Temporal activity
  ├─ ToolGate tools/list                     bearer token resolved for agentName
  └─ loop up to maxToolRounds:
       ├─ LLM chat with native tool calling   tool schemas = MCP inputSchema from the catalog
       ├─ ToolGate tools/call                 same identity, deterministic JSON-RPC id per call
       └─ tool result appended to the chat; stops when the model returns a final answer
```

**ToolGate facts this design relies on**
- Identity comes **only** from the runtime bearer token. The Gateway ignores identity in the request body or
  forwarded headers.
- Each ToolGate agent has a *workload binding* that issues its token. The groups and grants on that agent decide
  what `tools/list` returns.
- `POST /mcp` (MCP 2026-07-28) supports `tools/list` and `tools/call`.
- A tool outside the agent's catalog returns `403`. A tool under an ASK policy returns `PENDING_APPROVAL` and does
  not run until someone approves it.
- If Control is unavailable, ToolGate fails closed, and cached metadata never grants execution.

**OLO-side rules**
- **Agent identity.** Every AGENT node has `configuration.agentName`. It defaults to the agent's id (for example
  `log-failure-agent`), and users can change it, so the same workflow can run under different identities with
  different tool sets.
- **Credentials.** A `ToolGateCredentialResolver` maps `agentName` to a bearer token. The first implementation
  reads `<credentialsDir>/<agentName>.token` or the env var `OLO_TOOLGATE_TOKEN_<AGENT_NAME>`. Tokens never
  appear in workflow JSON, logs or Temporal history.
- **Temporal safety.** Each traversal step already runs as one Temporal activity, so `tools/list` and
  `tools/call` never run in workflow code. On `/mcp` the JSON-RPC `id` is ToolGate's idempotency key: a retry
  with the same `id` and caller maps to the same invocation, and a finished result is returned, not re-run.
  The kernel sends `id = olo-<sha256(runId, workflow/node, pass, round, index, tool, canonical arguments)>`,
  so an activity retry repeats the same ids, while a later pass of the node or different arguments is a new
  invocation.
- **No cross-run caching.** The catalog is fetched fresh for every agent run.
- **Fail closed.** If ToolGate is unreachable or the token is missing, the agent run fails with a clear error. It
  never carries on without tools.

---

## M0 — Discovery and plan (weight 5) ✅

- [x] Survey the ToolGate Gateway API, MCP surface, agent/workload-binding model and mocked tools
- [x] Inventory tool code across olo-mono (8 modules) and the workflow JSON (67 files)
- [x] Confirm that agent delegation (`agentCalls`) shares the planner schema with `toolCalls`
- [x] Write this plan

## M1 — ToolGate client and agent tool loop (weight 25)

- [x] **1.1** New module `olo-toolgate-client` (`McpToolGateClient`): an MCP 2026-07-28 client for `tools/list` and
      `tools/call`, with mirrored `MCP-Protocol-Version`/`Mcp-Method`/`Mcp-Name` headers, JSON or SSE replies, TLS
      with ToolGate's CA, timeouts, and error mapping to typed `ToolGateException` kinds
- [x] **1.2** `ToolGateCredentialResolver` plus `FileAndEnvCredentialResolver`: `OLO_TOOLGATE_TOKEN_<AGENT_NAME>`
      first, then `<credentialsDir>/<agentName>.token`, re-read on every call so rotation applies at once. Agent
      names are restricted to `[A-Za-z0-9._-]`, so a name can't escape the credentials folder.
- [x] **1.3** Worker config section `toolgate: { enabled, mcpUrl, caCertificate, credentialsDir, timeoutMs,
      maxToolRounds }`, validated at load, with relative paths resolved against the config folder. Bootstrap step
      4/5 (ToolGate) installs access into the kernel and probes the Gateway; the worker won't start if ToolGate is
      enabled but unreachable. Disabled sections were added to the docker and local-debug sample configs.
- [x] **1.4** `agentName` and `maxToolRounds` on AGENT node `configuration` (`AgentToolSettings`), validated at run
      time. `agentName` defaults to the workflow id for the primary agent (node `agent`, or the only AGENT node),
      otherwise the node id. *The Studio property moved to M3.6, because adding it regenerates every workflow JSON.*
- [x] **1.5** `LlmClient.chat(messages, tools)` with native tool calling in `OllamaLlmClient`: sends `tools`, parses
      `message.tool_calls`, and accepts stringified-JSON arguments
- [x] **1.6** `AgentToolLoop`, called from `DefaultLlmInvocationService` for non-planner LLM agents: list, then chat,
      then call, with model-safe tool names (`incidents.host-status` → `incidents_host-status`), deterministic
      JSON-RPC ids, results truncated to 8,000 characters, and a round limit followed by one final answer without
      tools. An empty catalog runs the plain prompt exactly as before. Node output records `agentName`,
      `toolCatalog`, `toolInvocations` (tool, request id, status, invocation id; no arguments or results) and
      `agentOutcome`.
- [x] **1.7** `PENDING_APPROVAL` ends the turn with outcome `WAITING_APPROVAL` and a notice naming the tool and the
      invocation id (D2, option a). A tool outside the catalog, or one that fails in its executor, is shown to the
      model as `ERROR: ...` instead of failing the run.
- [x] **1.8** Tests, all passing:
      - `olo-toolgate-client`: 18 (fake Gateway on the JDK HTTP server; exact frame and headers, per-agent tokens,
        allowed/403/ASK/executor error/401/503/SSE/unreachable, token rotation, path-safe names)
      - `olo-kernel`: 27 (`AgentToolLoopTest`, `AgentToolSettingsTest`, `ToolGateAgentExecutionTest`,
        `OllamaToolCallParsingTest`)
      - `olo-worker-configuration`: 4 validator tests
      - `olo-worker`: 4 (`WorkerToolGateSetupTest`)

**Acceptance.** ✅ An AGENT node with `agentName=X` sees exactly X's catalog and calls tools through the Gateway
client under X's token (`ToolGateAgentExecutionTest`). The fake-Gateway tests pass. A live ToolGate run is part of M5.

**Known unrelated failures.** 7 existing `olo-kernel` tests fail the same way on a clean `origin/main`. Five need
`olo-workflow-input/samples/minimal-local/workflow-input.json`, which `.gitignore` (`*/samples/*/*.json`) keeps out
of the repo. Two read conversation history that earlier tests leave in `olo-core/tools/demo-data/conversations`.
M3 deletes that tool data; the missing sample needs its own fix.

## M2 — Separate agent delegation from tool calls (weight 10)

- [ ] **2.1** Rename `org.olo.kernel.toolcall` to `org.olo.kernel.agentcall`, keeping only the `agentCalls` parsing
      and merging
- [ ] **2.2** Rename `ToolCallExpansionExecutionStrategy` to `AgentCallExpansionExecutionStrategy` (agent subgraphs only)
- [ ] **2.3** Planner JSON schema: only `agentCalls`. Strip `toolCalls` from the validator, the prompts and the retry
      variables.
- [ ] **2.4** Update the orchestrator tests (log-RCA and research) to use delegation only

**Acceptance.** Orchestrators still delegate to child agents, and the child agents use M1 tools.

## M3 — Remove all tool code from olo-mono (weight 25)

No deprecations, shims or compatibility readers. A workflow that still contains `tools` or `TOOL` fails validation.

- [ ] **3.1** `olo-spi`: delete `spi/tool/*` (`Tool`, `ToolRequest`, `ToolResult`, `ToolStatus`), `@ToolId` and
      `ToolProvider`. Also delete `@ImplementationId` if nothing else uses it.
- [ ] **3.2** `olo-annotation` and `olo-annotation-processor`: delete `@OloTool`, `ToolDescriptor` and
      `CatalogToolCollector`, the tool branches in `CatalogSpiValidator` and `ExtensionCatalogValidator`, and the
      `tools.json` catalog output
- [ ] **3.3** `olo-core`: delete the whole `tools` module, including its `demo-data` and the tool human-input
      plugins. Also delete `runtime/ToolRegistry`, the tool branch of `ExecutionEngine`, and tool entries in
      `CoreExtensionCatalog`.
- [ ] **3.4** `olo-definition`: delete `ToolDefinition`, `ToolCallPlannerSupport`, `NodeType.TOOL`,
      `WorkflowDefinition.tools` (field, builders, `WorkflowDefinitionFields`), and `capability.tool_requirements`.
      Remove the tool rules from `WorkflowRegistryValidator`, `WorkflowRuntimeValidator`,
      `WorkflowCapabilityValidator` and `PlannerCatalog`, and the TOOL entries in `StudioDesignerDefaults`.
- [ ] **3.5** `olo-kernel`: delete `ToolNodeTypeHandler`, `ToolChainMerger`, `AvailableToolsJsonResolver`,
      `ToolLabelResolver`, `ParsedToolCall`, `ToolCallRetryVariables`, and the tool half of the subgraph merger
      and the dynamic-graph labels
- [ ] **3.6** Workflow JSON: regenerate all 67 files from the `*Definitions` / `*RegenerationTest` sources. This
      removes `tools[]`, TOOL nodes and their edges, `{availableToolsJson}`, and the TOOL designer node types.
      It also adds the AGENT Studio property `agentName` (moved here from M1.4, since it changes every file).
- [ ] **3.7** Delete the tool tests, and rewrite the travel and research end-to-end tests against the fake Gateway
- [ ] **3.8** Guard: `grep -riE "OloTool|ToolRequest|NodeType.TOOL|availableToolsJson|toolCalls"` finds nothing,
      and `publish-libs` plus every module's tests pass

**Acceptance.** No tool type, annotation, node, schema or JSON field is left in olo-mono, and the build is green.

## M4 — Move conversation memory and RAG into ToolGate tool packages (weight 10)

`conversation-load` and `conversation-store` are in every workflow, and the documents index/delete pipelines use
RAG ingest/query/delete. Per D1 they become ToolGate tools, built with the ToolGate Java Tool SDK in the
olo-toolgate repo (none of it lives in olo-mono), and are granted to agents like any other tool.

- [x] **4.0** Settle decision D1: ToolGate tools
- [ ] **4.1** ToolGate package `olo.memory` with `memory.load` and `memory.store`. Turns are persisted through
      declared `@HttpRoute`s (`context.http()`) to the shared store, keyed by `conversationId`.
- [ ] **4.2** ToolGate package `olo.rag` with `rag.ingest`, `rag.query` and `rag.delete`, calling qdrant and the
      Ollama embedding endpoint through declared routes
- [ ] **4.3** Run context in olo-mono: the agent loop fills tool arguments that the run already knows, such as
      `conversationId` and `tenantId`, and hides them from the model's schema. The model can't omit them or forge them.
- [ ] **4.4** Deterministic memory: an AGENT node's `memory: { load, store }` setting makes the executor call
      `memory.load` before the first LLM turn and `memory.store` after the final answer, through ToolGate under the
      agent's own identity. Chat continuity then doesn't depend on the model choosing to call memory tools.
- [ ] **4.5** Document pipelines (documents-index, documents-delete) become an AGENT with `rag.*` granted, or use the
      same deterministic pre/post call, chosen when the pipeline is migrated
- [ ] **4.6** Tests: conversation round-trip and index/query/delete through ToolGate against qdrant

**Acceptance.** Chat continuity and the document pipelines work through ToolGate with no TOOL nodes. An agent
whose identity has no `memory.*` grant gets no conversation history.

## M5 — Multi-agent `incident-recovery` pipeline on ToolGate mocks (weight 15)

This uses ToolGate's `examples/mocked-tools/server-recovery` package. It returns fixed data, and its scenario is
that payments-api on app-02 was killed by the OOM killer after release 2.14.0 (INC-5001).

| OLO agent (`agentName`) | ToolGate grant | Role |
|---|---|---|
| `incident-commander` (orchestrator) | none | Delegates to the agents below and writes the summary |
| `triage-agent` | `incidents.incidents`, `incidents.host-status`, `incidents.crash-report` | Finds the incident and its root cause |
| `remediation-agent` | `incidents.runbook`, `incidents.remediate` (**ASK**), `incidents.verify` | Follows the runbook, rolls back after approval, verifies |
| `incident-report-agent` | `incidents.update-incident` | Records the outcome and resolves INC-5001 |

- [ ] **5.1** `debug/toolgate-setup.ps1`, which:
      - builds `server-recovery.tgpkg` and deploys it to Tool Host, so no device is needed;
      - creates the four ToolGate agents, a tool group per agent, agent groups, grants, and the ASK policy on
        `remediate`;
      - issues the workload tokens into the worker's `credentialsDir`.
- [ ] **5.2** Workflow definitions for `incident-recovery-orchestrator` and its three child agents, generated from
      `*Definitions` like the existing scenarios, plus a README
- [ ] **5.3** Identity test: `triage-agent` never sees `incidents.remediate`, and running the triage workflow with
      `agentName=incident-report-agent` returns a different catalog
- [ ] **5.4** ASK test: remediation stops at `PENDING_APPROVAL` until the call is approved in the ToolGate console
- [ ] **5.5** End-to-end run on the debug stack with `llama3.2:latest`, keeping the transcript and the ToolGate audit
      log as evidence

**Acceptance.** One chat request triggers triage, remediation (after approval) and the report. Every tool call
appears in ToolGate's audit log under the correct agent identity.

## M6 — Debug stack, docs and cross-repo follow-ups (weight 10)

- [ ] **6.1** `debug.ps1 -ToolGate`: start or attach the ToolGate quickstart, join it to `olo-net`, add readiness
      probes, and mount the worker credentials
- [ ] **6.2** olo-docker: remove the `OLO_SHARED_SOURCE` / tools demo-data mounts and `tools.json`, and add the
      ToolGate env/config
- [ ] **6.3** olo (backend), olo-ui and olo-chat: remove TOOL nodes from the Studio palette and the catalog
      readers. Each is a separate PR in its own repo.
- [ ] **6.4** Docs: an architecture page for ToolGate tools, the `agentName` reference, a guide to granting tools
      to an agent, and the `incident-recovery` walkthrough

**Acceptance.** A fresh checkout runs `debug.ps1 up -ToolGate`, and then the incident-recovery demo works.

---

## Decisions

| # | Question | Options | Recommendation | Status |
|---|---|---|---|---|
| D1 | Where do conversation memory and RAG go once tools are removed? | (a) platform node types in `olo-core/nodes`; (b) ToolGate tools | **(b) ToolGate tools** (M4) | **Decided 2026-10-23** |
| D2 | How does a run resume after an ASK approval? | (a) end the run at `WAITING_APPROVAL`, and the user re-runs; (b) wait on a Temporal signal fed by a ToolGate notification or by polling | (a) in v1, then (b) once ToolGate's API for reading the outcome of an approved invocation is confirmed | Open |
| D3 | Do different *end users* get different tools? | (a) per `agentName`, with a static service token per agent; (b) delegated/OIDC tokens on behalf of the signed-in user | (a) now, (b) as a later phase | Open |

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| `llama3.2` (3B) is unreliable on long tool chains | Flaky M5 demo | Keep 1–3 tools per agent, and allow a larger model through `-Model` |
| The olo, olo-ui and olo-chat images break while TOOL nodes still exist there | Debug stack regressions | Land M6.3 PRs in the same window as M3, and pin images in `debug.ps1` until then |
| ToolGate is a development release (Tool Host sandboxing is unfinished) | Not production-safe | Use it for evaluation and demos only. Mock tools return fixed data. |
| A Temporal retry duplicates a side-effecting tool call | A double remediation | Send a deterministic `Idempotency-Key`. The mock `remediate` also runs once per `reference`. |
| Memory and RAG now depend on ToolGate being up (D1) | ToolGate down means no chat history and no document pipelines | Fail closed with a clear error, and add ToolGate readiness to the debug stack's required-container check (M6.1) |
| An ASK policy on `memory.*` would stall every chat turn | Chat hangs on approval | Recommend ALLOW policies for `memory.*`, and document this in the grant guide (M6.4) |
