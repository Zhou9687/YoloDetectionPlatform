<template>
  <div class="auth-page">
    <section class="auth-card">
      <h2 class="auth-title">登录 Yolo检测平台</h2>
      <label>账号</label>
      <input v-model.trim="username" placeholder="请输入账号" />
      <label>密码</label>
      <input v-model="password" type="password" placeholder="请输入密码" />

      <div class="auth-actions">
        <button type="button" @click="handleLogin" :disabled="loading">
          {{ loading ? "登录中..." : "登录" }}
        </button>
      </div>
      <p v-if="message" class="muted">{{ message }}</p>
      <RouterLink class="auth-link" to="/register">没有账号？去注册</RouterLink>
    </section>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { loginUser } from "../api/authApi";
import { setAuth } from "../utils/auth";

const router = useRouter();
const username = ref("");
const password = ref("");
const loading = ref(false);
const message = ref("");

async function handleLogin() {
  if (!username.value || !password.value) {
    message.value = "请输入账号和密码";
    return;
  }
  loading.value = true;
  message.value = "";
  try {
    const result = await loginUser(username.value, password.value);
    setAuth(result.token, result.username);
    await router.replace("/annotate");
  } catch (error) {
    message.value = `登录失败: ${error.message}`;
  } finally {
    loading.value = false;
  }
}
</script>

