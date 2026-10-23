# M2.2 — Identity context contract

**Status: proposed, awaiting sign-off** from olo, olo-mono and ToolGate.

This is the identity that travels with every governed call, from the OLO backend through the OLO runtime to
ToolGate, and from ToolGate into the executor.

## Principals

| Path | Subject | Actor | Mode in ToolGate `RequestContext` |
|---|---|---|---|
| A. Platform calls (memory, RAG) | Signed-in user | none | `HUMAN` |
| B. Agent tool calls | Signed-in user | OLO agent (server-mapped) | `DELEGATED` |
| C. Service jobs | Platform service principal | none | `SERVICE` |

## Fields

| Field | A | B | C | Source of truth | Rules |
|---|---|---|---|---|---|
| `tenantId` | ✔ | ✔ | ✔ | Verified token plus OLO's tenant-membership record | Never taken from a request body. Never falls back to `sub`. |
| `userId` | ✔ | ✔ | — | OLO's internal user id, mapped from the verified `iss` and `sub` | Stable across sessions. Never an email address or display name. |
| `sessionId` / `sessionEpoch` | ✔ | ✔ | — | OLO session store | Revoking a session bumps its epoch, and older epochs are refused. |
| `agentId` | — | ✔ | ✔¹ | Server-side mapping (M3.9) | Path B: an OLO agent mapped to the tenant and workflow. Path C: the reserved platform principal. |
| `audience` | ✔ | ✔ | ✔ | Credential issuer | Exactly the ToolGate Gateway. Any other audience is refused. |
| `expiresAt` | ✔ | ✔ | ✔ | Credential issuer | 5 minutes or less for user-bound credentials (proposed). |
| `resource` | ✔ | ✔ | ✔ | The request | A locator, not a permission ([resources-and-permissions.md](resources-and-permissions.md)) |
| `operation` | ✔ | ✔ | ✔ | The request | One permission from the catalog |
| `runId`, `turnId`, `logicalOperationId`, `invocationId` | ✔ | ✔ | ✔ | OLO runtime / ToolGate | Used for correlation and dedup; never for authorization |

¹ G2 in the gap matrix: `SERVICE` credentials still require an agent record today.

## Provenance rules

1. **The OLO backend verifies; nothing else does.** Only the OLO backend turns a user's sign-in into identity, after
   M3.1 verifies the signature, issuer, audience and expiry. The browser's token is never forwarded.
2. **The runtime holds a reference, not a token.** A run carries
   `{tenantId, userId, sessionId, sessionEpoch, bindingRef}`. `bindingRef` is an opaque id that the credential service
   resolves (see [credentials.md](credentials.md)). None of these fields is a secret, and none of them grants anything
   on its own.
3. **ToolGate authenticates where the identity came from.** ToolGate trusts a field only when it arrives inside a
   credential that ToolGate verified (G1 and G4). A field in the arguments, a header or the model's output is never
   identity.
4. **The executor receives identity only through ToolGate's protected context** (M4.1): the SDK `Invocation`, filled
   from the verified `RequestContext`. A tool's arguments can't set it.
5. **No identity, no call.** A user-scoped operation without a verified user context is refused (M3.3, M4.3). It never
   falls back to the service principal.

## Changes needed in each repo

| Repo | Change |
|---|---|
| olo | M3.1–M3.2: verified JWT, a stable user id, tenant membership, session epochs. Remove the `sub` fallback in `JwtTenantIdDecoder`. |
| olo-mono | M3.3: a run identity record in `WorkflowInput`, filled by the backend. The kernel refuses user-scoped calls without it. |
| ToolGate | M3.4–M3.5, M4.1: a user-bound credential kind on the Gateway, and the principal fields on the SDK `Invocation` |
