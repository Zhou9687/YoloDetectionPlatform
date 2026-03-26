import { httpRequest } from "./http";

function resolveApiBase() {
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
  }
  if (typeof window !== "undefined" && window.location?.protocol === "file:") {
    return "http://127.0.0.1:8081";
  }
  return "";
}

const API_BASE = resolveApiBase();

export function uploadImage(file, batchId, relativePath) {
  const form = new FormData();
  form.append("file", file);
  if (batchId) form.append("batchId", batchId);
  if (relativePath) form.append("relativePath", relativePath);
  return httpRequest("/api/annotations/images", { method: "POST", body: form });
}

export function listImages(batchId) {
  const query = batchId ? `?batchId=${encodeURIComponent(batchId)}` : "";
  return httpRequest(`/api/annotations/images${query}`);
}

export function clearImages() {
  return httpRequest("/api/annotations/images", { method: "DELETE" });
}

export function getImage(imageId) {
  return httpRequest(`/api/annotations/images/${imageId}`);
}

export function deleteImage(imageId) {
  return httpRequest(`/api/annotations/images/${imageId}`, { method: "DELETE" });
}

export function saveImageBoxes(imageId, boxes) {
  return httpRequest(`/api/annotations/images/${imageId}/boxes`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ boxes })
  });
}

export function buildDataset(payload) {
  return httpRequest("/api/datasets/build", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
}

export function listDirectoryRoots() {
  return httpRequest("/api/datasets/directory-roots");
}

export function listDirectories(path) {
  const query = path ? `?path=${encodeURIComponent(path)}` : "";
  return httpRequest(`/api/datasets/directories${query}`);
}

export function predictFiles(files, paths, options = {}) {
  const form = new FormData();
  for (const file of files) {
    form.append("files", file);
  }
  for (const path of paths || []) {
    form.append("paths", path);
  }
  if (options.modelPath) form.append("modelPath", options.modelPath);
  if (options.conf != null) form.append("conf", String(options.conf));
  if (options.iou != null) form.append("iou", String(options.iou));
  if (options.device) form.append("device", options.device);
  if (options.imgsz != null) form.append("imgsz", String(options.imgsz));
  if (options.maxDet != null) form.append("maxDet", String(options.maxDet));
  if (options.augment != null) form.append("augment", String(options.augment));
  return httpRequest("/api/detection/predict", {
    method: "POST",
    body: form,
    timeoutMs: options.timeoutMs
  });
}

export function startTraining(datasetPath, epochs, batch, modelPath, conf) {
  return httpRequest("/api/training/start", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ datasetPath, epochs, batch, modelPath, conf })
  });
}

export function getTrainingJob(jobId) {
  return httpRequest(`/api/training/${jobId}`);
}

export function imageContentUrl(imageId) {
  return `${API_BASE}/api/annotations/images/${imageId}/content`;
}

export function detectionResultUrl(resultId) {
  return `${API_BASE}/api/detection/results/${resultId}`;
}

export function pickDirectory(title = "选择文件夹") {
  return httpRequest("/api/system/pick-directory", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title })
  });
}

export function pickFile(title = "选择文件", extensions = ["pt"]) {
  return httpRequest("/api/system/pick-file", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title, extensions })
  });
}
