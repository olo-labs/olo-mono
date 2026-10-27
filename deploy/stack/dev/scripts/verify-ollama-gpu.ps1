# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
[CmdletBinding()]
param(
    [string]$Container='olo-ollama',
    [string]$Origin='http://localhost:11435',
    [string]$Model='qwen2.5:7b',
    [ValidateRange(0,128)][int]$ExpectedGpuCount=0,
    [switch]$Warmup
)
$ErrorActionPreference='Stop'
function Read-Gpus([switch]$InContainer) {
    $arguments=@('--query-gpu=index,name,driver_version,compute_cap,memory.total,memory.used','--format=csv,noheader,nounits')
    $rows=if($InContainer){& docker exec $Container nvidia-smi @arguments}else{& nvidia-smi @arguments}
    if($LASTEXITCODE -ne 0){throw 'NVIDIA GPU inspection failed. Check host drivers and Docker GPU passthrough.'}
    @($rows | ConvertFrom-Csv -Header Index,Name,Driver,Compute,TotalMiB,UsedMiB | ForEach-Object {
        [pscustomobject]@{Index=$_.Index.Trim();Name=$_.Name.Trim();Driver=$_.Driver.Trim();Compute=[double]$_.Compute.Trim();TotalMiB=[int]$_.TotalMiB.Trim();UsedMiB=[int]$_.UsedMiB.Trim()}
    })
}
$hostGpus=@(Read-Gpus)
$containerGpus=@(Read-Gpus -InContainer)
if(!$hostGpus.Count){throw 'No NVIDIA GPUs detected.'}
if($ExpectedGpuCount -gt 0 -and $containerGpus.Count -ne $ExpectedGpuCount){throw "Expected $ExpectedGpuCount Docker GPUs; found $($containerGpus.Count)."}
if($containerGpus.Count -ne $hostGpus.Count){throw 'Docker does not expose every host GPU.'}
foreach($gpu in $containerGpus){
    if($gpu.Compute -lt 5.0){throw "$($gpu.Name) compute capability $($gpu.Compute) is unsupported by current Ollama."}
    # Current Ollama CUDA builds require R570+ on Maxwell/Pascal (compute 5.0–6.2).
    # https://docs.ollama.com/gpu
    $minimum=if($gpu.Compute -le 6.2){570}else{550}
    if(([version]$gpu.Driver).Major -lt $minimum){
        throw "$($gpu.Name) driver $($gpu.Driver) is too old for current Ollama. Install a supported NVIDIA driver $minimum or newer; GPU visibility alone does not prove GPU inference."
    }
}
$raw=& docker inspect $Container
if($LASTEXITCODE -ne 0){throw 'Ollama container inspection failed.'}
$configuration=($raw | ConvertFrom-Json)[0]
if('OLLAMA_SCHED_SPREAD=true' -notin $configuration.Config.Env){throw 'Multi-GPU spreading is not enabled. Recreate Ollama using the updated text-AI Compose file.'}
$gpuBytes=$null
$usedGpus=@()
if($Warmup){
    $body=@{model=$Model;messages=@(@{role='user';content='Reply with READY.'});stream=$false;options=@{temperature=0;num_predict=4};keep_alive='5m'} | ConvertTo-Json -Depth 5
    $null=Invoke-RestMethod -Uri ($Origin.TrimEnd('/')+'/api/chat') -Method Post -ContentType application/json -Body $body -TimeoutSec 180
    $loaded=(Invoke-RestMethod -Uri ($Origin.TrimEnd('/')+'/api/ps')).models | Where-Object {$_.name -eq $Model -or $_.model -eq $Model} | Select-Object -First 1
    if(!$loaded -or $loaded.size_vram -le 0){throw 'The requested model is not using GPU memory; CPU fallback is not a GPU pass.'}
    $gpuBytes=$loaded.size_vram
    $after=@(Read-Gpus -InContainer)
    $usedGpus=@($after | Where-Object {
        $device=$_
        $before=$containerGpus | Where-Object {$_.Index -eq $device.Index} | Select-Object -First 1
        $device.UsedMiB -gt $before.UsedMiB
    } | Select-Object -ExpandProperty Index)
    Write-Host (& docker exec $Container ollama ps | Out-String)
}
[pscustomobject]@{VisibleGpuCount=$containerGpus.Count;SpreadEnabled=$true;DriverPreflightPassed=$true;InferenceVerified=[bool]$Warmup;Model=$(if($Warmup){$Model}else{$null});ModelGpuBytes=$gpuBytes;GpuMemoryIncreasedOn=$usedGpus;Devices=$containerGpus}
