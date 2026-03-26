param(
    [Parameter(Mandatory = $true)]
    [string]$DatasetPath,

    [Parameter(Mandatory = $true)]
    [string]$ModelPath,

    [string]$PythonExe = "D:\AppDownload\Anaconda3\envs\yolov8\python.exe",

    [string]$OutputDir = ".\health-report",

    [switch]$RunPredict,

    [string]$PredictSource = "",

    [ValidateSet("auto", "cpu", "0", "1")]
    [string]$Device = "auto",

    [double]$Conf = 0.05,
    [double]$Iou = 0.50,
    [int]$ImgSz = 640
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$checks = New-Object System.Collections.Generic.List[object]

function Add-Check {
    param(
        [string]$Name,
        [string]$Status,
        [string]$Detail,
        [string]$Fix = ""
    )
    $checks.Add([pscustomobject]@{
        Name   = $Name
        Status = $Status
        Detail = $Detail
        Fix    = $Fix
    })
}

function Get-FileCount {
    param([string]$Path, [string[]]$Patterns)
    if (-not (Test-Path -LiteralPath $Path)) { return 0 }
    $total = 0
    foreach ($p in $Patterns) {
        $total += (Get-ChildItem -LiteralPath $Path -File -Filter $p -ErrorAction SilentlyContinue | Measure-Object).Count
    }
    return $total
}

function Get-SetPath {
    param([string]$Root, [string]$SetName, [string[]]$Candidates)
    foreach ($c in $Candidates) {
        $full = Join-Path $Root "$SetName/$c"
        if (Test-Path -LiteralPath $full) { return $full }
    }
    return ""
}

$start = Get-Date

# 1) Basic paths
if (Test-Path -LiteralPath $DatasetPath) {
    Add-Check "Dataset path" "PASS" "Found: $DatasetPath"
} else {
    Add-Check "Dataset path" "FAIL" "Not found: $DatasetPath" "Use a valid dataset folder containing data.yaml"
}

if (Test-Path -LiteralPath $ModelPath) {
    Add-Check "Model path" "PASS" "Found: $ModelPath"
} else {
    Add-Check "Model path" "FAIL" "Not found: $ModelPath" "Point to trained best.pt"
}

if (Test-Path -LiteralPath $PythonExe) {
    Add-Check "Python path" "PASS" "Found: $PythonExe"
} else {
    Add-Check "Python path" "FAIL" "Not found: $PythonExe" "Set -PythonExe to your conda python path"
}

# 2) Parse data.yaml
$dataYamlPath = Join-Path $DatasetPath "data.yaml"
$nc = $null
$trainRel = ""
$valRel = ""
$testRel = ""
$namesRaw = ""

if (Test-Path -LiteralPath $dataYamlPath) {
    Add-Check "data.yaml" "PASS" "Found: $dataYamlPath"
    $yaml = Get-Content -LiteralPath $dataYamlPath -Raw

    if ($yaml -match "(?m)^\s*nc\s*:\s*(\d+)\s*$") { $nc = [int]$Matches[1] }
    if ($yaml -match "(?m)^\s*train\s*:\s*(.+?)\s*$") { $trainRel = $Matches[1].Trim() }
    if ($yaml -match "(?m)^\s*val\s*:\s*(.+?)\s*$") { $valRel = $Matches[1].Trim() }
    if ($yaml -match "(?m)^\s*test\s*:\s*(.+?)\s*$") { $testRel = $Matches[1].Trim() }
    if ($yaml -match "(?m)^\s*names\s*:\s*(.+?)\s*$") { $namesRaw = $Matches[1].Trim() }

    if ($null -eq $nc) {
        Add-Check "data.yaml.nc" "FAIL" "Missing 'nc'" "Set nc to class count, e.g. nc: 1"
    } else {
        Add-Check "data.yaml.nc" "PASS" "nc=$nc"
    }

    if ([string]::IsNullOrWhiteSpace($namesRaw)) {
        Add-Check "data.yaml.names" "WARN" "Missing 'names'" "Set names, e.g. names: [apple]"
    } elseif ($namesRaw -eq "[object]") {
        Add-Check "data.yaml.names" "WARN" "names is [object]" "Consider explicit name, e.g. [apple]"
    } else {
        Add-Check "data.yaml.names" "PASS" "names=$namesRaw"
    }
} else {
    Add-Check "data.yaml" "FAIL" "Not found: $dataYamlPath" "Dataset root must contain data.yaml"
}

# 3) Split structure and image-label matching
$imgExt = @("*.jpg", "*.jpeg", "*.png", "*.bmp", "*.webp")
$setSummaries = @()

foreach ($setName in @("train", "val", "test")) {
    $imgDir = Get-SetPath -Root $DatasetPath -SetName $setName -Candidates @("imgs", "images")
    $labelDir = Join-Path $DatasetPath "$setName/labels"

    if ([string]::IsNullOrWhiteSpace($imgDir)) {
        Add-Check "$setName images dir" "FAIL" "Missing $setName/imgs or $setName/images" "Create $setName/imgs and copy images"
        continue
    }
    if (-not (Test-Path -LiteralPath $labelDir)) {
        Add-Check "$setName labels dir" "FAIL" "Missing: $labelDir" "Create $setName/labels and copy YOLO txt labels"
        continue
    }

    $imgFiles = Get-ChildItem -LiteralPath $imgDir -File -Include $imgExt -ErrorAction SilentlyContinue
    if (-not $imgFiles) {
        $imgFiles = @()
        foreach ($pat in $imgExt) {
            $imgFiles += Get-ChildItem -LiteralPath $imgDir -File -Filter $pat -ErrorAction SilentlyContinue
        }
    }
    $labelFiles = Get-ChildItem -LiteralPath $labelDir -File -Filter *.txt -ErrorAction SilentlyContinue

    $imgCount = ($imgFiles | Measure-Object).Count
    $labelCount = ($labelFiles | Measure-Object).Count

    if ($imgCount -eq 0) {
        Add-Check "$setName images count" "FAIL" "0 images in $imgDir" "Put images into $setName/imgs"
    } else {
        Add-Check "$setName images count" "PASS" "$imgCount images"
    }

    if ($labelCount -eq 0) {
        Add-Check "$setName labels count" "FAIL" "0 labels in $labelDir" "Put label txt files into $setName/labels"
    } else {
        Add-Check "$setName labels count" "PASS" "$labelCount labels"
    }

    $imgBases = @{}
    foreach ($f in $imgFiles) { $imgBases[$f.BaseName] = $true }
    $labelBases = @{}
    foreach ($f in $labelFiles) { $labelBases[$f.BaseName] = $true }

    $missingLabel = @($imgBases.Keys | Where-Object { -not $labelBases.ContainsKey($_) })
    $orphanLabel = @($labelBases.Keys | Where-Object { -not $imgBases.ContainsKey($_) })

    if ($missingLabel.Count -eq 0 -and $orphanLabel.Count -eq 0) {
        Add-Check "$setName pairing" "PASS" "Image/label basenames are aligned"
    } else {
        $detail = "missingLabel=$($missingLabel.Count), orphanLabel=$($orphanLabel.Count)"
        Add-Check "$setName pairing" "WARN" $detail "Ensure each image has same-basename label .txt"
    }

    $setSummaries += [pscustomobject]@{
        setName = $setName
        images = $imgCount
        labels = $labelCount
        missingLabel = $missingLabel.Count
        orphanLabel = $orphanLabel.Count
    }
}

# 4) Label format sanity
$labelIssues = New-Object System.Collections.Generic.List[string]
if ($null -ne $nc) {
    foreach ($txt in Get-ChildItem -LiteralPath $DatasetPath -Recurse -File -Filter *.txt -ErrorAction SilentlyContinue | Where-Object { $_.FullName -match "\\(train|val|test)\\labels\\" }) {
        $lineNo = 0
        foreach ($line in Get-Content -LiteralPath $txt.FullName) {
            $lineNo++
            if ([string]::IsNullOrWhiteSpace($line)) { continue }
            $parts = $line.Trim() -split "\s+"
            if ($parts.Count -ne 5) {
                $labelIssues.Add("$($txt.Name):$lineNo bad_col_count=$($parts.Count)")
                continue
            }
            $cid = -1
            if (-not [int]::TryParse($parts[0], [ref]$cid)) {
                $labelIssues.Add("$($txt.Name):$lineNo bad_class_id=$($parts[0])")
                continue
            }
            if ($cid -lt 0 -or $cid -ge $nc) {
                $labelIssues.Add("$($txt.Name):$lineNo class_out_of_range=$cid")
            }
            for ($i = 1; $i -le 4; $i++) {
                $v = 0.0
                if (-not [double]::TryParse($parts[$i], [System.Globalization.NumberStyles]::Float, [System.Globalization.CultureInfo]::InvariantCulture, [ref]$v)) {
                    $labelIssues.Add("$($txt.Name):$lineNo not_float=$($parts[$i])")
                    continue
                }
                if ($v -lt 0.0 -or $v -gt 1.0) {
                    $labelIssues.Add("$($txt.Name):$lineNo out_of_range=$v")
                }
            }
        }
    }
}

if ($labelIssues.Count -eq 0) {
    Add-Check "Label format" "PASS" "YOLO txt format looks valid"
} else {
    $preview = ($labelIssues | Select-Object -First 5) -join "; "
    Add-Check "Label format" "FAIL" "$($labelIssues.Count) issues. Sample: $preview" "Fix label lines: class x y w h, values normalized to [0,1]"
}

# 5) Python env checks
$pythonReady = $false
$ultralyticsReady = $false
$gpuInfo = "unknown"

if (Test-Path -LiteralPath $PythonExe) {
    try {
        $pyVersion = & $PythonExe --version 2>&1
        Add-Check "Python execution" "PASS" ($pyVersion -join " ")
        $pythonReady = $true
    } catch {
        Add-Check "Python execution" "FAIL" "Failed to execute python" "Validate -PythonExe"
    }
}

if ($pythonReady) {
    try {
        & $PythonExe -c "import ultralytics; print(ultralytics.__version__)" 1> $null 2> $null
        Add-Check "Ultralytics" "PASS" "ultralytics import OK"
        $ultralyticsReady = $true
    } catch {
        Add-Check "Ultralytics" "FAIL" "ultralytics not importable" "pip install ultralytics"
    }

    try {
        $gpuInfo = & $PythonExe -c "import torch; print('cuda=' + str(torch.cuda.is_available()) + ', count=' + str(torch.cuda.device_count()))"
        if ($gpuInfo -match "cuda=True") {
            Add-Check "GPU" "PASS" ($gpuInfo -join " ")
        } else {
            Add-Check "GPU" "WARN" ($gpuInfo -join " ") "No CUDA GPU detected; training will be slower on CPU"
        }
    } catch {
        Add-Check "GPU" "WARN" "Cannot query torch CUDA" "Install torch with matching CUDA if GPU is needed"
    }
}

# 6) Optional quick predict run
$predictResult = "SKIPPED"
if ($RunPredict) {
    if (-not $ultralyticsReady) {
        Add-Check "Quick predict" "FAIL" "Skipped because ultralytics is unavailable" "Install ultralytics first"
    } else {
        if ([string]::IsNullOrWhiteSpace($PredictSource)) {
            $candidate = Get-SetPath -Root $DatasetPath -SetName "val" -Candidates @("imgs", "images")
            if (-not [string]::IsNullOrWhiteSpace($candidate)) {
                $PredictSource = $candidate
            }
        }

        if ([string]::IsNullOrWhiteSpace($PredictSource) -or -not (Test-Path -LiteralPath $PredictSource)) {
            Add-Check "Quick predict" "FAIL" "Predict source not found" "Set -PredictSource to an image folder or file"
        } else {
            New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
            $runName = "health_predict_" + (Get-Date -Format "yyyyMMdd_HHmmss")
            $effectiveDevice = $Device
            if ($Device -eq "auto") {
                if (($gpuInfo -join " ") -match "cuda=True") { $effectiveDevice = "0" } else { $effectiveDevice = "cpu" }
            }

            $args = @(
                "-m", "ultralytics", "detect", "predict",
                "model=$ModelPath",
                "source=$PredictSource",
                "conf=$Conf",
                "iou=$Iou",
                "imgsz=$ImgSz",
                "device=$effectiveDevice",
                "project=$OutputDir",
                "name=$runName",
                "save=True"
            )

            try {
                & $PythonExe $args
                Add-Check "Quick predict" "PASS" "Saved under $OutputDir/$runName"
                $predictResult = "PASS"
            } catch {
                Add-Check "Quick predict" "FAIL" "Predict command failed" "Run the same command manually and check traceback"
                $predictResult = "FAIL"
            }
        }
    }
}

# 7) Output report
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$reportPath = Join-Path $OutputDir ("health_report_" + (Get-Date -Format "yyyyMMdd_HHmmss") + ".json")

$statusOrder = @{ "FAIL" = 3; "WARN" = 2; "PASS" = 1 }
$sortedChecks = $checks | Sort-Object @{Expression = { $statusOrder[$_.Status] }; Descending = $true }, Name

$summary = [pscustomobject]@{
    generatedAt = (Get-Date).ToString("s")
    datasetPath = $DatasetPath
    modelPath = $ModelPath
    pythonExe = $PythonExe
    runPredict = [bool]$RunPredict
    predictResult = $predictResult
    counts = [pscustomobject]@{
        fail = ($checks | Where-Object { $_.Status -eq "FAIL" } | Measure-Object).Count
        warn = ($checks | Where-Object { $_.Status -eq "WARN" } | Measure-Object).Count
        pass = ($checks | Where-Object { $_.Status -eq "PASS" } | Measure-Object).Count
    }
    splits = $setSummaries
    checks = $sortedChecks
    elapsedSeconds = [math]::Round(((Get-Date) - $start).TotalSeconds, 2)
}

$summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportPath -Encoding UTF8

Write-Host ""
Write-Host "================ YOLO Health Check ================" -ForegroundColor Cyan
$sortedChecks | Format-Table -AutoSize
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Report: $reportPath" -ForegroundColor Yellow
Write-Host ""

if ($summary.counts.fail -gt 0) {
    exit 2
}
exit 0

