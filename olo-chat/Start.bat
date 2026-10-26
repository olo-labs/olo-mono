@echo off
REM Copyright (c) 2026 Olo Labs
REM SPDX-License-Identifier: Apache-2.0
cd /d "%~dp0"
echo Starting Olo UI (olo-chat)...

call :EnsureNpm
if errorlevel 1 (
    pause
    exit /b 1
)

echo Installing dependencies...
call npm install
if errorlevel 1 (
    echo npm install failed.
    pause
    exit /b 1
)
echo Starting dev server...
call npm run dev
pause
exit /b 0

:EnsureNpm
if exist "%ProgramFiles%\nodejs\npm.cmd" set "PATH=%ProgramFiles%\nodejs;%PATH%"
if exist "%LocalAppData%\Programs\node\npm.cmd" set "PATH=%LocalAppData%\Programs\node;%PATH%"

where npm >nul 2>&1
if not errorlevel 1 exit /b 0

echo npm not found. Installing Node.js (includes npm)...

set "INSTALLED=0"
where winget >nul 2>&1
if not errorlevel 1 (
    echo Using winget...
    winget install -e --id OpenJS.NodeJS.LTS --accept-package-agreements --accept-source-agreements
    if not errorlevel 1 set "INSTALLED=1"
)

if "%INSTALLED%"=="0" (
    where choco >nul 2>&1
    if not errorlevel 1 (
        echo Using Chocolatey...
        choco install nodejs-lts -y
        if not errorlevel 1 set "INSTALLED=1"
    )
)

if "%INSTALLED%"=="0" (
    echo.
    echo Could not install Node.js automatically.
    echo Install Node.js 18+ from https://nodejs.org/ then run this script again.
    exit /b 1
)

if exist "%ProgramFiles%\nodejs\npm.cmd" set "PATH=%ProgramFiles%\nodejs;%PATH%"
if exist "%LocalAppData%\Programs\node\npm.cmd" set "PATH=%LocalAppData%\Programs\node;%PATH%"

where npm >nul 2>&1
if not errorlevel 1 exit /b 0

echo.
echo Node.js was installed but npm is not available in this session.
echo Close this window, open a new terminal, and run Start.bat again.
exit /b 1
