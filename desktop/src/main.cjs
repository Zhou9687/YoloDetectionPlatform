const { app, BrowserWindow, dialog, ipcMain } = require("electron");
const { spawn, spawnSync } = require("child_process");
const fs = require("fs");
const net = require("net");
const path = require("path");

const API_PORT = Number(process.env.BACKEND_PORT || 8081);
const FRONTEND_DEV_URL = process.env.FRONTEND_DEV_URL || "http://127.0.0.1:5173";
const STARTUP_TIMEOUT_MS = 120000;
const JAVA_COMMAND_OVERRIDE = process.env.JAVA_COMMAND;
const MIN_JAVA_MAJOR = 17;

let backendProcess = null;
let backendLogPath = null;
let lastBackendFailureMessage = null;

function javaExecutableName() {
  return process.platform === "win32" ? "java.exe" : "java";
}

function safeJavaFromHome(homeDir) {
  if (!homeDir) {
    return null;
  }
  const javaPath = path.join(homeDir, "bin", javaExecutableName());
  return fs.existsSync(javaPath) ? javaPath : null;
}

function appendJavaCandidate(candidates, seen, candidate) {
  if (!candidate) {
    return;
  }
  const value = String(candidate).trim();
  if (!value) {
    return;
  }
  const key = process.platform === "win32" ? value.toLowerCase() : value;
  if (seen.has(key)) {
    return;
  }
  seen.add(key);
  candidates.push(value);
}

function collectJetBrainsJdkCandidates() {
  const jdksDir = path.join(app.getPath("home"), ".jdks");
  if (!fs.existsSync(jdksDir)) {
    return [];
  }
  const children = fs.readdirSync(jdksDir, { withFileTypes: true });
  const javaBinList = [];
  for (const child of children) {
    if (!child.isDirectory()) {
      continue;
    }
    const javaPath = path.join(jdksDir, child.name, "bin", javaExecutableName());
    if (fs.existsSync(javaPath)) {
      javaBinList.push(javaPath);
    }
  }
  javaBinList.sort().reverse();
  return javaBinList;
}

function bundledJavaCandidates() {
  if (!isPackaged()) {
    return [];
  }
  return [
    path.join(process.resourcesPath, "runtime", "bin", javaExecutableName()),
    path.join(process.resourcesPath, "runtime", "win-jre17", "bin", javaExecutableName())
  ].filter((candidate) => fs.existsSync(candidate));
}

function resolveJavaRuntime() {
  const candidates = [];
  const seen = new Set();

  appendJavaCandidate(candidates, seen, JAVA_COMMAND_OVERRIDE);
  for (const candidate of bundledJavaCandidates()) {
    appendJavaCandidate(candidates, seen, candidate);
  }
  appendJavaCandidate(candidates, seen, safeJavaFromHome(process.env.JAVA_HOME));
  for (const candidate of collectJetBrainsJdkCandidates()) {
    appendJavaCandidate(candidates, seen, candidate);
  }
  appendJavaCandidate(candidates, seen, "java");

  const inspected = [];
  for (const command of candidates) {
    const info = getJavaRuntimeInfo(command);
    if (!info) {
      inspected.push(`${command}: unavailable`);
      continue;
    }
    inspected.push(`${command}: ${info.raw}`);
    if (info.major >= MIN_JAVA_MAJOR) {
      return { command, info, inspected };
    }
  }

  return { command: null, info: null, inspected };
}

function isPackaged() {
  return app.isPackaged;
}

function projectRootDev() {
  return path.resolve(__dirname, "..", "..");
}

function backendDir() {
  return isPackaged()
    ? path.join(process.resourcesPath, "backend")
    : path.join(projectRootDev(), "yolov8-detection");
}

function frontendDistIndex() {
  return isPackaged()
    ? path.join(process.resourcesPath, "frontend-dist", "index.html")
    : path.join(projectRootDev(), "frontend", "dist", "index.html");
}

function findBackendJar(dir) {
  if (!fs.existsSync(dir)) {
    return null;
  }
  const files = fs.readdirSync(dir);
  const candidates = files.filter((name) => name.endsWith(".jar") && !name.endsWith(".jar.original"));
  candidates.sort();
  if (candidates.length === 0) {
    return null;
  }
  return path.join(dir, candidates[candidates.length - 1]);
}

function backendLogsDir() {
  const dir = path.join(app.getPath("userData"), "logs");
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

function writeBackendLog(logStream, chunk) {
  if (!chunk) {
    return;
  }
  logStream.write(chunk);
}

function waitForPort(host, port, timeoutMs) {
  const start = Date.now();
  return new Promise((resolve, reject) => {
    function probe() {
      if (lastBackendFailureMessage) {
        reject(new Error(lastBackendFailureMessage));
        return;
      }
      if (backendProcess && backendProcess.exitCode !== null) {
        reject(
          new Error(
            `Backend exited before ready (code=${backendProcess.exitCode}). Log: ${backendLogPath || "(not found)"}`
          )
        );
        return;
      }

      const socket = new net.Socket();
      socket.setTimeout(1500);
      socket.once("connect", () => {
        socket.destroy();
        resolve();
      });
      socket.once("timeout", () => socket.destroy());
      socket.once("error", () => socket.destroy());
      socket.once("close", () => {
        if (Date.now() - start >= timeoutMs) {
          reject(new Error(`Backend not ready on ${host}:${port} within ${timeoutMs}ms. Log: ${backendLogPath || "(not found)"}`));
        } else {
          setTimeout(probe, 600);
        }
      });
      socket.connect(port, host);
    }
    probe();
  });
}

function parseJavaMajor(versionText) {
  if (!versionText) {
    return null;
  }
  const match = versionText.match(/version\s+"([^"]+)"/i);
  if (!match) {
    return null;
  }
  const raw = match[1];
  const majorText = raw.startsWith("1.") ? raw.split(".")[1] : raw.split(".")[0];
  const major = Number(majorText);
  if (Number.isNaN(major)) {
    return null;
  }
  return { major, raw };
}

function getJavaRuntimeInfo(javaCommand) {
  const result = spawnSync(javaCommand, ["-version"], {
    encoding: "utf8",
    windowsHide: true
  });
  if (result.error) {
    return null;
  }
  const text = `${result.stdout || ""}\n${result.stderr || ""}`;
  const parsed = parseJavaMajor(text);
  if (!parsed) {
    return null;
  }
  return parsed;
}

function startBackend() {
  lastBackendFailureMessage = null;

  const runtime = resolveJavaRuntime();
  if (!runtime.command || !runtime.info) {
    const checked = runtime.inspected.length ? runtime.inspected.join(" | ") : "(none)";
    throw new Error(
      `No Java ${MIN_JAVA_MAJOR}+ runtime found. Checked: ${checked}. ` +
        `Please install JDK ${MIN_JAVA_MAJOR}+ or set JAVA_COMMAND/JAVA_HOME correctly.`
    );
  }
  const javaCommand = runtime.command;

  const backendRoot = backendDir();
  const jarDir = isPackaged() ? backendRoot : path.join(backendRoot, "target");
  const jarPath = findBackendJar(jarDir);
  if (!jarPath) {
    throw new Error(
      isPackaged()
        ? `No backend jar found in ${jarDir}. Please rebuild desktop package.`
        : "No backend jar found. Run: mvn -f yolov8-detection/pom.xml -DskipTests package"
    );
  }

  backendLogPath = path.join(backendLogsDir(), "backend.log");
  const logStream = fs.createWriteStream(backendLogPath, { flags: "a" });
  logStream.write(`\n===== ${new Date().toISOString()} START =====\n`);
  logStream.write(`cwd=${backendRoot}\njar=${jarPath}\njava=${javaCommand} (${runtime.info.raw})\n`);

  backendProcess = spawn(javaCommand, ["-jar", jarPath], {
    cwd: backendRoot,
    stdio: ["ignore", "pipe", "pipe"],
    windowsHide: true,
    env: {
      ...process.env,
      SERVER_PORT: String(API_PORT)
    }
  });

  if (backendProcess.stdout) {
    backendProcess.stdout.on("data", (chunk) => {
      writeBackendLog(logStream, chunk);
      if (!isPackaged()) {
        process.stdout.write(chunk);
      }
    });
  }

  if (backendProcess.stderr) {
    backendProcess.stderr.on("data", (chunk) => {
      writeBackendLog(logStream, chunk);
      if (!isPackaged()) {
        process.stderr.write(chunk);
      }
    });
  }

  backendProcess.on("error", (err) => {
    lastBackendFailureMessage = `Failed to start backend process (${javaCommand}): ${err.message}. Ensure Java 17+ is installed and 'java -version' works. Log: ${backendLogPath}`;
  });

  backendProcess.on("exit", (code) => {
    if (code !== 0 && code !== null) {
      lastBackendFailureMessage = `Backend exited early with code ${code}. Check DB/Python config. Log: ${backendLogPath}`;
      console.error(`Backend exited with code ${code}`);
    }
  });

  backendProcess.on("close", () => {
    logStream.end(`\n===== ${new Date().toISOString()} END =====\n`);
  });
}

ipcMain.handle("dialog:pick-directory", async () => {
  const focused = BrowserWindow.getFocusedWindow();
  const owner = focused || BrowserWindow.getAllWindows()[0] || undefined;
  const result = await dialog.showOpenDialog(owner, {
    title: "选择文件夹",
    properties: ["openDirectory", "dontAddToRecent"]
  });
  if (result.canceled || !Array.isArray(result.filePaths) || result.filePaths.length === 0) {
    return "";
  }
  return result.filePaths[0] || "";
});

ipcMain.handle("dialog:pick-file", async (_event, options) => {
  const focused = BrowserWindow.getFocusedWindow();
  const owner = focused || BrowserWindow.getAllWindows()[0] || undefined;
  const extensions = Array.isArray(options?.extensions) && options.extensions.length > 0
    ? options.extensions.map((ext) => String(ext).replace(/^\./, ""))
    : ["pt"];
  const result = await dialog.showOpenDialog(owner, {
    title: options?.title || "选择文件",
    properties: ["openFile", "dontAddToRecent"],
    filters: [{ name: "Model Files", extensions }]
  });
  if (result.canceled || !Array.isArray(result.filePaths) || result.filePaths.length === 0) {
    return "";
  }
  return result.filePaths[0] || "";
});

async function createMainWindow() {
  const win = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1200,
    minHeight: 800,
    autoHideMenuBar: true,
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      preload: path.join(__dirname, "preload.cjs")
    }
  });

  if (!isPackaged()) {
    try {
      await win.loadURL(FRONTEND_DEV_URL);
      win.webContents.openDevTools({ mode: "detach" });
      return;
    } catch (err) {
      console.warn("Frontend dev server unavailable, fallback to dist/index.html", err);
    }
  }

  const indexFile = frontendDistIndex();
  if (!fs.existsSync(indexFile)) {
    throw new Error(
      isPackaged()
        ? `Missing bundled frontend file: ${indexFile}`
        : "Frontend dist not found. Run: npm --prefix frontend run build"
    );
  }
  await win.loadFile(indexFile);
}

function stopBackend() {
  if (!backendProcess || backendProcess.killed) {
    return;
  }

  const pid = backendProcess.pid;
  if (backendProcess.stdin) {
    backendProcess.stdin.end();
  }

  try {
    backendProcess.kill();
  } catch (_err) {
    // ignore
  }

  // Force-kill child tree on Windows to avoid orphaned java process.
  if (process.platform === "win32" && pid) {
    try {
      spawnSync("taskkill", ["/PID", String(pid), "/T", "/F"], {
        windowsHide: true,
        stdio: "ignore"
      });
    } catch (_err) {
      // ignore
    }
  }
}

const gotSingleInstanceLock = app.requestSingleInstanceLock();
if (!gotSingleInstanceLock) {
  app.quit();
} else {
  app.on("second-instance", () => {
    const allWindows = BrowserWindow.getAllWindows();
    if (allWindows.length > 0) {
      const win = allWindows[0];
      if (win.isMinimized()) {
        win.restore();
      }
      win.focus();
    }
  });

  app.whenReady().then(async () => {
    try {
      startBackend();
      await waitForPort("127.0.0.1", API_PORT, STARTUP_TIMEOUT_MS);
      await createMainWindow();
    } catch (error) {
      dialog.showErrorBox("启动失败", String(error.message || error));
      stopBackend();
      app.exit(1);
    }
  });

  app.on("window-all-closed", () => {
    stopBackend();
    if (process.platform !== "darwin") {
      app.quit();
    }
  });

  app.on("before-quit", () => {
    stopBackend();
  });

  process.on("exit", () => {
    stopBackend();
  });
}
