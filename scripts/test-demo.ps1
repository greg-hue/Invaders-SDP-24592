[CmdletBinding()]
param([string]$JdkRoot)
$ErrorActionPreference = 'Stop'
$taskBuild = & (Join-Path $PSScriptRoot 'build.ps1') -JdkRoot $JdkRoot
$taskRoot = $taskBuild.RepositoryPath
# Compile sources together so these standalone tests need no test framework.
$taskSources = @(Get-ChildItem -LiteralPath (Join-Path $taskRoot 'src') -Recurse -Filter '*.java' |
    Select-Object -ExpandProperty FullName)
$taskSources += @((Join-Path $taskRoot 'tests\demo\DemoPlaybackTest.java'),
    (Join-Path $taskRoot 'tests\engine\DemoNavigationTest.java'),
    (Join-Path $taskRoot 'tests\engine\DemoRequirementsTest.java'),
    (Join-Path $taskRoot 'tests\screen\DemoGameplayTest.java'))
& $taskBuild.CompilerExecutable -encoding UTF-8 -d $taskBuild.ClassDirectory @taskSources
if ($LASTEXITCODE -ne 0) { throw 'Demo test compilation failed.' }
$taskClasspath = $taskBuild.ClassDirectory + [IO.Path]::PathSeparator + $taskBuild.ResourceDirectory
Push-Location -LiteralPath $taskRoot
try {
    foreach ($taskSuite in @('demo.DemoPlaybackTest', 'screen.DemoGameplayTest', 'engine.DemoNavigationTest', 'engine.DemoRequirementsTest')) {
        & $taskBuild.JavaExecutable '-Djava.awt.headless=true' -ea -cp $taskClasspath $taskSuite
        if ($LASTEXITCODE -ne 0) { throw "$taskSuite failed." }
    }
    & $taskBuild.JavaExecutable '-Djava.awt.headless=true' '-Dinvaders.demo.autoAdvance=true' '-Dinvaders.demo.scale=0.01' -cp $taskClasspath engine.Core --demo
    if ($LASTEXITCODE -ne 0) { throw 'Demo scene tour failed.' }
    Write-Host 'All demo scenes completed.'
} finally { Pop-Location }
