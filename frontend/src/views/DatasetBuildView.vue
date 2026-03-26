<template>
  <section class="card">
    <h3>数据集构建</h3>
    <label>数据集名称</label>
    <input v-model="datasetName" placeholder="例如: my-yolo-dataset" />

    <label>保存路径（可选，留空默认 data/datasets）</label>
    <DirectoryPicker v-model="outputPath" placeholder="例如: D:/datasets" />

    <label>验证集比例（val）</label>
    <input type="number" min="0" max="0.9" step="0.01" v-model.number="valRatio" />

    <label>测试集比例（test）</label>
    <input type="number" min="0" max="0.9" step="0.01" v-model.number="testRatio" />

    <div style="display:flex; gap:8px;">
      <button @click="handleBuild">开始构建</button>
      <button class="secondary" @click="resetForm">重置</button>
    </div>
    <p class="muted">将生成 train/val/test（每个目录都含 imgs 与 labels）和 data.yaml。</p>
  </section>

  <section class="card" v-if="resultJson">
    <h4>构建结果</h4>
    <pre>{{ resultJson }}</pre>
  </section>
</template>

<script setup>
import { ref } from "vue";
import { buildDataset } from "../api/yoloApi";
import DirectoryPicker from "../components/DirectoryPicker.vue";

const datasetName = ref(`dataset-${Date.now()}`);
const outputPath = ref("");
const valRatio = ref(0.3);
const testRatio = ref(0.1);
const resultJson = ref("");

async function handleBuild() {
  try {
    const result = await buildDataset({
      datasetName: datasetName.value,
      outputPath: outputPath.value,
      outputPreset: outputPath.value ? null : "WORKSPACE_DATASETS",
      valRatio: valRatio.value,
      testRatio: testRatio.value
    });
    resultJson.value = JSON.stringify(result, null, 2);
  } catch (error) {
    resultJson.value = `构建失败: ${error.message}`;
  }
}

function resetForm() {
  datasetName.value = `dataset-${Date.now()}`;
  outputPath.value = "";
  valRatio.value = 0.3;
  testRatio.value = 0.1;
  resultJson.value = "";
}
</script>

<style scoped>
/* 手输模式无需额外样式 */
</style>
