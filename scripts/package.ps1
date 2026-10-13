$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
& "$PSScriptRoot/build.ps1"
$jdkPath = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:/Program Files/Java/jdk-27' }
if (Test-Path dist/GestionPiscicola) { throw 'Ya existe dist/GestionPiscicola. Conserve sus datos y mueva ese directorio antes de generar otro paquete.' }
New-Item -ItemType Directory -Force build/package-input,dist,build/package-temp | Out-Null
Copy-Item build/piscicola.jar,lib/gson-2.13.2.jar build/package-input -Force
& "$jdkPath/bin/jpackage.exe" --type app-image --name GestionPiscicola --app-version 1.0.0 --vendor 'Gestión Piscícola' --dest dist --temp build/package-temp --input build/package-input --main-jar piscicola.jar --main-class piscicola.App --module-path "$jdkPath/jmods;lib" --add-modules 'javafx.controls,java.desktop,java.naming,jdk.unsupported' --java-options '--add-modules=javafx.controls' --java-options '--enable-native-access=javafx.graphics' --java-options '-Dpiscicola.data=$APPDIR/data' --java-options '-Djavafx.cachedir=$APPDIR/cache'
if ($LASTEXITCODE -ne 0) { throw 'Error al crear el paquete Windows.' }
Copy-Item README.md dist/GestionPiscicola/LEEME.md
Write-Host 'Paquete creado: dist/GestionPiscicola/GestionPiscicola.exe'
