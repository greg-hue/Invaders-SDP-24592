[CmdletBinding()]
param([string]$JdkRoot, [switch]$Demo,
    [ValidateRange(0.001, 100.0)][double]$DemoScale = 1,
    [string]$DemoScript, [string]$DemoProfile)
$ErrorActionPreference = 'Stop'
if (-not $Demo) {
    & (Join-Path $PSScriptRoot 'invoke-gradle.ps1') -JdkRoot $JdkRoot -Tasks @('run')
    exit $LASTEXITCODE
}
$taskRoot = Split-Path $PSScriptRoot -Parent
if (-not $JdkRoot) { $JdkRoot = $env:JAVA_HOME }
$taskJava = if ($JdkRoot) { Join-Path $JdkRoot 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
$taskJavac = Join-Path (Split-Path $taskJava -Parent) 'javac.exe'
$taskClasses = Join-Path $taskRoot 'out/demo-classes'
New-Item -ItemType Directory -Force -Path $taskClasses | Out-Null
$taskSources = @(Get-ChildItem (Join-Path $taskRoot 'src') -Filter *.java -Recurse | ForEach-Object FullName)
& $taskJavac -encoding UTF-8 --release 17 -d $taskClasses @taskSources
if ($LASTEXITCODE -ne 0) { throw 'Demo compilation failed.' }
$taskOptions = @('-Dfile.encoding=UTF-8', ('-Dinvaders.demo.scale=' + $DemoScale.ToString([Globalization.CultureInfo]::InvariantCulture)))
if ($DemoScript) { $taskOptions += '-Dinvaders.demo.script=' + (Resolve-Path -LiteralPath $DemoScript).Path }
if ($DemoProfile) { $taskOptions += '-Dinvaders.demo.profile=' + [IO.Path]::GetFullPath($DemoProfile) }
Push-Location -LiteralPath $taskRoot
try {
    & $taskJava @taskOptions -cp ($taskClasses + [IO.Path]::PathSeparator + (Join-Path $taskRoot 'res')) engine.Core --demo
    if ($LASTEXITCODE -ne 0) { throw 'Demo launch failed.' }
} finally { Pop-Location }
