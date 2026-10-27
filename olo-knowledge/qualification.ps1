# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
<#
.SYNOPSIS
  Start, qualify, inspect or completely remove an owned local staging environment.
.EXAMPLE
  .\qualification.ps1 start
  .\qualification.ps1 test
  .\qualification.ps1 stop
#>
[CmdletBinding()]
param(
    [Parameter(Position=0)][ValidateSet('start','test','status','stop')][string]$Action='start',
    [string]$Python='python',
    [switch]$FullStack,
    [switch]$Pipeline,
    [switch]$Delegation,
    [switch]$Incident,
    [switch]$Operations,
    [string]$IncidentPackage,
    [switch]$LatencyBaseline,
    [switch]$BuildImages,
    [switch]$BuildPipelineImages,
    [string]$BackendSource=(Join-Path $PSScriptRoot '..'),
    [string]$WorkerSource=(Join-Path $PSScriptRoot '..'),
    [string]$ToolGateSource=(Join-Path $PSScriptRoot '../../olo-toolgate'),
    [string]$ImageArchive,
    [switch]$Evaluation
)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
if($BuildImages -and $ImageArchive){throw 'Choose BuildImages or ImageArchive, not both'}
$Root=$PSScriptRoot
$env:TOOLGATE_SOURCE=[IO.Path]::GetFullPath($ToolGateSource)
$Runtime=Join-Path $Root 'build/qualification/runtime'
$Reports=Join-Path $Root 'build/qualification'
$StateFile=Join-Path $Runtime 'session.json'
$Kubeconfig=Join-Path $Runtime 'kubeconfig'
$Kind=Join-Path $Root 'build/tooling/kind.exe'
$Cluster='olo-qualification'
$NodeImage='kindest/node:v1.37.0@sha256:a1ed56cfb0e7b93589bdf97c8cd566405a265939e3620fc4f5de89adff580ae5'
function Get-OfficialChecksum([object]$Content) {
    $checksumText=if($Content -is [byte[]]){[Text.Encoding]::UTF8.GetString($Content)}else{[string]$Content}
    $checksum=($checksumText.Trim() -split '\s+')[0]
    if($checksum -notmatch '^[0-9a-fA-F]{64}$'){throw 'Official checksum response is malformed'}
    return $checksum
}
function Invoke-Checked([string]$Executable,[string[]]$Arguments) {
    & $Executable @Arguments
    if($LASTEXITCODE -ne 0){throw "$Executable exited with code $LASTEXITCODE"}
}
function Get-DockerJson([string[]]$Arguments) {
    $value=& docker @Arguments
    if($LASTEXITCODE -ne 0){throw 'Docker inspection failed'}
    return ($value | ConvertFrom-Json)
}
function Get-DockerLines([string[]]$Arguments) {
    $lines=@(& docker @Arguments)
    if($LASTEXITCODE -ne 0){throw 'Docker resource inventory failed; retaining ownership records for cleanup retry'}
    return $lines
}
function Get-Session {
    if(!(Test-Path -LiteralPath $StateFile)){throw 'No owned staging session. Run start first.'}
    $session=Get-Content -Raw -LiteralPath $StateFile | ConvertFrom-Json
    if($session.formatVersion -ne 1 -or $session.cluster -ne $Cluster -or $session.workspace -ne $Root){throw 'Session ownership mismatch'}
    return $session
}
function Remove-PrivateRuntime {
    $absolute=[IO.Path]::GetFullPath($Runtime)
    $expected=[IO.Path]::GetFullPath((Join-Path $Root 'build/qualification/runtime'))
    if($absolute -ne $expected){throw 'Refusing unverified cleanup path'}
    if((Get-Item -LiteralPath $absolute).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Runtime directory must not be a link'}
    Remove-Item -LiteralPath $absolute -Recurse -Force
}
function Stop-FixtureProcess {
    foreach($processFile in @(Get-ChildItem -LiteralPath $Runtime -Filter 'fixture-process-*.json' -ErrorAction SilentlyContinue | ForEach-Object FullName)) {
        $record=Get-Content -Raw -LiteralPath $processFile | ConvertFrom-Json
        if($record.workspace -ne $Root -or $record.session -notmatch '^olo-qualification-[0-9a-f]{12}$'){throw 'Fixture process ownership mismatch'}
        $process=Get-CimInstance Win32_Process -Filter "ProcessId = $($record.pid)"
        if($process) {
            if($process.Name -notmatch '^python(3)?(.exe)?$' -or ($record.script -notin @('qualification_stack.py','qualification_pipeline.py','qualification_delegation.py','qualification_incident.py','qualification_operations.py','qualification_debug.py','qualification_gallery.py','qualification_latency.py','test_services.py') -or $process.CommandLine -notlike "*$($record.script)*")){throw 'Refusing to terminate an unrelated process'}
            if($record.script -eq 'qualification_gallery.py'){
                $studioLibraries=[IO.Path]::GetFullPath((Join-Path $Root '../olo-ui/olo-be/build/libs'))
                foreach($child in @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $($record.pid)")){
                    if($child.Name -match '^java(.exe)?$' -and $child.CommandLine -like "*$studioLibraries*olo-be-*.jar*"){
                        Stop-Process -Id $child.ProcessId -Force
                        Wait-Process -Id $child.ProcessId -ErrorAction SilentlyContinue
                    }
                }
            }
            Stop-Process -Id $record.pid -Force
            Wait-Process -Id $record.pid -ErrorAction SilentlyContinue
        }
    }
}
function Stop-FixtureResources {
    Stop-FixtureProcess
    $containers=@(Get-DockerLines @('ps','-aq','--no-trunc','--filter','label=io.olo.qualification.owner=olo-knowledge'))
    foreach($container in $containers){Invoke-Checked 'docker' @('rm','-f','-v',$container)}
    foreach($kind in @('volume','network')) {
        $resources=@(Get-DockerLines @($kind,'ls','-q','--filter','label=io.olo.qualification.owner=olo-knowledge'))
        foreach($resource in $resources){Invoke-Checked 'docker' @($kind,'rm',$resource)}
    }
    $images=@(Get-DockerLines @('image','ls','-q','--filter','label=io.olo.qualification.owner=olo-knowledge') | Select-Object -Unique)
    foreach($image in $images){Invoke-Checked 'docker' @('image','rm',$image)}
    foreach($resourceType in @('container','volume','network','image')) {
        $listArguments=@($resourceType,'ls','-q','--filter','label=io.olo.qualification.owner=olo-knowledge')
        if($resourceType -eq 'container'){$listArguments+= '--all'}
        $remaining=@(Get-DockerLines $listArguments)
        if($remaining.Count -gt 0){throw "Owned $resourceType cleanup incomplete; retaining ownership records for retry"}
    }
}
function Stop-Session {
    $session=Get-Session
    Stop-FixtureResources
    # Verify exact node IDs before kind deletes by cluster name. Foreign resources are never adopted.
    $nodes=@(Get-DockerLines @('ps','-aq','--no-trunc','--filter',"label=io.x-k8s.kind.cluster=$Cluster"))
    foreach($node in $nodes) {
        if($node -and $node -notin $session.nodeIds){throw 'Unexpected cluster node; retaining ownership record for inspection'}
    }
    if($nodes.Count -gt 0){Invoke-Checked $Kind @('delete','cluster','--name',$Cluster,'--kubeconfig',$Kubeconfig)}
    $remaining=@(Get-DockerLines @('ps','-aq','--no-trunc','--filter',"label=io.x-k8s.kind.cluster=$Cluster"))
    if($remaining.Count -gt 0){throw 'Cluster cleanup incomplete; retaining session for retry'}
    if(!$session.networkExisted) {
        $networks=@(Get-DockerLines @('network','ls','-q','--filter','name=^kind$'))
        foreach($network in $networks) {
            $details=@(Get-DockerJson @('network','inspect',$network))[0]
            if(@($details.Containers.PSObject.Properties).Count -ne 0){throw 'Kind network still has users; preserving it and session record'}
            Invoke-Checked 'docker' @('network','rm',$network)
        }
    }
    if(!$session.nodeImageExisted) {
        $imageJson=& docker image inspect $NodeImage 2>$null
        if($LASTEXITCODE -eq 0) {
            $image=($imageJson | ConvertFrom-Json)[0]
            $users=@(Get-DockerLines @('ps','-aq','--filter',"ancestor=$($image.Id)"))
            if($users.Count -gt 0){throw 'The new node image acquired another user; preserving ownership evidence'}
            foreach($reference in @($NodeImage,($NodeImage -split '@')[0])) {
                & docker image inspect $reference *> $null
                if($LASTEXITCODE -eq 0){Invoke-Checked 'docker' @('image','rm',$reference)}
            }
        }
    }
    Remove-PrivateRuntime
    Write-Host 'Staging cluster, owned containers/volumes/networks/images, session-owned node image and private credentials/kubeconfig removed. Sanitized reports retained.'
}
if($Action -eq 'stop') {
    if(!(Test-Path -LiteralPath $StateFile)){Stop-FixtureResources;if(Test-Path -LiteralPath $Runtime){Remove-PrivateRuntime};Write-Host 'No owned staging session remains.';exit 0}
    Stop-Session;exit 0
}
if($Action -eq 'status') {
    $session=Get-Session
    Invoke-Checked 'kubectl' @('--kubeconfig',$Kubeconfig,'get','nodes','-o','wide')
    Write-Host "Owned cluster: $($session.cluster). Cleanup: .\qualification.ps1 stop"
    exit 0
}
if($Action -eq 'test') {
    $null=Get-Session
    Invoke-Checked $Python @((Join-Path $Root 'tools/qualify_network.py'),'--kubeconfig',$Kubeconfig)
    if($Evaluation){Invoke-Checked $Python @((Join-Path $Root 'tools/test_services.py'),'--evaluation','--with-tls')}
    if($FullStack){Invoke-Checked $Python @((Join-Path $Root 'tools/qualification_stack.py'))}
    if($Pipeline){Invoke-Checked $Python @((Join-Path $Root 'tools/qualification_pipeline.py'))}
    if($Delegation){Invoke-Checked $Python @((Join-Path $Root 'tools/qualification_delegation.py'))}
    if($Operations){Invoke-Checked $Python @((Join-Path $Root 'tools/qualification_operations.py'))}
    if($Incident){
        if(!$IncidentPackage){throw 'Incident qualification requires -IncidentPackage with the official built server-recovery .tgpkg'}
        Invoke-Checked $Python @((Join-Path $Root 'tools/qualification_incident.py'),'--package',$IncidentPackage)
    }
    if($LatencyBaseline){Invoke-Checked $Python @((Join-Path $Root 'tools/qualification_latency.py'))}
    exit 0
}
if(Test-Path -LiteralPath $StateFile){throw 'A session already exists. Use status/test, or stop before a fresh installation.'}
if(@(& docker ps -aq --no-trunc --filter "label=io.x-k8s.kind.cluster=$Cluster").Count -gt 0){throw 'An unowned cluster already uses the staging name; refusing to adopt or replace it'}
New-Item -ItemType Directory -Force -Path $Runtime,(Split-Path $Kind) | Out-Null
$downloadedKind=!(Test-Path -LiteralPath $Kind)
if($downloadedKind) {
    $url='https://github.com/kubernetes-sigs/kind/releases/download/v0.33.0/kind-windows-amd64'
    $expected=Get-OfficialChecksum (Invoke-WebRequest ($url+'.sha256sum')).Content
    Invoke-WebRequest $url -OutFile $Kind
    if((Get-FileHash -LiteralPath $Kind -Algorithm SHA256).Hash -ne $expected){Remove-Item -LiteralPath $Kind;throw 'Official kind checksum mismatch'}
}
$networkExisted=@(& docker network ls -q --filter 'name=^kind$').Count -gt 0
$null=& docker image inspect $NodeImage 2>$null
$nodeImageExisted=$LASTEXITCODE -eq 0
$session=@{formatVersion=1;cluster=$Cluster;workspace=$Root;nodeIds=@();networkExisted=$networkExisted;nodeImageExisted=$nodeImageExisted}
$session | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $StateFile -Encoding utf8
$config=Join-Path $Runtime 'kind.yaml'
@'
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
networking:
  disableDefaultCNI: true
  podSubnet: "192.168.0.0/16"
nodes:
- role: control-plane
- role: worker
'@ | Set-Content -LiteralPath $config -Encoding utf8
try {
    if($BuildImages){Invoke-Checked $Python @((Join-Path $Root 'tools/build_qualification.py'),'--source',$ToolGateSource)}
    if($BuildPipelineImages){Invoke-Checked $Python @((Join-Path $Root 'tools/build_pipeline_qualification.py'),'--backend-source',$BackendSource,'--worker-source',$WorkerSource)}
    if($ImageArchive){Invoke-Checked $Python @((Join-Path $Root 'tools/import_qualification.py'),$ImageArchive)}
    # Ready requires the CNI, installed immediately after the API server is available.
    & $Kind create cluster --name $Cluster --image $NodeImage --config $config --kubeconfig $Kubeconfig
    $creationCode=$LASTEXITCODE
    $session.nodeIds=@(& docker ps -aq --no-trunc --filter "label=io.x-k8s.kind.cluster=$Cluster")
    $session | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $StateFile -Encoding utf8
    if($creationCode -ne 0){throw 'Cluster creation failed'}
    Invoke-Checked 'docker' @('update','--cpus','2','--memory','3g','--memory-swap','3g',"$Cluster-control-plane")
    Invoke-Checked 'docker' @('update','--cpus','4','--memory','8g','--memory-swap','8g',"$Cluster-worker")
    $calico=Join-Path $Runtime 'calico.yaml'
    Invoke-WebRequest 'https://raw.githubusercontent.com/projectcalico/calico/v3.33.0/manifests/calico.yaml' -OutFile $calico
    if((Get-FileHash -LiteralPath $calico -Algorithm SHA256).Hash -ne '2DE8F47595FB9C41B3F47D7B767A1F8E72ECF84057AF834738FF12689A234DA5'){throw 'Calico manifest checksum mismatch'}
    Invoke-Checked 'kubectl' @('--kubeconfig',$Kubeconfig,'apply','--server-side','-f',$calico)
    Invoke-Checked 'kubectl' @('--kubeconfig',$Kubeconfig,'wait','--for=condition=Ready','nodes','--all','--timeout=240s')
    Write-Host 'Staging ready. Run .\qualification.ps1 test [-Evaluation] [-FullStack]. Remove everything with .\qualification.ps1 stop.'
} catch {
    # Preserve exact owned nodes even when kind returned a partial installation.
    $session.nodeIds=@(& docker ps -aq --no-trunc --filter "label=io.x-k8s.kind.cluster=$Cluster")
    $session | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $StateFile -Encoding utf8
    try{Stop-Session}catch{Write-Warning 'Cleanup requires retry: .\qualification.ps1 stop'}
    throw
}
