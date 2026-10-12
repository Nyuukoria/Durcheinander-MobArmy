# Portable dependencies live beside the server. Nothing is installed system-wide.
function Get-MobArmyDownload {
    param([string]$Url, [string]$Destination, [string]$Sha256)
    if ($Sha256 -notmatch '^[a-fA-F0-9]{64}$') { throw 'The download provider did not supply a valid SHA-256 checksum.' }
    if ((Test-Path $Destination) -and ((Get-FileHash $Destination -Algorithm SHA256).Hash -ieq $Sha256)) { return }
    $Partial = "$Destination.partial"
    for ($Attempt = 1; $Attempt -le 3; $Attempt++) {
        try {
            Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Partial -TimeoutSec 900 -UserAgent 'MobArmy-Local-Setup'
            if ((Get-FileHash $Partial -Algorithm SHA256).Hash -ine $Sha256) { throw 'Downloaded file checksum did not match. Please rerun setup.' }
            Move-Item $Partial $Destination -Force
            return
        } catch {
            Remove-Item $Partial -Force -ErrorAction SilentlyContinue
            if ($Attempt -eq 3) { throw }
            Write-Host "Download interrupted; retrying ($Attempt/3)..."
        }
    }
}

function Initialize-MobArmyRuntime {
    param([switch]$IncludeGit)
    if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT -or -not [Environment]::Is64BitOperatingSystem) {
        throw 'The automatic setup is for 64-bit Windows. On Linux use the source build instructions.'
    }
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
    $ProgressPreference = 'SilentlyContinue'
    $RuntimeRoot = Join-Path $PSScriptRoot 'runtime'
    $Downloads = Join-Path $RuntimeRoot 'downloads'
    New-Item -ItemType Directory -Force $Downloads | Out-Null
    $JavaHome = Join-Path $RuntimeRoot 'java'
    $Java = Join-Path $JavaHome 'bin\java.exe'
    $Javac = Join-Path $JavaHome 'bin\javac.exe'
    if (-not (Test-Path $Java) -or -not (Test-Path $Javac)) {
        Write-Host '[1/3] Downloading portable Java 25 automatically...'
        $Assets = @(Invoke-RestMethod -Uri 'https://api.adoptium.net/v3/assets/latest/25/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse' -TimeoutSec 90 -UserAgent 'MobArmy-Local-Setup')
        if ($Assets.Count -eq 0) { throw 'Adoptium did not return a Windows Java 25 package. Check your internet connection and rerun setup.' }
        $Package = $Assets[0].binary.package
        $Archive = Join-Path $Downloads 'java25-windows.zip'
        Get-MobArmyDownload -Url $Package.link -Destination $Archive -Sha256 $Package.checksum
        Unblock-File -LiteralPath $Archive
        $Stage = Join-Path $RuntimeRoot 'java-extract'
        if (Test-Path $Stage) { Remove-Item $Stage -Recurse -Force }
        Expand-Archive -LiteralPath $Archive -DestinationPath $Stage -Force
        $Roots = @(Get-ChildItem $Stage -Directory | Where-Object { (Test-Path (Join-Path $_.FullName 'bin\java.exe')) -and (Test-Path (Join-Path $_.FullName 'bin\javac.exe')) })
        if ($Roots.Count -ne 1) { throw 'Unexpected Java ZIP layout. Java has not been installed.' }
        if (Test-Path $JavaHome) { Remove-Item $JavaHome -Recurse -Force }
        Move-Item $Roots[0].FullName $JavaHome
        Remove-Item $Stage -Recurse -Force
    }
    $JavaVersion = (& $Java --version | Out-String)
    if ($LASTEXITCODE -ne 0 -or $JavaVersion -notmatch '(?m)^(?:openjdk|java) 25(?:\.|\s)') { throw "The portable Java runtime failed its Java 25 check: $JavaVersion" }
    $JavacVersion = (& $Javac -version | Out-String)
    if ($LASTEXITCODE -ne 0 -or $JavacVersion -notmatch 'javac 25(?:\.|\s)') { throw 'The portable Java compiler is missing or unusable. Delete local-test/runtime/java and rerun setup.' }
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;$env:PATH"
    $GitHome = Join-Path $RuntimeRoot 'git'
    if ($IncludeGit) {
        $Git = Join-Path $GitHome 'cmd\git.exe'
        $Bash = Join-Path $GitHome 'usr\bin\bash.exe'
        if (-not (Test-Path $Git) -or -not (Test-Path $Bash)) {
            Write-Host '[2/3] Downloading portable Git and Git Bash automatically...'
            $Release = Invoke-RestMethod -Uri 'https://api.github.com/repos/git-for-windows/git/releases/latest' -TimeoutSec 90 -UserAgent 'MobArmy-Local-Setup'
            $Packages = @($Release.assets | Where-Object { $_.name -like 'PortableGit-*-64-bit.7z.exe' })
            if ($Packages.Count -ne 1) { throw 'Git for Windows did not return a portable 64-bit release. Please rerun setup later.' }
            $Package = $Packages[0]
            $Checksum = [string]$Package.digest
            if (-not $Checksum.StartsWith('sha256:')) { throw 'The Git release did not provide a SHA-256 checksum.' }
            $Archive = Join-Path $Downloads 'portable-git.7z.exe'
            Get-MobArmyDownload -Url $Package.browser_download_url -Destination $Archive -Sha256 $Checksum.Substring(7)
            Unblock-File -LiteralPath $Archive
            $Stage = Join-Path $RuntimeRoot 'git-extract'
            if (Test-Path $Stage) { Remove-Item $Stage -Recurse -Force }
            # Official PortableGit is a self-extracting archive, not a system installer.
            $Extraction = Start-Process -FilePath $Archive -ArgumentList @('-y', "-o`"$Stage`"") -Wait -PassThru
            if ($Extraction.ExitCode -ne 0 -or -not (Test-Path (Join-Path $Stage 'cmd\git.exe')) -or -not (Test-Path (Join-Path $Stage 'usr\bin\bash.exe'))) { throw 'Portable Git extraction failed. Please rerun setup.' }
            if (Test-Path $GitHome) { Remove-Item $GitHome -Recurse -Force }
            Move-Item $Stage $GitHome
        }
        $env:PATH = "$GitHome\cmd;$GitHome\bin;$GitHome\usr\bin;$env:PATH"
        $GitVersion = (& $Git --version | Out-String)
        if ($LASTEXITCODE -ne 0 -or $GitVersion -notmatch '^git version ') { throw 'Portable Git could not start.' }
        $BashVersion = (& $Bash --version | Out-String)
        if ($LASTEXITCODE -ne 0 -or $BashVersion -notmatch 'GNU bash') { throw 'Portable Git Bash could not start.' }
    }
    return [PSCustomObject]@{ Java = $Java; JavaHome = $JavaHome; GitHome = $GitHome }
}
