# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
$ErrorActionPreference='Stop'
$probe=Join-Path $PSScriptRoot 'verify-ollama-gpu.ps1'
$global:oloGpuDriver='556.39'
$global:oloGpuVram=5000000000
function nvidia-smi {
    $global:LASTEXITCODE=0
    foreach($index in 0..3){"$index, Quadro P4000, $global:oloGpuDriver, 6.1, 8192, 0"}
}
function docker {
    $global:LASTEXITCODE=0
    if($args[0] -eq 'inspect'){'[{"Config":{"Env":["OLLAMA_SCHED_SPREAD=true"]}}]'}
    elseif($args[2] -eq 'nvidia-smi'){nvidia-smi}
    else{'qwen2.5:7b 100% GPU'}
}
function Invoke-RestMethod {
    param($Uri,$Method,$ContentType,$Body,$TimeoutSec)
    if($Uri -like '*/api/ps'){@{models=@(@{name='qwen2.5:7b';size_vram=$global:oloGpuVram})}}
    else{@{message=@{content='READY'}}}
}
function Must-Fail($Action,$Expected){
    try{& $Action | Out-Null;throw 'Expected GPU verification to fail'}
    catch{if($_.Exception.Message -notlike "*$Expected*"){throw}}
}
Must-Fail {& $probe -ExpectedGpuCount 4} 'too old'
$global:oloGpuDriver='582.99'
Must-Fail {& $probe -ExpectedGpuCount 3} 'Expected 3 Docker GPUs'
$global:oloGpuVram=0
Must-Fail {& $probe -ExpectedGpuCount 4 -Warmup} 'not using GPU memory'
$global:oloGpuVram=5000000000
$report=& $probe -ExpectedGpuCount 4 -Warmup
if(!$report.InferenceVerified -or $report.VisibleGpuCount -ne 4 -or $report.ModelGpuBytes -le 0){throw 'GPU verification result was incorrect'}
Write-Host 'PASS: driver compatibility, device count, CPU fallback and GPU inference checks.'
