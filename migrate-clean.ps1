[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [switch]$Force,
    [switch]$IncludeModels,
    [switch]$IncludeUltralytics
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

Write-Host "[clean] Project root: $ProjectRoot"
Write-Host "[clean] Force=$Force IncludeModels=$IncludeModels IncludeUltralytics=$IncludeUltralytics WhatIf=$WhatIfPreference"

$targets = @(
    "frontend/node_modules",
    "frontend/dist",
    "desktop/node_modules",
    "desktop/dist",
    "yolov8-detection/target",
    "data/images",
    "data/predict-results",
    "data/datasets",
    "runs",
    "health-report",
    "desktop/runtime/win-jre17"
)

if ($IncludeUltralytics) {
    $targets += "yolov8-detection/scripts/ultralytics-main"
}

$removed = 0
$skipped = 0

foreach ($relative in $targets) {
    $full = Join-Path $ProjectRoot $relative
    if (Test-Path $full) {
        if ($PSCmdlet.ShouldProcess($full, "Remove directory recursively")) {
            Remove-Item -LiteralPath $full -Recurse -Force:$Force -ErrorAction Stop
            Write-Host "[removed] $relative"
            $removed++
        }
    } else {
        Write-Host "[skip] Not found: $relative"
        $skipped++
    }
}

# Remove log files across workspace
$logFiles = Get-ChildItem -Path $ProjectRoot -Recurse -File -Filter *.log -ErrorAction SilentlyContinue
foreach ($log in $logFiles) {
    if ($PSCmdlet.ShouldProcess($log.FullName, "Remove log file")) {
        Remove-Item -LiteralPath $log.FullName -Force:$Force -ErrorAction SilentlyContinue
        Write-Host "[removed] $($log.FullName.Replace($ProjectRoot + '\\', ''))"
        $removed++
    }
}

if ($IncludeModels) {
    $modelFiles = Get-ChildItem -Path $ProjectRoot -Recurse -File -Filter *.pt -ErrorAction SilentlyContinue
    foreach ($model in $modelFiles) {
        if ($PSCmdlet.ShouldProcess($model.FullName, "Remove model file")) {
            Remove-Item -LiteralPath $model.FullName -Force:$Force -ErrorAction SilentlyContinue
            Write-Host "[removed] $($model.FullName.Replace($ProjectRoot + '\\', ''))"
            $removed++
        }
    }
} else {
    Write-Host "[keep] *.pt files retained (use -IncludeModels to delete them)"
}

Write-Host "[clean] Done. Removed=$removed, Skipped=$skipped"

