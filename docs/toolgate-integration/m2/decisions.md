# M2.6–M2.8 — Proposed decisions D5, D6 and D7

**Status: proposed, awaiting sign-off.** D4 is in [credentials.md](credentials.md).

## D5 — Who owns the memory and RAG services, and where they run (M2.6)

| Option | Notes |
|---|---|
| **(a) A new repo, `olo-knowledge`,** with two services, `olo-memory` and `olo-rag`, owned by the OLO team | Independent release cycle. Owns its databases and their credentials. |
| (b) Inside the olo backend | Puts storage credentials next to user-facing code, and the services could be called without going through ToolGate |
| (c) Inside ToolGate packages | ToolGate becomes the storage engine, which the plan rules out (D1) |

**Recommendation: (a).**
- **Deployment:** two containers on a private network segment, reachable only from ToolGate executors (M4.6).
- **Storage:** PostgreSQL for conversations, working state and ACLs; Qdrant for vectors.
- **Fresh install:** each service creates its own schema on an empty database. There is no migration from the demo
  stores.

## D6 — Long-term user memory in the first release (M2.7)

**Recommendation: no.**
- The first release ships conversation history and working state (M6.1, M6.2) and RAG (M6.4).
- Long-term memory (M6.3) needs its own provenance, correction and retention design and its own permissions
  (`ltm.*`).
- The permissions and resource locator are reserved now (`olo://{t}/memory/{userId}`), so adding it later needs no
  contract change.
- M6.3 moves to a follow-up milestone, and M6's weight stays the same because RAG hardening absorbs the effort.

## D7 — Protecting sensitive Temporal payloads (M2.8)

| Option | Notes |
|---|---|
| **(a) Encryption payload codec** | A Temporal `PayloadCodec` in the worker encrypts every payload with AES-256-GCM. Keys come from a key manager and are rotated by key id. Temporal stores only ciphertext. |
| (b) References only | Workflow state holds ids, and the content lives in the memory service. History stays small, but every activity has to fetch its content. |

**Recommendation: (a) for everything, plus (b) for large items.**
- Prompts, tool arguments and results, and retrieved chunks are encrypted by the codec.
- Retrieved documents and full conversation history are passed by reference (context item ids) and fetched under
  authorization at use.
- **Keys:** a codec key per tenant, held in the key manager and never in workflow state, with rotation by `kid`. Old
  keys stay decrypt-only until their retention ends.
- **Never in any payload:** bearer tokens, refresh tokens, or `bindingRef` secrets (`bindingRef` holds none anyway).
- **Temporal UI:** a codec server with authentication, so operators see decrypted payloads only if they are authorized.
