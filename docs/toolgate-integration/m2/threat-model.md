# M2.9 — Threat model (OLO side)

**Status: proposed, awaiting sign-off.** ToolGate's half belongs in `olo-toolgate/docs/security/threat-models/`. That
change is drafted from the ToolGate rows below and filed in that repo separately.

## Assets

- Conversation history
- Documents, chunks and embeddings
- Long-term memory (reserved)
- Credentials: user-bound tokens, the service credential, codec keys, database credentials
- The integrity of effects: tool calls and their downstream changes

## Trust boundaries

```
Browser ─▶ OLO backend ─▶ OLO runtime (Temporal worker) ─▶ ToolGate Gateway ─▶ Control ─▶ executor ─▶ memory/RAG service ─▶ DB
          [B1]            [B2]                             [B3]                           [B4]        [B5]
```

## Threats and mitigations

| # | Boundary | Threat | Mitigation | Contract / milestone |
|---|---|---|---|---|
| T1 | B1 | Forged or unsigned JWT accepted | Verify signature, issuer, audience and expiry; no `sub` fallback | identity-context, M3.1 |
| T2 | B1 | Tenant spoofed through a request field | Tenant comes only from verified membership | identity-context, M3.1 |
| T3 | B2 | Workflow author selects a privileged agent identity | Server-side agent mapping; the service principal can't be mapped | credentials, M3.9 |
| T4 | B2 | The model forges `userId` or a resource id in its arguments | Identity comes only from verified context. Resources are checked by ToolGate and by the service. | identity-context, resources-and-permissions |
| T5 | B2 | Token leaks through Temporal history or logs | Never persisted; codec; redaction | credentials, decisions D7, M10.3 |
| T6 | B2 | A retry duplicates an effect | Logical operation recorded before dispatch; dedup key with a stored fingerprint | invocation-lifecycle, M5.1, M5.3 |
| T7 | B2 | An unknown outcome is retried blindly | `OUTCOME_UNKNOWN`, never redispatched automatically; reconciliation | invocation-lifecycle, M5.4 |
| T8 | B3 | User credential replayed by a different caller or tenant | Audience, tenant and `sessionEpoch` claims; 5-minute lifetime | credentials |
| T9 | B4 | A genuine executor sends a scope it wasn't granted | Signed service permit bound to the request fingerprint | authorization-evidence |
| T10 | B4 | A permit is used after revocation | Online consumption for writes; ≤ 15 s permits plus a revocation feed for reads | authorization-evidence |
| T11 | B5 | Something reaches the service directly, bypassing ToolGate | mTLS, network policy, permit required | authorization-evidence, M4.6 |
| T12 | B5 | Cross-tenant or cross-user retrieval | Mandatory scope filter in search, release-time re-check, tenant-keyed caches | plan *RAG controls*, M6.4 |
| T13 | — | Revoked content survives in derived context | Source dependencies; discard at check points 1 and 3 | invocation-lifecycle |
| T14 | — | Prompt injection through retrieved text | Retrieved text marked untrusted; the model can't change filters; citations | plan *RAG controls*, M7.5 |
| T15 | — | A deleted document is recreated by an indexing job | Tombstones and a version check before write | plan *RAG controls*, M6.4 |

## ToolGate rows (to file in olo-toolgate)

- **T8 and G1:** a user-bound credential kind, so the Gateway accepts `HUMAN` mode for OLO
- **T9 and T10:** service-permit issuance, consumption by downstream services, and a revocation feed
- **T3 and G2:** a reserved platform service principal that can't be confused with an OLO workflow agent
