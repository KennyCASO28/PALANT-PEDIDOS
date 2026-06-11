@echo off
setlocal enabledelayedexpansion
echo.
echo ====================================================
echo   AUTODETECTOR DE EMPAQUETADO PALANT v2.8.0
echo ====================================================
echo.

:: 1. Verificación del JAR
if not exist "target\PALANT-PEDIDOS-2.8.0.jar" (
    echo [ERROR] No encuentro el archivo: "target\PALANT-PEDIDOS-2.8.0.jar"
    echo Por favor, ve a IntelliJ y dale a Maven -> Lifecycle -> Package primero.
    pause
    exit /b 1
)

:: 2. Buscar jpackage (Comando Vital)
set JP_PATH=jpackage
where jpackage >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [AVISO] jpackage no esta en el PATH. Buscando en carpetas comunes...
    
    :: Buscar en Program Files
    for /d %%D in ("C:\Program Files\Java\jdk-*") do (
        if exist "%%D\bin\jpackage.exe" set JP_PATH="%%D\bin\jpackage"
    )
    
    :: Si aun no lo encontramos, intentamos con JAVA_HOME
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\jpackage.exe" set JP_PATH="%JAVA_HOME%\bin\jpackage"
    )
)

:: Verificar si al final lo encontramos
%JP_PATH% --version >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] No se pudo encontrar la herramienta 'jpackage'.
    echo Asegurate de tener instalado el JDK 21 (o superior) de Java.
    echo Puedes descargarlo de: https://adoptium.net/
    pause
    exit /b 1
)

echo Usando: %JP_PATH%
echo.

:: 3. Limpieza
if exist PalantPedidos (
    echo Borrando carpeta antigua...
    rmdir /S /Q PalantPedidos
)

:: 4. JPackage
echo [2/3] Generando carpeta PalantPedidos (Ten paciencia, demora)...
%JP_PATH% --name PalantPedidos ^
  --input target/ ^
  --main-jar PALANT-PEDIDOS-2.8.0.jar ^
  --main-class org.example.Launcher ^
  --type app-image ^
  --icon src\main\resources\vectors\logo_small.ico ^
  --win-shortcut ^
  --vendor PALANT ^
  --app-version 2.8.0

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Fallo al crear el empaquetado.
    pause
    exit /b 1
)

echo.
echo [3/3] ¡LISTO! Todo generado con exito.
echo Ahora abre Inno Setup y compila "installer_palant.iss".
echo.
pause
