# Governed memory and RAG operations

Fresh installations and new workflows only. Memory and RAG authorize the signed-in user through ToolGate;
agents cannot grant access. The services own their separate PostgreSQL credentials. Treat the application stores,
ToolGate invocation/approval/permit state, Temporal history and payload keys as one recovery boundary.

## Start and stop a local qualification

From this repository, follow the prerequisites and source-build instructions in the README. Use a dedicated
Python installation with `cryptography`, Java 21, Docker Desktop Linux containers and kubectl.

```powershell
.\qualification.ps1 start -BuildImages -BuildPipelineImages -ToolGateSource C:\src\olo-toolgate -BackendSource C:\src\olo -WorkerSource C:\src\olo-mono
.\qualification.ps1 test -FullStack -Pipeline
.\qualification.ps1 test -Incident -IncidentPackage C:\src\olo-toolgate\examples\mocked-tools\server-recovery\build\toolgate\server-recovery-0.1.0.tgpkg
.\qualification.ps1 stop
```

The incident archive must come from the official `examples/mocked-tools/server-recovery` source, built with
the matching Java SDK and ToolGate CLI. Compile/stage with `:server-recovery:toolgatePackage` from
`examples/mocked-tools`; the SDK task invokes `toolgate pack`. Record source revision and archive SHA-256.
The test provisions exact user grants, separate agent capability ceilings, delegations and actor credentials.
Its test model chooses calls from the actual returned catalog; the real Tool Host runs the fictional tools.

For a database-only restore/rotation and unknown-outcome drill, use `qualification.ps1 test -Operations`.
It does not qualify coordinated ToolGate/Temporal recovery or every key rollover. Its AES-GCM key exists only
for that process; production backups require the managed-key procedure below.

## Grant provisioning

Provision users and verified tenant/session bindings before enabling workflows. Create a grant whose resource
is the exact root `olo://<tenant>/conversation/<conversation-uuid>` or `olo://<tenant>/kb/<kb-uuid>`; a resource ID
does not establish ownership. A user's Team grant and the service's owner/member/document ACL must both allow it.
Use separate Teams or scoped grants when different users require different operations.

| Purpose | Explicit permissions and policy |
|---|---|
| Normal conversation turn | `memory.read`, `memory.append`, ALLOW, on the user's authorized conversation |
| Conversation administration | Separate `memory.delete`, `memory.share`, `memory.export` grants and policies |
| Retrieval | `rag.query` on the authorized KB; document-level ACL filtering still applies |
| Source lifecycle | Separate `rag.ingest`, `rag.update`, `rag.delete`, `rag.share` grants and policies |
| Scheduled indexing | Dedicated ToolGate service principal, a SERVICE grant for the exact KB and `rag.update`; no user impersonation or agent mapping |
| Model-selected business operations | HUMAN grants for the user plus CAPABILITY grants for each mapped agent's group and an applicable user-Team/agent-group delegation |

Do not grant `memory.*`, `rag.*`, all actions or arbitrary root-resource wildcards. Ordinary chat must not ASK.
Apply ASK independently to effects that need review; the approver Team grants review authority rather than runtime
data authority. Keep ordinary requesters outside super-administrator Teams so their test cannot use the documented
super-administrator review override. Revoke grants through Control and test already-loaded, derived and in-flight
content against the 30-second bound; removing a tool from discovery alone does not prove revocation.

Always run `stop`, including after a failure. It removes owned runtime containers, volumes, networks, images,
private keys and credentials. Shared dependency/model caches and sanitized reports remain. It refuses to adopt
an unowned environment. An incomplete cleanup retains ownership records for a retry. Do not prune unrelated
Docker resources. Save sanitized evidence outside generated `build/` before removing a workstation.

## Unknown outcomes and reconciliation

1. Stop the affected logical operation. Preserve its tenant, user, resource, logical operation ID, ToolGate
   invocation ID and exact request fingerprint. Never create a replacement invocation or ask the model to
   regenerate the same write.
2. Re-authenticate the user and verify their current grant and service ACL before inspecting the result.
   A credential or approval saved before revocation is insufficient. Never substitute an agent or service identity.
3. Through ToolGate, call `memory.read` or `rag.query` on the original root with:
   `{"type":"receipt","invocationId":"<original ToolGate invocation ID>"}`.
   `COMMITTED` proves the stored write committed; `NOT_FOUND` is inconclusive and does not permit redispatch.
4. Follow the ToolGate invocation's declared reconciliation mode. Use status lookup or the original downstream
   idempotency key only through ToolGate's reconciliation flow. `MANUAL` requires an operator settlement with
   independent review. Record evidence and the settlement; never edit invocation tables to force success.
   Memory/RAG write packages declare `MANUAL`, because the protected service permit is single-use. A downstream
   message/document idempotency key does not authorize issuing another write permit or automatic resubmission.
5. Resume only after durable settlement and a fresh authorization check. Unknown exports contain no receipt
   payload; a receipt is not permission to release exported data.

Approval waits keep the original invocation. Denial, cancellation, expiry or revocation terminates it without
dispatch. Approval must come from an eligible independent reviewer, and dispatch still checks current authority.

## Backup and restore

- Encrypt all backups with managed keys stored outside the backup and outside Temporal. Restrict access to the
  backup service identity, record access, test decryption and rotate wrapping keys. Set explicit retention with
  the data owner; do not keep indefinite ad hoc dumps.
- Coordinate a write quiescence or documented consistent recovery point across both service databases,
  ToolGate durable invocation/approval/permit state and Temporal persistence. Back up service schemas, source
  content, ACLs, versions, tombstones, receipts and working-state expiry. Include the protected payload-key
  versions needed to decrypt retained history. Use ToolGate's supported backup/restore procedure for its state.
- PostgreSQL backups must use a dedicated authorized backup role; never grant that role to the application.
  Restore into isolated empty stores with the same schema version, separate non-superuser service roles,
  forced row-level security and current secrets. Keep the restored stack disconnected from dispatch while
  validating integrity and reconciling in-flight invocations. Do not recover old permits as fresh authority.
- Rebuild a lost Qdrant index from retained SQL sources through authorized `rag.update` calls with
  `{"type":"reindex","docId":"<id>","force":true}`. Preserve document tombstones and ACL/version filters.
  Deleted sources must remain absent from retrieval after rebuilding. A vector snapshot alone is insufficient.
- Before reopening traffic, test two users and two tenants, exact receipt lookup, deleted-source exclusion,
  one completed-turn replay and decryption of retained history. Record actual recovery time and recovery point.
  A successful SQL restore alone does not qualify the complete application recovery boundary.

## Credential and key rotation

Keep secrets in the deployment secret manager and mount them as protected files. Do not put secrets in workflow
JSON, source control, command arguments or diagnostic reports.

| Credential | Rollover and verification |
|---|---|
| User exchange signing keys | Publish the new verification key before issuing with its key ID. Verify old unexpired and new credentials during overlap; then retire the old key after the maximum credential lifetime. Test revoked-session denial throughout. |
| Agent workload credentials | Rotate the Control workload binding/hash and epoch with the protected Gateway credential file and reload. Verify the new actor works only alongside a valid user and delegation; the old actor is denied after cutover. |
| Executor/service mTLS | Add the new CA to verifier trust, enroll/issue the replacement certificate and roll executors/listeners. Validate hostname, audience, permit and executor-key binding. Retire old trust only after all callers move. |
| Database role passwords | Use the database's supported rotation, update only the relevant service secret and restart/reload its connection boundary. Confirm new connections work and the old password cannot authenticate. Terminate old sessions according to the maintenance policy. |
| Service Control credentials | Provision the least-privilege replacement, update the service credential file and verify authenticated epoch/JWKS refresh and permit consumption. Revoke the old credential and confirm fail-closed behavior. |
| Temporal payload keys | Add a new key ID, make it active for new payloads on every client/worker, retain old keys for retained history and backups, then test old-history replay and new-history encryption. Remove an old key only after its last retained dependency expires. |

Use bounded overlap and independent review where ToolGate requires it. Do not retire a key while any retained
history, backup, credential or permit still legitimately depends on it. Record failed-old/successful-new evidence.

## Retention, diagnostics and release gates

Working-state TTL is checked on reads and swept periodically; expiration cannot depend solely on the sweeper.
Document deletion commits a tombstone before asynchronous vector cleanup. Define conversation/source retention,
legal retention requirements where applicable, backup expiry and physical erasure with the data owner. Never
purge ToolGate dedup state earlier than the accepted 37-day window or while an unresolved execution depends on it.

Diagnostics should contain operation IDs, status and timing. They must omit credentials, prompts, retrieved text
and model responses. Keep private test canaries out of saved transcripts and logs. Test redaction at every boundary.

This runbook is a procedure, not evidence that its drills have passed. On 2026-10-26 the user approved moving
production transport/at-rest qualification, broader credential/key rollover, coordinated restore and production
retention qualification to [olo-mono #19](https://github.com/olo-labs/olo-mono/issues/19). Performance acceptance
is closed for the integration plan and tracked separately in [ToolGate #204](https://github.com/olo-labs/olo-toolgate/issues/204).
Missing local PHP cross-language contract validation is deferred in
[ToolGate #205](https://github.com/olo-labs/olo-toolgate/issues/205); PHP is not a memory/RAG runtime dependency.
These are scope deferrals, not passing test results or production-readiness sign-off. The local fixture uses
development transport and a scripted IdP/model. Local authorization, revocation/freshness, retry/cancellation,
quotas, legacy-code removal and final clean-install regression remain required.
