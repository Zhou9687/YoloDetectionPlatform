export function resolveApiBase() {
  const isFileProtocol = typeof window !== "undefined" && window.location?.protocol === "file:";
  return import.meta.env.VITE_API_BASE_URL || (isFileProtocol ? "http://127.0.0.1:8081" : "");
}

export const API_BASE = resolveApiBase();
const DEFAULT_TIMEOUT_MS = Number(import.meta.env.VITE_HTTP_TIMEOUT_MS || 180000);

export async function httpRequest(path, options = {}) {
  const controller = new AbortController();
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      signal: options.signal ?? controller.signal
    });
    const contentType = response.headers.get("content-type") || "";
    const isJson = contentType.includes("application/json");
    const payload = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      const message = typeof payload === "object" && payload?.error ? payload.error : `HTTP ${response.status}`;
      throw new Error(message);
    }

    return payload;
  } catch (error) {
    if (error?.name === "AbortError") {
      throw new Error(`请求超时（>${Math.round(timeoutMs / 1000)}s）`);
    }
    if (error instanceof TypeError) {
      throw new Error("网络连接失败，请检查后端是否已启动以及接口地址是否正确");
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}
