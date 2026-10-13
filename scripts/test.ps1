$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
& "$PSScriptRoot/build.ps1"
$jdkPath = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:/Program Files/Java/jdk-27' }
& "$jdkPath/bin/javac.exe" -encoding UTF-8 --release 23 -cp 'build/classes;lib/gson-2.13.2.jar' -d build/classes src/test/java/piscicola/ServiceTest.java
if ($LASTEXITCODE -ne 0) { throw 'Error al compilar las pruebas.' }
& "$jdkPath/bin/java.exe" -cp 'build/classes;lib/gson-2.13.2.jar' piscicola.ServiceTest
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas.' }
