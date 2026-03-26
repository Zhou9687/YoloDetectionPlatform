<template>
  <div class="auth-page">
    <section class="auth-card">
      <h2 class="auth-title">注册账号</h2>
      <label>账号</label>
      <input v-model.trim="username" placeholder="请输入账号（3-32位）" />
      <label>密码</label>
      <input v-model="password" type="password" placeholder="请输入密码（至少6位）" />
      <label>确认密码</label>
      <input v-model="confirmPassword" type="password" placeholder="请再次输入密码" />

      <div class="auth-actions">
        <button type="button" @click="handleRegister" :disabled="loading">
          {{ loading ? "注册中..." : "注册" }}
        </button>
      </div>
      <p v-if="message" class="muted">{{ message }}</p>
      <RouterLink class="auth-link" to="/login">已有账号？去登录</RouterLink>
    </section>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { registerUser } from "../api/authApi";

const router = useRouter();
const username = ref("");
const password = ref("");
const confirmPassword = ref("");
const loading = ref(false);
const message = ref("");

async function handleRegister() {
  if (!username.value || !password.value || !confirmPassword.value) {
    message.value = "请完整填写注册信息";
    return;
  }
  if (password.value !== confirmPassword.value) {
    message.value = "两次输入的密码不一致";
    return;
  }

  loading.value = true;
  message.value = "";
  try {
    await registerUser(username.value, password.value);
    message.value = "注册成功，正在跳转登录页...";
    setTimeout(() => {
      router.replace("/login");
    }, 600);
  } catch (error) {
    message.value = `注册失败: ${error.message}`;
  } finally {
    loading.value = false;
  }
}
</script>

