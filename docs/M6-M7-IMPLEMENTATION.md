# M6/M7 implementation and fresh-install contract

Implemented together across `olo-mono`, `olo`, `olo-toolgate`, and the separate `olo-knowledge` source tree.
`plan.md` is unchanged. This record describes implementation and evidence, not milestone sign-off.

## Ownership and authorization

- `olo-knowledge/olo-memory` owns conversation history, extractive summaries and expiring working state.
- `olo-knowledge/olo-rag` owns document source/ACL/version publication and vector cleanup.
- Each is a separate Java process with its own PostgreSQL credentials, TLS identity and deployment.
- Every operation traverses Gateway → Tool Host package → permit-bound mTLS service. Neither OLO process has store credentials.
- User platform calls use HUMAN authority without an agent actor or catalog lookup. Model-selected business tools retain delegated user-plus-agent authorization.
- Scheduled `OloDocumentWorkflow` operations use only `olo-platform-service`, `rag.update`, and resource-scoped ToolGate grants. The encrypted workflow tenant, operation tenant and authenticated Gateway SERVICE tenant must agree.
- Gateway `olo/identity` supplies authenticated binding evidence using the same opaque credential used for dispatch. It grants no tool permission and is never cached.
- Both Control issuance and downstream verification reject delegated memory/RAG authority; SERVICE is restricted to `rag.update`.

## Fresh workflow and API

Use `olo-definition/olo-configuration/governed/governed-chat.json` and the sample
`olo-worker-configuration/samples/worker-config-governed.json`. Provision the existing M5 encrypted Temporal data converter and signing/CA material before starting workers.

The backend exposes `/api/v1/conversations/{uuid}` for create/delete, `/turns` for accepted turns,
`/messages`, `/turns/{turnId}`, `/turns/{turnId}/cancel`, `/share`, `/export`, and
`/receipts/{invocationId}`. Document operations are POST `/api/v1/knowledge/{kbUuid}/{query|ingest|update|delete|share}`.
Mutations require a UUID `Idempotency-Key`. Use the same key and body to resume an approval or recover a lost response.
Query via `rag.query` with `type=receipt` reconciles a document write.

Accepted user messages are recorded before scheduling, and the root lifecycle repeats the same stable append.
History excludes that message, so the current input appears once. Children inherit context and dependencies but never write history.
Every model attempt gets a recorded fresh source check; queue delays expire the proof. Final response persistence precedes final release,
with another source check after persistence. Pending, failure and cancellation markers share the turn's stable identity.
A failed marker cannot prevent cancellation of recorded tool invocations. Backend cancellation can rebuild its run binding from governed history after restart.

Old session/upload/RAG/run-query and WebSocket controllers are unregistered; retired paths return 410 where fenced.
Authenticated worker event ingestion remains, but stores only status metadata. It never writes a second conversation history or stores model response text.
There are no migration readers, compatibility switches or support for old executions. The old UI needs the M9 fresh-API update.

## Scheduled reindex

Provision a Temporal schedule targeting `OloDocumentWorkflow` on the configured worker queue. Its workflow ID must
use `TenantWorkflowIds.run(tenantId, uniqueId)` so M5 encryption resolves the correct tenant key.
Input is `DocumentOperation(tenantId, null, "olo-platform-service", stableOperationId, "rag.update", kbUuid,
{type:"reindex",docId:docUuid})`. No bearer credential or source content is stored in the schedule.
Each scheduled execution incorporates the Temporal run ID into its logical operation; activity retries retain that identity.
Grant the SERVICE subject the exact KB resource in ToolGate and the matching service membership in the RAG database.
No user fallback exists. A wrong tenant, delegated identity, ambiguous user/service identity, or other operation fails closed.

## Storage and reconciliation

Apply only fresh SQL from `olo-knowledge/deploy/`, using separate non-owner, non-superuser application roles with FORCE RLS.
Memory appends serialize per conversation; final outcome is unique per turn. Working-state size/version/TTL and history budgets are bounded.
Summaries track transitive dependencies and are invalidated on changes. Revoked derived context fails the turn closed.
RAG parses bounded plain text/Markdown only, pins the real embedding artifact digest, stores no source text in Qdrant,
filters authorized versions, and rechecks SQL visibility before release. Tombstones cannot be reversed. A durable outbox and orphan sweep remove old/unpublished vectors.

Writes consume single-use permits, check the commit deadline, and record scoped receipts in the same transaction.
The Tool Host preserves OUTCOME_UNKNOWN after a possibly dispatched service write or lost commit acknowledgement.
Use the actual ToolGate invocation ID in the receipt lookup. COMMITTED is affirmative evidence; NOT_FOUND is not proof of no effect.
Do not automatically issue a new write after OUTCOME_UNKNOWN.

## Verification and remaining gates

Recorded local checks cover real PostgreSQL RLS and concurrency, Qdrant/Ollama retrieval and deletion,
separate memory/RAG JVMs with mTLS and revocation, ToolGate Control bridge/TLS, Gateway/Tool Host,
client binding, Temporal retry/approval recovery, worker wiring, and backend authentication/retired routes.
The HTTPS application tests simulate Control; the ToolGate bridge tests separately exercise Control.
A single deployed Gateway → broker → application chain, CNI negative-access checks, production recovery drills,
and the accepted RAG quality/freshness/latency/load targets remain M8/M10 qualification work.
No benchmark sign-off or production deployment is claimed. See `olo-knowledge/README.md` for exact environment,
empty-database setup, model provisioning, package builds and the disposable integration-test command.

## Local verification record — 2026-10-24

| Suite | Result |
|---|---:|
| Kernel governed lifecycle/tool/recovery selection | 64 passed |
| ToolGate Java client | 25 passed |
| Worker | 13 passed |
| Backend, including disposable Redis identity store | 81 passed |
| Memory service, PostgreSQL and separate-JVM mTLS | 10 passed |
| RAG service, PostgreSQL, real vectors/embeddings and separate-JVM mTLS | 11 passed |
| Downstream permit verifier | 7 passed |
| Control service-permit bridge/TLS selection | 8 passed |
| Gateway and Tool Host Rust suites | 81 passed, plus the new identity-router case passed separately |

Both SDK package descriptor checks and archives passed the current ToolGate CLI validation.
The native Windows package task cannot find a native CLI on this machine; its staged directories were packed
successfully using the current locally built Linux CLI in an owned Docker invocation. Archives remain under ignored build directories.
The kernel result is the named affected test selection, not the entire legacy kernel suite.
No external repository was created or code deployed by this implementation run. The knowledge source tree is a separate local Git repository.
