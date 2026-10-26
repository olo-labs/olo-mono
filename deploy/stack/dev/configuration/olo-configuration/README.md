# Fresh workflow configuration

These presets require a fresh installation and the governed ToolGate platform services.
No legacy tool registries, local conversation files, migration readers, or resumed old executions are supported.

`default/` contains chat roles; `governed/governed-chat.json` is the backend chat pipeline.
Conversation history and RAG are deterministic user-authorized platform operations, independent of agent selection.
`incident-recovery/` contains a commander and three delegated business-tool agents. Business tools require explicit user and actor grants in ToolGate.
`current-active/agent.json` is the local editor selection.

Generate a new configuration set with `:olo-definition:generateFreshConfiguration` in the `olo-definition` build.
The generator writes to an empty build staging folder; review and copy the resulting presets for a new installation.
Use the monorepo `olo-knowledge/qualification.ps1` entry point for the isolated governed qualification stack.
