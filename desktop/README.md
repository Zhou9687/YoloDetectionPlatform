# Electron Desktop Shell

该目录是桌面壳工程，负责：

- 自动启动后端 `yolov8-detection`（`java -jar`）
- 等待后端 `8081` 就绪
- 加载前端页面（开发优先走 `http://127.0.0.1:5173`，否则回退到 `frontend/dist`）

## 目录约定

- 前端：`../frontend`
- 后端：`../yolov8-detection`

## 启动前提

- 开发运行时，本机可用 `java`（建议 Java 17+）。
- MySQL 服务已启动，且 `../yolov8-detection/src/main/resources/application.yml` 中数据库账号可连接。
- Python/YOLO 相关路径配置仍由后端 `application.yml` 管理。

> 说明：桌面壳会启动后端，如果数据库或 Python 配置不可用，窗口会提示启动失败。

## 打包内置 JRE 17（推荐）

为了避免用户机器默认 Java 8 导致启动失败，`dist:win` 已自动执行 `prepare:jre`：

- 优先读取 `JRE17_HOME`
- 如果没有 `JRE17_HOME`，回退到 `JAVA_HOME`
- 将该目录完整复制到 `desktop/runtime/win-jre17`
- 打包后进入安装目录：`resources/runtime/win-jre17`

建议先设置环境变量（示例）：

```powershell
$env:JRE17_HOME='C:\Users\123\.jdks\ms-17.0.18'
```

然后直接打包：

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\desktop
npm run dist:win
```

打包后可验证内置 Java：

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\desktop\dist\win-unpacked\resources\runtime\win-jre17\bin
.\java.exe -version
```

## 开发运行

先构建后端 jar（桌面壳启动时要用）：

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\yolov8-detection
mvn -DskipTests package
```

安装桌面壳依赖并启动：

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\desktop
npm install
npm run dev
```

如果你同时启动了前端 dev server，桌面壳会优先加载：

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\frontend
npm run dev
```

## 打包 Windows 安装包

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\desktop
npm install
npm run dist:win
```

输出目录：`desktop/dist/`。

- 安装包：`desktop/dist/YOLOv8 Detection Desktop Setup 1.0.0.exe`
- 解压运行目录：`desktop/dist/win-unpacked/`

## 网络说明

`desktop/.npmrc` 已设置 Electron 镜像：

- `electron_mirror=https://npmmirror.com/mirrors/electron/`

`package.json` 的 `dist:win` 已使用 electron-builder 二进制镜像：

- `ELECTRON_BUILDER_BINARIES_MIRROR=https://npmmirror.com/mirrors/electron-builder-binaries/`

可降低下载 `electron`、`nsis`、`nsis-resources` 失败概率。

## 常见打包问题

1. `ENOENT ... 7za.exe`

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\desktop
npm install -D 7zip-bin@5.2.0
```

2. `nsis` 下载超时/校验失败（网络抖动）

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\desktop
npm run dist:win
```

如果仍失败，直接重试 1-2 次通常可恢复。

## 启动失败排查（安装后双击无响应/弹窗失败）

如果提示 `Backend not ready on 127.0.0.1:8081`，请先查看后端日志：

- 日志目录：`%APPDATA%\yolov8-detection-desktop\logs\backend.log`

常见原因：

1. `java` 不可用（未安装或未进 PATH）
2. MySQL 未启动或账号密码不对
3. Python/模型路径配置错误导致后端启动即退出
4. 端口冲突（`8081` 被其他进程占用）

可先在命令行验证：

```powershell
java -version
```

```powershell
Test-NetConnection 127.0.0.1 -Port 3306
```

```powershell
netstat -ano | findstr LISTENING | findstr :8081
```

## 常见问题

1. `UnsupportedClassVersionError`（class file version 61.0 / 52.0）

这表示当前运行的是 Java 8（52.0），但后端需要 Java 17（61.0）。

现在优先建议使用“内置 JRE 17 打包”，避免依赖用户系统 Java；若仍走系统 Java，再按下列方式修复：

```powershell
# 查看当前被系统调用的 java
Get-Command java | Format-List Name,Source
```

```powershell
# 临时设置当前终端（示例按你的实际JDK路径修改）
$env:JAVA_HOME='C:\Users\123\.jdks\ms-17.0.18'
$env:Path="$env:JAVA_HOME\\bin;$env:Path"
java -version
```

```powershell
# 永久设置（新开终端生效）
setx JAVA_HOME "C:\Users\123\.jdks\ms-17.0.18"
setx PATH "%JAVA_HOME%\\bin;%PATH%"
```

然后重新安装/启动桌面应用。
