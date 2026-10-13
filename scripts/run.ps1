$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$jdkPath = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME/bin/javac.exe")) { $env:JAVA_HOME } elseif (Test-Path 'C:/Program Files/Java/jdk-27/bin/java.exe') { 'C:/Program Files/Java/jdk-27' } else { throw 'Configure JAVA_HOME con JDK 23 o superior.' }
if (!(Test-Path build/piscicola.jar)) { & "$PSScriptRoot/build.ps1" }
New-Item -ItemType Directory -Force build/javafx-cache | Out-Null
& "$jdkPath/bin/java.exe" '-Djavafx.cachedir=build/javafx-cache' --enable-native-access=javafx.graphics --module-path lib --add-modules javafx.controls -cp 'build/piscicola.jar;lib/gson-2.13.2.jar' piscicola.App
