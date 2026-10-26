# Governed fresh installation

The old local-tool/demo-data deployment has been removed. Use a new installation, empty memory and RAG databases, and newly generated workflow presets. Old executions and data have no supported reader or migration.

For the repeatable local acceptance environment, start from the monorepo root:

```powershell
$env:TOOLGATE_SOURCE = 'D:\olo\olo-toolgate'
.\olo-knowledge\qualification.ps1 start -BuildImages -BuildPipelineImages -ToolGateSource $env:TOOLGATE_SOURCE
.\olo-knowledge\qualification.ps1 test -FullStack -Pipeline
.\olo-knowledge\qualification.ps1 test -Delegation
.\olo-knowledge\qualification.ps1 test -Incident -IncidentPackage '<official server-recovery.tgpkg>'
.\olo-knowledge\qualification.ps1 test -Operations
.\olo-knowledge\qualification.ps1 stop
```

The prerequisites, package build, identities, exact user/actor/service-principal grants, private key provisioning and ownership-safe cleanup are documented in `olo-knowledge/README.md` and `olo-knowledge/docs/OPERATIONS.md`. The fixture provisions real ToolGate, separate memory/RAG executors and databases, Temporal, Redis, vector search and a scripted test model. It creates new workflows and removes all owned runtime resources on stop. Performance, PHP foundation validation and production-environment qualification remain the separately accepted follow-ups.

The generic development Compose stack requires an externally provisioned governed stack. Set `OLO_TOOLGATE_MCP_URL`, `OLO_TOOLGATE_CONTROL_URL` and the backend exchange/assertion credentials and trust roots described in the operations guide; it cannot bootstrap trusted credentials from defaults. `toolgate.agentMappings` is empty for platform chat. Add explicit server-owned actor mappings only for delegated business tools. Keep database, embedding and vector-store credentials out of backend/worker configuration.

The new Chat UI uses `/api/v1/conversations` and `/api/v1/knowledge`. It reads content only through live user authorization. Its Redis directory contains opaque references only, scoped by verified issuer, tenant and user. It never grants access to messages or documents. Human workflow input and cancellation are separate from ToolGate ASK approval, which remains in ToolGate's reviewer flow.

Backend and worker mount the generated `olo-ui/olo-be/docker/runtime-configuration` set through `OLO_RUNTIME_WORKFLOW_SOURCE`. It contains one copy of each workflow. The Studio catalog and `current-active` editor files are separate and are never recursively loaded as duplicate runtime definitions.
