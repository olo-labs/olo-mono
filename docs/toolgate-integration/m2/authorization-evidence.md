# M2.10 — Authorization evidence for memory and RAG services (P1)

**Status: proposed, awaiting sign-off.** This contract must be agreed before M4.6 implements it.

## The problem

A memory or RAG service sits behind a ToolGate executor. mTLS tells the service *who is calling* (a ToolGate executor).
It doesn't tell the service *what was authorized*, because the executor's own permit (G8) is meant for the executor,
not the service. The service has to verify for itself that the user, operation and resource in front of it were
authorized, that the authorization is for exactly this request, and that it is still current.

## Evidence format

ToolGate Control issues a **service permit** for each service call. It follows the same pattern as the existing effect
permit: an RS256 JWS, with header `typ: olo-service-permit+jws` and a `kid` from Control's published key set.

| Claim | Meaning | The service checks |
|---|---|---|
| `iss` | Control | It is Control's key, by `kid` |
| `aud` | The specific service, for example `olo-memory` | Equals its own name |
| `tenantId` | Verified tenant | Equals the resource's tenant segment |
| `sub` | User id, or the service principal | Present; used for the ownership and share check |
| `act` | Agent, for path B only | Recorded in audit |
| `operation` | For example `memory.append` | Matches the endpoint being called |
| `resources` | `ResourceDescriptor[]` | Every resource touched is in the list |
| `requestFingerprint` | sha256 of the exact request body | Equals the hash of the body received |
| `invocationId`, `nonce` | Correlation and single use | Recorded; the nonce is consumed (option a) |
| `authorizationEpoch` | Epoch at issue | Used by option (a) |
| `iat`, `exp` | Lifetime | `exp` is in the future. The lifetime is 60 seconds or less. |

A request is refused if any claim is missing, if any check fails, or if the signature doesn't verify.

## Meeting the revocation bound

A valid signature and a future `exp` don't prove permission is still current. Each operation type uses one of two
options:

| Option | How | Used for |
|---|---|---|
| **(a) Online at the effect** | Immediately before the effect (inside the database transaction, before commit), the service calls `POST /permits/consume` on Control with the nonce. Control runs `fresh()`, which re-checks expiry and `authorizationEpoch` and re-runs the decision (G9). Only then does the service commit. | **All writes:** `memory.append`, `memory.delete`, `memory.share`, `memory.export`, `ltm.write`, `ltm.delete`, `rag.ingest`, `rag.update`, `rag.delete`, `rag.share` |
| **(b) Bounded offline validity** | The service verifies the signature and claims locally. The permit lifetime is 15 seconds or less, which is under the 30-second revocation bound. Services also subscribe to Control's revocation feed (epoch bumps), and refuse permits issued before the latest epoch. | **Reads only:** `memory.read`, `ltm.read`, `rag.query`, where the release-time re-check (check point 3) also applies |

**Why this stays within the bound.**
- Under (a), the decision runs at commit time, so revocation is effective immediately.
- Under (b), the worst case is the permit lifetime (15 seconds) plus the feed delay (proposed 10 seconds or less),
  which is 25 seconds or less. M8.2 measures it.

## Queueing, approvals and long-running operations

- **Permits are issued at dispatch, never earlier.** A queued operation, one waiting for approval, or one between steps
  of a long job holds **no** service permit. It holds only its `logicalOperationId`.
- When it finally runs, the executor asks Control for a fresh permit. Control re-evaluates current authority then, and
  the service applies option (a) or (b) at the effect.
- **Long-running service work** (for example indexing a large document) is split into chunks. Each chunk commit is an
  effect with its own option (a) consumption. A revocation stops the job at the next chunk, and the partial version is
  never published ([rag controls](../../../plan.md#rag-controls): atomic version publication).

## Required tests (M4.6 acceptance)

- Revocation after the permit is issued but before the service commits: refused (option a).
- The same for a queued call, a call waiting for approval, and a long-running indexing job.
- An option (b) read with a permit issued before an epoch bump: refused.
- A genuine executor sending a body whose hash differs from `requestFingerprint`: refused.
- A permit for `olo-rag` presented to `olo-memory`: refused.

## ToolGate work

- Service-permit issuance for downstream audiences
- Exposing consumption to the services: today only executors call `/permits/consume`
- A revocation feed
- Package manifests that declare which option each operation uses
