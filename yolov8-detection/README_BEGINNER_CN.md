# YOLOv8 Detection 新手讲解文档（Spring Boot + Vue + Python）

> 面向第一次接触该项目的同学：从“项目是做什么的”到“怎么跑起来、怎么用、怎么排错”，按步骤讲清楚。

## 0. 这份文档会带你做什么

你将学会：

1. 项目的整体流程和每个模块的作用。
2. 前后端 + Python 推理脚本是如何配合的。
3. 如何在本机启动项目并访问页面。
4. 如何完成“标注 -> 构建数据集 -> 训练 -> 预测”的完整闭环。
5. 新手最常见问题（Maven、Node、超时、漏检）怎么处理。

---

## 1. 项目一句话说明

这是一个 YOLOv8 的可视化流程项目：

- 前端页面（Vue）用于上传图片、画框标注、发起训练、做预测；
- 后端（Spring Boot）管理数据、接收请求、调用 Python；
- Python 脚本（Ultralytics YOLO）负责真正的训练和检测。

你可以理解为：

**前端是操作台 -> 后端是调度员 -> Python 是模型引擎**。

---

## 2. 整体架构（先有全局认知）

### 2.1 技术栈

- 后端：Spring Boot（默认端口 `8081`）
- 前端：Vue3 + Vite（默认端口 `5173`）
- 训练/推理：Python + Ultralytics YOLOv8
- 启动推荐：根目录 `start-all.ps1` 一键拉起前后端

### 2.2 核心目录（你最常用）

```text
yolo/
  yolov8-detection/
    src/main/java/com/zhou/
      controller/      # 接口层
      service/         # 业务逻辑层
      dto/             # 请求参数对象
      model/           # 领域模型
    src/main/resources/
      application.yml  # 项目配置（Python路径、模型路径、默认阈值等）
    scripts/
      yolov8_runner.py # Python桥接脚本（detect/train）
      ultralytics-main/# YOLO代码目录
  frontend/
    src/
      views/           # 页面（标注/数据集/训练/预测）
      api/             # 前端调用后端接口
```

---

## 3. 四大功能模块（按业务流程）

## 3.1 图片标注模块（数据准备）

你在这里做的事：

- 上传图片（支持文件夹批次上传）；
- 对每张图手动画框；
- 保存后生成 YOLO 标签（`.txt`）。

你需要理解：

- 模型训练质量，90% 来自标注质量；
- 框的位置、大小、类别必须准确；
- 漏标、错标会直接导致模型漏检或误检。

### 3.2 数据集构建模块（整理训练数据）

作用：把标注结果转换成训练可用目录结构。

构建后会包含：

- `train/images` + `train/labels`
- `val/images` + `val/labels`
- `test/images` + `test/labels`
- `data.yaml`

关键参数：

- `datasetName`：数据集名字
- `outputPath`：输出目录（留空使用默认）
- `valRatio`：验证集比例
- `testRatio`：测试集比例

### 3.3 训练任务模块（训练模型）

你输入：

- 数据集路径 `datasetPath`
- 训练轮次 `epochs`
- 批大小 `batch`
- 初始模型 `modelPath`（可选）
- 训练 conf（可选）

你得到：

- 训练任务状态（运行中/成功/失败）
- 训练进度
- 最终权重路径（`best.pt`）

### 3.4 检测预测模块（实际推理）

你输入：

- 模型路径（可选，建议填你训练出的 `best.pt`）
- `conf / iou / device / imgsz / maxDet / augment`
- 上传待预测图片（单张或文件夹）

你看到：

- 每张图的检测框坐标与置信度
- 结果图（渲染后图片）

### 3.5 账号登录与账户管理模块（新增）

你在这里做的事：

- 新用户注册、登录
- 未登录时禁止访问业务页面（路由守卫）
- 登录后可在右上角头像菜单修改账号名、修改密码、退出登录

对应后端接口：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/profile/username`
- `POST /api/auth/profile/password`

兼容旧路径（用于历史前端版本）：

- `POST /api/auth/update-username`
- `POST /api/auth/update-password`

---

## 4. 新手最关心：如何启动项目

> 下面命令按 Windows PowerShell 写法。

### 4.0 一键启动（推荐）

```powershell
Set-Location D:\ProjectCode\IDEA\yolo
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

该脚本会自动：

1. 清理 `8081` 端口占用。
2. 启动后端并等待 `GET /api/system/version` 就绪。
3. 再启动前端。

### 4.1 启动后端（手动方式）

先进入后端目录（**必须在有 `pom.xml` 的目录执行**）：

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\yolov8-detection
mvn -DskipTests package
$env:SERVER_PORT=8081
mvn spring-boot:run
```

如果 `mvn` 提示找不到命令，请先执行：

```powershell
mvn -v
```

确认 Maven 可用再继续。

### 4.2 启动前端

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\frontend
npm install
npm run dev
```

浏览器访问：

- `http://localhost:5173`（先登录）

### 4.3 启动桌面版（可选）

如果你不想分开启动前后端，可以直接启动桌面壳：

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\desktop
npm install
npm run dev
```

说明：桌面壳会自动拉起后端并加载页面。请先确保 MySQL 可连接。

### 4.4 打包桌面安装包（Windows）

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\desktop
npm run dist:win
```

成功后会生成：

- `D:\ProjectCode\IDEA\yolo\desktop\dist\YOLOv8 Detection Desktop Setup 1.0.0.exe`
- `D:\ProjectCode\IDEA\yolo\desktop\dist\win-unpacked\`

如果出现 `nsis` 下载超时，直接重试上面命令即可（项目已配置镜像）。

---

## 5. application.yml 关键配置（一定要懂）

配置文件：`src/main/resources/application.yml`

重点字段：

- `python-command`：Python 解释器路径（例如你的 Anaconda 环境）
- `detect-script` / `train-script`：Python 脚本路径
- `model-path`：默认模型路径（不填模型时使用）
- `conf` / `iou`：默认阈值
- `device`：推理/训练设备（`cpu` 或 `0` 表示第1块GPU）

新手建议：

1. 先用绝对路径，减少路径错误。
2. 模型路径优先使用你自己训练出的 `best.pt`。
3. 漏检多时，先把 `conf` 降低（如 `0.05` 或 `0.03`）测试召回率。

---

## 6. 一次完整实操（建议照着做一遍）

### Step A：上传并标注

1. 上传一批图片。
2. 每张图画框并保存。
3. 确认“已标注/未标注”状态正确。

### Step B：构建数据集

1. 输入 `datasetName`。
2. 选择/填写 `outputPath`（可留空）。
3. 设置 `valRatio=0.3`、`testRatio=0.1`（示例）。
4. 点击开始构建，记录返回的 `datasetPath`。

### Step C：训练

1. 在训练页面填 `datasetPath`。
2. 先用保守参数：`epochs=50`、`batch=8`（按显存调整）。
3. 训练结束后记录 `bestModelPath`。

### Step D：预测

1. 在预测页面把 `modelPath` 指向 `bestModelPath`。
2. 上传待测图片。
3. 看结果图和 JSON。
4. 漏检多先降 `conf`，误检多再升 `conf`。

---

## 7. 常见问题与解决（结合本项目高频问题）

## 7.0 前端提示“启动自检失败”

原因：前端启动时会先请求 `GET /api/system/version` 做后端可用性和能力检查。

解决：

1. 确认后端在 `8081` 已启动。
2. 直接访问 `http://127.0.0.1:8081/api/system/version`，应返回 JSON。
3. 若返回 404，说明你跑的是旧后端，重启并重新编译后端。

## 7.1 Maven 报错：no POM in this directory

原因：你在错误目录执行了 Maven。

解决：切换到 `yolov8-detection` 后再执行。

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\yolov8-detection
mvn -DskipTests package
```

## 7.2 Python 脚本找不到（can't open file yolov8_runner.py）

原因：脚本路径配置不对。

检查：`application.yml` 里的 `detect-script`、`train-script` 是否指向 `scripts/yolov8_runner.py`。

## 7.3 前端 npm 权限问题（EPERM）

常见于缓存目录权限异常。

可临时指定项目本地缓存后再执行：

```powershell
$env:npm_config_cache='D:\ProjectCode\IDEA\yolo\frontend\.npm-cache'
npm install
npm run dev
```

## 7.4 预测超时 / 进度长时间很高

原因通常是：图片多、分辨率大、TTA开启、CPU推理慢。

建议：

1. 先减少单批图片数。
2. 降低 `imgsz`（如从 1280 调到 960 或 640）。
3. 关掉 `augment` 看是否改善。
4. 使用 GPU（`device=0`）。

## 7.5 绝大多数图片漏检

优先检查：

1. 是否在用基础模型而不是自己的 `best.pt`。
2. `conf` 是否太高（先降到 `0.05` 或 `0.03`）。
3. 标注是否一致、是否有漏标。
4. 数据集样本数量是否太少。

---

## 8. 给初学者的调参建议（够用版）

先按这个思路调：

- 先追求“能检出”（高召回）：`conf` 低一点（`0.03~0.08`）
- 再压误检：逐步把 `conf` 往上调（每次 `+0.02`）
- 小目标漏检：提高 `imgsz`
- 速度慢：降低 `imgsz`、减小批量、关闭 `augment`

一句话：**先让模型看见，再让模型看准**。

---

## 9. 项目学习路径（建议）

如果你是新手，建议按这个顺序看代码：

1. 前端请求定义：`frontend/src/api/yoloApi.js`
2. 后端接口入口：`controller/*Controller.java`
3. 业务逻辑：`service/*Service.java`
4. Python桥接：`scripts/yolov8_runner.py`
5. 配置：`application.yml`

这样你会很快建立“一个按钮点击后，系统内部发生了什么”的完整链路认知。

---

## 10. 你可以继续做的优化（进阶）

1. 给预测增加“取消任务”能力。
2. 给训练增加更多参数（`lr0`、`patience`、`mosaic` 等）。
3. 增加模型管理页（历史权重、最佳指标对比）。
4. 增加数据集质量检查（空标签、越界框、类别分布）。

---

如果你希望，我可以下一步再给你补一份《部署文档》（含前后端打包、生产环境配置、开机自启、日志排查）。
