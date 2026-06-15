@echo off
REM Copyright (c) 2026 Olo Labs
REM SPDX-License-Identifier: Apache-2.0
setlocal

cd /d "%~dp0"

echo.
echo ========================================
echo   OLO Temporal worker (olo-mono)
echo ========================================
echo   Worker:   http://localhost:8080
echo   Temporal: localhost:47233
echo   LLM:      http://localhost:11435 (override with OLO_LLM_BASE_URL)
echo   Config:   ..\olo-worker-configuration\samples\worker-config.local-debug.yaml
echo ========================================
echo.

if not defined OLO_LLM_BASE_URL set "OLO_LLM_BASE_URL=http://localhost:11435"

call :EnsureJava
if errorlevel 1 (
    pause
    exit /b 1
)

call "%~dp0..\publish-libs.bat"
if errorlevel 1 (
    pause
    exit /b 1
)

call gradlew.bat run
pause
exit /b %ERRORLEVEL%

:EnsureJava
for %%v in (21 22 23 24 25) do (
  if exist "%ProgramFiles%\Microsoft\jdk-%%v" (
    for /d %%d in ("%ProgramFiles%\Microsoft\jdk-%%v*") do (
      set "JAVA_HOME=%%~d"
      set "PATH=%%~d\bin;%PATH%"
      goto :JavaReady
    )
  )
)
where java >nul 2>&1
if not errorlevel 1 goto :JavaReady
echo ERROR: Java not found. Install JDK 21+.
exit /b 1
:JavaReady
exit /b 0
