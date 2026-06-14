@echo off
REM Copyright (c) 2026 Olo Labs
REM SPDX-License-Identifier: Apache-2.0
setlocal enabledelayedexpansion

echo.
echo Stopping OLO Temporal worker (port 8080)...
set FOUND=0
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr :8080 ^| findstr LISTENING') do (
  taskkill /PID %%a /F >nul 2>&1
  echo Stopped process on port 8080. PID: %%a
  set FOUND=1
)
if !FOUND!==0 (
  echo No process found listening on port 8080.
) else (
  echo Worker stopped.
)
echo.
if not defined NONINTERACTIVE pause
endlocal
