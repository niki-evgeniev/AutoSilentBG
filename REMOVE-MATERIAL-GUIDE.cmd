@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\remove-material-guide.ps1"
if errorlevel 1 pause
