import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import "./style.css";
import { API_BASE } from "./api/http";

const REQUIRED_CAPABILITIES = [
  "auth.profile.username",
  "auth.profile.password",
  "system.pick-directory",
  "system.pick-file"
];

function renderStartupError(message) {
  const appRoot = document.getElementById("app");
  if (!appRoot) return;
  appRoot.innerHTML = `
    <div style="min-height:100vh;display:grid;place-items:center;background:#f3f6fb;padding:20px;">
      <div style="max-width:760px;width:100%;background:#fff;border:1px solid #d8e2ef;border-radius:8px;padding:16px;box-shadow:0 8px 22px rgba(8,30,56,.12);">
        <h3 style="margin:0 0 10px;color:#1b3f64;">启动自检失败</h3>
        <p style="margin:0 0 6px;color:#2d4765;">前端检测到后端未就绪或版本不匹配，请先启动/重启后端。</p>
        <pre style="white-space:pre-wrap;background:#0e2237;color:#e7f3ff;border-radius:6px;padding:10px;">${message}</pre>
      </div>
    </div>
  `;
}

function withTimeout(promise, timeoutMs, message) {
  return Promise.race([
    promise,
    new Promise((_, reject) => setTimeout(() => reject(new Error(message)), timeoutMs))
  ]);
}

async function preflightCheck() {
  const response = await withTimeout(
    fetch(`${API_BASE}/api/system/version`, { method: "GET" }),
    8000,
    "请求 /api/system/version 超时（8s）"
  );

  if (!response.ok) {
    throw new Error(`后端接口 /api/system/version 返回 HTTP ${response.status}`);
  }

  const payload = await response.json();
  const capabilities = Array.isArray(payload?.capabilities) ? payload.capabilities : [];
  const missing = REQUIRED_CAPABILITIES.filter((item) => !capabilities.includes(item));
  if (missing.length > 0) {
    throw new Error(`后端版本过旧，缺少能力: ${missing.join(", ")}`);
  }
}

(async () => {
  try {
    await preflightCheck();
    createApp(App).use(router).mount("#app");
  } catch (error) {
    const reason = error instanceof Error ? error.message : String(error);
    renderStartupError(`API_BASE=${API_BASE || "(same-origin)"}\n${reason}`);
  }
})();
