# Live OLO UI tours — October 27, 2026

The root README has two animated tours: [OLO Chat](olo-chat-tour.gif) and
[OLO UI](olo-ui-tour.gif). The original browser captures are linked below at full
resolution. `screenshots.json` records their page URLs, capture times and SHA-256
checksums; `verify_captures.py` checks integrity and required page/state coverage.

These are real application screens from a disposable local environment with
fictional data. The identity provider is a test fixture. Deterministic acceptance uses
a scripted model; interactive Ollama captures are identified separately.
ToolGate, its Java tool packages, memory/RAG storage and Temporal execution are real.
Scheduled pages show the current placeholder UI, not completed functionality.

## Authentication and environment

OLO Chat has no native sign-in screen in this revision. The screenshots use the
user-approved **test sign-in gateway**, which verifies the dedicated demo account
through ToolGate and supplies a server-bound fixture identity to OLO. Credentials
are held in ignored runtime state and are never embedded in client builds or images.

Operator Insights remains on its separate mTLS listener. A separate loopback test
gateway holds a short-lived operator certificate from an ephemeral operator CA.
Its sign-in page is labelled as test infrastructure; tenant credentials alone
cannot access the operator listener. This is not production authentication qualification.

The captured clients are built from monorepo main `05e8552`. ToolGate includes the
Analytics and tool-drilldown changes merged through `6732c00e`, plus the HTTP conflict
response correction in the coordinated ToolGate PR. The published backend and worker
images both identify monorepo `05e8552` and are copied into owner-labelled test images.
Exact image identities and the acceptance result are recorded with the capture evidence.

## Repeat the walkthrough

### One-click reload on Windows

On this machine, double-click [`scripts/ui-tour.bat`](../../scripts/ui-tour.bat)
to start or resume the snapshot stack. It reuses an already running tour; otherwise it
prepares the current sources, reruns deterministic acceptance, and starts Chat,
Studio, ToolGate and Temporal with the configured Ollama model.

Double-click [`scripts/ui-tour-reload.bat`](../../scripts/ui-tour-reload.bat) for
a fresh run. It stops and cleans the previous tour, then rebuilds and starts a new
one. Use this after temporary actor credentials expire or when replaying the whole
use case. To continue photographing the existing run without discarding it, use
the start/resume launcher instead.

On another checkout, first copy [`ui-tour.example.json`](../../scripts/ui-tour.example.json)
to `scripts/ui-tour.local.json` and set `ToolGateSource` and `Python`. Relative
paths resolve from the monorepo root. Optional `Gradle8`, `Gradle9` and
`IncidentPackage` settings select installed build tools/artifacts. The local file
is ignored by Git and must contain paths/model settings only, never credentials.
Explicit command-line arguments override local settings; `TOOLGATE_SOURCE` and
`OLO_QUALIFICATION_PYTHON` override their corresponding local values.

PowerShell 7, Docker Desktop's Linux engine, Node.js, Java, and Python with
`cryptography` are prerequisites. Start the development Ollama service on port
11435 with `qwen2.5:7b` installed before starting the tour. It remains a separately
managed service and is not deleted by tour cleanup.

Double-click [`scripts/ui-tour-stop.bat`](../../scripts/ui-tour-stop.bat) to remove
the tour's owned resources, storage, private credentials and test images. Run
`scripts\ui-tour.bat status` to show the current endpoints. Use the same launcher
again after stopping; the default one-click start includes `-Prepare`, so deleted
test images are rebuilt. Startup takes several minutes on a cold installation.
Stopping discards this disposable run; the next start generates new test IDs and
credentials, so retake related screens from that new run rather than reusing old
resource references. Sanitized reports and committed snapshots are retained.

First complete the image, SDK, service-distribution and official incident-package
prerequisites in [the qualification guide](../../olo-knowledge/README.md). Use a
Python environment with `cryptography`, Node.js, Java and the documented Gradle versions.
Reserve loopback ports 58000, 58001, 58002 and 58082. Existing user services are not adopted.

Use the committed monorepo wrapper to prepare and start this exact environment:

```powershell
.\scripts\ui-tour.ps1 start -Prepare -ToolGateSource D:\path\to\olo-toolgate -Python D:\path\to\python.exe
.\scripts\ui-tour.ps1 status
```

For the interactive open-source model demonstration, first run
`deploy\stack\install.bat` and select the additional `qwen2.5:7b` text model.
The development stack exposes Ollama at `http://localhost:11435`. Supply its address
as seen from the worker container, together with the installed model name:

```powershell
.\scripts\ui-tour.ps1 start -ToolGateSource D:\path\to\olo-toolgate -Python D:\path\to\python.exe -OllamaUrl http://host.docker.internal:11435 -OllamaModel qwen2.5:7b
```

The deterministic approval/recovery acceptance runs first. The interactive worker
then switches to the specified Ollama model. Its generated responses and tool choices
must be verified separately; the deterministic acceptance report does not qualify
the model's behavior. The externally installed Ollama server is not owned or deleted
by the disposable tour. Stop the development stack separately when finished.

The GPU walkthrough uses the short [fictional incident runbook](incident-runbook.md)
as a single document and retrieved passage. Agent answers are kept concise to
limit inference work on the four Quadro P4000 cards. Browser captures wait for
the requested state to finish loading, then pause for two seconds before saving.

`-Prepare` builds the current clients, Studio backend, SDKs, service distributions,
native ToolGate runtime, sealed core packages and official incident package. It also
verifies the published backend/worker image revisions against the runtime source.
The default Gradle wrappers use 8.12 and 9.7.1; optional `-Gradle8` and `-Gradle9`
arguments select installed executables. Repeat starts may omit `-Prepare` to reuse
the prepared artifacts. This is a development build, not a performance benchmark.

The helper first runs real incident, approval and recovery acceptance. Only then
does it publish the browser endpoints. Open Chat at `http://localhost:58000`, Studio
at `http://localhost:58001`, and the operator test gateway at `http://localhost:58002`.
The ignored `olo-knowledge/build/qualification/runtime/gallery-session.json` holds
the generated test credentials and the dynamic ToolGate/Temporal URLs. Never commit it.
Use the dedicated requester for Chat and the independent reviewer for ToolGate approval.

To stop, from another terminal:

```powershell
.\scripts\ui-tour.ps1 stop
```

The tour removes its services, storage, network, Studio process and private runtime
directory. The qualification stop command also removes owned staging resources and
images. Dependency/build caches and sanitized reports remain. Unrelated installations
are outside the ownership boundary.

To verify the committed media and rebuild the GIFs (Pillow required for GIF generation):

```powershell
python docs/media/verify_captures.py --integrity-only
python docs/media/build_tours.py
```

Omit `--integrity-only` to enforce every required page and UI state. That strict
check intentionally fails until the pending captures listed below are collected.

## Walkthrough coverage

The saved walkthrough follows the signed-in requester from adding an incident runbook,
through a cited knowledge search, to incident identification and independent recovery
approval. The final native recovery result and remaining views still need capture.
The pipeline builder shows components, typed variables, the Ollama provider and the
three delegated agents. Business-tool access is selected and constrained by ToolGate;
conversation memory and retrieval remain user-authorized platform operations.

The requester and independent reviewer use separate browser sessions. Recovery
evidence includes ToolGate approval views and the diagnosis's Temporal child view.
The final parent/child recovery views and Studio post-execution audit are pending.
Studio pages marked
Scheduled remain placeholders and are documented that way.

Original images are checked for checksum consistency before gallery generation.
The gallery builders also reject blank images. Each capture still requires a visual
review for loading indicators, errors, clipped menus and readable results.

## Current capture refresh

**In progress:** 35 fresh capture entries from the October 27 GPU-backed walkthrough. Older captures and the accidentally expired sign-in view are excluded from the new sliders. The capture session ended before every view was collected; the remaining views are listed below. This is not a complete end-to-end model qualification.

**Model:** Qwen2.5 7B (`qwen2.5:7b`) through Ollama 0.40.11 on four NVIDIA Quadro P4000 GPUs, driver 582.99. RAG uses one short fictional runbook and one retrieved passage. Every new capture pauses for two seconds after the requested page/state loads.

A native diagnosis completed and was read from governed history after a reload; [Chat refresh defect #25](https://github.com/olo-labs/olo-mono/issues/25) tracks that UI issue. The model also produced unsupported diagnosis claims and initially invalid delegation JSON; [model qualification #26](https://github.com/olo-labs/olo-mono/issues/26) tracks these separately. The retry reached a real independent ToolGate approval. Its final recovery result was not captured. Approval and tool-call screenshots are actual records; model-generated prose requires review.

The separate scripted acceptance passed real authorization, worker-crash recovery, three Temporal child workflows and approval terminal checks. That evidence does not qualify native model behavior. Operator session `ui-tour-fixture-checks` contains caller-declared acceptance correlations, not verified Chat session identity. Scheduled Studio pages remain placeholders.

[Capture integrity and remaining coverage](capture-status.json) · [Capture manifest](screenshots.json)

## Fresh snapshot index

| Screen | View |
| --- | --- |
| [chat-sign-in](screenshots/chat-sign-in.jpg) | Clearly labelled test gateway; interactive responses use Ollama |
| [chat-running](screenshots/chat-running.jpg) | Live Ollama diagnosis running with the selected small runbook |
| [console-sign-in](screenshots/console-sign-in.jpg) | ToolGate tenant console sign-in |
| [console-overview](screenshots/console-overview.jpg) | Loaded tenant overview and scoped demo directories |
| [console-approvals-pending](screenshots/console-approvals-pending.jpg) | Live Ollama remediation waiting for independent approval |
| [console-approval-review](screenshots/console-approval-review.jpg) | Independent reviewer checks the exact delegated remediation invocation |
| [temporal-running](screenshots/temporal-running.jpg) | Live Ollama diagnosis running in Temporal |
| [console-approval-approved](screenshots/console-approval-approved.jpg) | Independent approval recorded; original invocation resumes through ToolGate |
| [studio-builder](screenshots/studio-builder.jpg) | Complete incident pipeline and delegated-agent connections |
| [studio-overview](screenshots/studio-overview.jpg) | Overview · current scheduled placeholder |
| [studio-debugger](screenshots/studio-debugger.jpg) | Debugger · current scheduled placeholder |
| [studio-templates](screenshots/studio-templates.jpg) | Templates · current scheduled placeholder |
| [studio-versions](screenshots/studio-versions.jpg) | Versions · current scheduled placeholder |
| [studio-executions](screenshots/studio-executions.jpg) | Executions · current scheduled placeholder |
| [studio-observability](screenshots/studio-observability.jpg) | Observability · current scheduled placeholder |
| [studio-extensions](screenshots/studio-extensions.jpg) | Extensions · current scheduled placeholder |
| [studio-tenants](screenshots/studio-tenants.jpg) | Current tenant administration |
| [studio-tenant-create](screenshots/studio-tenant-create.jpg) | Tenant creation form · inspection only |
| [studio-scenarios](screenshots/studio-scenarios.jpg) | Current scenario administration |
| [studio-agents](screenshots/studio-agents.jpg) | Four incident workflows in the current active catalog |
| [studio-agent-context-menu](screenshots/studio-agent-context-menu.jpg) | Fitted agent canvas with the full context menu inside the viewport |
| [studio-builder-components](screenshots/studio-builder-components.jpg) | Components palette after dragging the complete incident canvas into focus |
| [chat-rag-document](screenshots/chat-rag-document.jpg) | One short fictional runbook; user-scoped knowledge ingestion |
| [chat-rag-ingested](screenshots/chat-rag-ingested.jpg) | Short runbook saved as document version 1 |
| [chat-rag-retrieval](screenshots/chat-rag-retrieval.jpg) | One short retrieved passage, document version and complete source citation |
| [chat-rag-selected](screenshots/chat-rag-selected.jpg) | Short governed runbook selected for GPU-backed incident diagnosis |
| [studio-ollama-provider](screenshots/studio-ollama-provider.jpg) | Ollama provider: Qwen2.5 7B and the complete incident-agent graph |
| [console-analytics](screenshots/console-analytics.jpg) | Tenant Analytics · actual memory, RAG and incident tool calls |
| [console-analytics-tool](screenshots/console-analytics-tool.jpg) | Remediation tool drilldown · actual calls and outcomes |
| [chat-knowledge-create](screenshots/chat-knowledge-create.jpg) | Knowledge creation route with the current governed collection |
| [chat-knowledge-status](screenshots/chat-knowledge-status.jpg) | Knowledge status route and verified source version |
| [chat-documents](screenshots/chat-documents.jpg) | Documents route uses the governed knowledge document interface |
| [temporal-diagnosis-child](screenshots/temporal-diagnosis-child.jpg) | Live diagnosis delegates only the triage child agent |
| [chat-incident-identification](screenshots/chat-incident-identification.jpg) | Actual Ollama-generated incident diagnosis after governed-history reload; model claims require review |
| [console-analytics-tools](screenshots/console-analytics-tools.jpg) | Actual tool-call counts for memory, RAG and the incident agents |

## Pending retakes

- `chat-conversation.jpg` — Retake in the current GPU-backed walkthrough; previous capture excluded.
- `temporal-parent-timeline.jpg` — Retake in the current GPU-backed walkthrough; previous capture excluded.
- `temporal-event-history.jpg` — Retake in the current GPU-backed walkthrough; previous capture excluded.
- `temporal-relationships.jpg` — Retake in the current GPU-backed walkthrough; previous capture excluded.
- `chat-completed.jpg` — Retake in the current GPU-backed walkthrough; previous capture excluded.
- `temporal-workers.jpg` — Retake in the current GPU-backed walkthrough; previous capture excluded.
- `studio-execution-log.jpg` — Retake in the current GPU-backed walkthrough; previous capture excluded.
- `studio-pipeline-variable.jpg` — Retake in the current GPU-backed walkthrough; previous capture excluded.
- `console-tool-picker.jpg` — Retake in the current GPU-backed walkthrough; previous capture excluded.
- `console-triage-tool-scope.jpg` — Retake in the current GPU-backed walkthrough; previous capture excluded.
- `chat-ollama-recovery.jpg` — required new state not captured.

Also finish the remaining Temporal detail tabs and successful operator invocation/attempt views.
