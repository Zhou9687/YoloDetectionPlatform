# 前端（Vue3 + Vite）

## 运行

推荐先用根目录一键脚本启动前后端：

```powershell
Set-Location D:\ProjectCode\IDEA\yolo
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

仅启动前端（需要后端已在 `8081` 就绪）：

```powershell
Set-Location D:\ProjectCode\IDEA\yolo\frontend
npm install --cache .npm-cache
npm run dev
```

默认访问 `http://localhost:5173`，并通过 Vite 代理将 `/api` 转发到 `http://localhost:8081`。

## 启动自检（新增）

前端入口在挂载前会请求：`GET /api/system/version`。

- 若后端未启动/不可达：页面会显示“启动自检失败”并给出原因。
- 若后端版本过旧（缺少能力声明）：会提示缺失能力列表，避免进入页面后才出现 `Not Found`。

## 构建

```powershell
npm run build
npm run preview
```

## 页面与路由

- `/login`：登录
- `/register`：注册
- `/annotate`：上传图片、查询图片、编辑并保存标注框（需登录）
- `/dataset`：构建数据集（需登录）
- `/predict`：上传图片并查看预测结果（需登录）
- `/training`：启动训练并轮询任务状态（需登录）

## 账号管理（顶栏头像）

登录后右上角头像菜单支持：
- 修改账户名称
- 修改密码
- 退出登录

对应接口：
- `POST /api/auth/profile/username`
- `POST /api/auth/profile/password`
- 兼容旧路径：`POST /api/auth/update-username`、`POST /api/auth/update-password`
