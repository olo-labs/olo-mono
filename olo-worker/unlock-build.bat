@echo off
REM Copyright (c) 2026 Olo Labs
REM SPDX-License-Identifier: Apache-2.0
REM Stop Gradle daemons that share olo-spi / olo-annotation jar outputs (Windows file-lock fix).
setlocal

set "WORKER=%~dp0"
set "LABS=%WORKER%..\..\"
set "OLO=%LABS%olo"
set "OLO_BE=%LABS%olo-ui\olo-be"

echo.
echo Releasing shared composite-build jar locks (olo-spi, olo-annotation)...
echo.

cd /d "%WORKER%"
if exist gradlew.bat (
  echo [olo-worker]
  call gradlew.bat --stop
)

if exist "%OLO%\gradlew.bat" (
  echo [olo backend]
  pushd "%OLO%"
  call gradlew.bat --stop
  popd
)

if exist "%OLO_BE%\gradlew.bat" (
  echo [olo-ui olo-be]
  pushd "%OLO_BE%"
  call gradlew.bat --stop
  popd
)

echo.
echo Done. Retry your Gradle build or IDE run.
echo If jars are still locked, stop the olo backend (port 7080) and run this script again.
echo.
if not defined NONINTERACTIVE pause
endlocal
