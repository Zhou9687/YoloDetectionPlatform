# 团队启动 SOP（简版）

适用目录：`D:\ProjectCode\IDEA\yolo`

## 1. 启动前检查（30 秒）

- 确认 MySQL 已启动，库名：`yolo_detection`
- 确认 `yolov8-detection/src/main/resources/application.yml` 中 Python 路径、模型路径可用
- 确认端口规划：后端 `8081`、前端 `5173`

## 2. 标准启动（推荐）

在项目根目录执行：

```powershell
Set-Location D:\ProjectCode\IDEA\yolo
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

脚本会自动完成：

1. 清理 `8081` 占用进程
2. 启动后端并等待 `GET /api/system/version` 就绪
3. 启动前端 `5173`

## 3. 验证是否启动成功

```powershell
Invoke-RestMethod http://127.0.0.1:8081/api/system/version | ConvertTo-Json -Depth 4
```

- 能返回 JSON，说明后端可用
- 浏览器打开 `http://127.0.0.1:5173`，出现登录页并可登录

## 4. 常见故障快速处理

### 4.1 报端口占用

```powershell
netstat -ano | findstr LISTENING | findstr :8081
taskkill /PID <PID> /F
```

### 4.2 Maven 参数报错（Unknown lifecycle phase）

不要手动传复杂 `-Dspring-boot.run.arguments`，优先使用 `start-all.ps1`。

### 4.3 前端提示“启动自检失败”

- 先确认 `http://127.0.0.1:8081/api/system/version` 可访问
- 若 404，说明后端版本过旧，重新编译并重启后端

## 5. 标准停止

- 关闭 `start-all.ps1` 启动的两个终端窗口（后端/前端）
- 如有残留，再手动清理 8081 端口进程

