项目名称：Yolo检测平台（yolov8-detection）

概述（中文）
- 这是一个基于 YOLOv8 的目标检测训练/标注/预测平台，包含后端（Spring Boot）、前端（Vite + Vue）和桌面壳（Electron）三部分，以及用于调用 Python YOLOv8 推理/训练的脚本目录。

快速目录说明（以仓库相对路径）
- `/yolov8-detection`：后端 Java 服务，Spring Boot 项目（`pom.xml`）。主要负责：任务管理、训练/预测任务调度、与 Python runner 的交互、数据库存储（MySQL）。
- `/frontend`：Web 前端（Vite + Vue）。开发时用 `npm install` + `npm run dev`，生产构建 `npm run build`。
- `/desktop`：Electron 桌面壳（将前端打包并捆绑后端 jar 以构建桌面应用）。常用命令 `npm run dist:win`（需要本机安装 Node、npm 以及 electron-builder 等）。
- `/scripts`、`/yolov8-detection/scripts/ultralytics-main`：放置 YOLOv8（Ultralytics）的 Python 源码/runner 的目录（用于训练/推理实际调用）。注意：项目运行需要 `yolov8_runner.py` 等脚本存在于预期位置，否则训练任务会报错（示例错误："can't open file '.../scripts/yolov8_runner.py'"）。
- `/data`：默认数据/临时文件目录，包含 `datasets`、`annotations`、`predict-results` 等。
- `/runs`：训练产生的权重/日志输出（例如 `runs/detect/.../weights/best.pt`）。

运行环境（已知配置 / 推荐）
- Java：JDK 17（项目已用 Java 17 编译），推荐至少安装 JRE/JDK 17 与 Maven。示例环境：`Java 17.0.18`。
- Maven：3.9.x（用于构建后端）。
- Node.js / npm：用于前端和 Electron，建议安装 LTS 版本（Node >= 18）。注意：`vite` 和 `electron-builder` 需要通过 `npm install` 安装到本地 `node_modules` 才能被识别。
- Python：推荐使用 Anaconda 创建独立环境（示例：`D:\AppDownload\Anaconda3\envs\yolov8\python.exe`）。在该环境中安装依赖（`ultralytics`、`torch` 等），并确保 GPU（如需）对应的 CUDA 驱动与 PyTorch 版本匹配。
- 数据库：MySQL（示例：已创建数据库 `yolo_detection`）。

默认端口与路径
- 后端（Spring Boot）默认端口：`8081`（开发中已建议改为 8081，避免 8080 冲突；请检查 `src/main/resources/application.yml` 或 `application.properties` 中的 `server.port`）。
- 前端（开发模式 Vite）：`localhost:5173`（默认），生产包交由 Electron 承载。
- Python runner 路径：`yolov8-detection/scripts/yolov8_runner.py`（确保存在并在 `application.yml` 中配置正确的 `pythonPath`）。

重要配置（建议）
- `application.yml`（后端）需包含：
  - `server.port: 8081`
  - 数据库连接信息（`spring.datasource`）
  - `yolo` 配置：`modelPath`, `conf`, `iou`, `device`, `pythonPath`（示例：`D:\AppDownload\Anaconda3\envs\yolov8\python.exe`）。

常见启动命令（在项目根或对应子目录下运行）
- 构建并运行后端（开发）

```powershell
mvn -f yolov8-detection/pom.xml -DskipTests package
mvn -f yolov8-detection/pom.xml -DskipTests spring-boot:run
```

- 仅打包后端 jar（用于 Electron 打包时嵌入）：

```powershell
mvn -f yolov8-detection/pom.xml -DskipTests package
# 生成 target/yolov8-detection-1.0-SNAPSHOT.jar
```

- 前端（开发）

```powershell
cd frontend
npm install
npm run dev
# 或生产打包： npm run build
```

- Electron 打包（需在 `desktop` 目录）：

```powershell
cd desktop
npm install
# 打包 Windows 可执行安装包（可能需要管理员权限）
npm run dist:win
```

- 启动全部（start-all 脚本）：

```powershell
powershell -ExecutionPolicy Bypass -File start-all.ps1
```
注意：`start-all.ps1` 中可能覆盖 `$PID` 等只读 PowerShell 变量会报错，需要修改脚本避免修改内置变量名。

已知问题与修复建议（摘录项目里出现过的主要问题）
1) Maven 下载依赖超时 / 无法连接 central：检查网络、设置 Maven mirror 或代理；手动删除本地损坏的依赖目录（如 `.m2/repository/...`），然后重试。
2) 在非项目目录执行 `mvn`：请确保 `pom.xml` 路径正确（使用 `-f` 指定）。错误示例：MissingProjectException。
3) 后端启动报错：端口占用（8080/8081） -> 停止占用进程或修改 `server.port`。可用命令（Windows）查看并结束进程：

```powershell
netstat -ano | Select-String ":8081"
Stop-Process -Id <pid> -Force
```

4) MySQL 数据列过短导致启动失败（Data truncation: Data too long for column 'message'） -> 数据库迁移：将 `message` 字段改为 `TEXT` 或增大 `VARCHAR` 长度。

5) Electron 打包失败：缺少 `7za.exe`（`7zip-bin` package），或 `electron-builder` 未安装为全局/项目依赖。解决：安装 `7zip-bin`、确保 `node_modules` 完整、在 Windows 下以管理员权限运行打包命令。若要捆绑 JRE（第二层保险），请确认 `desktop/scripts/prepare-jre.cjs` 指向正确的 JRE 路径并捆绑到 `desktop/runtime`。

6) Python runner 未找到：训练任务失败并提示找不到 `yolov8_runner.py`，请把 runner 放到 `yolov8-detection/scripts/` 下或在 `application.yml` 中把 `yolo.pythonScriptPath` 指向实际文件位置。

7) 前端：`vite`/`node` 命令未找到或 `EPERM` 错误 -> 先 `npm install`，如果 `EPERM`，检查权限/杀毒软件占用或清空 npm 缓存（注意 Windows 文件锁问题）。

8) 桌面版运行时 Java 版本不匹配：如果用户机器上 JRE 版本过低（报错示例：class file version 61.0），请在安装包中捆绑 JRE 17，或提示用户安装 JRE 17+。

功能实现状态（高层）
- 已实现（后端/前端基本骨架）
  - 训练任务的提交与状态记录（存在 DB 表 `training_jobs`）
  - 前端基础页面、上传/预测功能的基础实现
  - Electron 桌面壳和打包脚本（需完善捆绑与权限）

- 待完善/已接到需求（优先级）
  - 并发上传文件夹（并发数默认），上传进度与批次管理（高）
  - 手动标注交互（点击选中删除单框、拖拽调整、上一张/下一张）
  - 数据集构建（train/val/test 划分、data.yaml 生成）
  - 训练参数 UI（增加 `conf`、`batch`、`device`、`epochs`）与训练进度显示、更稳定的 job 追踪
  - 将长文本字段（如 `message`）的 DB 字段扩容为 `TEXT`
  - 将 Spring Data JPA 切换为 MyBatis（如果团队偏好），或继续用 JPA 简化开发
  - 文件选择器（`Browse`）在浏览器/桌面端兼容性和 UI 精细调整

快速检查清单（开发者可逐项核对）
- [ ] `yolov8_runner.py` 存在并且 `application.yml` 配置了正确的 `pythonPath` 与 `pythonScriptPath`
- [ ] 数据库 `yolo_detection` 创建成功并能连接（检查 `spring.datasource.*`）
- [ ] 后端端口（`server.port`）设置为本地不冲突端口（如 8081）
- [ ] 前端 `npm install` 已完成，能运行 `npm run dev` 或 `npm run build`
- [ ] Electron 打包前运行 `npm ci` / `npm install` 并确保 `7zip`/`7zip-bin` 可用

推荐的下一步（短期）
1. 在 `yolov8-detection/src/main/resources/application.yml` 中加入 `yolo` 配置项（`modelPath/conf/iou/device/pythonPath/pythonScriptPath`），并在 `YoloProperties` 中扩展相应字段。
2. 把 `training_jobs.message` 字段改为 `TEXT`，避免 Data truncation 错误。
3. 补齐 `yolov8_runner.py` 并测试从 Java 后端调用 Python 的启动命令（使用绝对路径）。
4. 修复 `start-all.ps1` 中对 `$PID` 的覆盖逻辑，避免 PowerShell 只读变量写入错误。

维护人员/联系方式
- 仓库维护者：请在 `README.md` 中列出实际负责人（姓名/邮箱/代码所有者）。

附录：快速命令参考
- 构建后端 jar

```powershell
mvn -f yolov8-detection/pom.xml -DskipTests package
```

- 运行后端（开发）

```powershell
mvn -f yolov8-detection/pom.xml -DskipTests spring-boot:run
# 或
java -jar yolov8-detection/target/yolov8-detection-1.0-SNAPSHOT.jar --server.port=8081
```

- 运行前端（开发）

```powershell
cd frontend
npm install
npm run dev
```

- 打包桌面应用（Windows）

```powershell
cd desktop
npm install
# 若为首次，全局安装 electron-builder 或确保项目内有
npm run dist:win
```

- 清理本地 maven 依赖（当某个 artifact 损坏时）

```powershell
Remove-Item -Recurse -Force "$env:USERPROFILE\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.23.1" -ErrorAction SilentlyContinue
```

---

文件已创建：`PROJECT_INFO.md`（项目根）。如需我把 `application.yml` 示例或 `PROJECT_INFO.md` 中建议的 `YoloProperties` Java 代码直接写入项目并进行编译/测试，我可以继续按您要求实现并运行构建验证。
