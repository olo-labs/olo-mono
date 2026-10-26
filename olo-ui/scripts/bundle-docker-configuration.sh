#!/usr/bin/env bash
# Copy olo-mono workflow JSON (default + scenario collections) for Docker builds.
# Does not use current-active/ — that folder is for manual local testing only.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/olo-be/docker/runtime-configuration"
CONFIG_ROOT="$ROOT/../olo-definition/olo-configuration"
DEFAULT="$CONFIG_ROOT/default"

mkdir -p "$DEST"
find "$DEST" -mindepth 1 ! -name README.md -exec rm -rf {} + 2>/dev/null || true

if [ ! -d "$DEFAULT" ]; then
  echo "Missing workflow source: $DEFAULT" >&2
  exit 1
fi

echo "Bundling workflow presets from default + scenario collections"
cp "$DEFAULT"/*.json "$DEST/"
for scenario in research-planner travel-planner travel-Planner dynamic-graph-creation; do
  scenario_dir="$CONFIG_ROOT/$scenario"
  if [ -d "$scenario_dir" ] && [ -n "$(find "$scenario_dir" -maxdepth 1 -name '*.json' -print -quit)" ]; then
    target_name="$(basename "$scenario_dir" | tr '[:upper:]' '[:lower:]')"
    mkdir -p "$DEST/$target_name"
    cp "$scenario_dir"/*.json "$DEST/$target_name/"
  fi
done
if [ -f "$CONFIG_ROOT/dynamic-graph-creation/dynamic-graph-creation.json" ]; then
  cp "$CONFIG_ROOT/dynamic-graph-creation/dynamic-graph-creation.json" "$DEST/dynamic-graph-creation.json"
fi

count="$(find "$DEST" -name '*.json' ! -path '*/log/*' | wc -l | tr -d ' ')"
if [ "$count" = "0" ]; then
  echo "No workflow JSON files copied into $DEST" >&2
  exit 1
fi
echo "Bundled $count workflow preset(s) into $DEST"
