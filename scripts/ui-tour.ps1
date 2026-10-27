# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
<# Starts, checks and removes the disposable environment used for the README UI snapshots. #>
[CmdletBinding()]
param(
    [Parameter(Position=0)][ValidateSet('start','restart','stop','status')][string]$Action='start',
    [string]$Python=$(if($env:OLO_QUALIFICATION_PYTHON){$env:OLO_QUALIFICATION_PYTHON}else{'python'}),
    [string]$ToolGateSource=$env:TOOLGATE_SOURCE,
    [string]$IncidentPackage='',
    [switch]$Prepare,
    [string]$Gradle8='',
    [string]$Gradle9='',
    [string]$OllamaUrl='',
    [string]$OllamaModel='',
    [int]$TimeoutSeconds=900
)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$MonoRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$Knowledge=Join-Path $MonoRoot 'olo-knowledge'
$Runtime=Join-Path $Knowledge 'build/qualification/runtime'
$Manifest=Join-Path $Runtime 'gallery-session.json'
$ProcessRecord=Join-Path $Runtime 'ui-tour-process.json'
$Gallery=Join-Path $Knowledge 'tools/qualification_gallery.py'
$Logs=Join-Path $MonoRoot 'build'
$LocalConfig=Join-Path $PSScriptRoot 'ui-tour.local.json'
if(!(Test-Path -LiteralPath $LocalConfig)){$LocalConfig=Join-Path $PSScriptRoot 'ui-tour.example.json'}
if(Test-Path -LiteralPath $LocalConfig){
    $settings=Get-Content -Raw -LiteralPath $LocalConfig | ConvertFrom-Json
    $allowed=@('Python','ToolGateSource','IncidentPackage','Gradle8','Gradle9','OllamaUrl','OllamaModel')
    foreach($setting in $settings.PSObject.Properties){
        if($setting.Name -notin $allowed){throw "Unknown UI-tour setting: $($setting.Name)"}
        if($PSBoundParameters.ContainsKey($setting.Name)){continue}
        if($setting.Name -eq 'Python' -and $env:OLO_QUALIFICATION_PYTHON){continue}
        if($setting.Name -eq 'ToolGateSource' -and $env:TOOLGATE_SOURCE){continue}
        if([string]::IsNullOrWhiteSpace([string]$setting.Value)){continue}
        $value=[string]$setting.Value
        if($setting.Name -in @('ToolGateSource','IncidentPackage','Python','Gradle8','Gradle9') -and
            ![IO.Path]::IsPathRooted($value) -and $value -match '[/\\]'){
            $value=[IO.Path]::GetFullPath((Join-Path $MonoRoot $value))
        }
        Set-Variable -Name $setting.Name -Value $value
    }
}
function Invoke-Checked([string]$Executable,[string[]]$Arguments){
    & $Executable @Arguments
    if($LASTEXITCODE -ne 0){throw "$Executable failed with exit code $LASTEXITCODE"}
}
function Read-OwnedProcess {
    if(!(Test-Path -LiteralPath $ProcessRecord)){return $null}
    $record=Get-Content -Raw -LiteralPath $ProcessRecord | ConvertFrom-Json
    if($record.workspace -ne $MonoRoot){throw 'UI tour process ownership mismatch'}
    $process=Get-CimInstance Win32_Process -Filter "ProcessId = $($record.pid)"
    if($process -and ($process.Name -notmatch '^python(3)?(.exe)?$' -or $process.CommandLine -notlike "*$Gallery*")){throw 'Refusing to control an unrelated process'}
    return @{record=$record;process=$process}
}
function Read-Ready {
    $owned=Read-OwnedProcess
    if(!$owned -or !$owned.process -or !(Test-Path -LiteralPath $Manifest)){return $null}
    $ready=Get-Content -Raw -LiteralPath $Manifest | ConvertFrom-Json
    if($ready.workspace -ne $Knowledge -or $ready.runId -notmatch '^olo-qualification-[0-9a-f]{12}$'){throw 'UI tour readiness ownership mismatch'}
    if($ready.pid -ne $owned.record.pid){
        # Windows virtual-environment launchers may create the actual interpreter as a child.
        $child=Get-CimInstance Win32_Process -Filter "ProcessId = $($ready.pid)"
        if(!$child -or $child.ParentProcessId -ne $owned.record.pid -or $child.Name -notmatch '^python(3)?(.exe)?$' -or $child.CommandLine -notlike "*$Gallery*"){throw 'UI tour interpreter ownership mismatch'}
    }
    return $ready
}
function Show-Ready($Ready){
    Write-Host "UI tour READY ($($Ready.runId)); incident acceptance passed."
    Write-Host "Chat: $($Ready.chat) | Studio: $($Ready.studio)"
    Write-Host "ToolGate: $($Ready.toolgate) | Temporal: $($Ready.temporal) | Operator gateway: $($Ready.operator)"
    Write-Host "Private demo credentials: $Manifest (ignored; never commit it)."
    Write-Host 'Cleanup: .\scripts\ui-tour.ps1 stop'
}
if($Action -eq 'restart'){
    # Run cleanup in a separate process: its successful exit must not end this restart.
    Invoke-Checked (Get-Process -Id $PID).Path @('-NoProfile','-File',$PSCommandPath,'stop')
    $Action='start'
    $Prepare=$true
}
if($Action -eq 'status'){
    $ready=Read-Ready
    if($ready){Show-Ready $ready;exit 0}
    $owned=Read-OwnedProcess
    if($owned -and $owned.process){Write-Host 'UI tour is starting; inspect build/ui-tour.stdout.log and build/ui-tour.stderr.log.';exit 0}
    Write-Host 'No UI tour process is running.';exit 0
}
if($Action -eq 'stop'){
    $owned=Read-OwnedProcess
    if($owned -and $owned.process -and (Test-Path -LiteralPath $Manifest)){
        if(!$PSBoundParameters.ContainsKey('Python')){$Python=$owned.record.python}
        try{
            Invoke-Checked $Python @($Gallery,'--stop')
            Wait-Process -Id $owned.record.pid -Timeout 90 -ErrorAction SilentlyContinue
        }catch{Write-Warning 'Graceful shutdown was unavailable; continuing with validated owned-resource cleanup.'}
    }
    # The shared qualification stop validates exact process identities, owner labels and staging node IDs.
    Invoke-Checked (Join-Path $Knowledge 'qualification.ps1') @('stop')
    if(Test-Path -LiteralPath $ProcessRecord){Remove-Item -LiteralPath $ProcessRecord -Force}
    Write-Host 'UI tour stopped. Owned runtime resources and private credentials removed; reports/build caches retained.'
    exit 0
}
if(Read-Ready){Show-Ready (Read-Ready);exit 0}
$owned=Read-OwnedProcess
if($owned -and $owned.process){throw 'A UI tour is already starting. Use status or stop.'}
if(!$ToolGateSource){throw 'Set TOOLGATE_SOURCE or pass -ToolGateSource with the current ToolGate checkout.'}
$ToolGateSource=(Resolve-Path -LiteralPath $ToolGateSource).Path
$env:TOOLGATE_SOURCE=$ToolGateSource
$env:PYTHONUTF8='1'
if(!$IncidentPackage){$IncidentPackage=Join-Path $ToolGateSource 'examples/mocked-tools/server-recovery/build/toolgate/server-recovery-0.1.0.tgpkg'}
if($Prepare){
    $prepareArgs=@((Join-Path $Knowledge 'tools/prepare_ui_tour.py'),'--toolgate-source',$ToolGateSource)
    if($Gradle8){$prepareArgs+=@('--gradle8',$Gradle8)}
    if($Gradle9){$prepareArgs+=@('--gradle9',$Gradle9)}
    Invoke-Checked $Python $prepareArgs
}
if(!(Test-Path -LiteralPath $IncidentPackage)){throw 'Build the official server-recovery incident package first (see docs/media/README.md).'}
New-Item -ItemType Directory -Force -Path $Runtime,$Logs | Out-Null
$Executable=(Get-Command $Python -ErrorAction Stop).Source
# Windows file paths cannot contain quotes; each absolute path is passed as a separate quoted argument.
$galleryArgs=@('-u',('"'+$Gallery+'"'),'--package',('"'+$IncidentPackage+'"'))
if([bool]$OllamaUrl -ne [bool]$OllamaModel){throw 'Supply both -OllamaUrl and -OllamaModel.'}
if($OllamaUrl){
    if($OllamaUrl -match '["\s]' -or $OllamaModel -notmatch '^[a-zA-Z0-9_./:-]+$'){throw 'Invalid Ollama origin or model name.'}
    $galleryArgs+=@('--ollama-url',$OllamaUrl,'--ollama-model',$OllamaModel)
}
$process=Start-Process -FilePath $Executable -ArgumentList $galleryArgs `
    -WorkingDirectory $MonoRoot -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $Logs 'ui-tour.stdout.log') -RedirectStandardError (Join-Path $Logs 'ui-tour.stderr.log')
@{workspace=$MonoRoot;pid=$process.Id;python=$Executable} | ConvertTo-Json | Set-Content -LiteralPath $ProcessRecord -Encoding utf8
$deadline=(Get-Date).AddSeconds($TimeoutSeconds)
while((Get-Date) -lt $deadline){
    $process.Refresh()
    if($process.HasExited){throw 'UI tour startup/acceptance failed. Inspect build/ui-tour.stderr.log, then run stop.'}
    $ready=Read-Ready
    if($ready){Show-Ready $ready;exit 0}
    Start-Sleep -Seconds 2
}
throw 'UI tour readiness timed out. Inspect the logs and run stop to remove its owned resources.'
