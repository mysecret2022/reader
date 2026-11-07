<template>
  <div class="lx-login-container" :class="isDark ? 'theme-dark' : 'theme-light'">
    <!-- 动态极光渐变背景与微网格 -->
    <div class="ambient-canvas">
      <div class="aurora-blob aurora-1"></div>
      <div class="aurora-blob aurora-2"></div>
      <div class="aurora-blob aurora-3"></div>
    </div>
    <div class="bg-grid-mask"></div>

    <!-- 左上方主题与返回快捷按钮 -->
    <div class="top-nav-bar">
      <button
        type="button"
        class="nav-pill-btn"
        @click="toggleTheme"
        :title="isDark ? '切换亮色主题' : '切换暗色主题'"
      >
        <i :class="isDark ? 'el-icon-moon' : 'el-icon-sunny'"></i>
        <span>{{ isDark ? "暗色模式" : "亮色模式" }}</span>
      </button>

      <button
        type="button"
        class="nav-pill-btn"
        v-if="canCancel"
        @click="cancel"
        title="返回书架"
      >
        <i class="el-icon-close"></i>
        <span>返回</span>
      </button>
    </div>

    <!-- 登录主卡片 -->
    <div class="glass-card" :class="{ 'shake-active': isErrorShake }">
      <!-- 头部：Logo 与 标题 -->
      <div class="card-header">
        <div class="logo-box">
          <i class="el-icon-reading"></i>
        </div>
        <h1 class="card-title">Reader 阅 读</h1>
        <p class="mode-desc">
          {{ isLogin ? "登录您的专属阅读空间，同步书架与书源" : "创建新账号开启纯净阅读体验" }}
        </p>
      </div>

      <!-- 模式切换选择器 (Segmented Control) -->
      <div class="segmented-control">
        <div
          class="tab-pill"
          :style="{ transform: isLogin ? 'translateX(0)' : 'translateX(100%)' }"
        ></div>
        <button
          type="button"
          class="tab-btn"
          :class="{ active: isLogin }"
          @click="switchMode(true)"
        >
          <i class="el-icon-user"></i>
          <span>账号登录</span>
        </button>
        <button
          type="button"
          class="tab-btn"
          :class="{ active: !isLogin }"
          @click="switchMode(false)"
        >
          <i class="el-icon-edit-outline"></i>
          <span>新用户注册</span>
        </button>
      </div>

      <!-- 表单区域 -->
      <form @submit.prevent="handleSubmit" class="login-form">
        <!-- 用户名 -->
        <div class="input-wrapper">
          <span class="input-icon"><i class="el-icon-user"></i></span>
          <input
            type="text"
            v-model="loginForm.username"
            placeholder="请输入用户名"
            class="modern-input"
            autocomplete="username"
            autocapitalize="none"
            spellcheck="false"
            required
          />
        </div>

        <!-- 密码 -->
        <div class="input-wrapper">
          <span class="input-icon"><i class="el-icon-lock"></i></span>
          <input
            :type="showPassword ? 'text' : 'password'"
            v-model="loginForm.password"
            placeholder="请输入密码"
            class="modern-input"
            autocomplete="current-password"
            required
          />
          <button
            type="button"
            class="input-action-btn"
            @click="showPassword = !showPassword"
            :title="showPassword ? '隐藏密码' : '显示密码'"
          >
            <i :class="showPassword ? 'el-icon-view' : 'el-icon-lock'"></i>
          </button>
        </div>

        <!-- 注册邀请码 (注册时显示) -->
        <div class="input-wrapper register-fade" v-if="!isLogin">
          <span class="input-icon"><i class="el-icon-key"></i></span>
          <input
            type="text"
            v-model="loginForm.code"
            placeholder="邀请码（未启用邀请码则留空）"
            class="modern-input"
            autocomplete="off"
          />
        </div>

        <!-- 记住登录 / 选项 -->
        <div class="form-options">
          <label class="checkbox-label">
            <input type="checkbox" v-model="remember" />
            <span class="checkbox-custom"></span>
            <span class="checkbox-text">记住登录状态</span>
          </label>
        </div>

        <!-- 错误提示横条 -->
        <div class="error-banner" v-if="errorMsg">
          <i class="el-icon-warning-outline"></i>
          <span>{{ errorMsg }}</span>
        </div>

        <!-- 提交按钮 -->
        <button
          type="submit"
          class="btn-gradient-submit"
          :disabled="loading"
        >
          <i class="el-icon-loading" v-if="loading"></i>
          <i class="el-icon-right" v-else></i>
          <span>{{ loading ? "正在提交..." : (isLogin ? "立即登录" : "注册账号") }}</span>
        </button>
      </form>

      <!-- 底部辅助信息 -->
      <div class="card-footer">
        <span>开源阅读服务 Web 端</span>
        <button
          type="button"
          class="footer-link-btn"
          @click="clearLocalCache"
          title="清除本地可能缓存的失效凭据"
        >
          <i class="el-icon-refresh-left"></i> 清理本地凭据
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import Axios from "../plugins/axios";

export default {
  name: "LoginView",
  props: {
    canCancel: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      isLogin: true,
      remember: true,
      showPassword: false,
      loading: false,
      errorMsg: "",
      isErrorShake: false,
      loginForm: {
        username: "",
        password: "",
        code: ""
      }
    };
  },
  computed: {
    isDark() {
      return this.$store.getters.isNight;
    }
  },
  methods: {
    toggleTheme() {
      const nextNight = !this.isDark;
      this.$store.commit("setIsNight", nextNight);
      this.$store.commit("setConfig", {
        key: "isNight",
        value: nextNight
      });
    },
    switchMode(isLogin) {
      this.isLogin = isLogin;
      this.errorMsg = "";
    },
    triggerShake(msg) {
      this.errorMsg = msg || "登录失败，请检查账号密码";
      this.isErrorShake = true;
      setTimeout(() => {
        this.isErrorShake = false;
      }, 500);
    },
    cancel() {
      this.$emit("cancel");
    },
    async handleSubmit() {
      if (!this.loginForm.username || !this.loginForm.password) {
        this.triggerShake("请完整填写用户名和密码");
        return;
      }
      this.loading = true;
      this.errorMsg = "";
      try {
        const res = await Axios.post(
          "/login",
          {
            ...this.loginForm,
            isLogin: this.isLogin
          },
          { alert: false }
        );
        if (res && res.data && res.data.isSuccess) {
          this.$message.success(this.isLogin ? "登录成功" : "注册成功");
          this.$emit("success", {
            data: res.data.data,
            remember: this.remember
          });
        } else {
          const err =
            (res && res.data && res.data.errorMsg) || "操作失败，请重试";
          this.triggerShake(err);
        }
      } catch (err) {
        const msg =
          (err && (err.errorMsg || err.message)) ||
          "网络连接异常，请检查服务状态";
        this.triggerShake(msg);
      } finally {
        this.loading = false;
      }
    },
    clearLocalCache() {
      this.$store.commit("setToken", "");
      this.loginForm.username = "";
      this.loginForm.password = "";
      this.loginForm.code = "";
      this.$message.info("本地凭据已重置");
    }
  }
};
</script>

<style scoped>
.lx-login-container {
  position: fixed;
  inset: 0;
  z-index: 2500;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  user-select: none;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Segoe UI",
    Roboto, "Helvetica Neue", Arial, sans-serif;
  transition: background 0.4s ease, color 0.4s ease;
}

/* ===== 暗色模式 ===== */
.lx-login-container.theme-dark {
  background: #090d16;
  color: #f3f4f6;
}

.lx-login-container.theme-dark .bg-grid-mask {
  background-image: linear-gradient(
      to right,
      rgba(255, 255, 255, 0.03) 1px,
      transparent 1px
    ),
    linear-gradient(
      to bottom,
      rgba(255, 255, 255, 0.03) 1px,
      transparent 1px
    );
}

.lx-login-container.theme-dark .glass-card {
  background: rgba(18, 24, 38, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.75),
    0 18px 36px -18px rgba(0, 0, 0, 0.8),
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    inset 0 0 20px rgba(16, 185, 129, 0.05);
}

.lx-login-container.theme-dark .modern-input {
  background: rgba(11, 15, 25, 0.75);
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.lx-login-container.theme-dark .modern-input:hover {
  border-color: rgba(255, 255, 255, 0.22);
  background: rgba(15, 21, 34, 0.88);
}

.lx-login-container.theme-dark .modern-input:focus {
  border-color: #10b981;
  background: rgba(16, 24, 39, 0.98);
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.2),
    0 4px 20px rgba(16, 185, 129, 0.18);
}

.lx-login-container.theme-dark .segmented-control {
  background: rgba(0, 0, 0, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.lx-login-container.theme-dark .nav-pill-btn {
  background: rgba(18, 24, 38, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #f3f4f6;
  box-shadow: 0 8px 24px -4px rgba(0, 0, 0, 0.5);
}

.lx-login-container.theme-dark .card-title {
  color: #ffffff;
}

.lx-login-container.theme-dark .mode-desc {
  color: #94a3b8;
}

/* ===== 亮色模式 ===== */
.lx-login-container.theme-light {
  background: #f0fdf4;
  color: #1e293b;
}

.lx-login-container.theme-light .bg-grid-mask {
  background-image: linear-gradient(
      to right,
      rgba(16, 185, 129, 0.08) 1px,
      transparent 1px
    ),
    linear-gradient(
      to bottom,
      rgba(16, 185, 129, 0.08) 1px,
      transparent 1px
    );
}

.lx-login-container.theme-light .glass-card {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 30px 60px -12px rgba(16, 185, 129, 0.18),
    0 18px 36px -18px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 1),
    inset 0 0 20px rgba(16, 185, 129, 0.06);
}

.lx-login-container.theme-light .modern-input {
  background: rgba(248, 250, 252, 0.9);
  border: 1.5px solid rgba(203, 213, 225, 0.8);
  color: #0f172a;
}

.lx-login-container.theme-light .modern-input::placeholder {
  color: #94a3b8;
}

.lx-login-container.theme-light .modern-input:hover {
  border-color: rgba(16, 185, 129, 0.45);
  background: #ffffff;
}

.lx-login-container.theme-light .modern-input:focus {
  border-color: #10b981;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.16),
    0 4px 16px rgba(16, 185, 129, 0.12);
}

.lx-login-container.theme-light .segmented-control {
  background: rgba(241, 245, 249, 0.9);
  border: 1px solid rgba(203, 213, 225, 0.7);
}

.lx-login-container.theme-light .nav-pill-btn {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(203, 213, 225, 0.8);
  color: #0f172a;
  box-shadow: 0 8px 20px -4px rgba(16, 185, 129, 0.15);
}

.lx-login-container.theme-light .card-title {
  color: #0f172a;
}

.lx-login-container.theme-light .mode-desc {
  color: #64748b;
}

/* 动态微网格与极光背景 */
.ambient-canvas {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.aurora-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
  mix-blend-mode: screen;
  animation: floatAurora 18s ease-in-out infinite alternate;
}

.theme-light .aurora-blob {
  opacity: 0.4;
  mix-blend-mode: normal;
}

.aurora-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(
    circle,
    #059669 0%,
    rgba(16, 185, 129, 0.4) 50%,
    transparent 70%
  );
  top: -10%;
  left: -5%;
  animation-duration: 20s;
}

.aurora-2 {
  width: 560px;
  height: 560px;
  background: radial-gradient(
    circle,
    #0284c7 0%,
    rgba(6, 182, 212, 0.4) 50%,
    transparent 70%
  );
  bottom: -15%;
  right: -10%;
  animation-duration: 24s;
  animation-delay: -5s;
}

.aurora-3 {
  width: 400px;
  height: 400px;
  background: radial-gradient(
    circle,
    #6366f1 0%,
    rgba(99, 102, 241, 0.3) 50%,
    transparent 70%
  );
  top: 45%;
  left: 35%;
  animation-duration: 16s;
  animation-delay: -9s;
}

@keyframes floatAurora {
  0% {
    transform: translate(0, 0) scale(1) rotate(0deg);
  }
  50% {
    transform: translate(50px, 40px) scale(1.1) rotate(45deg);
  }
  100% {
    transform: translate(-30px, -20px) scale(0.95) rotate(-30deg);
  }
}

.bg-grid-mask {
  position: absolute;
  inset: 0;
  background-size: 40px 40px;
  mask-image: radial-gradient(
    ellipse 80% 80% at 50% 50%,
    #000 60%,
    transparent 100%
  );
  pointer-events: none;
  z-index: 1;
}

/* 顶部浮动控制导航栏 */
.top-nav-bar {
  position: absolute;
  top: 24px;
  left: 24px;
  right: 24px;
  z-index: 20;
  display: flex;
  justify-content: space-between;
  align-items: center;
  pointer-events: none;
}

.nav-pill-btn {
  pointer-events: auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 9999px;
  cursor: pointer;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  font-size: 13px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
}

.nav-pill-btn:hover {
  transform: translateY(-2px) scale(1.02);
  border-color: #10b981;
}

.nav-pill-btn:active {
  transform: translateY(0.5px) scale(0.98);
}

/* 磨砂玻璃卡片 */
.glass-card {
  position: relative;
  z-index: 10;
  width: 90%;
  max-width: 440px;
  border-radius: 28px;
  padding: 40px 36px;
  box-sizing: border-box;
  backdrop-filter: blur(28px) saturate(190%);
  -webkit-backdrop-filter: blur(28px) saturate(190%);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Header & Logo */
.card-header {
  text-align: center;
  margin-bottom: 26px;
}

.logo-box {
  width: 66px;
  height: 66px;
  border-radius: 20px;
  margin: 0 auto 16px auto;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #10b981 0%, #06b6d4 100%);
  box-shadow: 0 12px 28px -4px rgba(16, 185, 129, 0.45);
  position: relative;
}

.logo-box::after {
  content: "";
  position: absolute;
  inset: -4px;
  border-radius: 24px;
  background: linear-gradient(135deg, #10b981, #06b6d4);
  filter: blur(12px);
  opacity: 0.6;
  z-index: -1;
  animation: pulseGlow 3s ease-in-out infinite alternate;
}

@keyframes pulseGlow {
  0% {
    opacity: 0.35;
    transform: scale(0.96);
  }
  100% {
    opacity: 0.75;
    transform: scale(1.05);
  }
}

.logo-box i {
  color: #ffffff;
  font-size: 32px;
}

.card-title {
  margin: 0 0 8px 0;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.mode-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  transition: color 0.3s;
}

/* 分段选项卡切换 */
.segmented-control {
  position: relative;
  display: flex;
  padding: 4px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.tab-pill {
  position: absolute;
  top: 4px;
  bottom: 4px;
  left: 4px;
  width: calc(50% - 4px);
  border-radius: 12px;
  background: linear-gradient(
    135deg,
    rgba(16, 185, 129, 0.95),
    rgba(5, 150, 105, 0.95)
  );
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.4);
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 1;
}

.tab-btn {
  position: relative;
  z-index: 2;
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px 0;
  font-size: 13px;
  font-weight: 700;
  color: #94a3b8;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 12px;
  transition: color 0.3s;
  outline: none;
}

.tab-btn.active {
  color: #ffffff;
}

/* 表单与输入框 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.modern-input {
  width: 100%;
  border-radius: 14px;
  padding: 13px 44px 13px 44px;
  font-size: 15px;
  outline: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;
}

.input-icon {
  position: absolute;
  left: 15px;
  color: #94a3b8;
  font-size: 18px;
  pointer-events: none;
  transition: color 0.25s;
}

.modern-input:focus + .input-icon,
.input-wrapper:focus-within .input-icon {
  color: #10b981;
}

.input-action-btn {
  position: absolute;
  right: 12px;
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 6px;
  font-size: 16px;
  outline: none;
  transition: color 0.2s;
}

.input-action-btn:hover {
  color: #10b981;
}

.register-fade {
  animation: fadeInSlide 0.3s ease;
}

@keyframes fadeInSlide {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 记住登录状态自定义多选 */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2px;
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #94a3b8;
  user-select: none;
}

.checkbox-label input {
  display: none;
}

.checkbox-custom {
  width: 17px;
  height: 17px;
  border-radius: 5px;
  border: 1.5px solid #94a3b8;
  display: inline-block;
  position: relative;
  transition: all 0.2s;
}

.checkbox-label input:checked + .checkbox-custom {
  background: #10b981;
  border-color: #10b981;
}

.checkbox-label input:checked + .checkbox-custom::after {
  content: "";
  position: absolute;
  left: 5px;
  top: 2px;
  width: 4px;
  height: 8px;
  border: solid #ffffff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

/* 错误提示横条 */
.error-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 12.5px;
  background: rgba(225, 29, 72, 0.15);
  border: 1px solid rgba(225, 29, 72, 0.35);
  color: #fb7185;
}

/* 流光提交按钮 */
.btn-gradient-submit {
  width: 100%;
  padding: 14px 0;
  border-radius: 14px;
  border: none;
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(135deg, #10b981 0%, #059669 50%, #0284c7 100%);
  background-size: 200% 200%;
  box-shadow: 0 8px 24px -4px rgba(16, 185, 129, 0.45);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  margin-top: 6px;
}

.btn-gradient-submit:hover:not(:disabled) {
  background-position: 100% 0%;
  box-shadow: 0 12px 30px -4px rgba(16, 185, 129, 0.6),
    0 4px 12px rgba(6, 182, 212, 0.4);
  transform: translateY(-1.5px) scale(1.01);
}

.btn-gradient-submit:active:not(:disabled) {
  transform: translateY(0.5px) scale(0.985);
}

.btn-gradient-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  filter: grayscale(0.2);
}

/* 底部说明与清理 */
.card-footer {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid rgba(156, 163, 175, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #94a3b8;
}

.footer-link-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  outline: none;
  transition: color 0.2s;
  padding: 0;
}

.footer-link-btn:hover {
  color: #10b981;
}

/* 错误震动动画 */
.shake-active {
  animation: shakeHard 0.45s ease-in-out;
}

@keyframes shakeHard {
  0%,
  100% {
    transform: translateX(0);
  }
  20% {
    transform: translateX(-10px);
  }
  40% {
    transform: translateX(10px);
  }
  60% {
    transform: translateX(-6px);
  }
  80% {
    transform: translateX(6px);
  }
}

@media (max-width: 480px) {
  .glass-card {
    padding: 30px 22px;
    border-radius: 22px;
  }
}
</style>
