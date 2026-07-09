# Builds Brutal Typing for every supported Minecraft version and collects jars into dist\
# Usage:  .\build-all.ps1
$ErrorActionPreference = "Stop"

# ALWAYS force the ATLauncher JDK 25: system-wide JAVA_HOME is Microsoft JDK 21 and cannot compile 26.x targets.
$env:JAVA_HOME = "C:\Users\Windows 11\AppData\Roaming\ATLauncher\runtimes\minecraft\java-runtime-epsilon\windows-x64\java-runtime-epsilon"

$targets = @('1.21.1', '1.21.3', '1.21.4', '1.21.5', '1.21.8', '1.21.10', '1.21.11', '26.1.2', '26.2')
$failed = @()

foreach ($t in $targets) {
    Write-Host "=== Building target $t ===" -ForegroundColor Cyan
    & .\gradlew.bat build "-Ptarget=$t" --console=plain -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "FAILED: $t" -ForegroundColor Red
        $failed += $t
    }
}

New-Item -ItemType Directory -Force dist | Out-Null
Copy-Item "build\libs\brutaltyping-*+mc*.jar" dist\ -Force
Write-Host "`nJars in dist\:" -ForegroundColor Green
Get-ChildItem dist\*.jar | Select-Object -ExpandProperty Name

if ($failed.Count -gt 0) {
    Write-Host "`nFailed targets: $($failed -join ', ')" -ForegroundColor Red
    exit 1
}
