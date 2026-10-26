# Local qualification evidence

## Incident and approval lifecycle — 2026-10-26

Run `olo-qualification-e32d652c9182` passed the complete currently implemented incident fixture.
This is development-transport qualification with a scripted IdP/model and real backend,
worker, Temporal, Gateway, Control, Tool Host, memory/RAG services and fictional incident tools.

| Component | Tested source revision |
|---|---|
| ToolGate | `297f397901ec1548168047169b8a31e6fa16d79a` |
| Backend | `f66c4bfa54646933a896241a65cd5167df6c1d0d` |
| Worker | `8d9c958e992a44775d32e17e07867281fcafbf70` |
| Knowledge fixture | `a1023b2410b9045a6133b397ae14036ffa1f0807` (clean) |

The official server-recovery package SHA-256 was
`708784d56ab18f4b7d11e76043601b20e8ed393aec12beb6bd17a47f7330016a`.

Passed assertions:

- All seven incident tools succeeded exactly once, with the correct signed-in user,
  delegated actor, exact resource and completion audit evidence.
- Triage could not see remediation, and an agent capability could not replace a missing user grant.
- The ordinary requester could not approve their own remediation. Independent approval
  resumed the original invocation.
- Denial retained the original invocation as `FAILED`.
- Cancellation acknowledged success; exact replay returned HTTP 409/`CONFLICT`.
  The original invocation remained `CANCELLED`, with no reserved permit or completed resources.
- Revoked and expired user authority blocked approval and dispatch.

### Defects found and fixed

The earlier HTTP 403 turn-read failure (backend issue #11) originated before the memory service:
Control's tenant admission rejected service-permit issuance with HTTP 429, and the broker
reported this transient rejection as an authorization refusal. ToolGate commit `0efb310d`
adds bounded, jittered backoff for this pre-dispatch permit request only. It retains the original
invocation/fence, allows at most four attempts, and stops at the earlier of the invocation deadline
and a three-second budget. It does not retry actual denials or downstream effects.

The next run reached cancellation replay and exposed a separate Gateway mapping defect:
Control returned `CONFLICT`, but Gateway mapped that error to HTTP 500. Commit `297f3979`
preserves HTTP 409. The fixture continued to require the correct status and retained cancellation;
it was not relaxed to accept the server error.

All 46 Tool Host tests and all 42 Gateway tests passed, with zero failures or ignored tests.
The final real-stack run passed both recovered paths and the complete approval-terminal matrix.

Sanitized reports and logs are generated under
`build/qualification/olo-qualification-e32d652c9182/`; principal artifacts are `incident.json`,
`incident-invocations.json`, `incident-audit.json`, `incident-terminal-approvals.json`
and `incident-cancellation-replay.json`. Each fixture removes its captured runtime resources on exit.

### Remaining acceptance boundary

The incident graph currently orchestrates its stages sequentially and ends with a tool-free
commander summary. This does not prove commander-generated child-agent delegation.
M9 removal, strict rejection of legacy workflow fields, the post-removal M8 rerun and final
local M10 acceptance remain required. Performance (#204), local PHP validation (#205) and
production-environment qualification (olo-mono #19) retain their user-approved deferrals.
