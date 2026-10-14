@echo off
set "MOBARMY_JAVA=%~dp0runtime\java\bin\java.exe"
if not exist "%MOBARMY_JAVA%" (
  echo Run setup.bat first. It downloads the portable Java runtime automatically.
  pause
  exit /b 1
)
if not exist "%~dp0server\server.jar" (
  echo Run setup.bat first.
  pause
  exit /b 1
)
set "MOBARMY_PLUGIN=%~dp0..\plugin\DurcheinanderMobArmy-1.7.0.jar"
if not exist "%MOBARMY_PLUGIN%" set "MOBARMY_PLUGIN=%~dp0..\target\DurcheinanderMobArmy-1.7.0.jar"
if not exist "%MOBARMY_PLUGIN%" (
  echo Plugin JAR missing. Extract the complete updated ZIP.
  pause
  exit /b 1
)
if not exist "%~dp0server\plugins" mkdir "%~dp0server\plugins"
copy /y "%MOBARMY_PLUGIN%" "%~dp0server\plugins\DurcheinanderMobArmy.jar" >nul
if errorlevel 1 (
  echo Could not update the plugin. Stop the server before starting it again.
  pause
  exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo-settings.ps1"
if errorlevel 1 (
  echo Demo configuration failed. Server was not started.
  pause
  exit /b 1
)
cd /d "%~dp0server"
"%MOBARMY_JAVA%" -Xms1G -Xmx3G -jar server.jar nogui
pause
