param(
  [int]$BackendPort = 8081,
  [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendPom = Join-Path $ProjectRoot "yolov8-detection\pom.xml"
$FrontendDir = Join-Path $ProjectRoot "frontend"
$NpmCmd = "D:\Java\node\npm.cmd"

function Get-ListeningPids([int]$Port) {
  $ids = @()

  # Prefer native TCP query when available.
  try {
    $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop
    foreach ($listener in $listeners) {
      if ($listener.OwningProcess -gt 0) {
        $ids += [int]$listener.OwningProcess
      }
    }
  } catch {
    # Fallback to netstat parsing for environments where Get-NetTCPConnection is unavailable.
    $lines = netstat -ano | Select-String "LISTENING"
    $pattern = "^\s*TCP\s+[^\s]+:$Port\s+[^\s]+\s+LISTENING\s+(\d+)\s*$"
    foreach ($line in $lines) {
      $m = [regex]::Match($line.ToString(), $pattern)
      if ($m.Success) {
        $ids += [int]$m.Groups[1].Value
      }
    }
  }

  return $ids | Select-Object -Unique
}

function Stop-PortProcess([int]$Port) {
  $pids = Get-ListeningPids -Port $Port
  if ($pids.Count -eq 0) {
    Write-Host "[startup] Port $Port is free."
    return
  }

  foreach ($procId in $pids) {
    Write-Host "[startup] Kill PID=$procId on port $Port"
    try {
      Stop-Process -Id $procId -Force -ErrorAction Stop
    } catch {
      taskkill /PID $procId /F *> $null
    }
  }

  # Second pass in case child processes quickly rebind the same port.
  Start-Sleep -Milliseconds 400
  $remain = Get-ListeningPids -Port $Port
  foreach ($procId in $remain) {
    Write-Host "[startup] Force kill remaining PID=$procId on port $Port"
    taskkill /PID $procId /F *> $null
  }
}

function Ensure-PortFree([int]$Port, [int]$TimeoutSec = 8) {
  $deadline = (Get-Date).AddSeconds($TimeoutSec)
  while ((Get-Date) -lt $deadline) {
    $pids = Get-ListeningPids -Port $Port
    if ($pids.Count -eq 0) { return }
    Start-Sleep -Milliseconds 400
  }

  $remain = Get-ListeningPids -Port $Port
  if ($remain.Count -gt 0) {
    throw "Port $Port still in use by PID: $($remain -join ', '). Please close conflicting app then retry."
  }
}

function Wait-BackendReady([int]$Port, [int]$TimeoutSec, $BackendProcess) {
  $deadline = (Get-Date).AddSeconds($TimeoutSec)
  while ((Get-Date) -lt $deadline) {
    if ($BackendProcess -and $BackendProcess.HasExited) {
      throw "Backend process exited early (PID=$($BackendProcess.Id), ExitCode=$($BackendProcess.ExitCode))."
    }
    try {
      $resp = Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:$Port/api/system/version" -TimeoutSec 2
      if ($resp -and $resp.service -eq "yolo-detection") {
        Write-Host "[startup] Backend ready on :$Port"
        return
      }
    } catch {
      Start-Sleep -Milliseconds 600
    }
  }
  throw "Backend startup timeout on :$Port"
}

Write-Host "[startup] Project root: $ProjectRoot"
Stop-PortProcess -Port $BackendPort
Ensure-PortFree -Port $BackendPort

# Also free the frontend dev-server port to avoid Vite startup failure.
Stop-PortProcess -Port $FrontendPort
Ensure-PortFree -Port $FrontendPort

Write-Host "[startup] Starting backend..."
$backendProc = Start-Process -FilePath "powershell.exe" -ArgumentList @(
  "-NoExit",
  "-Command",
  "`$env:SERVER_PORT=$BackendPort; mvn -f `"$BackendPom`" -DskipTests spring-boot:run"
) -WorkingDirectory $ProjectRoot -PassThru

Wait-BackendReady -Port $BackendPort -TimeoutSec 120 -BackendProcess $backendProc

Write-Host "[startup] Starting frontend..."
Start-Process -FilePath "powershell.exe" -ArgumentList @(
  "-NoExit",
  "-Command",
  "& `"$NpmCmd`" --prefix `"$FrontendDir`" run dev -- --port $FrontendPort"
) -WorkingDirectory $ProjectRoot | Out-Null

Write-Host "[startup] All services started."
Write-Host "[startup] Frontend: http://127.0.0.1:$FrontendPort"
Write-Host "[startup] Backend:  http://127.0.0.1:$BackendPort"
