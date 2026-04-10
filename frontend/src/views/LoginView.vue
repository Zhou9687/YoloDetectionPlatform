<template>
  <div class="login-container">
    <div class="login-left">
      <div class="logo-section">
        <div class="logo">
          <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="100" height="100" rx="20" fill="url(#logo-gradient)" />
            <path d="M50 25L75 50L50 75L25 50L50 25Z" stroke="white" stroke-width="3" />
            <circle cx="50" cy="50" r="15" stroke="white" stroke-width="2" />
            <path d="M50 35V65M35 50H65" stroke="#00E5FF" stroke-width="2" />
            <defs>
              <linearGradient id="logo-gradient" x1="0" y1="0" x2="100" y2="100">
                <stop offset="0%" stop-color="#0F172A" />
                <stop offset="100%" stop-color="#1E3A8A" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <h1 class="brand-name">YOLO<span class="brand-highlight">Detection</span></h1>
        <p class="brand-slogan">智能目标检测平台</p>
      </div>
      
      <div class="features">
        <div class="feature-item">
          <div class="feature-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="#00E5FF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 17L12 22L22 17" stroke="#00E5FF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 12L12 17L22 12" stroke="#00E5FF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="feature-text">
            <h3>实时检测</h3>
            <p>毫秒级响应速度</p>
          </div>
        </div>
        
        <div class="feature-item">
          <div class="feature-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" stroke="#00E5FF" stroke-width="2"/>
              <path d="M12 16C14.2091 16 16 14.2091 16 12C16 9.79086 14.2091 8 12 8C9.79086 8 8 9.79086 8 12C8 14.2091 9.79086 16 12 16Z" stroke="#00E5FF" stroke-width="2"/>
            </svg>
          </div>
          <div class="feature-text">
            <h3>高精度</h3>
            <p>YOLOv8最新算法</p>
          </div>
        </div>
        
        <div class="feature-item">
          <div class="feature-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 15C13.6569 15 15 13.6569 15 12C15 10.3431 13.6569 9 12 9C10.3431 9 9 10.3431 9 12C9 13.6569 10.3431 15 12 15Z" stroke="#00E5FF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M19.4 15C20.9 15 22 13.9 22 12.4C22 10.9 20.9 9.8 19.4 9.8C18.9 9.8 18.5 10 18.2 10.3" stroke="#00E5FF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M4.2 19.4C4.5 19.7 4.9 19.9 5.4 19.9C6.9 19.9 8 18.8 8 17.3C8 16.8 7.8 16.4 7.5 16.1" stroke="#00E5FF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M9.8 4.2C9.8 4.7 10 5.1 10.3 5.4C10.6 5.7 11 5.9 11.5 5.9C13 5.9 14.1 7 14.1 8.5C14.1 8.9 13.9 9.3 13.6 9.6" stroke="#00E5FF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="feature-text">
            <h3>多目标</h3>
            <p>同时识别多种物体</p>
          </div>
        </div>
      </div>
    </div>
    
    <div class="login-right">
      <div class="login-box">
        <div class="login-header">
          <h2 class="login-title">欢迎登录</h2>
          <p class="login-subtitle">请输入您的账号信息</p>
        </div>
        
        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label class="form-label">账号</label>
            <div class="input-wrapper">
              <svg class="input-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="#94A3B8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <circle cx="12" cy="7" r="4" stroke="#94A3B8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <input
                type="text"
                v-model.trim="form.account"
                class="form-input"
                placeholder="请输入账号"
                autocomplete="username"
              />
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label">密码</label>
            <div class="input-wrapper">
              <svg class="input-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" stroke="#94A3B8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M7 11V7C7 5.6 7.9 4.5 9 4.5C10.1 4.5 11 5.6 11 7V11" stroke="#94A3B8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M17 11V7C17 5.6 16.1 4.5 15 4.5C13.9 4.5 13 5.6 13 7V11" stroke="#94A3B8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <input
                type="password"
                v-model="form.password"
                class="form-input"
                placeholder="请输入密码"
                autocomplete="current-password"
              />
            </div>
          </div>
          
          <div class="form-options">
            <label class="remember-me">
              <input type="checkbox" v-model="form.remember" />
              <span>记住我</span>
            </label>
          </div>
          
          <p v-if="message" class="error-message">{{ message }}</p>
          
          <button type="submit" class="login-btn" :class="{ 'loading': loading }">
            <span v-if="!loading">登录系统</span>
            <span v-else>登录中...</span>
          </button>
          
          <div class="login-footer">
            <p>还没有账号？<router-link to="/register" class="register-link">立即注册</router-link></p>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { loginUser } from "../api/authApi";
import { setAuth } from "../utils/auth";

const router = useRouter();
const loading = ref(false);
const message = ref("");

const form = reactive({
  account: "",
  password: "",
  remember: false
});

async function handleLogin() {
  if (!form.account || !form.password) {
    message.value = "请输入账号和密码";
    return;
  }
  loading.value = true;
  message.value = "";
  try {
    const result = await loginUser(form.account, form.password);
    setAuth(result.token, result.username);
    await router.replace("/annotate");
  } catch (error) {
    message.value = `登录失败: ${error.message}`;
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(135deg, #0F172A 0%, #1E3A8A 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: 
    radial-gradient(circle at 15% 50%, rgba(0, 229, 255, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 85% 30%, rgba(79, 70, 229, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 50% 80%, rgba(0, 184, 212, 0.2) 0%, transparent 50%);
  animation: gradientShift 15s ease-in-out infinite alternate;
  z-index: 0;
}

@keyframes gradientShift {
  0% {
    background: 
      radial-gradient(circle at 15% 50%, rgba(0, 229, 255, 0.3) 0%, transparent 50%),
      radial-gradient(circle at 85% 30%, rgba(79, 70, 229, 0.3) 0%, transparent 50%),
      radial-gradient(circle at 50% 80%, rgba(0, 184, 212, 0.2) 0%, transparent 50%);
  }
  100% {
    background: 
      radial-gradient(circle at 25% 40%, rgba(0, 229, 255, 0.4) 0%, transparent 50%),
      radial-gradient(circle at 75% 60%, rgba(79, 70, 229, 0.4) 0%, transparent 50%),
      radial-gradient(circle at 50% 70%, rgba(0, 184, 212, 0.3) 0%, transparent 50%);
  }
}

.login-container::after {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('data:image/svg+xml,<svg width="80" height="80" viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg"><g fill="none" fill-rule="evenodd"><g fill="%2300E5FF" fill-opacity="0.05"><circle cx="40" cy="40" r="3"/></g></g></svg>');
  pointer-events: none;
  z-index: 0;
}

.login-left {
  flex: 1;
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: white;
  position: relative;
  overflow: hidden;
  z-index: 1;
}

.login-left::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at top right, rgba(0, 229, 255, 0.2) 0%, transparent 50%),
              radial-gradient(circle at bottom left, rgba(0, 229, 255, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

.logo-section {
  text-align: center;
  z-index: 1;
  margin-bottom: 80px;
}

.logo {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  animation: float 6s ease-in-out infinite;
}

.brand-name {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 12px;
  letter-spacing: 2px;
}

.brand-highlight {
  color: #00E5FF;
  text-shadow: 0 0 20px rgba(0, 229, 255, 0.5);
}

.brand-slogan {
  font-size: 16px;
  opacity: 0.8;
  letter-spacing: 1px;
}

.features {
  display: flex;
  gap: 30px;
  justify-content: center;
  z-index: 1;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 30px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(0, 229, 255, 0.2);
}

.feature-icon {
  color: #00E5FF;
}

.feature-text h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.feature-text p {
  font-size: 14px;
  opacity: 0.7;
  margin: 0;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  z-index: 1;
}

.login-box {
  width: 100%;
  max-width: 420px;
  padding: 50px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  animation: slideIn 0.6s ease-out;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-title {
  font-size: 32px;
  font-weight: 700;
  color: #0F172A;
  margin: 0 0 8px;
  text-align: center;
  background: linear-gradient(135deg, #0F172A 0%, #1E3A8A 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-subtitle {
  font-size: 15px;
  color: #64748B;
  text-align: center;
  margin: 0;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  transition: color 0.3s ease;
}

.form-input {
  width: 100%;
  padding: 14px 16px 14px 48px;
  border: 2px solid #E2E8F0;
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.3s ease;
  background: #F8FAFC;
}

.form-input::placeholder {
  color: #94A3B8;
}

.form-input:focus {
  border-color: #00E5FF;
  background: white;
  box-shadow: 0 0 0 4px rgba(0, 229, 255, 0.1);
  outline: none;
}

.form-input:focus + .input-icon,
.form-input:focus ~ .input-icon {
  color: #00E5FF;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #475569;
  user-select: none;
}

.remember-me input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: #00E5FF;
  cursor: pointer;
}

.error-message {
  color: #EF4444;
  font-size: 14px;
  margin-bottom: 16px;
  text-align: center;
}

.login-btn {
  width: 100%;
  height: 52px;
  padding: 0;
  background: linear-gradient(135deg, #00E5FF 0%, #00B8D4 100%);
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(0, 229, 255, 0.3);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 229, 255, 0.4);
}

.login-btn:hover::before {
  left: 100%;
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn.loading {
  opacity: 0.8;
  cursor: not-allowed;
}

.login-footer {
  text-align: center;
  margin-top: 30px;
  padding-top: 24px;
  border-top: 1px solid #E2E8F0;
}

.login-footer p {
  font-size: 14px;
  color: #64748B;
  margin: 0;
}

.register-link {
  color: #00E5FF;
  text-decoration: none;
  font-weight: 600;
  margin-left: 6px;
}

.register-link:hover {
  text-decoration: underline;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1200px) {
  .features {
    gap: 20px;
  }
  
  .feature-item {
    padding: 15px 20px;
  }
}

@media (max-width: 968px) {
  .login-left {
    display: none;
  }
  
  .login-right {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .login-right {
    padding: 20px;
  }
  
  .login-box {
    padding: 30px 20px;
  }
  
  .login-title {
    font-size: 28px;
  }
}
</style>
