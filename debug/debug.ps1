# Copyright (c) 2026 Olo Labs
# SPDX-License-Identifier: Apache-2.0
<#
.SYNOPSIS
    Bring up the olo-docker dev stack for debugging olo-mono, with a single text model.

.DESCRIPTION
    Reuses the compose files from olo-docker/dev (project name "olo", network "olo-net") and
    layers debug overlays from ./compose on top:
      - health checks for db / redis / qdrant / temporal / ollama
      - olo-worker built locally from this olo-mono checkout (other images optionally local)
      - workflow configuration mounted from olo-mono/olo-definition/olo-configuration
      - Ollama only (LocalAI, Kibana, pgAdmin, RedisInsight, image/audio/video AI are skipped)
      - exactly one text model pulled into Ollama (default llama3.2:latest)

    Required containers (postgres, redis, elasticsearch, qdrant, temporal, ollama) are checked
    one by one: a missing or stopped container is started, an unhealthy one is restarted, and the
    script stops with the container's recent logs if it still does not become ready.

.EXAMPLE
    .\debug.ps1                       # up: infra + olo + chat/ui + locally built worker in Docker
.EXAMPLE
    .\debug.ps1 up -Worker host -JvmDebug   # worker runs on the host via gradlew, JDWP on 5005
.EXAMPLE
    .\debug.ps1 check                 # only verify/start the required infrastructure containers
.EXAMPLE
    .\debug.ps1 collect               # dump status + logs of every debug container to ./logs
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('up', 'check', 'status', 'logs', 'collect', 'worker', 'model', 'down')]
    [string]$Action = 'up',

    # docker: worker container (built locally). host: gradlew run on this machine. none: no worker.
    [ValidateSet('docker', 'host', 'none')]
    [string]$Worker = 'docker',

    # Services to build from local sources instead of pulling from Docker Hub.
    [ValidateSet('olo-worker', 'olo', 'olo-ui', 'olo-chat')]
    [string[]]$LocalBuild = @('olo-worker'),

    # The single text model to pull into Ollama.
    [string]$Model = $(if ($env:OLO_DEBUG_MODEL) { $env:OLO_DEBUG_MODEL } else { 'llama3.2:latest' }),

    # Run Ollama without the NVIDIA GPU reservation.
    [switch]$Cpu,

    # Force a rebuild of the locally built images.
    [switch]$Rebuild,

    # Remove a conflicting container that has the expected name but belongs to another compose project.
    [switch]$Force,

    # Host worker only: open a JDWP debug port.
    [switch]$JvmDebug,
    [int]$JvmDebugPort = 5005,

    # logs: limit to one service.
    [string]$Service,

    # down: also delete the stack's volumes (database, ES, qdrant, models).
    [switch]$Volumes,

    # Seconds to wait for each container to become ready.
    [int]$Timeout = 180,

    [string]$OloDockerDir = $(if ($env:OLO_DOCKER_DIR) { $env:OLO_DOCKER_DIR } else { '' })
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ============================================================
# Paths and environment
# ============================================================

$DebugDir = $PSScriptRoot
$MonoRoot = (Resolve-Path (Join-Path $DebugDir '..')).Path
$WorkspaceRoot = (Resolve-Path (Join-Path $MonoRoot '..')).Path
if (-not $OloDockerDir) { $OloDockerDir = Join-Path $MonoRoot 'deploy/stack' }
$DevDir = Join-Path $OloDockerDir 'dev'
$ComposeDir = Join-Path $DebugDir 'compose'
$LogRoot = Join-Path $DebugDir 'logs'
$ProjectName = 'olo'

function ConvertTo-ComposePath([string]$Path) { return ($Path -replace '\\', '/') }

$env:OLO_MONO_ROOT = ConvertTo-ComposePath $MonoRoot
$env:OLO_BACKEND_ROOT = ConvertTo-ComposePath (Join-Path $MonoRoot 'olo')
$env:OLO_UI_ROOT = ConvertTo-ComposePath (Join-Path $MonoRoot 'olo-ui')
$env:OLO_CHAT_ROOT = ConvertTo-ComposePath (Join-Path $MonoRoot 'olo-chat')
# Workflow JSON and demo tool data come from this checkout so olo, olo-ui and olo-worker agree.
$env:OLO_CONFIGURATION_SOURCE = ConvertTo-ComposePath (Join-Path $MonoRoot 'olo-definition/olo-configuration')
$env:OLO_SHARED_SOURCE = ConvertTo-ComposePath (Join-Path $MonoRoot 'olo-core/tools')
# The olo API embeds this callback URL into runs; it must be reachable from wherever the worker runs.
if ($Worker -eq 'host') {
    $env:OLO_CHAT_CALLBACK_BASE_URL = 'http://localhost:47080'
} else {
    $env:OLO_CHAT_CALLBACK_BASE_URL = 'http://olo:7080'
}

# Infrastructure the worker and backend cannot run without, in start order.
# Probe returns $true when the service is ready to accept work.
$Required = @(
    @{ Service = 'db';            Container = 'olo-db';            Port = 45432; Url = 'postgres://localhost:45432'
       Probe = { (Invoke-Docker exec olo-db pg_isready -U pguser -d pgdb) -match 'accepting connections' } }
    @{ Service = 'redis';         Container = 'olo-redis';         Port = 46379; Url = 'redis://localhost:46379'
       Probe = { (Invoke-Docker exec olo-redis redis-cli ping) -match 'PONG' } }
    @{ Service = 'elasticsearch'; Container = 'olo-elasticsearch'; Port = 29200; Url = 'http://localhost:29200'
       Probe = { (Get-HttpText 'http://localhost:29200/_cluster/health') -match '"status":"(green|yellow)"' } }
    @{ Service = 'qdrant';        Container = 'olo-qdrant';        Port = 46333; Url = 'http://localhost:46333/dashboard'
       Probe = { Test-Http 'http://localhost:46333/readyz' } }
    @{ Service = 'temporal';      Container = 'olo-temporal';      Port = 47233; Url = 'localhost:47233'
       Probe = { (Invoke-Docker exec olo-temporal sh -c "tctl --address `$(hostname -i | cut -d' ' -f1):7233 cluster health") -match 'SERVING' } }
    @{ Service = 'ollama';        Container = 'olo-ollama';        Port = 11435; Url = 'http://localhost:11435'
       Probe = { Test-Http 'http://localhost:11435/api/tags' } }
)

# Application containers started after the infrastructure is ready.
$Apps = @(
    @{ Service = 'temporal-ui'; Container = 'olo-temporal-ui'; Port = 48092; Url = 'http://localhost:48092'
       Probe = { Test-Http 'http://localhost:48092' -AnyStatus } }
    @{ Service = 'olo';         Container = 'olo';             Port = 47080; Url = 'http://localhost:47080/swagger-ui.html'
       Probe = { Test-Http 'http://localhost:47080' -AnyStatus } }
    @{ Service = 'olo-chat';    Container = 'olo-chat';        Port = 43000; Url = 'http://localhost:43000'
       Probe = { Test-Http 'http://localhost:43000' -AnyStatus } }
    @{ Service = 'olo-ui';      Container = 'olo-ui';          Port = 43001; Url = 'http://localhost:43001'
       Probe = { Test-Http 'http://localhost:43001' -AnyStatus } }
)

$WorkerSpec = @{ Service = 'olo-worker'; Container = 'olo-worker'; Port = 0; Url = '(no port; see logs)'
                 Probe = { (Get-ContainerState 'olo-worker').Status -eq 'running' } }

# ============================================================
# Helpers
# ============================================================

function Write-Step([string]$Text) { Write-Host ''; Write-Host "[STEP] $Text" -ForegroundColor Cyan }
function Write-Ok([string]$Text)   { Write-Host "  [OK]   $Text" -ForegroundColor Green }
function Write-Info([string]$Text) { Write-Host "  [INFO] $Text" }
function Write-Warn([string]$Text) { Write-Host "  [WARN] $Text" -ForegroundColor Yellow }
function Write-Fail([string]$Text) { Write-Host "  [FAIL] $Text" -ForegroundColor Red }

# Runs docker and returns combined output as one string; never throws on a non-zero exit.
function Invoke-Docker {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $out = & docker @args 2>&1 | ForEach-Object { "$_" }
        return ($out -join "`n")
    } finally {
        $ErrorActionPreference = $prev
    }
}

function Get-ComposeFiles {
    $files = @(
        'docker-compose-db.yml'
        'docker-compose-cache.yml'
        'docker-compose-ElasticSearch.yml'
        'docker-compose-vectordb.yml'
        'docker-compose-temporal.yml'
        'docker-compose-ai-text.yml'
        'docker-compose-olo.yml'
    ) | ForEach-Object { Join-Path $DevDir $_ }
    $files += Join-Path $ComposeDir 'docker-compose.debug.yml'
    if ($Cpu) { $files += Join-Path $ComposeDir 'docker-compose.debug.cpu.yml' }
    foreach ($svc in $LocalBuild) { $files += Join-Path $ComposeDir "local/$svc.yml" }
    return $files
}

function Get-ComposeArgs {
    $composeArgs = @('compose', '-p', $ProjectName, '--project-directory', $DevDir)
    foreach ($f in Get-ComposeFiles) { $composeArgs += @('-f', $f) }
    return $composeArgs
}

# Runs docker compose with output streamed to the console; returns the exit code.
function Invoke-Compose {
    $composeArgs = (Get-ComposeArgs) + $args
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & docker @composeArgs 2>&1 | ForEach-Object { Write-Host "         $_" }
        return $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $prev
    }
}

function Get-ContainerState([string]$Name) {
    # Parse JSON rather than a Go template: PowerShell 5.1 strips the quotes a label lookup needs.
    $raw = Invoke-Docker inspect $Name
    if ($LASTEXITCODE -ne 0 -or -not $raw) {
        return @{ Exists = $false; Status = 'missing'; Health = ''; Project = '' }
    }
    $info = @($raw | ConvertFrom-Json)[0]
    $health = ''
    if ($info.State.PSObject.Properties['Health'] -and $info.State.Health) { $health = $info.State.Health.Status }
    $project = ''
    $labels = $info.Config.Labels
    if ($labels -and $labels.PSObject.Properties['com.docker.compose.project']) { $project = $labels.'com.docker.compose.project' }
    return @{ Exists = $true; Status = $info.State.Status; Health = $health; Project = $project }
}

function Get-HttpText([string]$Url) {
    try {
        return (Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3).Content
    } catch {
        return ''
    }
}

function Test-Http([string]$Url, [switch]$AnyStatus) {
    try {
        Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 | Out-Null
        return $true
    } catch [System.Net.WebException] {
        # Any HTTP response (404, 401, ...) still proves the server is listening.
        return ($AnyStatus -and $null -ne $_.Exception.Response)
    } catch {
        return $false
    }
}

function Test-PortInUse([int]$Port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $task = $client.ConnectAsync('127.0.0.1', $Port)
        return ($task.Wait(500) -and $client.Connected)
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Get-PortOwner([int]$Port) {
    $owner = Invoke-Docker ps --filter "publish=$Port" --format '{{.Names}}'
    if ($owner) { return "container '$($owner.Trim())'" }
    return 'a host process (see: netstat -ano | findstr :' + $Port + ')'
}

function Invoke-Probe($Spec) {
    try { return [bool](& $Spec.Probe) } catch { return $false }
}

function Wait-Ready($Spec, [int]$Seconds) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    $lastStatus = ''
    while ((Get-Date) -lt $deadline) {
        if (Invoke-Probe $Spec) { return $true }
        $state = Get-ContainerState $Spec.Container
        if ($state.Status -in @('exited', 'dead')) {
            Write-Fail "$($Spec.Container) exited while starting."
            return $false
        }
        $status = "$($state.Status) $($state.Health)".Trim()
        if ($status -ne $lastStatus) { Write-Info "$($Spec.Container): $status, waiting..."; $lastStatus = $status }
        Start-Sleep -Seconds 3
    }
    return $false
}

function Show-Diagnostics($Spec) {
    Write-Host ''
    Write-Host "  ---- docker inspect $($Spec.Container) (state) ----" -ForegroundColor DarkGray
    Write-Host (Invoke-Docker inspect -f '{{json .State}}' $Spec.Container)
    Write-Host "  ---- last 60 log lines of $($Spec.Container) ----" -ForegroundColor DarkGray
    Write-Host (Invoke-Docker logs --tail 60 $Spec.Container)
    Write-Host ''
}

# Makes sure one service's container exists, runs, and passes its readiness probe.
function Confirm-Service($Spec, [switch]$Build) {
    $name = $Spec.Container
    $state = Get-ContainerState $name

    if ($state.Exists -and $state.Project -ne $ProjectName) {
        if (-not $Force) {
            Write-Fail "$name exists but belongs to compose project '$($state.Project)', not '$ProjectName'."
            Write-Info "Remove it with: docker rm -f $name   (or re-run with -Force)"
            return $false
        }
        Write-Warn "$name belongs to project '$($state.Project)'; removing it (-Force)."
        Invoke-Docker rm -f $name | Out-Null
        $state = Get-ContainerState $name
    }

    if (-not $state.Exists) {
        if ($Spec.Port -gt 0 -and (Test-PortInUse $Spec.Port)) {
            Write-Fail "$name is missing and host port $($Spec.Port) is already taken by $(Get-PortOwner $Spec.Port)."
            return $false
        }
        Write-Warn "$name is not available -> creating it."
    } elseif ($state.Status -ne 'running') {
        Write-Warn "$name is $($state.Status) -> starting it."
    } elseif (Invoke-Probe $Spec) {
        # Still run compose so a container created without the debug overlay picks it up.
        Write-Info "$name is running and ready; syncing with debug overlay."
    } else {
        Write-Info "$name is running but not ready yet."
    }

    $upArgs = @('up', '-d', '--no-deps')
    if ($Build -and $Rebuild) { $upArgs += '--build' }
    $upArgs += $Spec.Service
    if ((Invoke-Compose @upArgs) -ne 0) {
        Write-Fail "docker compose could not start $($Spec.Service)."
        Show-Diagnostics $Spec
        return $false
    }

    if (Wait-Ready $Spec $Timeout) { Write-Ok "$name ready ($($Spec.Url))"; return $true }

    Write-Warn "$name did not become ready in $Timeout s -> restarting once."
    Invoke-Docker restart $name | Out-Null
    if (Wait-Ready $Spec $Timeout) { Write-Ok "$name ready after restart ($($Spec.Url))"; return $true }

    Write-Fail "$name is still not ready."
    Show-Diagnostics $Spec
    return $false
}

function Confirm-Docker {
    Write-Step 'Checking Docker'
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'docker CLI not found in PATH.' }
    $server = Invoke-Docker version --format '{{.Server.Version}}'
    if ($LASTEXITCODE -ne 0) { throw "Docker engine is not reachable. Start Docker Desktop.`n$server" }
    Write-Ok "Docker engine $($server.Trim())"
    if (-not (Test-Path (Join-Path $DevDir 'docker-compose-olo.yml'))) {
        throw "olo-docker dev stack not found at $DevDir (pass -OloDockerDir or set OLO_DOCKER_DIR)."
    }
    Write-Ok "olo-docker dev stack: $DevDir"

    $net = Invoke-Docker network ls --filter 'name=^olo-net$' --format '{{.Name}}'
    if (-not $net) {
        Invoke-Docker network create olo-net | Out-Null
        Write-Ok 'Created docker network olo-net'
    } else {
        Write-Ok 'Docker network olo-net exists'
    }

    foreach ($svc in $LocalBuild) {
        $root = switch ($svc) {
            'olo-worker' { $env:OLO_MONO_ROOT }
            'olo'        { $env:OLO_BACKEND_ROOT }
            'olo-ui'     { $env:OLO_UI_ROOT }
            'olo-chat'   { $env:OLO_CHAT_ROOT }
        }
        if (-not (Test-Path $root)) { throw "Local build of $svc requested but $root does not exist." }
        Write-Info "$svc image is built locally from $root"
    }

    Write-Step 'Validating compose configuration'
    if ((Invoke-Compose config --quiet) -ne 0) { throw 'docker compose config failed (see output above).' }
    Write-Ok 'Compose configuration is valid'
}

function Confirm-Infrastructure {
    Write-Step 'Checking required containers (postgres, redis, elasticsearch, qdrant, temporal, ollama)'
    $failed = @()
    foreach ($spec in $Required) {
        if (-not (Confirm-Service $spec)) {
            $failed += $spec.Container
            # temporal needs db + elasticsearch; the worker needs everything. Stop at the first failure.
            break
        }
    }
    if ($failed.Count -gt 0) { throw "Required container(s) not ready: $($failed -join ', ')" }
}

function Get-WorkflowModels {
    $dir = Join-Path $MonoRoot 'olo-definition/olo-configuration/current-active'
    $models = @()
    Get-ChildItem -Path $dir -Filter *.json -Recurse | ForEach-Object {
        $text = Get-Content -Raw -Path $_.FullName
        foreach ($m in [regex]::Matches($text, '"model"\s*:\s*"([^"]+)"')) { $models += $m.Groups[1].Value }
    }
    return $models | Sort-Object -Unique
}

function Confirm-Model {
    Write-Step "Ensuring the single text model '$Model' is in Ollama"
    $installed = Invoke-Docker exec olo-ollama ollama list
    $shortName = $Model -replace ':latest$', ''
    if ($installed -match ('(?m)^' + [regex]::Escape($shortName) + '(:latest)?\s')) {
        Write-Ok "$Model already present"
    } else {
        Write-Info "Pulling $Model (first run downloads a few GB)..."
        # ollama prints progress on stderr; don't let PowerShell 5.1 turn that into a terminating error.
        $prev = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            & docker exec olo-ollama ollama pull $Model 2>&1 | ForEach-Object { "$_" } |
                Where-Object { $_ -notmatch 'pulling [0-9a-f]+:\s+\d+%' -or $_ -match '100%' } |
                ForEach-Object { Write-Host "         $_" }
            $pullExit = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $prev
        }
        if ($pullExit -ne 0) { throw "ollama pull $Model failed (exit $pullExit)." }
        Write-Ok "$Model pulled"
    }

    # The worker's startup health check verifies every model referenced by active workflows.
    $others = @(Get-WorkflowModels | Where-Object { ($_ -replace ':latest$', '') -ne $shortName })
    if ($others.Count -gt 0) {
        Write-Warn "Active workflows also reference: $($others -join ', ')"
        Write-Warn 'The worker LLM health check will fail until those are pulled or the workflows are changed.'
    } else {
        Write-Ok 'All active workflows use this model'
    }
}

function Start-Apps {
    Write-Step 'Starting application containers (temporal-ui, olo, olo-chat, olo-ui)'
    foreach ($spec in $Apps) {
        $build = $LocalBuild -contains $spec.Service
        if (-not (Confirm-Service $spec -Build:$build)) { throw "$($spec.Container) failed to start." }
    }
}

function Start-DockerWorker {
    Write-Step 'Starting olo-worker container'
    if ($LocalBuild -contains 'olo-worker') {
        $image = Invoke-Docker image ls -q olo-worker:debug
        if ($Rebuild -or -not $image) {
            Write-Info 'Building olo-worker:debug from olo-mono (this takes a few minutes)...'
            if ((Invoke-Compose build olo-worker) -ne 0) { throw 'olo-worker image build failed.' }
        }
    }
    if (-not (Confirm-Service $WorkerSpec)) { throw 'olo-worker failed to start.' }
    # The worker exits if Temporal or the LLM check fails; give it a moment and re-check.
    Start-Sleep -Seconds 10
    $state = Get-ContainerState 'olo-worker'
    if ($state.Status -ne 'running') {
        Show-Diagnostics $WorkerSpec
        throw "olo-worker stopped after start (status: $($state.Status))."
    }
    Write-Ok 'olo-worker is running'
}

function Stop-DockerWorker {
    $state = Get-ContainerState 'olo-worker'
    if ($state.Exists -and $state.Status -eq 'running') {
        Write-Info 'Stopping olo-worker container so the host worker takes the Temporal task queue.'
        Invoke-Docker stop olo-worker | Out-Null
    }
}

function Start-HostWorker {
    Write-Step 'Running olo-worker on the host (gradlew run)'
    Stop-DockerWorker
    if (Test-PortInUse 8080) {
        Write-Warn "Port 8080 is in use by $(Get-PortOwner 8080); the worker's HTTP server binds 127.0.0.1:8080."
    }
    $env:OLO_LLM_BASE_URL = 'http://localhost:11435'
    if ($JvmDebug) {
        $env:JAVA_TOOL_OPTIONS = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$JvmDebugPort"
        Write-Info "JDWP debugger listening on port $JvmDebugPort (attach your IDE)."
    }
    Push-Location (Join-Path $MonoRoot 'olo-worker')
    try {
        & cmd /c "..\publish-libs.bat"
        if ($LASTEXITCODE -ne 0) { throw 'publish-libs.bat failed.' }
        & cmd /c "gradlew.bat run"
    } finally {
        Pop-Location
    }
}

function Show-Status {
    Write-Step 'Debug stack status'
    $rows = foreach ($spec in ($Required + $Apps + @($WorkerSpec))) {
        $state = Get-ContainerState $spec.Container
        $ready = if ($state.Status -eq 'running') { if (Invoke-Probe $spec) { 'yes' } else { 'NO' } } else { '-' }
        [pscustomobject]@{
            Service   = $spec.Service
            Container = $spec.Container
            State     = $state.Status
            Health    = $state.Health
            Ready     = $ready
            Endpoint  = $spec.Url
        }
    }
    $rows | Format-Table -AutoSize | Out-String | Write-Host
    $models = Invoke-Docker exec olo-ollama ollama list
    if ($LASTEXITCODE -eq 0) { Write-Host '  Ollama models:'; Write-Host $models }
}

function Save-Diagnostics {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $dir = Join-Path $LogRoot $stamp
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Write-Step "Collecting diagnostics into $dir"
    Invoke-Docker ps -a --filter "label=com.docker.compose.project=$ProjectName" --format 'table {{.Names}}\t{{.Status}}\t{{.Image}}\t{{.Ports}}' |
        Out-File -Encoding utf8 (Join-Path $dir 'ps.txt')
    (Invoke-Docker @((Get-ComposeArgs) + @('config'))) | Out-File -Encoding utf8 (Join-Path $dir 'compose-config.yml')
    foreach ($spec in ($Required + $Apps + @($WorkerSpec))) {
        if (-not (Get-ContainerState $spec.Container).Exists) { continue }
        Invoke-Docker logs --timestamps --tail 2000 $spec.Container | Out-File -Encoding utf8 (Join-Path $dir "$($spec.Container).log")
        Invoke-Docker inspect $spec.Container | Out-File -Encoding utf8 (Join-Path $dir "$($spec.Container).inspect.json")
    }
    $hostLogs = Join-Path $DevDir 'configuration/olo-worker/logs'
    if (Test-Path $hostLogs) { Copy-Item -Recurse -Path $hostLogs -Destination (Join-Path $dir 'olo-worker-app-logs') }
    Write-Ok "Diagnostics saved: $dir"
}

function Show-Summary {
    Write-Host ''
    Write-Host '============================================================' -ForegroundColor Cyan
    Write-Host ' OLO debug stack is up' -ForegroundColor Cyan
    Write-Host '============================================================' -ForegroundColor Cyan
    foreach ($spec in ($Required + $Apps)) { Write-Host ('  {0,-14} {1}' -f $spec.Service, $spec.Url) }
    Write-Host ('  {0,-14} {1}' -f 'text model', $Model)
    Write-Host ('  {0,-14} {1}' -f 'worker', $Worker)
    Write-Host ''
    Write-Host '  .\debug.ps1 status     readiness of every container'
    Write-Host '  .\debug.ps1 logs -Service olo-worker'
    Write-Host '  .\debug.ps1 collect    save all logs to debug\logs\<timestamp>'
    Write-Host ''
}

# ============================================================
# Main
# ============================================================

switch ($Action) {
    'up' {
        Confirm-Docker
        Confirm-Infrastructure
        Confirm-Model
        Start-Apps
        switch ($Worker) {
            'docker' { Start-DockerWorker }
            'host'   { Show-Summary; Start-HostWorker; return }
            'none'   { Stop-DockerWorker }
        }
        Show-Summary
    }
    'check' {
        Confirm-Docker
        Confirm-Infrastructure
        Confirm-Model
        Show-Status
    }
    'status' { Show-Status }
    'logs' {
        $logArgs = @('logs', '-f', '--tail', '200')
        if ($Service) { $logArgs += $Service }
        $composeArgs = (Get-ComposeArgs) + $logArgs
        & docker @composeArgs
    }
    'collect' { Save-Diagnostics }
    'worker' {
        Confirm-Docker
        Confirm-Infrastructure
        Confirm-Model
        if ($Worker -eq 'host') { Start-HostWorker } else { Start-DockerWorker }
    }
    'model' { Confirm-Model }
    'down' {
        Write-Step "Stopping the '$ProjectName' compose project"
        $downArgs = @('down', '--remove-orphans')
        if ($Volumes) { $downArgs += '-v'; Write-Warn 'Deleting volumes: database, Elasticsearch, qdrant and Ollama models.' }
        if ((Invoke-Compose @downArgs) -ne 0) { throw 'docker compose down failed.' }
        Write-Ok 'Stack stopped'
    }
}
# Failures throw (exit 1); don't leak the exit code of the last probe.
exit 0
