<template>
  <div v-if="showShell" class="app-shell">
    <header class="app-topbar">
      <div class="brand-block">
        <span class="brand-mark"></span>
        <span class="brand-text">Yolo检测平台</span>
      </div>
      <div class="top-actions" ref="menuRootRef">
        <button class="avatar-btn" type="button" @click="toggleMenu">{{ avatarInitial }}</button>
        <div v-if="menuOpen" class="avatar-menu">
          <button type="button" class="menu-item" @click="openRenameDialog">修改账户名称</button>
          <button type="button" class="menu-item" @click="openPasswordDialog">修改密码</button>
          <button type="button" class="menu-item danger" @click="logout">退出登录</button>
        </div>
      </div>
    </header>

    <div class="app-body">
      <aside class="left-sidebar">
        <div class="sidebar-title">功能导航</div>
        <RouterLink class="side-link" to="/annotate">图片标注</RouterLink>
        <RouterLink class="side-link" to="/dataset">数据集构建</RouterLink>
        <RouterLink class="side-link" to="/training">训练任务</RouterLink>
        <RouterLink class="side-link" to="/predict">检测预测</RouterLink>
      </aside>

      <main class="main-panel">
        <div class="module-tabs">
          <RouterLink class="tab-item" to="/annotate">图片标注</RouterLink>
          <RouterLink class="tab-item" to="/dataset">数据集构建</RouterLink>
          <RouterLink class="tab-item" to="/training">训练任务</RouterLink>
          <RouterLink class="tab-item" to="/predict">检测预测</RouterLink>
        </div>

        <div class="content-wrap">
          <RouterView v-slot="{ Component }">
            <KeepAlive>
              <component :is="Component" />
            </KeepAlive>
          </RouterView>
        </div>
      </main>
    </div>
  </div>

  <div v-if="showShell && showRenameDialog" class="dialog-mask" @click.self="closeDialogs">
    <section class="dialog-card">
      <h4>修改账户名称</h4>
      <label>当前账号</label>
      <input ref="currentUsernameRef" :value="username" readonly @click="handleCurrentUsernameClick" />
      <label>新账号名称</label>
      <input ref="newUsernameInputRef" v-model.trim="newUsername" placeholder="请输入新账号名称" />
      <p v-if="dialogMessage" class="muted">{{ dialogMessage }}</p>
      <div class="dialog-actions">
        <button type="button" @click="submitRename" :disabled="dialogLoading">{{ dialogLoading ? "提交中..." : "确认修改" }}</button>
        <button type="button" class="secondary" @click="closeDialogs" :disabled="dialogLoading">取消</button>
      </div>
    </section>
  </div>

  <div v-if="showShell && showPasswordDialog" class="dialog-mask" @click.self="closeDialogs">
    <section class="dialog-card">
      <h4>修改密码</h4>
      <label>原密码</label>
      <input v-model="oldPassword" type="password" placeholder="请输入原密码" />
      <label>新密码</label>
      <input v-model="newPassword" type="password" placeholder="请输入新密码" />
      <label>确认新密码</label>
      <input v-model="confirmPassword" type="password" placeholder="请再次输入新密码" />
      <p v-if="dialogMessage" class="muted">{{ dialogMessage }}</p>
      <div class="dialog-actions">
        <button type="button" @click="submitPassword" :disabled="dialogLoading">{{ dialogLoading ? "提交中..." : "确认修改" }}</button>
        <button type="button" class="secondary" @click="closeDialogs" :disabled="dialogLoading">取消</button>
      </div>
    </section>
  </div>

  <RouterView v-if="!showShell" />
</template>

<script setup>
import { KeepAlive, computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterLink, RouterView, useRoute, useRouter } from "vue-router";
import { updatePassword, updateUsername } from "./api/authApi";
import { clearAuth, getDisplayInitial, getUsername, setUsername } from "./utils/auth";

const route = useRoute();
const router = useRouter();
const showShell = computed(() => !route.meta?.hideShell);

const menuOpen = ref(false);
const menuRootRef = ref(null);
const username = ref("");

const showRenameDialog = ref(false);
const showPasswordDialog = ref(false);
const newUsername = ref("");
const oldPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const dialogMessage = ref("");
const dialogLoading = ref(false);
const currentUsernameRef = ref(null);
const newUsernameInputRef = ref(null);

const avatarInitial = computed(() => getDisplayInitial(username.value));

function toggleMenu() {
  menuOpen.value = !menuOpen.value;
}

function handleDocumentClick(event) {
  if (!menuOpen.value) return;
  const root = menuRootRef.value;
  if (!root) return;
  if (!root.contains(event.target)) {
    menuOpen.value = false;
  }
}

function syncUsernameFromStorage() {
  const stored = String(getUsername() || "").trim();
  username.value = stored;
}

function openRenameDialog() {
  syncUsernameFromStorage();
  menuOpen.value = false;
  dialogMessage.value = "";
  newUsername.value = username.value;
  showRenameDialog.value = true;
  nextTick(() => {
    if (newUsernameInputRef.value && typeof newUsernameInputRef.value.focus === "function") {
      newUsernameInputRef.value.focus();
      if (typeof newUsernameInputRef.value.select === "function") {
        newUsernameInputRef.value.select();
      }
    }
  });
}

function openPasswordDialog() {
  menuOpen.value = false;
  dialogMessage.value = "";
  oldPassword.value = "";
  newPassword.value = "";
  confirmPassword.value = "";
  showPasswordDialog.value = true;
}

function closeDialogs() {
  showRenameDialog.value = false;
  showPasswordDialog.value = false;
  dialogMessage.value = "";
  dialogLoading.value = false;
}

async function submitRename() {
  syncUsernameFromStorage();
  if (!username.value) {
    dialogMessage.value = "登录状态已失效，请重新登录后再修改账号名称";
    return;
  }
  if (!newUsername.value.trim()) {
    dialogMessage.value = "请输入新账号名称";
    return;
  }
  dialogLoading.value = true;
  dialogMessage.value = "";
  try {
    const response = await updateUsername(username.value, newUsername.value.trim());
    const updated = String(response?.username || newUsername.value.trim());
    username.value = updated;
    setUsername(updated);
    dialogMessage.value = "账号名称修改成功";
    setTimeout(closeDialogs, 500);
  } catch (error) {
    dialogMessage.value = `修改失败: ${error.message}`;
  } finally {
    dialogLoading.value = false;
  }
}

async function submitPassword() {
  syncUsernameFromStorage();
  if (!username.value) {
    dialogMessage.value = "登录状态已失效，请重新登录后再修改密码";
    return;
  }
  if (!oldPassword.value || !newPassword.value || !confirmPassword.value) {
    dialogMessage.value = "请填写完整密码信息";
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    dialogMessage.value = "两次输入的新密码不一致";
    return;
  }

  dialogLoading.value = true;
  dialogMessage.value = "";
  try {
    await updatePassword(username.value, oldPassword.value, newPassword.value);
    dialogMessage.value = "密码修改成功，请重新登录";
    setTimeout(() => {
      logout();
    }, 700);
  } catch (error) {
    dialogMessage.value = `修改失败: ${error.message}`;
  } finally {
    dialogLoading.value = false;
  }
}

function logout() {
  clearAuth();
  username.value = "";
  closeDialogs();
  menuOpen.value = false;
  router.replace("/login");
}

function handleCurrentUsernameClick() {
  if (currentUsernameRef.value && typeof currentUsernameRef.value.select === "function") {
    currentUsernameRef.value.select();
  }
  dialogMessage.value = "当前账号仅用于展示，请在下方输入新账号名称。";
}

onMounted(() => {
  syncUsernameFromStorage();
  document.addEventListener("click", handleDocumentClick);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleDocumentClick);
});

watch(
  () => route.fullPath,
  () => {
    if (showShell.value) {
      syncUsernameFromStorage();
    }
  },
  { immediate: true }
);
</script>
