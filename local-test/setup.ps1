$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Install a Java 25 JDK first: https://adoptium.net/temurin/releases/?version=25' }
$JavaVersion = (& java -version 2>&1 | Out-String)
if ($JavaVersion -notmatch 'version "(?:25|26|27)\.') { throw "Java 25+ is required. Detected: $JavaVersion" }
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { throw 'Install Git for Windows first: https://git-scm.com/download/win (include Git in PATH).' }
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) { throw 'A full Java 25 JDK (including javac) is required, not a JRE.' }
$GitRoot = Split-Path (Split-Path (Get-Command git).Source -Parent) -Parent
foreach ($GitBin in @((Join-Path $GitRoot 'usr\bin'), (Join-Path $GitRoot 'bin'))) {
    if (Test-Path $GitBin) { $env:PATH = "$GitBin;$env:PATH" }
}
if (-not (Get-Command bash -ErrorAction SilentlyContinue)) { throw 'Git Bash was not found. Install the standard Git for Windows distribution and rerun setup.' }
$Plugin = Join-Path $PSScriptRoot '..\plugin\DurcheinanderMobArmy-1.0.0.jar'
if (-not (Test-Path $Plugin)) { $Plugin = Join-Path $PSScriptRoot '..\target\DurcheinanderMobArmy-1.0.0.jar' }
if (-not (Test-Path $Plugin)) { throw 'Plugin JAR missing. Download the full test ZIP, or build the repository with: mvn clean package' }
$Server = Join-Path $PSScriptRoot 'server'
New-Item -ItemType Directory -Force $Server | Out-Null
$Jar = Join-Path $Server 'server.jar'
if (-not (Test-Path $Jar)) {
    $Build = Join-Path $PSScriptRoot 'buildtools'
    New-Item -ItemType Directory -Force $Build | Out-Null
    Invoke-WebRequest 'https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar' -OutFile (Join-Path $Build 'BuildTools.jar')
    Push-Location $Build
    try { & java -jar BuildTools.jar --rev 26.3 --compile SPIGOT; if ($LASTEXITCODE -ne 0) { throw 'Spigot BuildTools failed. Read the output above.' }
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
$Eula = Join-Path $Server 'eula.txt'
if (-not (Test-Path $Eula) -or (Get-Content $Eula -Raw) -notmatch 'eula=true') {
    Write-Host 'Read the Minecraft EULA: https://aka.ms/MinecraftEULA'
    $Accept = Read-Host 'Do YOU accept the Minecraft EULA? Type yes to continue'
    if ($Accept -ne 'yes') { 'eula=false' | Set-Content -Encoding ASCII $Eula; throw 'EULA not accepted. Server was not started.' }
    'eula=true' | Set-Content -Encoding ASCII $Eula
}
Write-Host 'Setup complete. Run start-server.bat, connect to localhost:25565, then type op YOUR_NAME in the server console.'
Write-Host 'Local test uses offline authentication and binds to 127.0.0.1. Keep these settings for PC-only testing.'
