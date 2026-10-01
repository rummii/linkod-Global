@echo off
cd /d "%~dp0"
echo Connect your phone and computer to the same trusted Wi-Fi.
echo Open the Phone demo URL printed below.
call npm run demo:lan
pause
