# Called by dev/install.bat. PlanOnly previews choices without contacting Docker.
[CmdletBinding()]
param(
    [ValidateSet(0,1)][int]$Audio = 0,
    [ValidateSet(0,1)][int]$Image = 0,
    [ValidateSet(0,1)][int]$Video = 0,
    [switch]$PlanOnly
)
$ErrorActionPreference = 'Stop'

function Model($Name, $Container, $Kind, $Source, $File) {
    [pscustomobject]@{ Name=$Name; Container=$Container; Kind=$Kind; Source=$Source; File=$File }
}
$catalog = [ordered]@{
    Text = @(
        (Model 'llama3.2:1b' 'olo-ollama' 'ollama' 'llama3.2:1b' ''),
        (Model 'llama3.2:3b' 'olo-ollama' 'ollama' 'llama3.2:3b' ''),
        (Model 'phi3:mini' 'olo-ollama' 'ollama' 'phi3:mini' ''),
        (Model 'qwen2.5:7b' 'olo-ollama' 'ollama' 'qwen2.5:7b' ''),
        (Model 'mistral:7b' 'olo-ollama' 'ollama' 'mistral:7b' '')
    )
    Audio = @(
        (Model 'Whisper tiny (speech to text)' 'olo-openai-oss-audio' 'whisper' 'https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin' 'ggml-tiny.bin'),
        (Model 'Whisper base' 'olo-openai-oss-audio' 'whisper' 'https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin' 'ggml-base.bin'),
        (Model 'Whisper small' 'olo-openai-oss-audio' 'whisper' 'https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin' 'ggml-small.bin')
    )
    Image = @(
        (Model 'SD Turbo (fast, ~5.2 GB)' 'olo-comfyui' 'checkpoint' 'https://huggingface.co/stabilityai/sd-turbo/resolve/main/sd_turbo.safetensors' 'sd_turbo.safetensors'),
        (Model 'SDXL Turbo (~6.9 GB)' 'olo-comfyui' 'checkpoint' 'https://huggingface.co/stabilityai/sdxl-turbo/resolve/main/sd_xl_turbo_1.0_fp16.safetensors' 'sd_xl_turbo_1.0_fp16.safetensors')
    )
    Video = @(
        (Model 'Stable Video Diffusion 14 frames (~9.6 GB)' 'olo-comfyui-video' 'checkpoint' 'https://huggingface.co/stabilityai/stable-video-diffusion-img2vid/resolve/main/svd.safetensors' 'svd.safetensors'),
        (Model 'Stable Video Diffusion XT 25 frames (~9.6 GB)' 'olo-comfyui-video' 'checkpoint' 'https://huggingface.co/stabilityai/stable-video-diffusion-img2vid-xt/resolve/main/svd_xt.safetensors' 'svd_xt.safetensors')
    )
}

function Get-SelectedModels {
    foreach ($category in $catalog.Keys) {
        if ($category -eq 'Audio' -and -not $Audio) { continue }
        if ($category -eq 'Image' -and -not $Image) { continue }
        if ($category -eq 'Video' -and -not $Video) { continue }
        $models = $catalog[$category]
        Write-Host "[$category] Default model: $($models[0].Name)"
        $models[0]
        $answer = Read-Host "Do you need more than 1 $category model? [y/N]"
        if ($answer.Trim() -ine 'y') { continue }
        foreach ($model in $models | Select-Object -Skip 1) {
            $answer = Read-Host "Install $($model.Name)? [y/N]"
            if ($answer.Trim() -ieq 'y') { $model }
        }
    }
}

function Invoke-DockerChecked([string[]]$Arguments) {
    & docker @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Docker failed: $($Arguments -join ' ')" }
}

function Wait-ModelServer($Model) {
    for ($attempt=1; $attempt -le 30; $attempt++) {
        if ($Model.Kind -eq 'ollama') {
            & docker exec $Model.Container ollama list *> $null
        } else {
            & docker exec $Model.Container sh -c 'test -d /models || test -d /opt/ComfyUI/models' *> $null
        }
        if ($LASTEXITCODE -eq 0) { return }
        Start-Sleep -Seconds 5
    }
    throw "Model server $($Model.Container) did not become ready."
}

function Install-SelectedModel($Model) {
    Wait-ModelServer $Model
    if ($Model.Kind -eq 'ollama') {
        Invoke-DockerChecked @('exec', $Model.Container, 'ollama', 'pull', $Model.Source)
        return
    }
    $directory = if ($Model.Kind -eq 'whisper') { '/models' } else { '/opt/ComfyUI/models/checkpoints' }
    $target = "$directory/$($Model.File)"
    & docker exec $Model.Container test -s $target *> $null
    if ($LASTEXITCODE -ne 0) {
        $temporary = Join-Path ([IO.Path]::GetTempPath()) ([IO.Path]::GetRandomFileName())
        try {
            # curl streams large weights to disk, follows redirects and fails on HTTP errors.
            & curl.exe --fail --location --retry 3 --output $temporary $Model.Source
            if ($LASTEXITCODE -ne 0) { throw "Download failed: $($Model.Name)" }
            Invoke-DockerChecked @('exec', $Model.Container, 'mkdir', '-p', $directory)
            Invoke-DockerChecked @('cp', $temporary, "$($Model.Container):$target.partial")
            Invoke-DockerChecked @('exec', $Model.Container, 'mv', "$target.partial", $target)
        } finally {
            if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary }
        }
    }
    if ($Model.Kind -eq 'whisper') {
        $configuration = Join-Path ([IO.Path]::GetTempPath()) ([IO.Path]::GetRandomFileName())
        try {
            $name = [IO.Path]::GetFileNameWithoutExtension($Model.File)
            [IO.File]::WriteAllText($configuration, "name: $name`nbackend: whisper`nparameters:`n  model: $($Model.File)`n")
            Invoke-DockerChecked @('cp', $configuration, "$($Model.Container):/models/$name.yaml")
        } finally {
            if (Test-Path -LiteralPath $configuration) { Remove-Item -LiteralPath $configuration }
        }
    }
}

try {
    $selected = @(Get-SelectedModels)
    if ($PlanOnly) { $selected; return }
    foreach ($model in $selected) {
        Write-Host "Installing $($model.Name) into $($model.Container)..."
        Install-SelectedModel $model
    }
    # Reload newly installed configs/checkpoints once per affected server.
    $servers = @($selected | Where-Object Kind -ne 'ollama' | Select-Object -ExpandProperty Container -Unique)
    foreach ($server in $servers) { Invoke-DockerChecked @('restart', $server) }
    Write-Host 'Selected model files installed. ComfyUI models are available in checkpoint selectors.'
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}