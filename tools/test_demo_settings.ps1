$ErrorActionPreference = 'Stop'
$Root = Split-Path $PSScriptRoot -Parent
$Settings = Join-Path $Root 'local-test/demo-settings.ps1'
$Temporary = Join-Path ([IO.Path]::GetTempPath()) ('mobarmy-demo-' + [guid]::NewGuid().ToString())
New-Item -ItemType Directory -Force $Temporary | Out-Null
try {
    & $Settings -ServerDirectory $Temporary
    $Config = Join-Path $Temporary 'plugins/DurcheinanderMobArmy/config.yml'
    $Properties = Join-Path $Temporary 'server.properties'
    $Text = [IO.File]::ReadAllText($Config)
    if ($Text -notmatch '(?m)^  solo: true\s*$' -or $Text -notmatch '(?m)^  disable-pvp: true\s*$') { throw 'Fresh demo does not enable solo mode and disable PvP.' }
    if ($Text -notmatch '(?m)^  disable-wipe-cooldown: true\s*$') { throw 'Fresh demo leaves wipe cooldown enabled.' }
    if ($Text -notmatch '(?m)^  local-resource-pack: true\s*$') { throw 'Local resource pack is not enabled.' }
    if ($Text -notmatch 'minimum-players: 5') { throw 'Demo settings rewrote the production week settings.' }
    if ([IO.File]::ReadAllText($Properties) -notmatch '(?m)^pvp=false\s*$') { throw 'Demo properties did not disable PvP.' }

    [IO.File]::WriteAllText($Config, "# custom config`ntesting:`n  solo: false`n  another-setting: keep`nweeks:`n  custom: preserve`n")
    [IO.File]::WriteAllText($Properties, "motd=Keep my MOTD`npvp=true`nserver-port=25566`n")
    $Plugins = Join-Path $Temporary 'plugins'
    [IO.File]::WriteAllText((Join-Path $Plugins 'DurcheinanderMobArmy-1.1.0.jar'), 'old plugin')
    [IO.File]::WriteAllText((Join-Path $Plugins 'DurcheinanderMobArmy.jar'), 'current plugin')
    [IO.File]::WriteAllText((Join-Path $Plugins 'AnotherPlugin.jar'), 'other plugin')
    & $Settings -ServerDirectory $Temporary
    if (Test-Path (Join-Path $Plugins 'DurcheinanderMobArmy-1.1.0.jar')) { throw 'Old versioned plugin remains discoverable.' }
    if ([IO.File]::ReadAllText((Join-Path $Plugins 'DurcheinanderMobArmy/old-jars/DurcheinanderMobArmy-1.1.0.jar')) -ne 'old plugin') { throw 'Old plugin was not backed up.' }
    if ([IO.File]::ReadAllText((Join-Path $Plugins 'DurcheinanderMobArmy.jar')) -ne 'current plugin' -or -not (Test-Path (Join-Path $Plugins 'AnotherPlugin.jar'))) { throw 'Current or unrelated plugin was changed.' }
    $Once = [IO.File]::ReadAllText($Config)
    if ($Once -notmatch 'another-setting: keep' -or $Once -notmatch 'custom: preserve') { throw 'Existing demo configuration was not preserved.' }
    if ($Once -notmatch '(?m)^  disable-pvp: true\s*$' -or $Once -notmatch '(?m)^  solo: true\s*$') { throw 'Existing demo flags were not updated.' }
    $SavedProperties = [IO.File]::ReadAllText($Properties)
    if ($SavedProperties -notmatch 'motd=Keep my MOTD' -or $SavedProperties -notmatch 'server-port=25566' -or $SavedProperties -match 'pvp=true') { throw 'Existing server properties were corrupted.' }
    & $Settings -ServerDirectory $Temporary
    if ([IO.File]::ReadAllText($Config) -ne $Once -or [IO.File]::ReadAllText($Properties) -ne $SavedProperties) { throw 'Repeated demo setup changed the files again.' }
    if ([IO.File]::ReadAllBytes($Config)[0] -eq 239) { throw 'Config contains an unexpected UTF-8 BOM.' }
    Write-Host 'Fresh setup, existing-server upgrade, unrelated-setting preservation and repeat setup passed.'
} finally { Remove-Item $Temporary -Recurse -Force }
