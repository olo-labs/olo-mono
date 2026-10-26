@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

rem ============================================================
rem OLO Development Stack Installer
rem ============================================================

echo.
echo ============================================================
echo OLO Development Stack Installer
echo ============================================================
echo Working directory : %CD%
echo Started at        : %DATE% %TIME%
echo ============================================================
echo.

rem ============================================================
rem STEP 1 - Verify Docker
rem ============================================================

echo [STEP 1] Checking Docker availability...

docker version >nul 2>&1

if errorlevel 1 (
    echo.
    echo [ERROR] Docker is not available.
    echo.
    echo Probable causes:
    echo   - Docker Desktop is not running.
    echo   - Docker CLI is not installed.
    echo   - Docker CLI is not available in PATH.
    echo   - Docker Engine failed to start.
    echo.
    echo Try running:
    echo   docker version
    echo.
    exit /b 1
)

echo [OK] Docker is available.
echo.


rem ============================================================
rem STEP 2 - Create shared Docker network
rem ============================================================

echo [STEP 2] Checking Docker network: olo-net

docker network inspect olo-net >nul 2>&1

if errorlevel 1 (
    echo [INFO] Network olo-net does not exist.
    echo [INFO] Creating network olo-net...

    docker network create olo-net

    if errorlevel 1 (
        echo.
        echo [ERROR] Failed to create Docker network olo-net.
        echo.
        echo Probable causes:
        echo   - Docker Engine is unavailable.
        echo   - Docker daemon permission problem.
        echo   - Docker network configuration problem.
        echo.
        exit /b 1
    )

    echo [OK] Network olo-net created.
) else (
    echo [OK] Network olo-net already exists.
)

echo.


rem ============================================================
rem STEP 3 - Pull latest OLO images
rem ============================================================

echo [STEP 3] Pulling latest OLO images...
echo.

call :PullImage "ololab/olo:latest"
if errorlevel 1 exit /b 1

rem Uncomment when olo-worker image is enabled
rem call :PullImage "ololab/olo-worker:latest"
rem if errorlevel 1 exit /b 1

call :PullImage "ololab/olo-ui:latest"
if errorlevel 1 exit /b 1

call :PullImage "ololab/olo-chat:latest"
if errorlevel 1 exit /b 1

echo.
echo [OK] OLO images pulled successfully.
echo.


rem ============================================================
rem STEP 4 - Select AI containers
rem ============================================================

echo [STEP 4] Select AI containers to deploy
echo.

set "DEPLOY_TEXT=1"
echo [INFO] Text AI is always enabled.

set "DEPLOY_SPEECH=0"
set "ANS="
set /p "ANS=Deploy speech/audio AI? [y/N]: "
if /I "!ANS!"=="Y" set "DEPLOY_SPEECH=1"

set "DEPLOY_IMAGE=0"
set "ANS="
set /p "ANS=Deploy image AI? [y/N]: "
if /I "!ANS!"=="Y" set "DEPLOY_IMAGE=1"

set "DEPLOY_VIDEO=0"
set "ANS="
set /p "ANS=Deploy video AI? [y/N]: "
if /I "!ANS!"=="Y" set "DEPLOY_VIDEO=1"

echo.
echo AI Selection:
echo   Text   : !DEPLOY_TEXT!
echo   Speech : !DEPLOY_SPEECH!
echo   Image  : !DEPLOY_IMAGE!
echo   Video  : !DEPLOY_VIDEO!
echo.


rem ============================================================
rem STEP 5 - Build Docker Compose configuration
rem ============================================================

echo [STEP 5] Building Docker Compose configuration...

set "COMPOSE_FILES=-f docker-compose-db.yml -f docker-compose-cache.yml -f docker-compose-ElasticSearch.yml -f docker-compose-vectordb.yml -f docker-compose-temporal.yml -f docker-compose-olo.yml"

set "AI_SELECTED="
set "AI_SERVICES=ollama openai-oss"

if "!DEPLOY_TEXT!"=="1" (
    set "COMPOSE_FILES=!COMPOSE_FILES! -f docker-compose-ai-text.yml"
    set "AI_SELECTED=!AI_SELECTED! text"
)

if "!DEPLOY_SPEECH!"=="1" (
    set "COMPOSE_FILES=!COMPOSE_FILES! -f docker-compose-ai-audio.yml"
    set "AI_SELECTED=!AI_SELECTED! speech"
    set "AI_SERVICES=!AI_SERVICES! openai-oss-audio"
)

if "!DEPLOY_IMAGE!"=="1" (
    set "COMPOSE_FILES=!COMPOSE_FILES! -f docker-compose-ai-image.yml"
    set "AI_SELECTED=!AI_SELECTED! image"
    set "AI_SERVICES=!AI_SERVICES! comfyui"
)

if "!DEPLOY_VIDEO!"=="1" (
    set "COMPOSE_FILES=!COMPOSE_FILES! -f docker-compose-ai-video.yml"
    set "AI_SELECTED=!AI_SELECTED! video"
    set "AI_SERVICES=!AI_SERVICES! comfyui-video"
)

echo [INFO] Core containers:
echo        db, cache, elastic, vectordb, temporal, olo

echo [INFO] AI containers:
echo       !AI_SELECTED!

echo.


rem ============================================================
rem STEP 6 - Validate Docker Compose configuration
rem ============================================================

echo [STEP 6] Validating Docker Compose configuration...

docker compose -p olo !COMPOSE_FILES! config --quiet

if errorlevel 1 (
    echo.
    echo [ERROR] Docker Compose configuration validation failed.
    echo.
    echo Probable causes:
    echo   - A docker-compose YAML file is missing.
    echo   - YAML syntax error.
    echo   - Invalid environment variable.
    echo   - Invalid volume or network configuration.
    echo.
    echo Running full validation output:
    echo.
    docker compose -p olo !COMPOSE_FILES! config
    echo.
    exit /b 1
)

echo [OK] Docker Compose configuration is valid.
echo.


rem ============================================================
rem STEP 7 - Start OLO stack
rem ============================================================

echo [STEP 7] Starting OLO development stack...
echo.

docker compose -p olo !COMPOSE_FILES! up -d db pgadmin redis redisinsight elasticsearch kibana qdrant temporal temporal-admin-tools temporal-ui olo olo-ui olo-chat olo-worker !AI_SERVICES!

if errorlevel 1 (
    echo.
    echo [ERROR] Docker Compose failed to start the OLO stack.
    echo.
    echo Probable causes:
    echo   - Container failed to start.
    echo   - Port already in use.
    echo   - Docker volume problem.
    echo   - Docker network problem.
    echo   - Insufficient memory or disk space.
    echo   - Image startup failure.
    echo.
    echo Current container status:
    echo.
    docker compose -p olo !COMPOSE_FILES! ps
    echo.
    echo Recent container events:
    echo.
    docker compose -p olo !COMPOSE_FILES! logs --tail 50
    echo.
    exit /b 1
)

echo.
echo [OK] Docker Compose command completed.
echo.


rem ============================================================
rem STEP 8 - Display container status
rem ============================================================

echo [STEP 8] Checking container status...
echo.

docker compose -p olo !COMPOSE_FILES! ps

echo.


rem Install one default model per selected category, with opt-in extras.
echo [STEP 9] Selecting and installing AI models...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install-selected-models.ps1" -Audio !DEPLOY_SPEECH! -Image !DEPLOY_IMAGE! -Video !DEPLOY_VIDEO!
if errorlevel 1 (
    echo [ERROR] Selected model installation failed. See the error above.
    exit /b 1
)

rem ============================================================
rem STEP 10 - Final container status
rem ============================================================

echo [STEP 10] Final container status...
echo.

docker compose -p olo !COMPOSE_FILES! ps

echo.


rem ============================================================
rem INSTALLATION COMPLETE
rem ============================================================

echo ============================================================
echo OLO INSTALLATION COMPLETED SUCCESSFULLY
echo ============================================================
echo.
echo Installed components:
echo   - OLO Core
echo   - OLO UI
echo   - OLO Chat
echo   - PostgreSQL
echo   - Redis
echo   - Elasticsearch
echo   - Qdrant
echo   - Temporal
echo   - Ollama
echo   - LocalAI
echo.
echo AI containers:
echo  !AI_SELECTED!
echo.
echo Ollama API:
echo   http://localhost:11435/api/tags
echo.
echo Completed at:
echo   %DATE% %TIME%
echo.
echo ============================================================

exit /b 0


rem ============================================================
rem FUNCTIONS
rem ============================================================

:PullImage

set "IMAGE_NAME=%~1"

echo ------------------------------------------------------------
echo [INFO] Pulling image: !IMAGE_NAME!
echo ------------------------------------------------------------

docker pull !IMAGE_NAME!

if errorlevel 1 (
    echo.
    echo [ERROR] Failed to pull Docker image:
    echo         !IMAGE_NAME!
    echo.
    echo Probable causes:
    echo   - Internet connection problem.
    echo   - Docker Hub is unavailable.
    echo   - Image name or tag is incorrect.
    echo   - Docker Hub rate limit.
    echo   - Authentication problem.
    echo.
    exit /b 1
)

echo [OK] Image pulled: !IMAGE_NAME!
echo.

exit /b 0

