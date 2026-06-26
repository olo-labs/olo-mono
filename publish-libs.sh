#!/usr/bin/env bash
# Publish shared library jars to olo-mono/build/repo (local Maven layout).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
for module in olo-spi olo-annotation olo-annotation-processor; do
  echo "Publishing $module to build/repo ..."
  (cd "$module" && ./gradlew "-PoloPublishBuildDir=../build/publish-work/$module" publishMavenPublicationToOloMonoRepository -x test)
done
echo "Publishing olo-definition to build/repo ..."
(cd olo-definition && ./gradlew "-PoloPublishBuildDir=../build/publish-work/olo-definition" :olo-definition:publishMavenPublicationToOloMonoRepository -x test)
echo "Publishing olo-workflow-input to build/repo ..."
(cd olo-workflow-input && ./gradlew "-PoloPublishBuildDir=../build/publish-work/olo-workflow-input" publishMavenPublicationToOloMonoRepository -x test)
echo "OLO libs published to $ROOT/build/repo"
