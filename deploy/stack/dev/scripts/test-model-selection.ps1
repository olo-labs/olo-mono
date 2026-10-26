$ErrorActionPreference = 'Stop'
$installer = Join-Path $PSScriptRoot 'install-selected-models.ps1'
$global:oloTestAnswers = [Collections.Generic.Queue[string]]::new()
$global:oloTestPrompts = [Collections.Generic.List[string]]::new()
function Read-Host($Prompt) {
    $global:oloTestPrompts.Add($Prompt)
    if ($global:oloTestAnswers.Count) { return $global:oloTestAnswers.Dequeue() }
    return ''
}
function Assert($Condition, $Message) { if (-not $Condition) { throw $Message } }
# Every category combination defaults to exactly one model per enabled category.
foreach ($audio in 0,1) { foreach ($image in 0,1) { foreach ($video in 0,1) {
    $global:oloTestPrompts.Clear()
    $plan = @(& $installer -Audio $audio -Image $image -Video $video -PlanOnly)
    Assert ($plan.Count -eq (1+$audio+$image+$video)) 'Default model count mismatch'
    Assert ($global:oloTestPrompts.Count -eq $plan.Count) 'Unexpected extra-model prompts'
    Assert ($plan[0].Source -eq 'llama3.2:1b') 'Wrong default text model'
}}}
# Yes opens each individual choice; only a Y selects an extra.
foreach ($answer in @('Y','n','y','','N')) { $global:oloTestAnswers.Enqueue($answer) }
$plan = @(& $installer -PlanOnly)
Assert ($plan.Count -eq 2) 'Mixed answers must select only one extra'
Assert ($plan[1].Name -eq 'phi3:mini') 'Wrong extra selected'
foreach ($i in 1..12) { $global:oloTestAnswers.Enqueue('y') }
$plan = @(& $installer -Audio 1 -Image 1 -Video 1 -PlanOnly)
Assert ($plan.Count -eq 12) 'All opt-in models should be selected'
Assert ($global:oloTestAnswers.Count -eq 0) 'Not all models prompted'
# Exercise installation without Docker or network: capture actual command arguments.
$global:oloTestCommands = [Collections.Generic.List[string]]::new()
function docker {
    $global:oloTestCommands.Add(($args -join ' '))
    $global:LASTEXITCODE = 0
    if ($args[0] -eq 'exec' -and $args[2] -eq 'test') { $global:LASTEXITCODE = 1 }
}
function curl.exe {
    $index = [Array]::IndexOf($args, '--output')
    [IO.File]::WriteAllText($args[$index+1], 'mock weights')
    $global:LASTEXITCODE = 0
}
& $installer -Audio 1 -Image 1 -Video 1
Assert ($global:oloTestCommands.Contains('exec olo-ollama ollama pull llama3.2:1b')) 'Default text not pulled'
Assert (($global:oloTestCommands | Where-Object { $_ -match '^restart ' }).Count -eq 3) 'Wrong servers restarted'
Assert (($global:oloTestCommands | Where-Object { $_ -match '^cp ' }).Count -eq 4) 'Expected three checkpoints and one audio config'
Assert (-not ($global:oloTestCommands | Where-Object { $_ -match 'phi3|mistral|qwen|svd_xt|sd_xl' })) 'Unselected models installed'
Write-Host 'PASS: category combinations, defaults, individual choices, and mocked installation.'