# M2.4 — Invocation lifecycle and revocation contract (P1)

**Status: proposed, awaiting sign-off.** This contract must be agreed before M3–M7 implement it.

## Logical operations

Every governed call (paths A, B and C) is a **logical operation**, which OLO records durably *before* dispatch (M5.1):

```
logicalOperationId = <runId>/<turnId or jobId>/<step>/<n>     stable across retries, never regenerated
requestFingerprint = sha256(canonical JSON of {tool, operation, resources, arguments})
```

## Dedup record

```
Lookup key   = tenantId + (userId | servicePrincipalId) + logicalOperationId
Stored value = requestFingerprint + invocationId + state + result reference
```

| Submission | Result |
|---|---|
| New key | Atomically create one invocation (a unique constraint on the key) |
| Same key, same fingerprint | Return the existing invocation, or resume it if it is still waiting. Never execute twice. |
| Same key, different fingerprint | `409 CONFLICT`. Nothing executes, and the operation is flagged for investigation. |
| Concurrent submissions of one key | Exactly one invocation is created. The others get it, or a conflict if their fingerprint differs. |

- **The key and the fingerprint never contain credentials,** so rotating or renewing a credential changes neither.
- **Mapping to ToolGate:** OLO sends `logicalOperationId` as the JSON-RPC `id` on `/mcp`, or as the
  `Idempotency-Key` on `/access/invocations`. ToolGate already maps a repeated id from the same caller to the same
  invocation (gap G11). **What ToolGate must add or prove:** a conflict on a fingerprint mismatch, and an atomic create.
- **Retention:** at least `maxWorkflowLifetime + approvalExpiry`. Proposed: 30 days, plus 7 days of approval expiry.
  After a record expires, a recovering operation is set to `OUTCOME_UNKNOWN` and goes to reconciliation. It is never
  re-sent blindly.

## States

ToolGate's states (G10) are used as they are. OLO adds handling rules:

| State | OLO behavior |
|---|---|
| `PENDING_APPROVAL` | Wait (M5.5). Re-authorize when resuming. |
| `QUEUED`, `RESERVED`, `DISPATCHED`, `EXECUTING` | Poll or await, with a bounded timeout |
| `SUCCEEDED` | Fetch the result through an authenticated call, re-checking release (check point 3) |
| `FAILED`, `CANCELLED`, `EXPIRED` | Terminal. The turn follows the failure rule ([conversation-turn.md](conversation-turn.md)). |
| `PARTIAL` | Terminal; treated as a failure, and the partial effects are reported |
| `OUTCOME_UNKNOWN` | **Never redispatched automatically.** Run the executor's reconciliation (below). If that can't decide, raise an operator task. |

## Reconciliation

Every side-effecting executor declares one reconciliation method in its package manifest:

| Method | When | How |
|---|---|---|
| `downstream-idempotency` | The target system accepts an idempotency key | Re-send with the same `downstreamIdempotencyKey` (that field already exists on invocations). The target deduplicates. |
| `status-lookup` | The target can be queried | The executor looks the effect up by `logicalOperationId` and reports `SUCCEEDED` or `FAILED` |
| `manual` | Neither is possible | Stays `OUTCOME_UNKNOWN` until an operator decides |

The memory and RAG services must support `downstream-idempotency`. Their writes are keyed by message id or document
version.

## Revocation

- **Bound: 30 seconds** (accepted 2026-10-23; see the [approval record](README.md#approval-record)). Revoking a grant, membership, share or session stops new effects, context use and
  result release within that time.
- **The three check points:**
  1. Before context reaches the model
  2. Before an effect executes
  3. Before a result is released
- **What must invalidate:** for each kind of change, ToolGate must show which counter changes (`authorizationEpoch`,
  `directoryRevision`, `sessionEpoch`, or a resource's ACL version) and that the M4.6 re-check sees it (gap G13).

### Derived context

Every context item the run builds carries its sources:

```json
{ "id": "ctx-…", "kind": "history|chunk|summary|model-output|child-output|tool-result",
  "sources": ["olo://t1/conversation/c9#msg-41", "olo://t1/kb/k2/doc/d7@v3#chunk-12"] }
```

- A model output's `sources` is the union of the sources of everything in its prompt. Precise per-claim tracking isn't
  required.
- Child-agent outputs and tool results inherit the sources of their inputs.
- At check points 1 and 3, the runtime re-authorizes the source set. Any item with a revoked source is **discarded**,
  and the next model call proceeds without it. Items aren't silently rewritten; regenerating a summary is the memory
  service's job (M6.1).
- Stored summaries record their sources and are rebuilt when a source is revoked or deleted.
