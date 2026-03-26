const fs = require("fs");
const path = require("path");

const projectRoot = path.resolve(__dirname, "..");
const targetDir = path.join(projectRoot, "runtime", "win-jre17");
const sourceArg = process.argv[2] && process.argv[2].trim();
const sourceFromEnv = process.env.JRE17_HOME || process.env.JAVA_HOME;
const sourceHome = sourceArg || sourceFromEnv;

function fail(message) {
  console.error(`[prepare:jre] ${message}`);
  process.exit(1);
}

function javaExe(home) {
  return path.join(home, "bin", "java.exe");
}

function readJavaVersion(javaPath) {
  const { spawnSync } = require("child_process");
  const result = spawnSync(javaPath, ["-version"], {
    encoding: "utf8",
    windowsHide: true
  });
  if (result.error) {
    return null;
  }
  const text = `${result.stdout || ""}\n${result.stderr || ""}`;
  const match = text.match(/version\s+"([^"]+)"/i);
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

function removeDirIfExists(dir) {
  if (fs.existsSync(dir)) {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

function copyDirRecursive(source, target) {
  fs.mkdirSync(target, { recursive: true });
  for (const entry of fs.readdirSync(source, { withFileTypes: true })) {
    const sourcePath = path.join(source, entry.name);
    const targetPath = path.join(target, entry.name);
    if (entry.isDirectory()) {
      copyDirRecursive(sourcePath, targetPath);
    } else if (entry.isSymbolicLink()) {
      const realPath = fs.realpathSync(sourcePath);
      const stats = fs.statSync(realPath);
      if (stats.isDirectory()) {
        copyDirRecursive(realPath, targetPath);
      } else {
        fs.copyFileSync(realPath, targetPath);
      }
    } else {
      fs.copyFileSync(sourcePath, targetPath);
    }
  }
}

if (!sourceHome) {
  fail("No source JDK/JRE home found. Set JRE17_HOME (preferred) or JAVA_HOME, or pass path arg.");
}

if (!fs.existsSync(sourceHome)) {
  fail(`Source path not found: ${sourceHome}`);
}

const sourceJava = javaExe(sourceHome);
if (!fs.existsSync(sourceJava)) {
  fail(`Missing java.exe under source path: ${sourceJava}`);
}

const version = readJavaVersion(sourceJava);
if (!version) {
  fail(`Cannot read Java version from: ${sourceJava}`);
}
if (version.major < 17) {
  fail(`Source Java is ${version.raw}. Need Java 17+.`);
}

removeDirIfExists(targetDir);
copyDirRecursive(sourceHome, targetDir);

const bundledJava = javaExe(targetDir);
if (!fs.existsSync(bundledJava)) {
  fail(`Bundle copy failed. Missing: ${bundledJava}`);
}

console.log(`[prepare:jre] Source: ${sourceHome}`);
console.log(`[prepare:jre] Version: ${version.raw}`);
console.log(`[prepare:jre] Bundled to: ${targetDir}`);

