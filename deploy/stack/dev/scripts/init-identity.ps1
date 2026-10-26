$ErrorActionPreference = 'Stop'
$envPath = Join-Path $PSScriptRoot '../.env'
$content = if (Test-Path -LiteralPath $envPath) { [IO.File]::ReadAllText($envPath) } else { '' }
foreach ($name in @('OLO_DEV_ADMIN_PASSWORD', 'OLO_DEV_USER_PASSWORD', 'OLO_CALLBACK_KEY')) {
    if ($content -notmatch "(?m)^$name=.+$") {
        $bytes = New-Object byte[] 32
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        $value = [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
        $content += "`n$name=$value`n"
    }
}
if ($content -notmatch '(?m)^OLO_CALLBACK_KEY_ID=.+$') { $content += "`nOLO_CALLBACK_KEY_ID=stack-dev`n" }
[IO.File]::WriteAllText([IO.Path]::GetFullPath($envPath), $content)
Write-Host 'Local identity credentials are ready in dev/.env (not tracked by Git).'
