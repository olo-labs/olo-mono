@echo off
REM Copyright (c) 2026 Olo Labs
REM SPDX-License-Identifier: Apache-2.0
REM Publish shared library jars to olo-mono/build/repo (local Maven layout).
setlocal enabledelayedexpansion

cd /d "%~dp0"

set FAILED=0
for %%m in (olo-spi olo-annotation olo-annotation-processor) do (
  echo Publishing %%m to build\repo ...
  pushd "%%m"
  call gradlew.bat -PoloPublishBuildDir=../build/publish-work/%%m publishMavenPublicationToOloMonoRepository -x test
  if errorlevel 1 set FAILED=1
  popd
)

if !FAILED! neq 0 (
  echo publish-libs failed.
  exit /b 1
)

echo.
echo OLO libs published to %~dp0build\repo
endlocal
exit /b 0
