# M2 — Capability gaps, contracts and open decisions

This is the M2 deliverable set for [plan.md](../../../plan.md#m2--capability-gaps-contracts-and-open-decisions).
Unless marked **Accepted**, everything here is **proposed**. M2 is accepted only when olo, olo-mono and ToolGate sign off the contracts and the
decisions below.

| Step | Document | P1 | Status |
|---|---|---|---|
| 2.1 | [gap-matrix.md](gap-matrix.md): 14 capabilities, with code evidence from ToolGate `fc4f6955` | | Drafted |
| 2.2 | [identity-context.md](identity-context.md) | | Proposed |
| 2.3 | [resources-and-permissions.md](resources-and-permissions.md) | | Proposed |
| 2.4 | [invocation-lifecycle.md](invocation-lifecycle.md): dedup record, states, reconciliation, revocation, derived context | ✔ | Revocation bound **Accepted**; contract Proposed |
| 2.5 | [credentials.md](credentials.md): lifecycle, renewal, misuse, **D4** | ✔ | **Accepted** |
| 2.6–2.8 | [decisions.md](decisions.md): **D5, D6, D7** | | **Accepted** |
| 2.9 | [threat-model.md](threat-model.md) (OLO side). ToolGate side: olo-toolgate `docs/security/threat-models/olo-integration.md` | | Proposed |
| 2.10 | [authorization-evidence.md](authorization-evidence.md): service permits and the revocation bound | ✔ | **Accepted** |
| 2.11 | [conversation-turn.md](conversation-turn.md) | | Proposed |
| 2.12 | [rag-targets.md](rag-targets.md) | | **Accepted** |

## Decisions

| Item | Decision | Status |
|---|---|---|
| D4: user-bound credentials | RFC 8693 token exchange, implemented in ToolGate. The backend requests ToolGate-audience tokens: the user for memory and RAG; the user plus the mapped agent actor for business tools. Lifetime 5 minutes at most, in memory only, renewed through the verified session binding with permissions rechecked. **No fallback mechanism.** | **Accepted** |
| D5: service ownership | `olo-knowledge`, owned by the OLO team: independently deployed `olo-memory` and `olo-rag`; PostgreSQL plus Qdrant; callable only by ToolGate executors; database credentials stay in the services | **Accepted** |
| D6: long-term memory | Excluded from the first release, with no unused endpoints built. A future decision must cover consent, provenance, correction and deletion. | **Accepted** |
| D7: payload protection | An AES-256-GCM codec with tenant keys (by `kid`) outside Temporal. Configured on every client and worker, including the olo backend's Temporal client. References for large content. No credentials in payloads. Tenant-scoped, authenticated decryption tools. | **Accepted** |
| Revocation bound | **30 seconds.** A revoked grant, membership, share or session stops new effects, context use and result release within 30 s. Measured in M8.2. | **Accepted** |
| RAG quality and performance targets | As in [rag-targets.md](rag-targets.md): zero unauthorized leakage, 100% correct citations, authorized recall@5 of 0.80 or more and precision@5 of 0.60 or more, indexing freshness p95 of 30 s or less, deletion and ACL changes within the revocation bound, context of 4,000 tokens or less, query p50/p95 of 300/800 ms or less, turn overhead p95 of 1.5 s or less, at 50 concurrent users, 10,000 documents per tenant and 5 tenants | **Accepted** |
| Evidence options (2.10) | Writes: online permit consumption at commit. Reads: permits of 15 seconds or less, plus a revocation feed. | **Accepted** |
| Sender binding (2.5) | Not required in the first release (private Gateway network); revisit for exposed deployments | **Accepted** |

## Still to sign off

| Item | Proposal |
|---|---|
| Dedup retention | 30 days, plus approval expiry (7 days) |
| Object authorization split | ToolGate decides operation + tenant + resource kind; the service decides ownership, membership and sharing; both must allow |

## Approval record

| Date | Approved | By | Notes |
|---|---|---|---|
| 2026-10-23 | D4, D5, D6, D7, as written in the *Decisions* table | Project owner (owner of olo, olo-mono and ToolGate), in the working session | D1–D3 and D8 unchanged. The revocation bound, the RAG targets and the remaining contracts are approved separately. |
| 2026-10-23 | The 30-second revocation bound, and the RAG quality and performance targets as in rag-targets.md | Project owner, in the working session | The remaining contracts (2.2–2.5, 2.10, 2.11) are still proposed. |
| 2026-10-23 | Contracts 2.5 (credential lifecycle and renewal, including sender binding not required) and 2.10 (authorization evidence, including the evidence options), as proposed | Project owner, in the working session | Still proposed: 2.2, 2.3, 2.4 (including dedup retention), 2.11. |

## Critical path the gap matrix found

1. **G1 — user-bound Gateway authentication is missing.** The Gateway rejects `HUMAN` mode, and JWT and
   introspection validators exist only as contract enums.
2. **G7 — the trusted executor context is missing.** The SDK `Invocation` has no tenant, session or resource.

Both are ToolGate work, in M3.4 and M4.1. Nothing in M6 starts until both are qualified.
