
; Script de Instalacion Completo para PALANT-PEDIDOS (Incluye Java Runtime)
; Generado por Antigravity

[Setup]
AppName=Palant Pedidos
AppVersion=2.8.0
AppPublisher=PALANT
DefaultDirName={pf}\PalantPedidos
DefaultGroupName=Palant Pedidos
OutputBaseFilename=Instalar_Palant_Pedidos_V2_8
Compression=lzma
SolidCompression=yes
SetupIconFile=src\main\resources\vectors\logo_small.ico
UninstallDisplayIcon={app}\PalantPedidos.exe

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; Copiamos toda la carpeta generada por jpackage (Incluye el EXE y el JRE de Java 21)
Source: "PalantPedidos\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; Copiamos tambien el icono suelto por si acaso
Source: "src\main\resources\vectors\logo_small.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
; Usamos el propio EXE como fuente del icono, que jpackage ya lo dejo bonito
Name: "{group}\Palant Pedidos"; Filename: "{app}\PalantPedidos.exe"; IconFilename: "{app}\PalantPedidos.exe"
Name: "{commondesktop}\Palant Pedidos"; Filename: "{app}\PalantPedidos.exe"; IconFilename: "{app}\PalantPedidos.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\PalantPedidos.exe"; Description: "{cm:LaunchProgram,Palant Pedidos}"; Flags: nowait postinstall skipifsilent
