<template>
  <section class="card">
    <h3>训练任务</h3>
    <label>数据集目录（包含 data.yaml）</label>
    <DirectoryPicker v-model="datasetPath" placeholder="例如: D:/ProjectCode/IDEA/yolo/data/datasets/my-dataset" />

    <label>预训练模型路径（可选）</label>
    <DirectoryPicker
      v-model="modelPath"
      mode="file"
      accept=".pt"
      :file-extensions="['pt']"
      placeholder="例如: D:/models/yolov8n.pt"
    />

    <label>epochs</label>
    <input type="number" min="1" v-model.number="epochs" />

    <label>batch</label>
    <input type="number" min="1" v-model.number="batch" />

    <label>conf（置信度阈值）</label>
    <input type="number" min="0" max="1" step="0.01" v-model.number="conf" />

    <div style="display:flex; gap:8px;">
      <button type="button" @click="startJob" :disabled="starting || !datasetPath.trim()">{{ starting ? "启动中..." : "启动训练" }}</button>
      <button type="button" class="secondary" @click="refreshJob" :disabled="!jobId">刷新状态</button>
      <button type="button" class="secondary" @click="resetForm">重置</button>
    </div>

    <p v-if="statusText" class="muted">{{ statusText }}</p>
    <p v-if="jobId" class="muted">任务ID: {{ jobId }}</p>
    <p v-if="job" class="muted">训练进度: {{ job.progress }}%</p>
  </section>

  <section class="card" v-if="job">
    <h4>任务状态</h4>
    <p class="muted">状态：{{ job.status === "SUCCESS" ? "训练成功" : job.status === "FAILED" ? "训练失败" : "训练中" }}</p>
    <p class="muted">权重文件：{{ job.bestModelPath || "-" }}</p>
    <p class="muted">Precision：{{ formatMetric(job.precision) }}</p>
    <p class="muted">Recall：{{ formatMetric(job.recall) }}</p>
    <p class="muted">mAP50：{{ formatMetric(job.map50) }}</p>
    <p class="muted">mAP50-95：{{ formatMetric(job.map95) }}</p>
    <p v-if="job.status === 'FAILED' && job.message" class="muted">失败原因：{{ job.message }}</p>
  </section>
</template>

<script setup>
import { onBeforeUnmount, ref } from "vue";
import { getTrainingJob, startTraining } from "../api/yoloApi";
import DirectoryPicker from "../components/DirectoryPicker.vue";

const datasetPath = ref("");
const modelPath = ref("");
const epochs = ref(50);
const batch = ref(16);
const conf = ref(0.12);
const jobId = ref("");
const job = ref(null);
const starting = ref(false);
const statusText = ref("");
let pollTimer = null;

function formatMetric(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return "-";
  }
  return Number(value).toFixed(4);
}

async function startJob() {
  if (!datasetPath.value.trim()) {
    statusText.value = "请先填写数据集目录（包含 data.yaml）";
    return;
  }

  starting.value = true;
  statusText.value = "正在提交训练任务...";
  try {
    const created = await startTraining(
      datasetPath.value.trim(),
      epochs.value,
      batch.value,
      modelPath.value.trim(),
      conf.value
    );
    jobId.value = created.jobId;
    job.value = created;
    statusText.value = "任务已提交，正在轮询状态";
    await refreshJob();
    startPolling();
  } catch (error) {
    statusText.value = "启动失败";
    job.value = {
      status: "FAILED",
      bestModelPath: "",
      message: error.message
    };
  } finally {
    starting.value = false;
  }
}

async function refreshJob() {
  if (!jobId.value) return;
  try {
    const latest = await getTrainingJob(jobId.value);
    job.value = latest;
    if (latest.status === "SUCCESS" || latest.status === "FAILED") {
      statusText.value = latest.status === "SUCCESS" ? "训练完成" : "训练失败";
      stopPolling();
    } else {
      statusText.value = `任务进行中: ${latest.progress ?? 0}%`;
    }
  } catch (error) {
    statusText.value = "查询失败";
    job.value = {
      status: "FAILED",
      bestModelPath: "",
      message: error.message
    };
    stopPolling();
  }
}

function startPolling() {
  stopPolling();
  pollTimer = setInterval(refreshJob, 2000);
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

function resetForm() {
  stopPolling();
  datasetPath.value = "";
  modelPath.value = "";
  epochs.value = 50;
  batch.value = 16;
  conf.value = 0.12;
  jobId.value = "";
  job.value = null;
  starting.value = false;
  statusText.value = "";
}

onBeforeUnmount(stopPolling);
</script>
