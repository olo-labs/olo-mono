@echo off
REM Copyright (c) 2026 Olo Labs
REM SPDX-License-Identifier: Apache-2.0
setlocal enabledelayedexpansion
echo.
echo Stopping Olo Chat UI (port 3000)...
set FOUND=0
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr :3000 ^| findstr LISTENING') do (
  taskkill /PID %%a /F >nul 2>&1
  echo Stopped process on port 3000. PID: %%a
  set FOUND=1
)
if !FOUND!==0 (
  echo No process found listening on port 3000.
) else (
  echo Chat UI stopped.
)
echo.
if not defined NONINTERACTIVE pause
