<template>
  <section class="card">
    <h3>数据集构建</h3>
    <label>数据集名称</label>
    <input v-model="datasetName" placeholder="例如: my-yolo-dataset" />

    <label>保存路径（可选，作为父目录，系统会在其下新建“数据集名称”文件夹）</label>
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

  <section class="card" v-if="buildResult">
    <h4>构建结果</h4>
    <p class="muted">状态：{{ buildResult.success ? "构建成功" : "构建失败" }}</p>
    <p class="muted">数据集位置：{{ buildResult.datasetPath || "-" }}</p>
    <p v-if="buildResult.message" class="muted">说明：{{ buildResult.message }}</p>
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
const buildResult = ref(null);

async function handleBuild() {
  try {
    const result = await buildDataset({
      datasetName: datasetName.value,
      outputPath: outputPath.value,
      outputPreset: outputPath.value ? null : "WORKSPACE_DATASETS",
      valRatio: valRatio.value,
      testRatio: testRatio.value
    });
    buildResult.value = {
      success: true,
      datasetPath: result?.datasetPath || "",
      message: ""
    };
  } catch (error) {
    buildResult.value = {
      success: false,
      datasetPath: "",
      message: error.message
    };
  }
}

function resetForm() {
  datasetName.value = `dataset-${Date.now()}`;
  outputPath.value = "";
  valRatio.value = 0.3;
  testRatio.value = 0.1;
  buildResult.value = null;
}
</script>

<style scoped>
/* 手输模式无需额外样式 */
</style>
