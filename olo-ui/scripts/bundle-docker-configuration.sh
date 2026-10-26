#!/usr/bin/env bash
# Copy olo-mono workflow JSON (fresh chat and incident recovery) for Docker builds.
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

echo "Bundling workflow presets from fresh chat and incident recovery"
mkdir -p "$DEST/default"
cp "$DEFAULT"/*.json "$DEST/default/"
for scenario in governed incident-recovery; do
  mkdir -p "$DEST/$scenario"
  cp "$CONFIG_ROOT/$scenario"/*.json "$DEST/$scenario/"
done

count="$(find "$DEST" -name '*.json' ! -path '*/log/*' | wc -l | tr -d ' ')"
if [ "$count" = "0" ]; then
  echo "No workflow JSON files copied into $DEST" >&2
  exit 1
fi
echo "Bundled $count workflow preset(s) into $DEST"
