# Implementation checkpoint — 2026-10-24

Fresh installations only. These changes are local working-tree implementation and validation, not merged milestone credit.

## Review fixes

- Model-selected tools require a verified user and the mapped agent's actor credential. A worker refuses to enable
  ToolGate without `userCredentialsUrl`, or with `delegatedAgents=false`. Direct-user clients remain available for the
  separate deterministic platform path; the model loop cannot use them.
- Exchange assertions include the run tenant. The backend validates the returned token's tenant and expiry before
  returning it to a worker. A backend connected to a different Control tenant refuses the request; it does not return
  that tenant's credential. This does not add dynamic cross-tenant Gateway routing.
- Session records retain the expiry established by verified authentication. A later verified token can extend it;
  a binding reference cannot. Revoked sessions are never revived by renewal.
- Revocations use a Redis-backed notification outbox, with five-second retry and event-specific acknowledgement.
  Pending notifications block new credentials for that user. Assertions and issued credentials are capped at fifteen
  seconds, leaving headroom inside the thirty-second revocation bound even if notification delivery fails.
- A Gateway JSON-RPC error containing `OUTCOME_UNKNOWN` remains an unknown outcome with its invocation reference.
  It cannot become an ordinary model-visible failure that invites a second effect under a new operation ID.
- Root and child workflow IDs have explicit prefixes: `run-<tenant>~<id>` and `child-<tenant>~<id>`.
  Tenant names such as `run-t1` round-trip correctly.

## Additional M5 work

- Dedup recovery uses the accepted 37-day retention window (30 days plus seven days of approval expiry).
- Approval expiry is checked before replaying the call, including an approval received after the local deadline.
- Terminal failed, cancelled, expired and partial invocations end the turn; the model cannot automatically issue
  another effect in response to one of those terminal states.
- Tenant payloads refuse cleartext and missing tenant keys; an unmapped tenant cannot fall back to the platform key.
- Failure messages and stack traces use Temporal's encoded failure attributes, in addition to encrypted payloads.
- The backend has optional Temporal remote-codec endpoints at `POST /api/codec/decode` and `/encode`, enabled by
  `OLO_CODEC_SERVER_ENABLED=true`. They require the verified `olo-admin` role and membership in the selected tenant.
  Configure `OLO_PAYLOAD_KEYS_FILE` on that backend. Only that authenticated tenant's payloads may be decoded;
  platform payloads are not exposed. Responses are not cached, and codec request bodies are excluded from request logs.
  Configure an authenticated UI/CLI integration; do not publish an anonymous decryption endpoint.

## Validation

- ToolGate client regression suite, including the actual Gateway unknown-outcome error shape.
- Workflow input and payload codec suite, including failure-message encryption and missing-key rejection.
- Kernel regression tests and the production Temporal workflow with its test server: loss of an activity result,
  retry with the same recorded operation, one downstream effect, approval timer/resume, and encrypted history.
- Worker startup and configuration suites.
- Complete backend suite, including credential/session checks and codec endpoint authorization.
- Isolated real Redis tests: concurrent session creation, verified expiry persistence, and durable outbox acknowledgement.

The Temporal tests substitute the LLM and Gateway with controlled test integrations. They do not replace M8's real
ToolGate/PostgreSQL/Qdrant stack qualification or the thirty-second revocation measurement.

## Remaining sequence

1. M5.6: propagate cancellation to ToolGate; qualify durable execution in parallel branches and inline paths.
2. M4.6: implement downstream service-permit issuance/consumption and the revocation-feed bridge in ToolGate.
   The current executor-bound permit cannot simply be passed to a storage service with another audience.
3. M6 plus M4.5/M4.6 qualification: build `olo-knowledge` memory/RAG services and ToolGate packages against that bridge.
4. M7: deterministic platform-call integration and conversation/document lifecycle.
5. M8, M9, M8 again, M10: full-stack qualification, removal of local tools, and clean-install/operations checks.

No migration, legacy reader, compatibility mode, or old-execution support is introduced.
