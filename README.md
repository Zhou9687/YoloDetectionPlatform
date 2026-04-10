# YOLOv8 Detection (Spring Boot + Vue)

本项目已打通「图片标注 -> 数据集构建 -> 训练任务 -> 检测预测」完整流程，并新增登录鉴权与账号管理。
npm --prefix D:\ProjectCode\IDEA\yolo\frontend run dev

- 后端：Spring Boot (`8081`)
- 前端：Vue3 + Vite (`5173`)
- Python：`scripts/yolov8_runner.py`（调用 Ultralytics YOLO）
- 一键启动：`start-all.ps1`（自动清理端口、拉起后端、等待健康检查、再启动前端）

## 功能总览

### 1) 图片标注模块
- 仅保留**上传文件夹**（一次一个批次）
- 上传前自动清空旧图片，仅保留当前批次
- 文件夹上传使用固定并发（5）并显示上传进度条
- 图片列表显示标记：`已标注 / 未标注`
- 支持上一张/下一张切图标注
- 标注框支持：绘制、拖拽移动、四角缩放、选中删除
- 保存标注后自动在该批次下生成 `labels/` 并写入 YOLO txt

### 2) 数据集构建模块
- 支持自定义：`datasetName`、`outputPath`、`valRatio`、`testRatio`
- 生成目录结构：
  - `train/imgs`, `train/labels`
  - `val/imgs`, `val/labels`
  - `test/imgs`, `test/labels`
  - `data.yaml`
- `train` 放全部已标注图片，`val/test` 按比例抽样

### 3) 训练任务模块
- 输入改为自定义 `datasetPath`（包含 `data.yaml`）
- 增加 `batch` 参数，支持可选 `modelPath`
- 轮询显示训练进度与状态
- 训练完成返回 `bestModelPath`（`.pt`）

### 4) 检测预测模块
- 支持自定义 `modelPath/conf/iou/device`
- 支持上传单文件或整个文件夹（同一入口）
- 返回每张图片的检测框与渲染后结果图

### 5) 账号与登录模块
- 登录/注册页面：`/login`、`/register`
- 业务路由增加登录守卫（未登录不可进入主界面）
- 顶部头像菜单支持：修改账户名称、修改密码、退出登录
- 账号密码写入 MySQL（密码使用 BCrypt 哈希）

## 关键目录

```text
yolo/
  yolov8-detection/
    src/main/java/com/zhou/
      controller/
      service/
      dto/
      model/
    scripts/
      yolov8_runner.py
      ultralytics-main/
  frontend/
    src/
      api/
      views/
```

## 快速启动

### 0) 推荐：一键启动（永久方案）

```powershell
Set-Location D:\ProjectCode\IDEA\yolo
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

脚本会自动：
- 清理后端端口占用（默认 `8081`）
- 启动后端并等待 `GET /api/system/version` 就绪
- 启动前端开发服务（默认 `5173`）

### 1) 手动启动后端

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\yolov8-detection
mvn -DskipTests package
$env:SERVER_PORT=8081
mvn spring-boot:run
```

### 2) 手动启动前端

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\frontend
npm install
npm run dev
```

访问：`http://localhost:5173`（首次进入需先登录）

## 桌面版（Electron）

已新增桌面壳目录：`D:\ProjectCode\IDEA\yolo\desktop`

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\desktop
npm install
npm run dev
```

打包安装包：

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\desktop
npm run dist:win
```

产物（Windows）：
- `D:\ProjectCode\IDEA\yolo\desktop\dist\YOLOv8 Detection Desktop Setup 1.0.0.exe`
- `D:\ProjectCode\IDEA\yolo\desktop\dist\win-unpacked\`

若打包中出现 `nsis` 下载超时，可重试同一命令；当前 `dist:win` 已内置镜像变量以降低失败概率。

## 已验证构建

已在当前仓库执行：
- `mvn -DskipTests package` -> 成功
- `npm run build` -> 成功

## 主要接口

- 标注
  - `POST /api/annotations/images`（`file + batchId + relativePath`）
  - `GET /api/annotations/images?batchId=...`
  - `PUT /api/annotations/images/{imageId}/boxes`
  - `DELETE /api/annotations/images`（清空所有）
  - `DELETE /api/annotations/images/{imageId}`
- 数据集构建
  - `POST /api/datasets/build`
- 训练
  - `POST /api/training/start`
  - `GET /api/training/{jobId}`
- 预测
  - `POST /api/detection/predict`（`files + paths + model/conf/iou/device`）
  - `GET /api/detection/results/{resultId}`
- 认证
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/profile/username`
  - `POST /api/auth/profile/password`
  - 兼容旧路径：`POST /api/auth/update-username`、`POST /api/auth/update-password`
- 系统
  - `GET /api/system/version`（前端启动自检/版本能力检测）
  - `POST /api/system/pick-directory`
  - `POST /api/system/pick-file`
