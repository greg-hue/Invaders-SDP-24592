[CmdletBinding()]
param([string]$JdkRoot, [switch]$Demo,
    [ValidateRange(0.001, 100.0)][double]$DemoScale = 1,
    [string]$DemoScript, [string]$DemoProfile)
$ErrorActionPreference = 'Stop'
$taskBuild = & (Join-Path $PSScriptRoot 'build.ps1') -JdkRoot $JdkRoot
$taskClasspath = $taskBuild.ClassDirectory + [IO.Path]::PathSeparator + $taskBuild.ResourceDirectory
Push-Location -LiteralPath $taskBuild.RepositoryPath
try {
    $taskJavaArguments = @()
    if ($Demo) {
        $taskJavaArguments += '-Dinvaders.demo.scale=' + $DemoScale.ToString([Globalization.CultureInfo]::InvariantCulture)
        if ($DemoScript) { $taskJavaArguments += '-Dinvaders.demo.script=' + (Resolve-Path -LiteralPath $DemoScript).Path }
        if ($DemoProfile) { $taskJavaArguments += '-Dinvaders.demo.profile=' + [IO.Path]::GetFullPath($DemoProfile) }
    }
    $taskJavaArguments += @('-cp', $taskClasspath, 'engine.Core')
    if ($Demo) { $taskJavaArguments += '--demo' }
    & $taskBuild.JavaExecutable @taskJavaArguments
    if ($LASTEXITCODE -ne 0) { throw 'Game launch failed.' }
} finally { Pop-Location }
