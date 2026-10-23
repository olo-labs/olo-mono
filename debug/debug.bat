@echo off
REM Copyright (c) 2026 Olo Labs
REM SPDX-License-Identifier: Apache-2.0
REM Wrapper so the debug stack can be started by double-click or from cmd.exe.
REM Usage: debug.bat [up|check|status|logs|collect|worker|model|down] [options]
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0debug.ps1" %*
set "EXIT_CODE=%ERRORLEVEL%"
if "%~1"=="" pause
exit /b %EXIT_CODE%
