<template>
  <div class="row">
    <section class="card">
      <h3>1) 上传文件夹</h3>
      <input type="file" accept="image/*" webkitdirectory directory multiple @change="onFolderChange" />
      <button @click="handleFolderUpload" :disabled="folderFiles.length === 0 || uploading">
        上传文件夹 ({{ folderFiles.length }} 张)
      </button>
      <div v-if="uploading" class="progress-wrap">
        <div class="progress-bar" :style="{ width: uploadPercent + '%' }"></div>
      </div>
      <p class="muted" v-if="uploading">图片上传进度: {{ uploadProgress }}</p>

      <h3>2) 图片列表（当前批次）</h3>
      <div style="display:flex; gap:8px;">
        <button class="secondary" @click="loadImages" :disabled="!currentBatchId">刷新列表</button>
        <button class="secondary" @click="clearAllImageList" :disabled="images.length === 0 || uploading">删除所有图片</button>
        <button class="secondary" @click="resetViewState" :disabled="uploading">重置</button>
      </div>
      <select v-model="selectedImageId" @change="loadImageDetail">
        <option value="">请选择图片</option>
        <option v-for="img in images" :key="img.imageId" :value="img.imageId">
          [{{ img.annotated ? "已标注" : "未标注" }}] {{ img.relativePath || img.originalFileName }}
        </option>
      </select>

      <div style="display:flex; gap:8px; margin-top:8px;">
        <button class="secondary" @click="goPrev" :disabled="!hasPrev">上一张</button>
        <button class="secondary" @click="goNext" :disabled="!hasNext">下一张</button>
      </div>
    </section>

    <section class="card">
      <h3>3) 手动标注</h3>
      <label>标注模式</label>
      <div style="display:flex; gap:8px; margin-bottom:8px;">
        <button type="button" class="secondary" :class="{ active: annotationMode === 'rect' }" @click="setAnnotationMode('rect')">矩形</button>
        <button type="button" class="secondary" :class="{ active: annotationMode === 'polygon' }" @click="setAnnotationMode('polygon')">多边形</button>
        <button type="button" class="secondary" @click="finishPolygon" :disabled="annotationMode !== 'polygon' || draftPolygonPoints.length < 3">完成多边形</button>
        <button type="button" class="secondary" @click="cancelPolygon" :disabled="annotationMode !== 'polygon' || draftPolygonPoints.length === 0">取消多边形</button>
      </div>

      <label>标签</label>
      <input v-model.trim="newBoxLabel" placeholder="例如 person" />
      <label>置信度</label>
      <input type="number" min="0" max="1" step="0.01" v-model.number="newBoxConfidence" />

      <div class="annotate-stage" v-if="selectedImageId">
        <img
          ref="previewImageRef"
          class="preview annotate-image"
          :src="previewUrl"
          alt="preview"
          @load="onPreviewLoaded"
          draggable="false"
        />
        <div
          ref="overlayRef"
          class="annotate-overlay"
          :style="overlayStyle"
          @click="onOverlayClick"
          @mousedown="startDraw"
          @mousemove="moveDraw"
          @mouseup="finishDraw"
          @mouseleave="cancelDraw"
        >
          <svg class="annotate-svg" :width="imageWidth" :height="imageHeight">
            <polygon
              v-for="poly in renderPolygons"
              :key="`poly-${poly.idx}`"
              :points="poly.points"
              class="poly-shape"
              :class="{ selected: poly.idx === selectedBoxIndex }"
              @click.stop="selectBox(poly.idx)"
            />
            <polyline v-if="draftPolylinePoints" :points="draftPolylinePoints" class="poly-draft" />
            <circle
              v-for="(point, idx) in draftPolygonPointsPx"
              :key="`draft-point-${idx}`"
              :cx="point.x"
              :cy="point.y"
              r="3"
              class="poly-point"
            />
          </svg>

          <div
            v-for="rect in renderRects"
            :key="rect.idx"
            class="box-rect"
            :class="{ selected: rect.idx === selectedBoxIndex }"
            :style="rect.style"
            @click.stop="selectBox(rect.idx)"
            @mousedown.stop="startMoveBox(rect.idx, $event)"
          >
            <span class="box-label">{{ rect.label }}</span>
            <span
              v-for="handle in resizeHandles"
              :key="`${rect.idx}-${handle}`"
              class="resize-handle"
              :class="`handle-${handle}`"
              @mousedown.stop="startResizeBox(rect.idx, handle, $event)"
            ></span>
          </div>
          <div v-if="draftRect" class="box-rect draft" :style="draftRect.style"></div>
        </div>
      </div>

      <div style="display:flex; gap:8px; margin-top:8px;">
        <button class="secondary" @click="removeSelectedBox" :disabled="selectedBoxIndex < 0">删除选中框</button>
        <button class="secondary" @click="removeLastBox" :disabled="boxes.length === 0 && draftPolygonPoints.length === 0">撤销最后一个</button>
        <button class="secondary" @click="clearBoxes" :disabled="boxes.length === 0 && draftPolygonPoints.length === 0">清空框</button>
      </div>

      <div style="display:flex; gap:8px; margin-top:8px;">
        <button @click="saveBoxes" :disabled="!selectedImageId">保存标注</button>
      </div>
      <p class="muted">保存后会自动在该批次目录生成 labels 子目录，并写入对应 YOLO txt。</p>
    </section>
  </div>

  <section class="card" v-if="message">
    <strong>提示:</strong> {{ message }}
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import {
  clearImages,
  getImage,
  imageContentUrl,
  listImages,
  saveImageBoxes,
  uploadImage
} from "../api/yoloApi";

const MAX_UPLOAD_CONCURRENCY = 5;

const images = ref([]);
const selectedImageId = ref("");
const boxes = ref([]);
const message = ref("");
const currentBatchId = ref("");

const folderFiles = ref([]);
const uploading = ref(false);
const uploadProgress = ref("0/0");
const uploadPercent = ref(0);

const newBoxLabel = ref("object");
const newBoxConfidence = ref(1);
const previewImageRef = ref(null);
const overlayRef = ref(null);
const imageWidth = ref(0);
const imageHeight = ref(0);
const drawing = ref(false);
const drawStart = ref(null);
const drawCurrent = ref(null);
const selectedBoxIndex = ref(-1);
const interactionMode = ref("idle");
const dragStart = ref(null);
const dragSourceRect = ref(null);
const activeResizeHandle = ref("");
const resizeHandles = ["nw", "ne", "sw", "se"];
const annotationMode = ref("rect");
const draftPolygonPoints = ref([]);

const previewUrl = computed(() => (selectedImageId.value ? imageContentUrl(selectedImageId.value) : ""));
const overlayStyle = computed(() => ({
  width: `${imageWidth.value}px`,
  height: `${imageHeight.value}px`
}));

const currentIndex = computed(() => images.value.findIndex((img) => img.imageId === selectedImageId.value));
const hasPrev = computed(() => currentIndex.value > 0);
const hasNext = computed(() => currentIndex.value >= 0 && currentIndex.value < images.value.length - 1);

const renderRects = computed(() => {
  if (!imageWidth.value || !imageHeight.value) return [];
  return boxes.value.map((box, idx) => {
    if (isPolygonBox(box)) {
      return null;
    }
    const px = boxToPixelRect(box);
    return {
      idx,
      label: `${box.label} ${(box.confidence ?? 0).toFixed(2)}`,
      style: {
        left: `${px.left}px`,
        top: `${px.top}px`,
        width: `${px.width}px`,
        height: `${px.height}px`
      }
    };
  }).filter(Boolean);
});

const renderPolygons = computed(() => {
  if (!imageWidth.value || !imageHeight.value) return [];
  return boxes.value.map((box, idx) => {
    if (!isPolygonBox(box)) {
      return null;
    }
    const points = box.points
      .map((point) => `${Math.round(clamp01(point.x) * imageWidth.value)},${Math.round(clamp01(point.y) * imageHeight.value)}`)
      .join(" ");
    return { idx, points };
  }).filter(Boolean);
});

const draftPolygonPointsPx = computed(() => {
  if (!imageWidth.value || !imageHeight.value) return [];
  return draftPolygonPoints.value.map((point) => ({
    x: Math.round(clamp01(point.x) * imageWidth.value),
    y: Math.round(clamp01(point.y) * imageHeight.value)
  }));
});

const draftPolylinePoints = computed(() => {
  const points = draftPolygonPointsPx.value;
  if (points.length === 0) return "";
  return points.map((point) => `${point.x},${point.y}`).join(" ");
});

const draftRect = computed(() => {
  if (!drawing.value || !drawStart.value || !drawCurrent.value) return null;
  const left = Math.min(drawStart.value.x, drawCurrent.value.x);
  const top = Math.min(drawStart.value.y, drawCurrent.value.y);
  const width = Math.abs(drawCurrent.value.x - drawStart.value.x);
  const height = Math.abs(drawCurrent.value.y - drawStart.value.y);
  return {
    style: {
      left: `${left}px`,
      top: `${top}px`,
      width: `${width}px`,
      height: `${height}px`
    }
  };
});

function isImageFile(file) {
  if (!file) return false;
  if (typeof file.type === "string" && file.type.startsWith("image/")) {
    return true;
  }
  const name = String(file.name || "").toLowerCase();
  return [".jpg", ".jpeg", ".png", ".bmp", ".webp"].some((ext) => name.endsWith(ext));
}

function onFolderChange(event) {
  const files = Array.from(event.target.files || []).filter(isImageFile);
  folderFiles.value = files;
  if (files.length === 0) {
    message.value = "未识别到可上传图片，请确认文件夹内包含 jpg/png/webp/bmp 图片。";
  } else {
    message.value = `已选择 ${files.length} 张图片，点击“上传文件夹”开始上传。`;
  }
}

async function loadImages() {
  if (!currentBatchId.value) {
    images.value = [];
    return;
  }
  try {
    const list = await listImages(currentBatchId.value);
    const normalized = Array.isArray(list) ? list : [];
    const hasBatchInfo = normalized.some((item) => typeof item?.batchId === "string" && item.batchId.length > 0);

    // Backward compatible: old backend records may not have batchId.
    images.value = hasBatchInfo
      ? normalized.filter((item) => item.batchId === currentBatchId.value)
      : normalized;

    if (!hasBatchInfo && normalized.length > 0) {
      message.value = "检测到后端返回的图片缺少 batchId，已兼容显示全部列表。建议重启后端加载最新接口。";
    }
  } catch (error) {
    message.value = `加载图片列表失败: ${error.message}`;
  }
}

async function handleFolderUpload() {
  if (folderFiles.value.length === 0) {
    message.value = "请先选择包含图片的文件夹。";
    return;
  }
  uploading.value = true;
  uploadPercent.value = 0;

  let successCount = 0;
  let failedCount = 0;

  try {
    const uploadBatchId = `batch-${Date.now()}`;
    currentBatchId.value = uploadBatchId;
    await clearImages();

    const total = folderFiles.value.length;
    let finished = 0;
    const queue = [...folderFiles.value];
    const workerCount = Math.min(MAX_UPLOAD_CONCURRENCY, total);

    const worker = async () => {
      while (queue.length) {
        const file = queue.shift();
        if (!file) return;
        const relativePath = file.webkitRelativePath || file.name;
        try {
          await uploadImage(file, uploadBatchId, relativePath);
          successCount += 1;
        } catch {
          failedCount += 1;
        } finally {
          finished += 1;
          uploadProgress.value = `${finished}/${total}`;
          uploadPercent.value = Math.round((finished / total) * 100);
        }
      }
    };

    await Promise.all(Array.from({ length: workerCount }, worker));
    message.value = `文件夹上传完成: 成功 ${successCount}/${folderFiles.value.length}，失败 ${failedCount}`;
    folderFiles.value = [];
    await loadImages();
    if (images.value.length > 0) {
      selectedImageId.value = images.value[0].imageId;
      await loadImageDetail();
    }
  } catch (error) {
    message.value = `上传失败: ${error.message}`;
  } finally {
    uploading.value = false;
  }
}

async function loadImageDetail() {
  if (!selectedImageId.value) {
    boxes.value = [];
    selectedBoxIndex.value = -1;
    return;
  }

  try {
    const detail = await getImage(selectedImageId.value);
    boxes.value = Array.isArray(detail.boxes) ? detail.boxes : [];
    selectedBoxIndex.value = -1;
  } catch (error) {
    message.value = `加载图片详情失败: ${error.message}`;
  }
}

async function clearAllImageList() {
  const confirmed = window.confirm("确认删除所有图片? 此操作会清空当前图片列表与标注。\n该操作不可撤销。");
  if (!confirmed) return;

  uploading.value = true;
  try {
    await clearImages();
    images.value = [];
    selectedImageId.value = "";
    boxes.value = [];
    folderFiles.value = [];
    currentBatchId.value = "";
    uploadProgress.value = "0/0";
    uploadPercent.value = 0;
    message.value = "已删除所有图片";
  } catch (error) {
    message.value = `删除失败: ${error.message}`;
  } finally {
    uploading.value = false;
  }
}

async function goPrev() {
  if (!hasPrev.value) return;
  selectedImageId.value = images.value[currentIndex.value - 1].imageId;
  await loadImageDetail();
}

async function goNext() {
  if (!hasNext.value) return;
  selectedImageId.value = images.value[currentIndex.value + 1].imageId;
  await loadImageDetail();
}

function onPreviewLoaded(event) {
  imageWidth.value = event.target.clientWidth;
  imageHeight.value = event.target.clientHeight;
}

function startDraw(event) {
  if (annotationMode.value !== "rect") {
    return;
  }
  if (!selectedImageId.value || !imageWidth.value || !imageHeight.value) return;
  if (event.target !== event.currentTarget) return;
  drawing.value = true;
  interactionMode.value = "draw";
  selectedBoxIndex.value = -1;
  const point = getOverlayPoint(event);
  drawStart.value = point;
  drawCurrent.value = point;
}

function moveDraw(event) {
  if (annotationMode.value !== "rect") {
    return;
  }
  if (drawing.value) {
    drawCurrent.value = getOverlayPoint(event);
    return;
  }
  if (interactionMode.value === "move") {
    moveSelectedBox(event);
    return;
  }
  if (interactionMode.value === "resize") {
    resizeSelectedBox(event);
  }
}

function finishDraw() {
  if (annotationMode.value !== "rect") {
    return;
  }
  if (drawing.value && drawStart.value && drawCurrent.value) {
    const left = Math.min(drawStart.value.x, drawCurrent.value.x);
    const right = Math.max(drawStart.value.x, drawCurrent.value.x);
    const top = Math.min(drawStart.value.y, drawCurrent.value.y);
    const bottom = Math.max(drawStart.value.y, drawCurrent.value.y);

    const widthPx = right - left;
    const heightPx = bottom - top;
    if (widthPx >= 6 && heightPx >= 6) {
      boxes.value.push({
        shapeType: "rect",
        label: newBoxLabel.value || "object",
        x: clamp01((left + widthPx / 2) / imageWidth.value),
        y: clamp01((top + heightPx / 2) / imageHeight.value),
        width: clamp01(widthPx / imageWidth.value),
        height: clamp01(heightPx / imageHeight.value),
        confidence: clamp01(Number(newBoxConfidence.value ?? 1))
      });
      selectedBoxIndex.value = boxes.value.length - 1;
    }
  }

  drawing.value = false;
  interactionMode.value = "idle";
  activeResizeHandle.value = "";
  dragStart.value = null;
  dragSourceRect.value = null;
  drawStart.value = null;
  drawCurrent.value = null;
}

function cancelDraw() {
  if (annotationMode.value !== "rect") {
    return;
  }
  drawing.value = false;
  interactionMode.value = "idle";
  activeResizeHandle.value = "";
  dragStart.value = null;
  dragSourceRect.value = null;
  drawStart.value = null;
  drawCurrent.value = null;
}

function onOverlayClick(event) {
  if (annotationMode.value !== "polygon") {
    return;
  }
  if (!selectedImageId.value || !imageWidth.value || !imageHeight.value) return;
  if (event.target !== event.currentTarget && !String(event.target?.className || "").includes("annotate-svg")) {
    return;
  }
  const point = getOverlayPoint(event);
  draftPolygonPoints.value.push({
    x: clamp01(point.x / imageWidth.value),
    y: clamp01(point.y / imageHeight.value)
  });
}

function finishPolygon() {
  if (annotationMode.value !== "polygon" || draftPolygonPoints.value.length < 3) {
    return;
  }
  const rect = pointsToRect(draftPolygonPoints.value);
  boxes.value.push({
    shapeType: "polygon",
    label: newBoxLabel.value || "object",
    x: rect.x,
    y: rect.y,
    width: rect.width,
    height: rect.height,
    confidence: clamp01(Number(newBoxConfidence.value ?? 1)),
    points: draftPolygonPoints.value.map((point) => ({ x: clamp01(point.x), y: clamp01(point.y) }))
  });
  selectedBoxIndex.value = boxes.value.length - 1;
  draftPolygonPoints.value = [];
}

function cancelPolygon() {
  draftPolygonPoints.value = [];
}

function setAnnotationMode(mode) {
  annotationMode.value = mode;
  drawing.value = false;
  drawStart.value = null;
  drawCurrent.value = null;
  interactionMode.value = "idle";
  dragStart.value = null;
  dragSourceRect.value = null;
  activeResizeHandle.value = "";
  if (mode !== "polygon") {
    draftPolygonPoints.value = [];
  }
}

function startMoveBox(index, event) {
  if (!boxes.value[index] || isPolygonBox(boxes.value[index]) || annotationMode.value !== "rect") return;
  selectedBoxIndex.value = index;
  interactionMode.value = "move";
  dragStart.value = getOverlayPoint(event);
  dragSourceRect.value = boxToPixelRect(boxes.value[index]);
}

function startResizeBox(index, handle, event) {
  if (!boxes.value[index] || isPolygonBox(boxes.value[index]) || annotationMode.value !== "rect") return;
  selectedBoxIndex.value = index;
  interactionMode.value = "resize";
  activeResizeHandle.value = handle;
  dragStart.value = getOverlayPoint(event);
  dragSourceRect.value = boxToPixelRect(boxes.value[index]);
}

function moveSelectedBox(event) {
  const idx = selectedBoxIndex.value;
  if (idx < 0 || !dragStart.value || !dragSourceRect.value) return;

  const point = getOverlayPoint(event);
  const dx = point.x - dragStart.value.x;
  const dy = point.y - dragStart.value.y;
  const src = dragSourceRect.value;
  const left = clamp(src.left + dx, 0, imageWidth.value - src.width);
  const top = clamp(src.top + dy, 0, imageHeight.value - src.height);

  boxes.value[idx] = pixelRectToBox(left, top, src.width, src.height, boxes.value[idx]);
}

function resizeSelectedBox(event) {
  const idx = selectedBoxIndex.value;
  if (idx < 0 || !dragStart.value || !dragSourceRect.value || !activeResizeHandle.value) return;

  const minSize = 6;
  const point = getOverlayPoint(event);
  const dx = point.x - dragStart.value.x;
  const dy = point.y - dragStart.value.y;
  const src = dragSourceRect.value;
  let left = src.left;
  let right = src.left + src.width;
  let top = src.top;
  let bottom = src.top + src.height;

  if (activeResizeHandle.value.includes("w")) {
    left = clamp(src.left + dx, 0, right - minSize);
  }
  if (activeResizeHandle.value.includes("e")) {
    right = clamp(src.left + src.width + dx, left + minSize, imageWidth.value);
  }
  if (activeResizeHandle.value.includes("n")) {
    top = clamp(src.top + dy, 0, bottom - minSize);
  }
  if (activeResizeHandle.value.includes("s")) {
    bottom = clamp(src.top + src.height + dy, top + minSize, imageHeight.value);
  }

  boxes.value[idx] = pixelRectToBox(left, top, right - left, bottom - top, boxes.value[idx]);
}

function selectBox(index) {
  selectedBoxIndex.value = index;
}

function getOverlayPoint(event) {
  const base = overlayRef.value || event.currentTarget;
  const rect = base.getBoundingClientRect();
  const x = Math.max(0, Math.min(rect.width, event.clientX - rect.left));
  const y = Math.max(0, Math.min(rect.height, event.clientY - rect.top));
  return { x, y };
}

function removeSelectedBox() {
  const idx = selectedBoxIndex.value;
  if (idx < 0 || idx >= boxes.value.length) return;
  boxes.value.splice(idx, 1);
  selectedBoxIndex.value = idx >= boxes.value.length ? boxes.value.length - 1 : idx;
}

function removeLastBox() {
  if (draftPolygonPoints.value.length > 0) {
    draftPolygonPoints.value.pop();
    return;
  }
  if (boxes.value.length === 0) return;
  boxes.value.pop();
  if (selectedBoxIndex.value >= boxes.value.length) {
    selectedBoxIndex.value = boxes.value.length - 1;
  }
}

function clearBoxes() {
  boxes.value = [];
  draftPolygonPoints.value = [];
  selectedBoxIndex.value = -1;
}

async function saveBoxes() {
  if (!selectedImageId.value) return;
  try {
    await saveImageBoxes(selectedImageId.value, boxes.value);
    message.value = "标注保存成功";
    await loadImages();
  } catch (error) {
    message.value = `保存失败: ${error.message}`;
  }
}

function boxToPixelRect(box) {
  const width = clamp01(box.width) * imageWidth.value;
  const height = clamp01(box.height) * imageHeight.value;
  const left = (clamp01(box.x) - clamp01(box.width) / 2) * imageWidth.value;
  const top = (clamp01(box.y) - clamp01(box.height) / 2) * imageHeight.value;
  return { left, top, width, height };
}

function pointsToRect(points) {
  const xs = points.map((point) => clamp01(point.x));
  const ys = points.map((point) => clamp01(point.y));
  const minX = Math.min(...xs);
  const maxX = Math.max(...xs);
  const minY = Math.min(...ys);
  const maxY = Math.max(...ys);
  const width = clamp01(maxX - minX);
  const height = clamp01(maxY - minY);
  return {
    x: clamp01(minX + width / 2),
    y: clamp01(minY + height / 2),
    width,
    height
  };
}

function isPolygonBox(box) {
  return box?.shapeType === "polygon" && Array.isArray(box?.points) && box.points.length >= 3;
}

function pixelRectToBox(left, top, width, height, source = {}) {
  return {
    ...source,
    shapeType: source.shapeType || "rect",
    label: source.label || newBoxLabel.value || "object",
    confidence: clamp01(Number(source.confidence ?? newBoxConfidence.value ?? 1)),
    x: clamp01((left + width / 2) / imageWidth.value),
    y: clamp01((top + height / 2) / imageHeight.value),
    width: clamp01(width / imageWidth.value),
    height: clamp01(height / imageHeight.value)
  };
}

function resetViewState() {
  folderFiles.value = [];
  uploadProgress.value = "0/0";
  uploadPercent.value = 0;
  selectedImageId.value = "";
  boxes.value = [];
  selectedBoxIndex.value = -1;
  draftPolygonPoints.value = [];
  annotationMode.value = "rect";
  message.value = "";
}

function onKeydown(event) {
  const isDeleteKey = event.key === "Delete" || event.key === "Backspace";
  if (!isDeleteKey || selectedBoxIndex.value < 0) return;

  const tagName = document.activeElement?.tagName;
  if (tagName === "INPUT" || tagName === "TEXTAREA") return;

  event.preventDefault();
  removeSelectedBox();
}

function clamp01(value) {
  if (!Number.isFinite(value)) return 0;
  return Math.max(0, Math.min(1, value));
}

function clamp(value, min, max) {
  if (!Number.isFinite(value)) return min;
  return Math.max(min, Math.min(max, value));
}

onMounted(() => {
  window.addEventListener("keydown", onKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", onKeydown);
});
</script>

<style scoped>
.progress-wrap {
  width: 100%;
  height: 10px;
  border-radius: 999px;
  background: #e5e7eb;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: #2563eb;
  transition: width 0.2s ease;
}

.annotate-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.poly-shape {
  fill: rgba(37, 99, 235, 0.15);
  stroke: #2563eb;
  stroke-width: 2;
  pointer-events: all;
}

.poly-shape.selected {
  fill: rgba(239, 68, 68, 0.15);
  stroke: #ef4444;
}

.poly-draft {
  fill: none;
  stroke: #16a34a;
  stroke-width: 2;
  stroke-dasharray: 6 4;
}

.poly-point {
  fill: #16a34a;
}

.secondary.active {
  border-color: #2563eb;
  background: #eff6ff;
}
</style>
