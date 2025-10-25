@echo off
REM Script para crear/actualizar la rama home-streamzone, commitear cambios y hacer push al remoto
REM Ejecutar desde cmd.exe (no desde PowerShell) para evitar diferencias en operadores como || y en comentarios.
cd /d "%~dp0"
echo Switching/creating branch home-streamzone...
git fetch --all
git checkout -B home-streamzone
echo Adding all changes...
git add -A
echo Checking for staged changes...
git diff --cached --quiet
if %errorlevel% equ 0 (
  echo No hay cambios para commitear.
) else (
  echo Committing changes...
  git commit -m "feat(home): remove web emulation; keep native Home; cleanup logo placeholders; backup web assets"
)
echo Pushing branch home-streamzone to origin...
git push -u origin home-streamzone
echo Done. Use 'git status' and 'git log -1' to verify.
pause
