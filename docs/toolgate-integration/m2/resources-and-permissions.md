# M2.3 — Resources and permissions

**Status: proposed, awaiting sign-off.**

## Resource identifiers

Every resource is a ToolGate `ResourceDescriptor{kind: CUSTOM, locator}`, with a locator in this form:

| Resource | Locator |
|---|---|
| Conversation | `olo://{tenantId}/conversation/{conversationId}` |
| User memory namespace (long-term memory) | `olo://{tenantId}/memory/{userId}` |
| Knowledge base | `olo://{tenantId}/kb/{kbId}` |
| Document | `olo://{tenantId}/kb/{kbId}/doc/{docId}` |

- Ids are opaque, and the service generates them (UUIDs). They are locators, not permissions.
- The tenant segment must equal the verified `tenantId`. A mismatch is refused before any lookup.
- The `olo.memory` and `olo.rag` packages declare `ControlResourceExtractor`s that build these locators from their
  arguments, so ToolGate evaluates the real resource instead of `tool://<tool-id>` (gap G6.1).

## Permissions

| Operation | Permission | Default for an ordinary user | Policy |
|---|---|---|---|
| Read conversation history | `memory.read` | Own and member conversations | ALLOW |
| Append to a conversation | `memory.append` | Own and member conversations | ALLOW |
| Delete a conversation | `memory.delete` | Own conversations | Separate policy |
| Share a conversation | `memory.share` | Own conversations | Separate policy |
| Export a conversation | `memory.export` | Own conversations | Separate policy |
| Read long-term memory | `ltm.read` | Own namespace | Separate policy (D6) |
| Write or correct long-term memory | `ltm.write` | Own namespace | Separate policy (D6) |
| Delete long-term memory | `ltm.delete` | Own namespace | Separate policy (D6) |
| Query a knowledge base | `rag.query` | Knowledge bases the user may read | ALLOW |
| Add documents | `rag.ingest` | Knowledge bases the user may write | Separate policy |
| Replace a document version | `rag.update` | Documents the user may write | Separate policy |
| Delete a document | `rag.delete` | Documents the user owns or administers | Separate policy |
| Share a document or knowledge base | `rag.share` | Owner | Separate policy |

- **No permission implies another.** `rag.query` never allows ingest or delete, and `memory.read` never allows export.
- **Wildcard grants (`memory.*`, `rag.*`, `allActions: true`, `match: ANY`) are refused** for these packages.
- Each permission is a ToolGate action on the package tool. A grant's `EnterpriseScope.actions` lists exact actions only.

## Who decides what (proposed split)

ToolGate grants are group-sourced, and ToolGate documents them as "never an individual ACL" (gap G6.2). Per-object
ownership and sharing is application data that changes on every new conversation. So the decision is split, and
**both halves must allow**:

| Question | Decided by | How |
|---|---|---|
| May this user perform *operation* on *resources of this kind in this tenant*? | **ToolGate** | Group or team grant: exact action, `CUSTOM` locator `olo://{tenantId}/conversation/` with `PREFIX` match, purpose `HUMAN` or `CAPABILITY` |
| Is this user the owner or a member of *this* conversation, or allowed on *this* document? | **The memory or RAG service** | Ownership, membership and share records, checked against the verified subject in the authorization evidence ([authorization-evidence.md](authorization-evidence.md)) |
| Is the evidence for this exact request, still current, and meant for this service? | **The service**, using ToolGate's evidence | M2.10 |

**The alternative, for sign-off.** ToolGate could evaluate ownership itself, using caller-bound locator templates (for
example `olo://{tenant}/memory/${subject.userId}`) or a resource-ownership callback. That moves more policy into
ToolGate, but it needs new ToolGate work (gap G6.3). **Recommendation:** use the split above for the first release,
and record the template feature as a ToolGate follow-up.

## Ownership and sharing semantics

- **Conversation:** created by its first user, who becomes the owner. Members are added only through `memory.share`.
  Default access is owner only.
- **Knowledge base:** created with an owner and an access list (users or teams, each with read or write). Documents
  inherit the knowledge base's list unless a document-level share narrows or widens it.
- **Every share, unshare and membership change** bumps the resource's ACL version. That version is used for cache keys
  and for the release-time re-check.
