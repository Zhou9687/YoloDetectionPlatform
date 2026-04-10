<template>
  <section class="card">
    <h3>检测预测</h3>
    <label>.pt 模型路径（可选）</label>
    <DirectoryPicker
      v-model="modelPath"
      mode="file"
      accept=".pt"
      :file-extensions="['pt']"
      placeholder="例如: D:/runs/train/exp/weights/best.pt"
    />

    <label>conf</label>
    <input type="number" min="0" max="1" step="0.01" v-model.number="conf" />

    <label>iou</label>
    <input type="number" min="0" max="1" step="0.01" v-model.number="iou" />

    <label>imgsz（建议 960/1280，越大越慢）</label>
    <input type="number" min="320" step="32" v-model.number="imgsz" />

    <label>maxDet（每张图最多保留框数）</label>
    <input type="number" min="1" step="1" v-model.number="maxDet" />

    <label>device（cpu/0 等）</label>
    <input v-model="device" placeholder="例如: 0 或 cpu" />

    <label>选择文件或文件夹</label>
    <input type="file" accept="image/*" multiple webkitdirectory directory @change="onFileChange" />
    <div style="display:flex; gap:8px;">
      <button @click="handlePredict" :disabled="predicting || predictFilesList.length === 0">
        {{ predicting ? "预测中..." : "上传并预测" }}
      </button>
      <button class="secondary" @click="resetForm" :disabled="predicting">重置</button>
    </div>

    <div v-if="showProgress" class="progress-wrap">
      <div class="progress-head">
        <span>预测进度</span>
        <span>{{ Math.floor(predictProgress) }}%</span>
      </div>
      <div class="progress-track" :class="`progress-${progressState}`">
        <div class="progress-fill" :style="{ width: `${predictProgress}%` }"></div>
      </div>
      <div class="muted">{{ progressText }}</div>
    </div>
  </section>

  <section class="card" v-if="resultItems.length">
    <h4>预测结果图</h4>
    <p class="muted">成功 {{ successCount }} 张，失败 {{ failedCount }} 张</p>
    <div class="result-grid">
      <div class="result-item" v-for="item in resultItems" :key="item._key">
        <div class="muted">{{ item.relativePath || item.fileName || item._key }}</div>
        <div v-if="item.success === false" class="error-text">失败：{{ item.error || 'unknown error' }}</div>
        <img v-else class="preview" :src="resolveResultUrl(item)
        " alt="predict-result" />
      </div>
    </div>
  </section>


</template>

<script setup>
import { computed, ref } from "vue";
import { detectionResultUrl, predictFiles } from "../api/yoloApi";
import DirectoryPicker from "../components/DirectoryPicker.vue";

const predictFilesList = ref([]);
const resultItems = ref([]);
const resultJson = ref("");

const modelPath = ref("");
const conf = ref(0.05);
const iou = ref(0.45);
const imgsz = ref(960);
const maxDet = ref(300);
const device = ref("0");
const predicting = ref(false);
const predictProgress = ref(0);
const progressText = ref("");
const progressState = ref("idle");
const showProgress = computed(() => predicting.value || progressState.value !== "idle");
const PREDICT_BATCH_SIZE = 4;
let progressTimer = null;
let resetProgressTimer = null;

function onFileChange(event) {
  predictFilesList.value = Array.from(event.target.files || []).filter((file) => file.type.startsWith("image/"));
}

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

function resolveResultUrl(item) {
  if (!item) return "";
  if (item.resultImageUrl) {
    if (/^https?:\/\//i.test(item.resultImageUrl)) return item.resultImageUrl;
    return `${API_BASE}${item.resultImageUrl}`;
  }
  return item.resultId ? detectionResultUrl(item.resultId) : "";
}

function normalizePredictResult(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.data)) return payload.data;
  if (payload && typeof payload === "object" && (payload.resultId || payload.resultImageUrl)) {
    return [payload];
  }
  return [];
}

function estimatePredictTimeoutMs(fileCount) {
  const perImageBaseMs = 25000;
  const imgszFactor = Math.max(1, (imgsz.value || 640) / 640);
  const estimated = Math.ceil(fileCount * perImageBaseMs * imgszFactor);
  return Math.max(120000, Math.min(estimated, 420000));
}

function startBatchProgress(startPercent, targetPercent, text) {
  stopPredictProgress();
  clearResetProgress();
  progressState.value = "running";
  predictProgress.value = Math.max(1, startPercent);
  progressText.value = text;
  const stepMs = 260;
  progressTimer = setInterval(() => {
    const current = predictProgress.value;
    const ceiling = Math.max(current, targetPercent - 0.4);
    if (current >= ceiling) return;
    const dynamicStep = Math.max(0.15, (ceiling - current) * 0.08);
    predictProgress.value = Math.min(ceiling, current + dynamicStep);
  }, stepMs);
}

function completePredictProgress(success, text) {
  stopPredictProgress();
  progressState.value = success ? "success" : "error";
  predictProgress.value = 100;
  progressText.value = text;
  clearResetProgress();
  resetProgressTimer = setTimeout(() => {
    progressState.value = "idle";
    predictProgress.value = 0;
    progressText.value = "";
  }, 1500);
}

function clearResetProgress() {
  if (resetProgressTimer) {
    clearTimeout(resetProgressTimer);
    resetProgressTimer = null;
  }
}

function stopPredictProgress() {
  if (progressTimer) {
    clearInterval(progressTimer);
    progressTimer = null;
  }
}

const failedCount = computed(() => resultItems.value.filter((item) => item.success === false).length);
const successCount = computed(() => resultItems.value.length - failedCount.value);

async function handlePredict() {
  if (predictFilesList.value.length === 0) return;
  predicting.value = true;
  resultJson.value = "";
  resultItems.value = [];
  try {
    const total = predictFilesList.value.length;
    const batchCount = Math.ceil(total / PREDICT_BATCH_SIZE);
    const allResults = [];

    for (let offset = 0; offset < total; offset += PREDICT_BATCH_SIZE) {
      const batchFiles = predictFilesList.value.slice(offset, offset + PREDICT_BATCH_SIZE);
      const batchPaths = batchFiles.map((file) => file.webkitRelativePath || file.name);
      const batchIndex = Math.floor(offset / PREDICT_BATCH_SIZE) + 1;
      const startPercent = (offset / total) * 100;
      const endPercent = ((offset + batchFiles.length) / total) * 100;
      startBatchProgress(startPercent, Math.min(99, endPercent), `正在预测第 ${batchIndex}/${batchCount} 批...`);

      const result = await predictFiles(batchFiles, batchPaths, {
        modelPath: modelPath.value,
        conf: conf.value,
        iou: iou.value,
        imgsz: imgsz.value,
        maxDet: maxDet.value,
        device: device.value,
        timeoutMs: estimatePredictTimeoutMs(batchFiles.length)
      });

      const normalized = normalizePredictResult(result).map((item, index) => ({
        success: item?.success !== false,
        ...item,
        _key:
          item.resultId ||
          item.resultImageUrl ||
          item.relativePath ||
          item.fileName ||
          `predict-${offset + index}`
      }));

      allResults.push(...normalized);
      resultItems.value = [...allResults];
      stopPredictProgress();
      predictProgress.value = Math.min(99, endPercent);
      progressText.value = `已完成 ${Math.min(offset + batchFiles.length, total)} / ${total} 张`;
    }

    resultJson.value = JSON.stringify(allResults, null, 2);
    const finishText = failedCount.value > 0
      ? `预测完成：成功 ${successCount.value} 张，失败 ${failedCount.value} 张`
      : `预测完成，共 ${successCount.value} 张结果图`;
    completePredictProgress(true, finishText);
  } catch (error) {
    resultJson.value = `预测失败: ${error.message}`;
    completePredictProgress(false, `预测失败：${error.message}`);
  } finally {
    predicting.value = false;
  }
}

function resetForm() {
  stopPredictProgress();
  clearResetProgress();
  predictFilesList.value = [];
  resultItems.value = [];
  resultJson.value = "";
  modelPath.value = "";
  conf.value = 0.05;
  iou.value = 0.45;
  imgsz.value = 960;
  maxDet.value = 300;
  device.value = "0";
  predictProgress.value = 0;
  progressText.value = "";
  progressState.value = "idle";
}
</script>

<style scoped>
.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.result-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px;
}

.progress-wrap {
  margin-top: 10px;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
}

.progress-track {
  height: 10px;
  width: 100%;
  background: #e5e7eb;
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #2563eb;
  transition: width 0.3s ease;
}

.progress-track.progress-success .progress-fill {
  background: #16a34a;
}

.progress-track.progress-error .progress-fill {
  background: #dc2626;
}

.error-text {
  color: #dc2626;
  margin-top: 6px;
  font-size: 13px;
  word-break: break-word;
}

.secondary {
  background-color: #f3f4f6;
  color: #111827;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.secondary:hover {
  background-color: #e5e7eb;
}
</style>
