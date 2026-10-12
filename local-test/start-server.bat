@echo off
cd /d "%~dp0server"
if not exist server.jar (
  echo Run setup.bat first.
  pause
  exit /b 1
)
java -Xms1G -Xmx3G -jar server.jar nogui
pause
