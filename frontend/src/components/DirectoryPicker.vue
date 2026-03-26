<template>
  <div class="directory-picker">
    <div class="picker-row">
      <input
        :value="modelValue || ''"
        :placeholder="placeholder"
        @input="onManualInput"
      />
      <button type="button" class="browse-btn" @click="browsePath">Browse...</button>
      <input
        v-if="isDirectoryMode"
        ref="folderInputRef"
        class="hidden-picker-input"
        type="file"
        webkitdirectory
        directory
        @change="onFolderPicked"
      />
      <input
        v-else
        ref="fileInputRef"
        class="hidden-picker-input"
        type="file"
        :accept="accept"
        @change="onFilePicked"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { pickDirectory as pickDirectoryByApi, pickFile as pickFileByApi } from "../api/yoloApi";

const props = defineProps({
  modelValue: {
    type: String,
    default: ""
  },
  placeholder: {
    type: String,
    default: "请选择文件夹"
  },
  mode: {
    type: String,
    default: "directory"
  },
  accept: {
    type: String,
    default: ".pt"
  },
  fileExtensions: {
    type: Array,
    default: () => ["pt"]
  }
});

const emit = defineEmits(["update:modelValue"]);

const folderInputRef = ref(null);
const fileInputRef = ref(null);
const isDirectoryMode = computed(() => props.mode !== "file");
const browserPathWarningShown = ref(false);

function onManualInput(event) {
  const value = String(event?.target?.value || "");
  emit("update:modelValue", value);
}

function getDirectoryFromFilePath(filePath) {
  const text = String(filePath || "");
  if (!text) return "";
  const lastSlash = Math.max(text.lastIndexOf("\\"), text.lastIndexOf("/"));
  return lastSlash > 0 ? text.slice(0, lastSlash) : "";
}

function getTopFolderFromRelative(relativePath) {
  const text = String(relativePath || "");
  if (!text) return "";
  const segments = text.split(/[\\/]/).filter(Boolean);
  return segments.length > 0 ? segments[0] : "";
}

function onFolderPicked(event) {
  const files = Array.from(event?.target?.files || []);
  if (files.length === 0) return;

  const first = files[0];
  const absoluteDir = getDirectoryFromFilePath(first?.path);
  if (absoluteDir) {
    emit("update:modelValue", absoluteDir);
  } else {
    const topFolder = getTopFolderFromRelative(first?.webkitRelativePath || "");
    if (topFolder) {
      emit("update:modelValue", topFolder);
      if (!browserPathWarningShown.value) {
        browserPathWarningShown.value = true;
        window.alert("当前环境无法直接读取系统绝对路径，已回退为目录名。建议检查后端文件选择器是否可用。");
      }
    }
  }

  // 允许重复选择同一个目录时依旧触发 change。
  if (folderInputRef.value) {
    folderInputRef.value.value = "";
  }
}

function onFilePicked(event) {
  const first = Array.from(event?.target?.files || [])[0];
  if (!first) return;
  const pickedPath = String(first?.path || first?.name || "");
  if (pickedPath) {
    emit("update:modelValue", pickedPath);
  }
  if (fileInputRef.value) {
    fileInputRef.value.value = "";
  }
}

async function browsePath() {
  const hasBridge = typeof window !== "undefined" && !!window.desktopBridge;
  if (hasBridge) {
    try {
      if (isDirectoryMode.value && typeof window.desktopBridge.pickDirectory === "function") {
        const picked = await window.desktopBridge.pickDirectory();
        if (picked) {
          emit("update:modelValue", String(picked));
        }
        return;
      }
      if (!isDirectoryMode.value && typeof window.desktopBridge.pickFile === "function") {
        const picked = await window.desktopBridge.pickFile({
          title: "选择模型文件",
          extensions: Array.isArray(props.fileExtensions) ? props.fileExtensions : ["pt"]
        });
        if (picked) {
          emit("update:modelValue", String(picked));
        }
        return;
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error || "unknown error");
      console.error("Native picker failed", error);
      window.alert(`原生文件选择器调用失败: ${message}`);
      return;
    }
  }

  try {
    if (isDirectoryMode.value) {
      const result = await pickDirectoryByApi("选择文件夹");
      const picked = String(result?.path || "");
      if (picked) {
        emit("update:modelValue", picked);
        return;
      }
      window.alert("未选择文件夹，或后端未返回绝对路径。");
      return;
    }

    const result = await pickFileByApi(
      "选择模型文件",
      Array.isArray(props.fileExtensions) ? props.fileExtensions : ["pt"]
    );
    const picked = String(result?.path || "");
    if (picked) {
      emit("update:modelValue", picked);
      return;
    }
    window.alert("未选择文件，或后端未返回绝对路径。");
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error || "unknown error");
    console.error("Backend picker failed", error);
    window.alert(`后端文件选择器不可用: ${message}`);
  }
}
</script>

<style scoped>
.picker-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.picker-row > input:not(.hidden-picker-input) {
  flex: 7 1 0;
  min-width: 0;
  height: 34px;
  padding: 6px 10px;
  border: 1px solid #c6c6c6;
  border-radius: 3px;
}

.browse-btn {
  flex: 3 1 0;
  min-width: 0;
  max-width: 220px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #c6c6c6;
  border-radius: 3px;
  background: #f3f3f3;
  color: #222;
  cursor: pointer;
}

.browse-btn:hover {
  background: #ececec;
}

.hidden-picker-input {
  display: none;
}
</style>
