"""Reject retired local execution APIs and shipped workflow contracts in a fresh checkout."""
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
RETIRED = {"tools", "toolCalls", "tool_requirements", "toolRequirements", "toolCallPlanner", "selectedTools", "injectCapabilities", "availableToolsVariable", "dynamicToolExecution", "aggregateToolResult", "toolSynthesis", "orchestration"}

def validate(value, source):
    if isinstance(value, dict):
        for key, child in value.items():
            if key in RETIRED or (key == "type" and isinstance(child, str) and child.strip().upper() == "TOOL"):
                raise ValueError(f"{source}: unsupported workflow field/type: {key}")
            validate(child, source)
    elif isinstance(value, list):
        for child in value:
            validate(child, source)
    elif isinstance(value, str) and "{availableToolsJson}" in value:
        raise ValueError(f"{source}: unsupported workflow placeholder")

def main():
    count = 0
    for folder in ["olo-definition/olo-configuration", "deploy/stack/dev/configuration/olo-configuration", "olo-ui/olo-be/docker/runtime-configuration"]:
        for path in (ROOT / folder).rglob("*.json"):
            validate(json.loads(path.read_text(encoding="utf-8")), path.relative_to(ROOT))
            count += 1
    retired_api = re.compile(r"org\.olo\.spi\.tool|@OloTool\b|class ToolDescriptor\b|NodeType\.TOOL\b|\bToolNodeTypeHandler\b|\bToolCallPlannerSupport\b|\bAvailableToolsJsonResolver\b|\bToolRegistry\b|\bToolDefinition\b|\bToolChainMerger\b|\bParsedToolCall\b|\bToolSynthesisSupport\b|\bWorkflowOrchestrationDefinition\b")
    for module in ROOT.glob("olo*"):
        if not module.is_dir():
            continue
        for path in module.rglob("*.java"):
            if "/src/main/" not in path.as_posix() or any(part in {"build", "node_modules", ".gradle"} for part in path.parts):
                continue
            if retired_api.search(path.read_text(encoding="utf-8")):
                raise ValueError(f"Retired local execution API: {path.relative_to(ROOT)}")
    for folder in ["deploy/stack/dev/configuration/olo-catalog", "olo-ui/olo-be/src/main/resources/static/catalog"]:
        if (ROOT / folder / "tools.json").exists():
            raise ValueError(f"Retired tools catalog: {folder}")
    print(f"Fresh-install contract passed for {count} shipped workflows and production Java sources")

if __name__ == "__main__":
    main()
