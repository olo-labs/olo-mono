# M2–M5 implementation checkpoint — 2026-10-24

Fresh installations, empty stores, and new workflows/pipelines only. No migration, compatibility reader or support for
old executions is included. M2 decisions are accepted; M3 is merged. Remaining M4/M5 implementation has passed local
qualification and is awaiting the companion PR merges. This does not mark M6–M10 complete.

## What is complete

| Milestone | Implementation and evidence |
|---|---|
| M2 | Accepted contracts/D4–D7, explicit owners, threat model, 30-second revocation bound, 37-day dedup retention and RAG targets. Original gap table is retained as discovery evidence with a resolution checkpoint. |
| M3 | Verified JWT/session/tenant bindings, direct-user credentials, non-delegable service principals, resource/operation grants, mapped agents and delegated business tools; six review fixes are in shared PR #14 and backend PR #7. |
| M4 | Control-derived service permits; Tool Host mTLS/header injection; audience, scope, body and executor-key verification; online single-use write consumption; authenticated tenant epoch/key snapshots; real TLS/storage and PostgreSQL isolation qualification. |
| M5 | Recorded model decisions and operation IDs across every branch phase; safe inline scheduling; encrypted payload/failure attributes and authenticated codec endpoints; dedup/reconciliation/approval handling; bounded lifetimes and cancellation through ToolGate; acceptance tests below. |

The six earlier review fixes remain in force: delegated-agent ceilings are mandatory for model-selected business
tools; credential exchange binds/validates tenant; sessions retain verified authentication expiry; revocations use a
durable retry outbox and short credentials; Gateway unknown outcomes retain their invocation identity; workflow IDs
use explicit tenant-bearing root/child prefixes. Memory/RAG use the separate direct-user platform path, with no agent
authorization dependency. Long-term user memory remains excluded from the first release.

## M4 service boundary

Control accepts a service-permit request only from the admitted executor certificate and its current execution fence.
It derives subject, actor, operation, resources and request fingerprint from the stored invocation. The Tool Host
presents the same key over mTLS to the service and injects the permit after broker route checks. Package code cannot
set the service-permit header. Services check the actual request and enforce their own ownership/membership/ACLs.

The `toolgate-service-authorization` library provides verified request/mTLS checks, authenticated key/epoch refresh,
write consumption and commit-deadline checks. Writes have one nonce per logical invocation/audience, including across
executor retry/fence renewal. Repeated minting returns the original unconsumed permit; consumed or ambiguous writes
require reconciliation. A lost Control response or failed service commit must never cause a new effect capability.
The service's invocation/idempotency ledger settles the storage outcome. Every indexing chunk is its own operation.

Service permits expire within 15 seconds and never outlive the original or verified renewed user credential deadline.
Control persists that deadline separately from approval/invocation lifetime and checks it at effect boundaries. This
prevents successive credentials/permits from extending stale authority. Read epoch/key snapshots refresh within five
seconds; stale epochs fail closed after ten seconds. Reads re-check before release. Writes verify/consume inside the
storage transaction and enforce a bounded commit deadline. Production timing measurement is M8.

The ToolGate companion includes an executable ingress NetworkPolicy for the new service namespace, endpoint/API
contracts, role and key-rotation instructions, transaction/reconciliation rules and a complete integration guide at
`docs/control-plane/olo-service-permits.md`. M6 supplies the application listeners, ACL implementation and stores.

## M5 durable execution and cancellation

A parallel fork now persists a continuation stack and returns after one node phase. No branch hides an entire model
loop in a retryable activity. Nested continuations and human-input resumes are normalized before routing the next
node. Inline LLM/effect nodes use activities; deterministic child-workflow dispatch remains on the workflow thread.

Temporal cancellation schedules a bounded detached cleanup activity using the last recorded state. It obtains fresh
caller credentials, rechecks the mapped actor, and cancels the original pending logical operations. The Gateway derives
the same caller-scoped invocation key. Control persists cancellation intent even before submission, preventing a late
request from starting an effect. Running/ambiguous effects become `OUTCOME_UNKNOWN`, never an assertion of undo.
Cleanup errors remain visible; there is no broader-identity fallback. Termination/timeouts that skip Temporal cleanup
are handled by bounded server authority and reconciliation. Child cancellation waits for child cleanup. Root and child
execution timeouts are 30 days; activity retries are limited to four attempts with bounded backoff and timeouts.

Approvals resume the original invocation and expire before dispatch after seven days. Expired/revoked user authority
ends the turn without another model decision or dispatch. In-process waiting also retains the original turn instead
of telling the user to create a new request. Unknown outcomes and terminal invocations stop automatic model retries.
`STATUS_LOOKUP` may use the declared lookup plus ToolGate's audited operator reconciliation; automatic lookup scheduling
is optional, outside the M5 acceptance requirement. No operator resolution automatically re-executes an unknown effect.

Payload protection requires tenant keys, rejects cleartext tenant payloads and encrypts failure messages/stack traces.
The backend's optional `/api/codec/encode` and `/decode` endpoints require tenant-admin authorization, reject foreign
payloads, set no-store and exclude bodies from request logs. Keys and authentication credentials stay outside history.

## M5 acceptance evidence

| Case | Concrete qualification |
|---|---|
| Crash after dispatch, lost activity result and activity retry | `TemporalAgentRecoveryTest`: production Temporal workflow/test server, same recorded operation, one downstream effect, no regenerated decision |
| Parallel/inline replay | `ParallelAgentRecoveryTest`: serialized production traversal snapshots, lost dispatch result, stable operations, both branches; human resume still routes the remaining agent into an activity |
| Unknown outcome stays undispatched | `AgentTurnOutcomesTest`, `EnterpriseOperationTest.unknownExternalOutcomeCannotTriggerBlindRetry`, Gateway ended-state tests |
| Dedup expiry | `AgentTurnOutcomesTest.aRecordedCallOlderThanTheDedupRetentionIsNeverSent` |
| Credential rotation, same key | Gateway dedup-key renewal/rotation test; `EnterpriseOperationTest.aRotatedCredentialReplaysTheSameInvocation` |
| Changed arguments/conflict; concurrent same-key submissions | `EnterpriseOperationTest` fingerprint/conflict and concurrent-submission tests |
| Approve/deny/expire; original invocation | `AgentTurnOutcomesTest`, `TemporalAgentRecoveryTest`, Control approval tests |
| Grant revoked during approval | `UserScopedToolsTest` and Control grant/approval-revocation consumption tests |
| Session expires during approval | `UserScopedToolsTest.anExpiredSessionOrRevokedGrantDuringApprovalEndsTheTurnWithoutAnotherDispatch` plus backend session/credential tests |
| Cancellation during approval; before first submission; running effect | `TemporalAgentRecoveryTest`, Gateway cancellation test, `ServicePermitBridgeTest` |
| No tokens/cleartext in history | `TemporalAgentRecoveryTest`, payload-codec/failure-attribute tests, backend codec authorization/logging tests |

## Validation results

- OLO kernel affected recovery/scheduling/resume suites: **56 passed**, no skips. Includes a real Temporal test server;
  the model and Gateway are controlled integrations. This is not the entire legacy kernel suite.
- OLO ToolGate client: **23 passed**.
- OLO backend: **79 passed**, including an isolated real Redis and codec authorization tests; SDK compiles with the
  30-day root execution limit.
- ToolGate Control suite: **213 passed, 7 skipped** (platform/optional worker/cache cases); complete enterprise suite:
  **150 passed**, no skips, with isolated PostgreSQL 16 and SQLite. Fresh V1 initialization and forced runtime row
  security include the authority/deadline and cancellation tables. Test JVM uses UTC for the container timezone catalog.
- Shared Java contracts: **4 passed**; service verifier: **6 passed**. Generated bindings verify against all **1,044**
  outputs. Full Gateway and Tool Host Rust suites passed, including broker package execution and service-header forgery.
- `git diff --check` passes. Disposable test PostgreSQL/Redis containers are removed by their test wrappers.

A broader legacy-kernel probe also reported failing return-value assertions in `WorkflowReturnResolverTest` and
`GraphTraverserTest` against old conversation plugins/workflows. Those assertions are outside this M2–M5 change;
the affected recovery/scheduling suites above pass. The probe's demo conversation file was restored and excluded.
M9 replaces that legacy workflow/plugin path under the new-install contract.

## Merge and next work

1. Merge/publish the ToolGate service-permit/cancellation bridge before deploying OLO's new cancellation calls.
2. Merge the olo-mono durable-boundary/cancellation completion and the backend companion(s), including existing backend
   PR #7 for credential/revocation/codec hardening. Publish shared libraries before backend deployment.
3. M6: implement `olo-knowledge` conversation memory, working-state and RAG services/packages on this bridge, including
   ownership/ACLs, transaction deadlines/idempotency and actual-store qualification.
4. M7: deterministic user-scoped platform calls and conversation/document lifecycles; memory/RAG remain outside agent
   selection/authorization.
5. M8 → M9 → M8 → M10: real-stack timing/quality/load checks, local tool removal, repeated qualification, clean install
   and operational checks. The accepted 30-second revocation/RAG targets still need those production measurements.
