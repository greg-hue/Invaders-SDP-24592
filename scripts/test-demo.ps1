[CmdletBinding()]
param([string]$JdkRoot)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
if (-not $JdkRoot) { $JdkRoot = $env:JAVA_HOME }
$taskJava = if ($JdkRoot) { Join-Path $JdkRoot 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
$taskJavac = Join-Path (Split-Path $taskJava -Parent) 'javac.exe'
$taskClasses = Join-Path $taskRoot 'out/demo-tests'
New-Item -ItemType Directory -Force -Path $taskClasses | Out-Null
$taskSources = @(Get-ChildItem (Join-Path $taskRoot 'src') -Recurse -Filter *.java | ForEach-Object FullName)
foreach ($taskFile in @('demo/DemoPlaybackTest.java','engine/DemoNavigationTest.java','engine/DemoRequirementsTest.java','screen/DemoGameplayTest.java')) {
    $taskSources += Join-Path (Join-Path $taskRoot 'test') $taskFile
}
& $taskJavac -encoding UTF-8 --release 17 -d $taskClasses @taskSources
if ($LASTEXITCODE -ne 0) { throw 'Demo test compilation failed.' }
Push-Location -LiteralPath $taskRoot
try {
    $taskClasspath = $taskClasses + [IO.Path]::PathSeparator + (Join-Path $taskRoot 'res')
    foreach ($taskSuite in @('demo.DemoPlaybackTest','screen.DemoGameplayTest','engine.DemoNavigationTest','engine.DemoRequirementsTest')) {
        & $taskJava '-Djava.awt.headless=true' -ea -cp $taskClasspath $taskSuite
        if ($LASTEXITCODE -ne 0) { throw "$taskSuite failed." }
    }
    & $taskJava '-Djava.awt.headless=true' '-Dinvaders.demo.autoAdvance=true' '-Dinvaders.demo.scale=0.001' -cp $taskClasspath engine.Core --demo
    if ($LASTEXITCODE -ne 0) { throw 'Demo scene tour failed.' }
    Write-Host 'All demo scenes completed.'
} finally { Pop-Location }
