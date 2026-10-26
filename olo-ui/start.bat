@echo off
REM Copyright (c) 2026 Olo Labs
REM Starts olo-be (8082) then olo-ui (3000). Bootstraps Gradle wrapper when missing.
setlocal enabledelayedexpansion

cd /d "%~dp0"

set "BE_DIR=%~dp0olo-be"
set "UI_DIR=%~dp0olo-ui"
set "WRAPPER_JAR=%BE_DIR%\gradle\wrapper\gradle-wrapper.jar"
set "GRADLE_CMD="

echo.
echo ========================================
echo   Olo UI dev stack
echo ========================================
echo.

REM --- Java ---
where java >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java not found. olo-be requires Java 17+.
  pause
  exit /b 1
)

REM --- Gradle wrapper (auto-bootstrap if missing) ---
call :BootstrapGradle
if errorlevel 1 (
  pause
  exit /b 1
)
set "GRADLE_CMD=%BE_DIR%\gradlew.bat"

REM --- Node ---
where npm >nul 2>&1
if errorlevel 1 (
  echo ERROR: npm not found. Install Node.js 18+ for olo-ui.
  pause
  exit /b 1
)

REM Worker refresh (Redis key olo:worker:refresh) needs Redis on spring.data.redis.* (default localhost:46379).
REM Set OLO_DISABLE_REDIS=1 to use in-memory tenants only — worker refresh will not work.
set "BE_ENV="
if "%OLO_DISABLE_REDIS%"=="1" (
  set "BE_ENV=set SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration &&"
)

echo Starting olo-be on http://localhost:8082 ...
start "olo-be" cmd /k "cd /d "%BE_DIR%" && %BE_ENV% "%GRADLE_CMD%" bootRun"

echo Waiting for backend health (up to 180s; first run downloads Gradle)...
set /a WAIT=0
:wait_health
timeout /t 3 /nobreak >nul
powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:8082/api/v1/health' -UseBasicParsing -TimeoutSec 3; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
if not errorlevel 1 goto health_ok
set /a WAIT+=3
if !WAIT! geq 180 (
  echo.
  echo ERROR: Backend did not respond on http://localhost:8082/api/v1/health
  echo Check the olo-be window for Java or port 8082 errors.
  pause
  exit /b 1
)
goto wait_health

:health_ok
echo Backend is up.

if not defined OLO_UI_PORT set "OLO_UI_PORT=3000"

echo Starting olo-ui on http://localhost:%OLO_UI_PORT% ...
start "olo-ui" cmd /k "cd /d "%UI_DIR%" && npm run dev -- --port %OLO_UI_PORT%"

echo.
echo ========================================
echo   Backend: http://localhost:8082
echo   UI:      http://localhost:%OLO_UI_PORT%
echo ========================================
echo Close the olo-be and olo-ui windows or run stop.bat to stop.
echo.
endlocal
exit /b 0

REM ---------------------------------------------------------------------------
REM Ensure gradle-wrapper.jar exists. Tries, in order:
REM   1) verify existing wrapper
REM   2) gradle wrapper (if gradle on PATH)
REM   3) download wrapper jar (gradlew downloads Gradle 8.5 on first run)
REM ---------------------------------------------------------------------------
:BootstrapGradle
if exist "%WRAPPER_JAR%" (
  pushd "%BE_DIR%"
  call gradlew.bat --version >nul 2>&1
  set "WRAPPER_OK=!ERRORLEVEL!"
  popd
  if !WRAPPER_OK! equ 0 exit /b 0
  echo Existing Gradle wrapper is invalid, re-downloading...
  del "%WRAPPER_JAR%"
)

echo Gradle wrapper not found. Bootstrapping...

if not exist "%BE_DIR%\gradle\wrapper" mkdir "%BE_DIR%\gradle\wrapper"

where gradle >nul 2>&1
if not errorlevel 1 (
  echo Running gradle wrapper in olo-be...
  pushd "%BE_DIR%"
  gradle wrapper --gradle-version 8.5
  popd
  if exist "%WRAPPER_JAR%" (
    echo Gradle wrapper ready.
    exit /b 0
  )
)

echo Downloading Gradle wrapper jar...
set "WRAPPER_DIR=%BE_DIR%\gradle\wrapper"
powershell -NoProfile -Command "$ErrorActionPreference='Stop'; New-Item -ItemType Directory -Force -Path '%WRAPPER_DIR%' | Out-Null; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%WRAPPER_JAR%' -UseBasicParsing"

if not exist "%WRAPPER_JAR%" goto bootstrap_fail

pushd "%BE_DIR%"
call gradlew.bat --version >nul 2>&1
set "WRAPPER_OK=!ERRORLEVEL!"
popd
if !WRAPPER_OK! equ 0 (
  echo Gradle wrapper ready. First bootRun may download Gradle 8.5 ^(1-2 min^).
  exit /b 0
)
echo ERROR: Gradle wrapper jar present but gradlew.bat failed. Delete %WRAPPER_JAR% and retry.
goto bootstrap_fail

:bootstrap_fail

echo ERROR: Could not bootstrap Gradle. Check network access and Java 17+.
exit /b 1
