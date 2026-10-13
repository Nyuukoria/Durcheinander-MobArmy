param([switch]$DependenciesOnly)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
. (Join-Path $PSScriptRoot 'bootstrap-runtime.ps1')
$Plugin = Join-Path $PSScriptRoot '..\plugin\DurcheinanderMobArmy-1.4.0.jar'
if (-not (Test-Path $Plugin)) { $Plugin = Join-Path $PSScriptRoot '..\target\DurcheinanderMobArmy-1.4.0.jar' }
if (-not $DependenciesOnly -and -not (Test-Path $Plugin)) { throw 'Plugin JAR missing. Download the full test ZIP, or build the repository with: python3 tools/build_pack.py then mvn clean package' }
$Server = Join-Path $PSScriptRoot 'server'
New-Item -ItemType Directory -Force $Server | Out-Null
$Jar = Join-Path $Server 'server.jar'
$Runtime = Initialize-MobArmyRuntime -IncludeGit:($DependenciesOnly -or -not (Test-Path $Jar))
if ($DependenciesOnly) { Write-Host 'Portable Java 25, Git and Bash verified.'; return }
if (-not (Test-Path $Jar)) {
    Write-Host '[3/3] Building Spigot 26.3. This first-time step can take several minutes...'
    $Build = Join-Path $PSScriptRoot 'buildtools'
    New-Item -ItemType Directory -Force $Build | Out-Null
    Invoke-WebRequest -UseBasicParsing 'https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar' -OutFile (Join-Path $Build 'BuildTools.jar')
    Push-Location $Build
    try { & $Runtime.Java -jar BuildTools.jar --rev 26.3 --compile SPIGOT; if ($LASTEXITCODE -ne 0) { throw 'Spigot BuildTools failed. Read the output above.' }
        $Built = Get-Item 'spigot-26.3.jar' -ErrorAction Stop
        Copy-Item $Built.FullName $Jar
    } finally { Pop-Location }
}
New-Item -ItemType Directory -Force (Join-Path $Server 'plugins') | Out-Null
Copy-Item $Plugin (Join-Path $Server 'plugins\DurcheinanderMobArmy.jar') -Force
$Properties = Join-Path $Server 'server.properties'
if (-not (Test-Path $Properties)) {
@'
server-ip=127.0.0.1
server-port=25565
online-mode=false
enforce-secure-profile=false
gamemode=survival
difficulty=normal
max-players=20
view-distance=6
simulation-distance=6
spawn-protection=0
enable-command-block=false
motd=Yuuki - Durcheinander MobArmy Local Test
'@ | Set-Content -Encoding ASCII $Properties
}
& (Join-Path $PSScriptRoot 'demo-settings.ps1') -ServerDirectory $Server
$Eula = Join-Path $Server 'eula.txt'
if (-not (Test-Path $Eula) -or (Get-Content $Eula -Raw) -notmatch 'eula=true') {
    Write-Host 'Read the Minecraft EULA: https://aka.ms/MinecraftEULA'
    $Accept = Read-Host 'Do YOU accept the Minecraft EULA? Type yes to continue'
    if ($Accept -ne 'yes') { 'eula=false' | Set-Content -Encoding ASCII $Eula; throw 'EULA not accepted. Server was not started.' }
    'eula=true' | Set-Content -Encoding ASCII $Eula
}
Write-Host 'Setup complete. Run start-server.bat, connect to localhost:25565, then type op YOUR_NAME in the server console.'
Write-Host 'Local test uses offline authentication and binds to 127.0.0.1. Keep these settings for PC-only testing.'
