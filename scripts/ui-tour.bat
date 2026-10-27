@echo off
setlocal
rem Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
set "OLO_TOUR_PWSH="
for /f "delims=" %%P in ('where pwsh.exe 2^>nul') do if not defined OLO_TOUR_PWSH set "OLO_TOUR_PWSH=%%P"
if not defined OLO_TOUR_PWSH if exist "%ProgramFiles%\PowerShell\7\pwsh.exe" set "OLO_TOUR_PWSH=%ProgramFiles%\PowerShell\7\pwsh.exe"
if not defined OLO_TOUR_PWSH if exist "%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\powershell\pwsh.exe" set "OLO_TOUR_PWSH=%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\powershell\pwsh.exe"
if not defined OLO_TOUR_PWSH (
  echo PowerShell 7 is required. Install it, then run this launcher again.
  pause
  exit /b 1
)
if "%~1"=="" (
  "%OLO_TOUR_PWSH%" -NoProfile -File "%~dp0ui-tour.ps1" start -Prepare
) else (
  "%OLO_TOUR_PWSH%" -NoProfile -File "%~dp0ui-tour.ps1" %*
)
set "OLO_TOUR_EXIT=%ERRORLEVEL%"
if not "%OLO_TOUR_EXIT%"=="0" pause
exit /b %OLO_TOUR_EXIT%
