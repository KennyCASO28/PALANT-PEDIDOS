@echo off
setlocal
cd /d "%~dp0"

:: 1. Search for java.exe
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] No se encontro Java en el sistema. Asegurate de tener Java 21 instalado.
    pause
    exit /b
)

:: 2. Search for the executable JAR
set "JAR_FILE=target\PALANT-PEDIDOS-2.8.0.jar"
if not exist "%JAR_FILE%" (
    echo [ERROR] No se encontro el archivo JAR.
    echo Por favor, ejecuta 'mvn clean package' o usa la pestaña Maven en IntelliJ.
    pause
    exit /b
)

:: 3. Run the application
echo [INFO] Iniciando PALANT PEDIDOS...
java -Dprism.order=sw -Dprism.d3d=false -jar "%JAR_FILE%"

if %errorlevel% neq 0 (
    echo [ERROR] El programa se cerro con errores.
    pause
)
endlocal
