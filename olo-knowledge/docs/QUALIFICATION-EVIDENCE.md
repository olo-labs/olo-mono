# Local qualification evidence

## Shared claim and outcome integration — 2026-11-01

The target adapter now commits a shared local claim and APPLYING before work,
then completes business SQL with a durable outcome record. The migration forces
tenant RLS, restricts binding/deletion privileges and guards state transitions.
The listener refuses unsafe/missing schema instead of falling back.

Qualification now exercises paused-owner recovery, two actual service JVMs with
one business mutation, writer termination, lost outcome acknowledgement,
Control refusal/mismatched response metadata, deadline expiry before vector
mutation and guarded SQL records. Component Control remains scripted; complete
Control/Gateway/deployment acceptance is separate.

Executed tools/test_services.py --with-vectors --with-tls --evaluation:
**32 passed, zero skips** (service-core 5, memory 10, RAG 17). Each real TLS
fixture runs two separate application JVMs sharing one service identity/database,
fences a paused claim owner, checks unavailable/other-claim/wrong-generation
Control replies, races one mutation, kills the writer, drops an outcome
acknowledgement and verifies recovery by the survivor. SQL tests exercise forced
tenant RLS, immutable bindings/tombstones and terminal-state guards. The RAG
deadline test allows embedding to finish but prevents the external vector write.

Both application distributions built; their installed SDK/contracts JAR hashes
match the qualified runtime artifacts. The full-stack provisioning script passes
syntax validation but its complete runtime qualification has not yet executed.
The retrieval evaluation again executes 200 documents/150 queries, recall@5 1.0,
precision@5 0.7333, zero unauthorized chunks and zero incorrect citations.

Known component parent: f885c89. Tested component SHA-256:
8d661624055f36d4ebb0b6da67c9a20f4679c6b0b4d96b7673b625fc0cd37c6a.
ToolGate known parent: 3665e52d7b8c622d4d776361eff0a564ad961c32.
Verifier source SHA-256:
3287fa65030eea04ccc0471f89939cc63312e700e67c487323b23858193453e1.
SDK JAR SHA-256:
16e961a02fc5b032578b8aecb1bf41daad0de157a2905c6552c7ce88307d35fe.
Contracts JAR SHA-256:
1daad128498a9dccd8a9bcf1969962213ff7d341a1716f8e52d732ea6fe28cc7.
All three module classpaths match those publications. ToolGate verify-only and
changed-contract/classpath/skipped-evidence negative controls pass.
Source selection follows the source recipe in the prior delivery.

## Signed effect metadata compatibility — 2026-11-01

The compatibility fixtures now include signed effect identity/generation for
bound writes. Gradle records the actual SDK/contracts runtime artifacts for each
module, and qualification compares those hashes with the selected publications.
The actual target adapter continues to use one-shot consumption; shared claim
orchestration and target outcome integration remain outstanding.

Executed tools/test_services.py --with-vectors --with-tls --evaluation:
**28 passed, zero skips** (service-core 2, memory 10, RAG 16). Dependencies are
real PostgreSQL/TLS/Qdrant/Ollama; component Control is scripted. The retrieval
evaluation again used 200 documents and 150 queries, with recall@5 1.0,
precision@5 0.7333, no unauthorized chunks and no incorrect citations.

Known component parent: 54d08504bdeec0fd5e62913b963c312e737a17bd.
Tested component source SHA-256:
a00ccb2722d9cf179235bd65bdc43338cb81d79b8970d87399cb65d0e437e260.
Digest selection follows the source recipe in the preceding delivery.
ToolGate known parent: afce2106b326f2296433507a56dd15c74dac47fe.
Verifier source SHA-256:
9b17d00509b4df64515c029fc0790c0400d43e0e2b873c885ccc53bc175754ac.
SDK JAR SHA-256:
8a574e87a9b75caf81d761d89a7fe15f0de6614b6a3dd900d9cd55c26b23939b.
Contracts JAR SHA-256:
1daad128498a9dccd8a9bcf1969962213ff7d341a1716f8e52d732ea6fe28cc7.
All three modules resolved those exact two runtime artifacts. ToolGate's
qualification wrapper verified the report and rejected changed contracts,
changed resolved-classpath and skipped-test evidence using temporary copies.
This component checkpoint does not rerun the full Control/Gateway stack or
production SLO qualification, and establishes no shared-claim integration.

## Bound HTTP target integration — 2026-10-31

The coordinated feature/zero-trust-execution-plan branch adds trusted transport
bindings to the shared memory/RAG HTTPS listener and upgrades the service library
and package plugin to ToolGate 1.0.0-dev.1. The verifier refuses unbound downgrade
before consumption. Queries remain prohibited by the endpoint; changed Accept
facts are independently fingerprint-checked. The accepted write then commits once.

Executed evidence:
- ToolGate base: 17b199e17005cb1b66878262fecea9714d028636 plus verifier source SHA-256
  866a71f71a09895c2c10e1f568b1e56c073fba592c952ded3ddf6f4f7b55b24d.
- SDK artifact SHA-256: 1007fb1878d376910d8578ff889d7287ec3d612d13eba8f556229d3c8081a35b.
- Knowledge base: 59c53d78ce8b1523c0f957d3bcf2594f51a3bc8d; tested component source SHA-256:
  dc45fad71d596a8db51f6b906e5c819611479dca711cc12324b41a03b513f160. Selection is sorted relative path, NUL, bytes for
  .java/.sql/.gradle/.kts/.py/.json files, excluding build/cache directories.
- tools/test_services.py --with-vectors --with-tls --evaluation passed:
  service-core **2**, memory **10**, RAG **16** tests; **28 passed, zero skips**.
- Real PostgreSQL/TLS/Qdrant/Ollama dependencies were used; Control is explicitly
  scripted in the component listener tests. Separate service JVMs verify actual
  accepted HTTP facts, ACLs, single-use consumption, receipts and revocation.
- Retrieval corpus: **200 documents, 150 queries**;
  recall@5 **1.0**, precision@5 **0.7333**,
  **0 unauthorized chunks**, **750 citations**,
  **0 citation errors**. Component-only query p50/p95:
  **57.56/67.32 ms**;
  these exclude Gateway and mTLS and are not whole-stack latency evidence.
- The memory/RAG SDK packages built and packed with Gradle 9.7.1 and the current
  ToolGate CLI. The service build/tests used Gradle 8.12 and Java 21.
- ToolGate's knowledge qualification entry point executed the complete run and
  verified source/SDK matches. Negative controls refused skipped or changed-source
  evidence. Reports remain ignored artifacts under build/qualification and
  ToolGate build/security/knowledge.

The test fixture now refreshes working SDK publications, records actual dependency
image/model digests and source pins, clears prior passing evidence before
provisioning and waits for final PostgreSQL TCP readiness. The deployment fixture
provisions trusted bindings before listener startup. That fixture's complete
Control/Gateway run and production SLO qualification have not been rerun by this
component checkpoint. No whole roadmap phase is marked complete.


## Consolidated monorepo runs — 2026-10-26

All OLO source and qualification scripts now live in `olo-mono`, including this directory.
ToolGate remains an independently versioned dependency. These runs use a scripted IdP/model
and development transport; the approved performance/PHP/production deferrals remain separate.

| Check | Evidence and outcome |
|---|---|
| Real commander/child workflow incident | `olo-qualification-9eb8283b8a48`: three completed Temporal children, hard worker stop during child ASK, original invocation resumed, seven tools once, correct completion audits and all approval negatives; private canary absent from application logs |
| Memory/RAG pipeline | `olo-qualification-12ac942e0a5f`: encrypted history, completed-turn restart replay, concurrent turns, agent-independent authority; in-flight source revocation excluded within 8.563 seconds, stored derived response denied, next model call blocked |
| Service component qualification | 10 memory and 15 RAG tests, zero failures/errors/skips, real PostgreSQL/TLS/Qdrant/Ollama dependencies |
| Retrieval corpus | 200 documents, 150 queries, recall@5 1.0, precision@5 0.7333, zero unauthorized chunks, 750 citations with zero errors; service component measurement |
| Backend / Studio | 87 backend tests with isolated Redis; 135 Studio tests and production build; zero test failures/skips |
| Local runtime removal | `0208373`: SPI 20, annotation 9, processor 27, kernel 138 and core 15 tests, zero failures/errors/skips |

The incident/pipeline images record backend `f3ead5a`, worker `8227a8d`, and ToolGate `297f3979`.
They precede local-runtime removal and do not replace the mandatory final rerun. Reports are
under `build/qualification/<run-id>/`, with image IDs/source labels and scoped assertions.

Consolidation had restored request body logging and omitted completed-turn reuse protection.
The backend now logs method, matched route template and status only, and Temporal rejects reuse
of an accepted turn ID after completion/failure. Bounded read recovery preserves one operation
ID while refreshing user credentials, without retrying denials or unknown outcomes.

M9 schema/workflow/Studio/deployment cleanup, child-summary revocation qualification and the
post-removal M8/M10 clean-install run remain open. Historical sections below preserve earlier evidence.

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


## Fenced actual-stack settlement (2026-11-01)

The owned actual Control/Gateway development fixture passed all functional gates
and independently joined memory and RAG committed claims/outcomes to actual Control
generation records. Runtime source: 1ad12d83c1d285614669c17e3ee2dd67933ff293;
image: sha256:cfd5fa9a28156bfd57d9d9ebfae60d9fc3fb5d0b3c1299d069f5c45c62aa8f11.
Component source SHA-256:
9552f0a7fecaf65c420540126beeb7e127efaea53c53c45662c676ba8ffc094c.
All 32 component tests passed without skips; nine qualification harness tests
passed. Missing/contradictory Control records, wrong generations, premature local
acknowledgements, changed source or installed JARs cannot reuse successful evidence.
The report records actual running image IDs and before/after source/JAR pins.

This supplements scripted-Control crash tests with actual normal-path settlement.
It does not establish production CNI, production revocation SLOs or complete
Control/Gateway partition/crash acceptance. Use the ToolGate qualification runner
with --full-stack; --verify-only --full-stack validates retained source-bound
evidence without repeating the fixture.


## Independent native-attempt scope review for ToolGate S18 (2026-11-03)

The current owned stack rerun passes 13 gates and joins three completed memory
writes and four RAG writes to actual Control generations, with no pending target
outcome delivery. All 32 PostgreSQL/TLS/vector/evaluation component cases and
30 tooling unit cases pass. Known source parent:
8e3f7a7141119b800cf10248ab2e2298bc8587dc; tested component code digest:
97258664c4b9426eb2fbf9f3a3c3581cb5042dd10cc22327a27fc839da2680be.

This fixture uses native Java forwarding packages, whose complete effect scope
is not automatically qualified as broker-only. It now records actual Control
revision-bound proposals and decisions from two independent fixture reviewers.
Only a sealed, successful consumed attempt with a complete journal, resolved
effects and no existing settlement is eligible. The successful run records 17
REVIEWER_CONFIRMED reviews. The fixture then re-reads the original logical
operation; ambiguous writes never acquire another execution ID or unsafe retry.

The existing target outcome checks remain independent: committed target claims,
authorization generations, result receipts and delivered outcomes must agree
with actual Control records. Neither a broker reply nor a reviewer attestation
can stand in for missing target completion evidence. Current ToolGate image
source binding and final refreshed image evidence are recorded in ToolGate's
authoritative issue-230 status document. These are owned development-fixture
observations; production native containment, CNI and full phase acceptance remain
outside this qualification.

## S20 current-only change under qualification

The fresh-build scope removes the additive claim-table upgrade, requires the
current schema identity and positive expiry, and gives each packaged write an
explicit stable effect key. Earlier evidence below/above describes its tested
revision; these S20 changes still require fresh component and governed-stack
qualification before delivery. No new qualification acceptance is claimed here.

The S20 current-only update also adjusts the memory/RAG HTTPS rejection fixtures.
They now construct current typed claims and deliberately remove mandatory
transport fields only from the signed malformed wire payload. This preserves
denial/no-consumption coverage without relying on removed compatibility
constructors. Requalification against the coordinated SDK and owned stack is
still pending; no new passing result is asserted here.


## S20 delivered current-only qualification — 2026-11-05

The S20 changes above are now qualified against the coordinated current ToolGate
SDK/contracts and fresh source-bound stack. No database upgrades, old schema
support, default write-key aliases or compatibility constructors are provided.
The fresh target schema requires toolgate-target-claims-v1 and positive expiry.
Memory and RAG packages give every logical write an explicit stable effect key.
Current signed malformed-payload fixtures still prove transport-binding refusal
before consumption.

Executed component qualification passed all **32 tests with zero skips**:
service-core 5, memory 10 and RAG 17, with PostgreSQL, TLS, vectors and retrieval
evaluation enabled. The actual owned Control/Gateway/Tool Host/memory/RAG stack
passed **13 gates** and confirmed **three memory and four RAG committed writes**
against the exact Control generations, with zero pending outcome delivery.
Original-operation replay, caller/tenant/ACL isolation, filtered retrieval,
versioned citations, deletion freshness, per-user quotas and live revocation are
exercised. Native fixture scope uses independent Control reviewers and retains
its REVIEWER_CONFIRMED grade; it does not establish native containment.

The first joined S20 run found a ToolGate broker placeholder refusing explicit
OLO keys. The coordinated broker fix removes that refusal while preserving
same-key/digest/handler single-use binding. The successful stack was rerun
after the fix; the 32 component cases were already passed on the unchanged
component source and SDK. ToolGate's verifier rechecked actual component source,
SDK/contracts, installed artifacts and runtime image against those executed
reports. No unexecuted component rerun is asserted.

Known knowledge parent: 63de7b94bbf2837aca570b124427cea5639a36ab.
Tested component code digest:
b6aa87dc556d03357bc4f015b8810b2854ef692221dd942b24ad189dc2ef6067.
Known ToolGate parent: 8da2b98d11bbba224f13b27bee85d2cfde4f9588.
ToolGate owned runtime-source digest:
2292274a0cb1515d215daff4267d3a52a418b5274eb6d086aa050707ea7e0620.
Owned stack image ID:
sha256:bf34b3ecc968f2500d9e51a40c87a7456b07aecd0a81c2ae0c21f367eec8029f.

Detailed reports remain in build/qualification/service-components.json and
build/qualification/real-stack.json, including installed artifact/service image
digests and target/Control evidence. Final documentation follows the tested code
checkpoint. The full issue-230 roadmap, generalized target fencing, production
SLOs and complete partition/crash/kill catalogs remain outstanding in ToolGate's
authoritative status document. This is one bounded stage delivery.
