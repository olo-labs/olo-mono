# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
<# Starts a fresh isolated backend/worker/Temporal/ToolGate/memory/RAG debug stack. #>
[CmdletBinding()]
param(
    [ValidateSet('up','check','status','logs','collect','down')][string]$Action='up',
    [string]$Python='python'
)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$Runtime=Join-Path $PSScriptRoot 'build/qualification/runtime'
$Reports=Join-Path $PSScriptRoot 'build/qualification'
$Manifest=Join-Path $Runtime 'debug-session.json'
$Script=Join-Path $PSScriptRoot 'tools/qualification_debug.py'
function Get-OwnedDebug {
    if(!(Test-Path -LiteralPath $Manifest)){return $null}
    $record=Get-Content -Raw -LiteralPath $Manifest | ConvertFrom-Json
    if($record.workspace -ne $PSScriptRoot -or $record.session -notmatch '^olo-qualification-[0-9a-f]{12}$'){throw 'Debug session ownership mismatch'}
    $process=Get-CimInstance Win32_Process -Filter "ProcessId = $($record.pid)"
    if(!$process){return $null}
    if($process.Name -notmatch '^python(3)?(.exe)?$' -or $process.CommandLine -notlike "*$Script*"){throw 'Debug process identity mismatch'}
    return $record
}
function Show-Ready {
    $record=Get-OwnedDebug
    if(!$record -or $record.status -ne 'READY'){throw 'No ready governed debug stack; run up or inspect the generated logs'}
    $details=& docker inspect @($record.containers)
    if($LASTEXITCODE -ne 0){throw 'Debug container inventory failed'}
    foreach($container in ($details | ConvertFrom-Json)){
        if($container.Config.Labels.'io.olo.qualification.owner' -ne 'olo-knowledge' -or !$container.State.Running){throw 'Owned debug service is missing or stopped'}
    }
    Write-Host "Governed debug stack READY: $($record.session). Backend: $($record.backend). $($record.scope)"
}
if($Action -eq 'down'){
    & (Join-Path $PSScriptRoot 'qualification.ps1') stop
    exit $LASTEXITCODE
}
if($Action -in @('status','check')){Show-Ready;exit 0}
if($Action -in @('logs','collect')){
    foreach($name in @('governed-debug.stdout.log','governed-debug.stderr.log')){
        $path=Join-Path $Reports $name
        if(Test-Path -LiteralPath $path){Get-Content -LiteralPath $path -Tail 80}
    }
    Write-Host "Sanitized fixture evidence: $Reports. Private runtime files are excluded."
    exit 0
}
if(Get-OwnedDebug){Show-Ready;exit 0}
if(@(Get-ChildItem -LiteralPath $Runtime -Filter 'fixture-process-*.json' -ErrorAction SilentlyContinue).Count -gt 0){throw 'A fixture process record already exists; finish that run or use qualification.ps1 stop before starting debug'}
New-Item -ItemType Directory -Force -Path $Runtime,$Reports | Out-Null
$Executable=(Get-Command $Python -ErrorAction Stop).Source
# Quoted absolute file argument; Windows paths cannot contain a quote.
$process=Start-Process -FilePath $Executable -ArgumentList @('-u',('"'+$Script+'"')) -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $Reports 'governed-debug.stdout.log') -RedirectStandardError (Join-Path $Reports 'governed-debug.stderr.log')
$deadline=(Get-Date).AddSeconds(240)
while((Get-Date) -lt $deadline){
    $process.Refresh()
    if($process.HasExited){throw 'Governed debug startup failed; inspect build/qualification/governed-debug.stderr.log and run down for cleanup'}
    if(Test-Path -LiteralPath $Manifest){
        $record=Get-OwnedDebug
        if($record -and $record.status -eq 'READY'){Show-Ready;exit 0}
        if($record -and $record.status -eq 'FAILED'){throw 'Governed debug fixture reported startup failure; run down for cleanup'}
    }
    Start-Sleep -Seconds 2
}
throw 'Governed debug readiness timed out; run down to clear owned runtime resources'
