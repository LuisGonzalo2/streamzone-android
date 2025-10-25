@echo off
REM Script corregido: Borra los archivos creados por el asistente: logo_*.xml y scripts temporales
cd /d "%~dp0"
SET DRAWABLE=%~dp0app\src\main\res\drawable
echo Directorio drawable: %DRAWABLE%
echo Eliminando logo_*.xml creados por el asistente (si existen)...
if exist "%DRAWABLE%\logo_*.xml" (
  del /Q "%DRAWABLE%\logo_*.xml"
  echo Eliminados.
) else (
  echo No se encontraron logo_*.xml para borrar.
)
echo Eliminando scripts temporales (si existen)...
if exist "%~dp0copy_logos_and_build.bat" del /Q "%~dp0copy_logos_and_build.bat"
if exist "%~dp0cleanup_logos_and_build.bat" del /Q "%~dp0cleanup_logos_and_build.bat"

echo Listado actual de logo_* en drawable:
dir /b "%DRAWABLE%\logo_*" || echo (ninguno)
echo Hecho. Recomendado: abrir Android Studio y hacer Build -> Clean Project; luego Build -> Rebuild Project.
pause
