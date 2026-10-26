#!/usr/bin/env bash
set -eux
GRADLE_INIT="$GITHUB_WORKSPACE/olo-ui/scripts/olo-mono-ci.init.gradle"
GRADLE_FLAGS=(-I "$GRADLE_INIT" --no-daemon -x test)
for script in olo-spi/gradlew olo-annotation/gradlew \
  olo-annotation-processor/gradlew olo-definition/gradlew \
  olo-workflow-input/gradlew olo-bootstrap/gradlew \
  olo-kernel-context/gradlew olo-kernel/gradlew \
  olo-core/gradlew; do
  chmod +x "$script"
done
publish() {
  local module="$1"
  shift
  local task="publishMavenPublicationToOloMonoRepository"
  if [ $# -gt 0 ]; then
    task="$1"
  fi
  echo "Publishing $module to build/repo ..."
  (cd "$module" && ./gradlew "${GRADLE_FLAGS[@]}" \
    -PoloPublishBuildDir=../build/publish-work/"$module" \
    "$task")
}
for module in olo-spi olo-annotation olo-annotation-processor; do
  publish "$module"
done
publish olo-definition :olo-definition:publishMavenPublicationToOloMonoRepository
for module in olo-workflow-input olo-bootstrap olo-kernel-context; do
  publish "$module"
done
test -f build/repo/org/olo/olo-spi/0.1.0-SNAPSHOT/maven-metadata.xml
test -f build/repo/org/olo/olo-annotation/0.1.0-SNAPSHOT/maven-metadata.xml
test -f build/repo/org/olo/olo-definition/0.1.0-SNAPSHOT/maven-metadata.xml
test -f build/repo/org/olo/olo-workflow-input/0.1.0-SNAPSHOT/maven-metadata.xml
test -f build/repo/org/olo/olo-bootstrap/0.1.0-SNAPSHOT/maven-metadata.xml
test -f build/repo/org/olo/olo-kernel-context/0.1.0-SNAPSHOT/maven-metadata.xml
cd olo-core
./gradlew "${GRADLE_FLAGS[@]}" dist
mkdir -p "$GITHUB_WORKSPACE/olo-ui/olo-be/src/main/resources/static/catalog"
cp dist/catalog/*.json "$GITHUB_WORKSPACE/olo-ui/olo-be/src/main/resources/static/catalog/"
test -f "$GITHUB_WORKSPACE/olo-ui/olo-be/src/main/resources/static/catalog/catalog.json"
mkdir -p "$GITHUB_WORKSPACE/olo-ui/olo-be/docker/runtime-configuration"
if [ -x "$GITHUB_WORKSPACE/olo-ui/scripts/bundle-docker-configuration.sh" ]; then
  "$GITHUB_WORKSPACE/olo-ui/scripts/bundle-docker-configuration.sh"
elif [ -d "olo-definition/olo-configuration/default" ]; then
  echo "Bundling from default + scenario collections (current-active is manual-only)"
  DEST="$GITHUB_WORKSPACE/olo-ui/olo-be/docker/runtime-configuration"
  CONFIG_ROOT="olo-definition/olo-configuration"
  find "$DEST" -mindepth 1 ! -name README.md -exec rm -rf {} + 2>/dev/null || true
  cp "$CONFIG_ROOT/default"/*.json "$DEST/"
  for scenario in research-planner travel-planner dynamic-graph-creation; do
    if [ -d "$CONFIG_ROOT/$scenario" ]; then
      mkdir -p "$DEST/$scenario"
      cp "$CONFIG_ROOT/$scenario"/*.json "$DEST/$scenario/" 2>/dev/null || true
    fi
  done
  cp "$CONFIG_ROOT/dynamic-graph-creation/dynamic-graph-creation.json" "$DEST/" 2>/dev/null || true
else
  echo "::warning::No workflow JSON in olo-mono checkout; using committed olo-be/docker/runtime-configuration"
  ls -la olo-definition/olo-configuration 2>/dev/null || ls -la olo-definition/ 2>/dev/null || true
fi
test "$(find "$GITHUB_WORKSPACE/olo-ui/olo-be/docker/runtime-configuration" -name '*.json' | wc -l)" -gt 0
