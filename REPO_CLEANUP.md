# REPO_CLEANUP: 仓库瘦身后恢复运行环境指南

本文用于说明：仓库为了上传 GitHub 已删除大体积/可再生成内容后，团队成员如何在本地恢复到可运行状态。

适用项目根目录：`D:\ProjectCode\IDEA\yolo`

## 0. 迁移到其他设备前，必须删除哪些文件？

建议在打包/拷贝项目前先删除（或确保不提交）以下内容：

- 必删（构建产物）
  - `frontend/dist`
  - `frontend/node_modules`
  - `desktop/dist`
  - `desktop/node_modules`
  - `yolov8-detection/target`
- 必删（运行数据/结果）
  - `data/images`
  - `data/predict-results`
  - `data/datasets`
  - `runs`
  - `health-report`
- 必删（本地环境绑定/缓存）
  - `desktop/runtime/win-jre17`（除非你明确要打包内置 JRE 一起分发）
  - 各目录下 `*.log`
  - IDE 目录（如 `.idea`）与临时文件
- 不建议随仓库迁移的大文件
  - `*.pt` 模型权重
  - `yolov8-detection/scripts/ultralytics-main`（可由目标设备 Python 环境重新安装 ultralytics）

## 1. 本次清理了什么

以下内容属于构建产物、缓存或运行输出，已从仓库移除/忽略：

- 前端与桌面端依赖和构建目录：
  - `frontend/node_modules`
  - `frontend/dist`
  - `desktop/node_modules`
  - `desktop/dist`
- 后端构建目录：
  - `yolov8-detection/target`
- 运行时数据和结果：
  - `data/images`
  - `data/predict-results`
  - `data/datasets`
  - `runs`
  - `health-report`
- 本地缓存、日志、IDE 目录（按 `.gitignore` 忽略）
- 大文件：
  - `*.pt` 模型权重
  - `desktop/runtime/win-jre17`（桌面包内置 JRE）
  - `yolov8-detection/scripts/ultralytics-main`（本地 YOLO 源码快照）

## 2. 恢复前准备（必须）

### 2.1 基础软件

建议版本：

- JDK 17
- Maven 3.9+
- Node.js 18+
- Python 3.10+（建议 Anaconda 虚拟环境）
- MySQL 8+

### 2.2 Python 环境（YOLO）

`application.yml` 当前默认 Python 路径是：

- `D:/AppDownload/Anaconda3/envs/yolov8/python.exe`

请按你本机实际路径创建/调整；并在该环境安装 `ultralytics`。

```powershell
D:\AppDownload\Anaconda3\Scripts\activate yolov8
python -m pip install --upgrade pip
pip install ultralytics
```

> 若你不使用该路径，请修改 `yolov8-detection/src/main/resources/application.yml` 的 `yolo.python-command`。

### 2.3 模型权重恢复（必须）

仓库中的 `.pt` 已删除，你需要自行放置模型文件（例如 `yolov8n.pt` 或训练后的 `best.pt`）。

当前配置文件里默认模型是：

- `yolo.model-path: scripts/ultralytics-main/yolov8n.pt`

由于 `ultralytics-main` 已清理，建议改成你自己的固定路径，例如：

- `D:/models/yolov8n.pt`

然后在 `application.yml` 中更新：

```yaml
yolo:
  model-path: D:/models/yolov8n.pt
```

## 3. 数据库恢复

项目当前使用 MyBatis + MySQL。

1) 确保数据库存在：`yolo_detection`

2) 默认连接（可通过环境变量覆盖）：

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`

示例（PowerShell 临时设置）：

```powershell
$env:MYSQL_URL="jdbc:mysql://127.0.0.1:3306/yolo_detection?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="123456"
```

## 4. 一键恢复依赖与构建

在项目根目录执行：

```powershell
cd D:\ProjectCode\IDEA\yolo

# Frontend
cd .\frontend
npm install
npm run build

# Backend
cd ..\yolov8-detection
mvn -DskipTests package

# Desktop (如需桌面版)
cd ..\desktop
npm install
```

## 5. 各模块启动方式

### 5.1 启动后端

```powershell
cd D:\ProjectCode\IDEA\yolo\yolov8-detection
$env:SERVER_PORT=8081
mvn spring-boot:run
```

后端默认地址：`http://127.0.0.1:8081`

### 5.2 启动前端

```powershell
cd D:\ProjectCode\IDEA\yolo\frontend
npm run dev
```

前端默认地址：`http://127.0.0.1:5173`

### 5.3 启动桌面版（开发）

```powershell
cd D:\ProjectCode\IDEA\yolo\desktop
npm run dev
```

### 5.4 打包桌面版（可选）

```powershell
cd D:\ProjectCode\IDEA\yolo\desktop
npm run dist:win
```

> 打包前需要：
> - `frontend/dist` 已构建
> - `yolov8-detection/target/*.jar` 已生成
> - `desktop/runtime/win-jre17` 已存在（如启用内置 JRE）

## 6. 最小可用自检清单

按顺序检查：

1. `mvn -v` 与 `node -v` 正常
2. Python 环境中 `import ultralytics` 成功
3. 后端启动无报错（8081 可访问）
4. 前端能打开并请求后端接口
5. 预测时模型路径有效，返回结果图

## 7. 常见报错与处理

### 7.1 `The goal you specified requires a project to execute but there is no POM`

原因：不在 `yolov8-detection` 目录执行 Maven。

处理：切到后端目录再执行。

### 7.2 `can't open file ... scripts\yolov8_runner.py`

原因：`yolo.detect-script` / `yolo.train-script` 路径不正确。

处理：确认 `yolov8-detection/scripts/yolov8_runner.py` 存在，并检查配置路径。

### 7.3 `Data too long for column 'message'`

原因：数据库字段长度不足。

处理：将对应表字段类型改大（如 `TEXT`），或清理旧异常长消息数据。

### 7.4 `UnsupportedClassVersionError ... class file version 61.0`

原因：JAR 使用 JDK17 编译，但运行时用了低版本 Java（如 Java 8）。

处理：统一使用 JDK17 运行后端/桌面包。

### 7.5 前端 `EPERM` / npm 缓存权限错误

原因：缓存目录被占用或权限不足。

处理：清理 npm cache 或切换缓存目录后重试。

### 7.6 预测无结果图

优先检查：

- `modelPath` 是否存在
- `conf` 是否过高
- 后端 `data/predict-results` 是否可写
- 图片是否实际上传成功

## 8. 团队协作建议

- 不要把 `node_modules`、`target`、`runs`、`*.pt` 重新提交到 Git。
- 模型和数据集统一放在团队共享位置（NAS/网盘），在 README 记录下载地址和版本。
- 新成员拉代码后，先按本文“2 -> 4 -> 5”执行。

---

如需进一步自动化，可新增 `scripts/bootstrap.ps1` 做“依赖检查 + 环境变量检查 + 一键安装/构建”。
