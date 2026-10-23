# M2.12 — RAG quality and performance targets

**Status: proposed, awaiting sign-off.** The numbers are starting targets for the first release, and should be revised
after the first measurement in M8.2a.

## Evaluation set

- **Corpus:** 200 fictional documents across 3 tenants and 6 users, with known owners, shares and versions.
  - Some documents are deliberately near-duplicates across tenants, to catch isolation errors.
  - Some are revoked or deleted mid-test.
- **Questions:** 150, each labelled with the authorized relevant chunks *for that asking user*. A relevant chunk the
  user may not see counts as a required **miss**.
- The set is stored in the repo, under `olo-knowledge` (D5), as fixtures.

## Targets

| Metric | How it is measured | Target |
|---|---|---|
| Authorized recall@5 | Share of labelled authorized chunks that appear in the top 5 | ≥ 0.80 |
| Authorized precision@5 | Share of the top 5 that are labelled relevant **and** authorized | ≥ 0.60 |
| Unauthorized leakage | Unauthorized chunks returned, over all queries | **0** (a hard requirement) |
| Citation correctness | Citations that name an authorized source and version containing the cited text | 100% (checked automatically) |
| Indexing freshness | Time from ingest commit to searchable, p95 | ≤ 30 s for documents up to 5 MB |
| Deletion and ACL freshness | Time from delete or unshare to excluded from results | Within the 30-second revocation bound ([invocation-lifecycle.md](invocation-lifecycle.md)) |
| Context limit | Retrieved context per model call | ≤ 4,000 tokens, never truncating mid-chunk |
| Query latency | `rag.query` through ToolGate, p50 / p95 | ≤ 300 ms / ≤ 800 ms |
| Turn latency overhead | Governed memory load + retrieval + append added to a chat turn, p95 (excluding model time) | ≤ 1.5 s |
| Concurrency | Sustained load on staging | 50 concurrent users; 10,000 documents per tenant; 5 tenants |

Leakage and citation correctness are pass or fail. The other metrics are measured in M8.2a and load-tested in M10.4.
