@echo off
set "MOBARMY_JAVA=%~dp0runtime\java\bin\java.exe"
if not exist "%MOBARMY_JAVA%" (
  echo Run setup.bat first. It downloads the portable Java runtime automatically.
  pause
  exit /b 1
)
cd /d "%~dp0server"
if not exist server.jar (
  echo Run setup.bat first.
  pause
  exit /b 1
)
"%MOBARMY_JAVA%" -Xms1G -Xmx3G -jar server.jar nogui
pause
