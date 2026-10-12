param([string]$ServerDirectory = (Join-Path $PSScriptRoot 'server'))
$ErrorActionPreference = 'Stop'

function Set-MobArmyDemoFlag {
    param([string]$Text, [string]$Name)
    $Lines = [System.Collections.Generic.List[string]]::new()
    foreach ($Line in ($Text -split '\r?\n')) { $Lines.Add($Line) }
    $Start = -1
    for ($Index = 0; $Index -lt $Lines.Count; $Index++) {
        if ($Lines[$Index] -match '^testing\s*:\s*(?:#.*)?$') { $Start = $Index; break }
    }
    if ($Start -lt 0) {
        if ($Lines | Where-Object { $_ -match '^testing\s*:' }) { throw 'The testing section must use normal YAML block formatting.' }
        $Lines.Add('testing:'); $Lines.Add("  ${Name}: true")
    } else {
        $End = $Start + 1
        while ($End -lt $Lines.Count -and ($Lines[$End] -match '^\s|^#|^$')) { $End++ }
        $Found = $false
        for ($Index = $Start + 1; $Index -lt $End; $Index++) {
            if ($Lines[$Index] -match "^  $([Regex]::Escape($Name))\s*:") { $Lines[$Index] = "  ${Name}: true"; $Found = $true; break }
        }
        if (-not $Found) { $Lines.Insert($Start + 1, "  ${Name}: true") }
    }
    return ($Lines -join [Environment]::NewLine)
}

$ConfigDirectory = Join-Path $ServerDirectory 'plugins\DurcheinanderMobArmy'
New-Item -ItemType Directory -Force $ConfigDirectory | Out-Null
$Config = Join-Path $ConfigDirectory 'config.yml'
if (-not (Test-Path $Config)) {
    $Template = Join-Path $PSScriptRoot '..\source\src\main\resources\config.yml'
    if (-not (Test-Path $Template)) { $Template = Join-Path $PSScriptRoot '..\src\main\resources\config.yml' }
    if (-not (Test-Path $Template)) { throw 'Default raid config is missing. Extract the complete test ZIP before starting.' }
    Copy-Item $Template $Config
}
$Text = [IO.File]::ReadAllText($Config)
$Text = Set-MobArmyDemoFlag -Text $Text -Name 'solo'
$Text = Set-MobArmyDemoFlag -Text $Text -Name 'disable-pvp'
[IO.File]::WriteAllText($Config, $Text, [Text.UTF8Encoding]::new($false))

$Properties = Join-Path $ServerDirectory 'server.properties'
$Text = if (Test-Path $Properties) { [IO.File]::ReadAllText($Properties) } else { '' }
if ($Text -match '(?m)^\s*pvp\s*[=:]') { $Text = [Regex]::Replace($Text, '(?m)^\s*pvp\s*[=:][^\r\n]*', 'pvp=false') }
else { $Text += [Environment]::NewLine + 'pvp=false' + [Environment]::NewLine }
[IO.File]::WriteAllText($Properties, $Text, [Text.UTF8Encoding]::new($false))
Write-Host 'Local demo: solo entry enabled; player PvP disabled in all worlds.'
