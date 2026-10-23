# M2.11 — Conversation turn contract

**Status: proposed, awaiting sign-off.**

## Sequence

```
1. authorize      memory.read + memory.append on olo://{t}/conversation/{c}
2. accept         persist the user message, msgId = turnId/user        (idempotent)
3. load history   memory.read, messages with seq < seq(user message)   (excludes the current message)
4. assemble       prompt = system + history + [current user message]    (the current message appears exactly once)
5. run            workflow agents, with context items carrying source dependencies
6. finalize       persist the response, msgId = turnId/assistant        (idempotent, retried)
7. complete       turn state = COMPLETE only after step 6 is durably committed; only then release the response
```

- **The current message appears exactly once.** History is loaded by sequence number strictly below the accepted
  message, and the current message is added once, as the turn input. A retry of step 3 or 4 rebuilds the same prompt.
- **Ids are stable.** `turnId` is created when the user message is accepted, and is the logical-operation scope for
  every write in the turn.
- **Only the lifecycle writes.** Child agents and tool calls never write history. Retries may repeat steps 2 and 6,
  which are idempotent by `msgId`, but they never create a duplicate record.
- **Concurrent turns** on one conversation are ordered by a per-conversation sequence. Each append is a
  compare-and-set on the conversation's `lastSeq`. A turn that loses the race reloads history and continues. Two turns
  never get the same sequence number.

## Outcomes

| Outcome | Persisted | Released to the user |
|---|---|---|
| Success | The user message, and the response | The response, after step 6 commits |
| Response persistence fails after generation | Step 6 is retried with the same `msgId`, up to the retry bound (proposed: 5 attempts over 60 seconds). If it still fails: the user message plus a `persist-failed` marker. | An error. The unrecorded response is **not** released as final. |
| Failure before a response | The user message, plus a `failed` marker | An error |
| Cancellation | The user message, plus a `cancelled` marker | Nothing further |
| Streaming | Nothing during the stream. The response is persisted once, at step 6. | Streamed tokens are shown as *provisional*. The turn shows as complete only after step 6. A broken stream follows the failure rule. |
| Pending approval | The user message, plus a `pending` marker linked to the invocation | A pending notice. The response is persisted when the resumed invocation finishes. |

## Revocation during a turn

- Before each model call (check point 1), the context items' sources are re-authorized. Items with a revoked source are
  dropped ([invocation-lifecycle.md](invocation-lifecycle.md#derived-context)).
- Before release (check point 3), the response's sources are re-authorized. If any source was revoked, the response is
  discarded and the turn ends with a `revoked` marker.
