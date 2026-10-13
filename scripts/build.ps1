$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$jdkPath = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME/bin/javac.exe")) { $env:JAVA_HOME } elseif (Test-Path 'C:/Program Files/Java/jdk-27/bin/javac.exe') { 'C:/Program Files/Java/jdk-27' } else { throw 'Instale JDK 23 o superior y configure JAVA_HOME.' }
New-Item -ItemType Directory -Force build/classes | Out-Null
$sources = @(Get-ChildItem src/main/java -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& "$jdkPath/bin/javac.exe" -encoding UTF-8 --release 23 --module-path lib --add-modules javafx.controls -cp lib/gson-2.13.2.jar -d build/classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Error de compilación.' }
Copy-Item src/main/resources/* build/classes -Force
New-Item -ItemType Directory -Force build/tmp | Out-Null
& "$jdkPath/bin/jar.exe" "-J-Djava.io.tmpdir=$((Resolve-Path build/tmp).Path)" --create --file build/piscicola.jar --main-class piscicola.App -C build/classes .
if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el JAR.' }
Write-Host 'Compilación correcta: build/piscicola.jar'
