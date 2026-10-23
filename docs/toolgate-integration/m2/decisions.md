# M2.6–M2.8 — Decisions D5, D6 and D7

**Status: accepted** on 2026-10-23 by the project owner (see the [approval record](README.md#approval-record)). D4 is in
[credentials.md](credentials.md).

## D5 — Who owns the memory and RAG services, and where they run (M2.6) — Accepted

**Decision.** A new repo, **`olo-knowledge`**, owned by the OLO team, with two independently deployed services:

- `olo-memory`: conversation records, working state, and their resource ACLs
- `olo-rag`: documents, chunks, embeddings, and their resource ACLs

| Concern | Decision |
|---|---|
| Storage | PostgreSQL for conversation records, working state and resource ACLs; Qdrant for vectors |
| Who may call them | **Only ToolGate executors** (M4.6): network policy, mTLS, and service permits |
| Database credentials | Stay inside the services. Never in ToolGate executors, the OLO worker or the backend. |
| Governance | ToolGate owns execution governance. The services enforce resource ownership and sharing. |
| Fresh install | Each service creates its own schema on an empty database. No migration from the demo stores (D8). |

Not chosen: putting the services inside the olo backend (storage credentials next to user-facing code, and a way
around ToolGate), or inside ToolGate packages (ToolGate would become the storage engine, which D1 rules out).

## D6 — Long-term user memory in the first release (M2.7) — Accepted

**Decision.** Long-term user memory is **excluded** from the first release.

- **The first release ships:** persistent conversation history, temporary working state, and document RAG.
- **What is excluded:** learned preferences, or facts carried across conversations.
- **A future decision is required** before any of it is built, covering consent, provenance, correction and deletion.
- **Nothing unused is built now:** no long-term-memory endpoints, permissions, resource locators or storage. The
  placeholder `ltm.*` permissions and the `olo://{tenant}/memory/{userId}` locator are removed from the contracts.

## D7 — Protecting sensitive Temporal payloads (M2.8) — Accepted

**Decision.** One protection strategy: an **AES-256-GCM payload codec**, with **authorized references** for large
content.

| Concern | Decision |
|---|---|
| Codec | An AES-256-GCM `PayloadCodec`. Temporal stores ciphertext only. |
| Keys | Tenant-specific keys, managed outside Temporal in a key manager, and identified by key id (`kid`). Old keys stay decrypt-only until their retention ends. |
| Where it is configured | **Every** Temporal client and worker that serializes sensitive payloads. That means the OLO worker **and the olo backend's Temporal client that starts workflows**. A client without the codec must not be able to start or signal these workflows. |
| Large content | Retrieved documents and full conversation history are passed as authorized references (context item ids), and fetched under authorization at use |
| Credentials | Authentication credentials (bearer tokens, refresh tokens, session secrets) never appear in any payload |
| Decryption tools | The codec server for Temporal UI and CLI requires authenticated, tenant-scoped access. An operator sees decrypted payloads only for tenants they are authorized for. |

Not chosen: references alone (every activity would have to fetch its content), or leaving payloads unprotected until
M10.
