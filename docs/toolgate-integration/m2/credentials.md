# M2.5 — Credential lifecycle and renewal contract (P1), and D4

**Status: accepted.** D4 and this whole contract (lifecycle, renewal and misuse rules) were approved on 2026-10-23 by the
project owner; see the [approval record](README.md#approval-record).

## D4: how OLO gets user-bound ToolGate credentials — Accepted

**Decision: RFC 8693 token exchange, implemented in ToolGate.** This is ToolGate implementation work (gap G1): a
token endpoint, plus a JWT credential validator on the Gateway.

- **Who asks:** the authenticated OLO backend requests short-lived credentials with the ToolGate audience.
- **Path A (memory and RAG):** `sub` is the user.
- **Path B (business tools):** `sub` is the user, and `act` is the mapped agent (M3.9).
- **Lifetime:** 5 minutes at most. Tokens are bound to tenant, user, `sessionEpoch` and audience, and are kept in
  memory only.
- **Renewal:** through the verified session binding, with current permissions rechecked each time (below).
- **No fallback authentication mechanism** in the first release.

Alternatives considered and not chosen:

| Option | Why not |
|---|---|
| Server binding with introspection | A second credential mechanism to build and secure. Excluded: no fallback in the first release. |
| Static credentials per user | Doesn't scale, can't represent `HUMAN` (the Gateway refuses that mode), and needs a reload per user |

## Lifecycle

| Stage | Rule |
|---|---|
| Issuance | Only the OLO backend's credential service may request a user-bound token, authenticated with its own mTLS workload identity. The OLO worker can't call the token endpoint directly. |
| Audience | Exactly the ToolGate Gateway |
| Tenant binding | The token's tenant must equal the user's verified membership. ToolGate refuses a mismatch. |
| Expiry | 5 minutes at most (D4). No refresh tokens are issued to OLO. |
| Renewal | A new exchange each time, from the stored non-secret binding (below) |
| Revocation | Revoking the session bumps `sessionEpoch` on both sides. A token carrying an older epoch is refused. Revoking a grant bumps `authorizationEpoch` (G9). |

## Renewal in long-running work

- **What the run stores:** `bindingRef = {tenantId, userId, sessionId, sessionEpoch}`. These are not secrets.
- **Before each user-scoped call:**
  1. The runtime asks the OLO credential service for a fresh token for `bindingRef`.
  2. The service checks that the session is still valid at `sessionEpoch`, the user is still a tenant member, and the
     account is enabled.
  3. Only then does it exchange for a ToolGate token.
  4. ToolGate re-evaluates current authority on the call (G9).
- **When the session has expired or been revoked:**

  | Operation type | Behavior |
  |---|---|
  | Interactive chat turn (path A) | **Terminate** the turn. The user signs in again and resends. |
  | Waiting for approval (path B) | **Pause** for re-authentication, up to the approval expiry, then terminate |
  | User-initiated ingest or delete | **Pause** for re-authentication, up to 24 hours (proposed), then terminate |
  | Service job (path C) | Not applicable: it uses the service principal's own credential |

- **Never a fallback.** A user-scoped operation is never continued under the service principal, or under any other
  identity.

## Misuse protection

| Threat | Protection |
|---|---|
| Impersonation | Only the backend credential service can mint tokens. Tokens carry the verified `sub`, and ToolGate signs them. |
| Use outside the audience | The `aud` claim is checked by the Gateway |
| Use outside the tenant or session | `tenantId` and `sessionEpoch` claims are checked against current state |
| Duplicate effects from retries | Dedup on the logical-operation key ([invocation-lifecycle.md](invocation-lifecycle.md)), not on the credential |
| Token theft from the worker | 5-minute lifetime, never persisted, only in memory |
| Sender binding | **Proposed: not required for the first release.** Lifetimes are short and the Gateway is on a private network. Revisit before any deployment where the Gateway is reachable from outside the platform network. |

## Path C service credential

- **What it is:** the existing static workload credential (the Gateway accepts `SERVICE` mode), issued to a reserved
  platform principal `olo-platform-service` (gap G2).
- **Grants:** explicit and resource-scoped, for example `rag.ingest` on `olo://{tenant}/kb/{kbId}` for a scheduled
  re-index.
- **Rotation:** at least every 30 days, using the Gateway's `SIGHUP` reload.
- **Kept apart from path B:** OLO's agent mapping (M3.9) refuses this principal.
