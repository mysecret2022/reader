<template>
  <div class="firefly-login-container">
    <!-- 顶部操作栏：模式与返回 -->
    <div class="top-nav-bar">
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

    <!-- VisionOS 极光悬浮毛玻璃登录卡片 (Derived from mysecret2022/firefly-panel) -->
    <div class="login-card" :class="{ 'shake-active': isErrorShake }">
      <!-- 极光流萤发光标题 -->
      <div class="login-title">
        <span
          class="text-3xl font-extrabold tracking-wide firefly-glow-title select-none"
        >
          Reader
        </span>
        <p class="subtitle-desc">
          {{
            isLogin
              ? "欢迎回来，畅享舒适阅读时光"
              : "创建专属账号，同步书架与书源"
          }}
        </p>
      </div>

      <!-- 模式切换：登录 / 注册 -->
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
          <span>用户注册</span>
        </button>
      </div>

      <!-- 表单区域 -->
      <form @submit.prevent="handleSubmit" class="login-form">
        <!-- 账号 -->
        <label
          class="input-wrapper"
          :class="{ focused: usernameFocus || loginForm.username }"
        >
          <span class="input-icon"><i class="el-icon-user"></i></span>
          <input
            ref="usernameInput"
            type="text"
            v-model="loginForm.username"
            placeholder="请输入账号"
            class="firefly-input"
            autocomplete="username"
            autocapitalize="none"
            spellcheck="false"
            required
            @focus="usernameFocus = true"
            @blur="usernameFocus = false"
          />
        </label>

        <!-- 密码 -->
        <label
          class="input-wrapper"
          :class="{ focused: passwordFocus || loginForm.password }"
        >
          <span class="input-icon"><i class="el-icon-lock"></i></span>
          <input
            :type="showPassword ? 'text' : 'password'"
            v-model="loginForm.password"
            placeholder="请输入密码"
            class="firefly-input"
            autocomplete="current-password"
            required
            @focus="passwordFocus = true"
            @blur="passwordFocus = false"
          />
          <button
            type="button"
            class="input-action-btn"
            @click.stop="showPassword = !showPassword"
            :title="showPassword ? '隐藏密码' : '显示密码'"
          >
            <i :class="showPassword ? 'el-icon-view' : 'el-icon-lock'"></i>
          </button>
        </label>

        <!-- 注册邀请码 (注册时展开) -->
        <transition name="fade">
          <label
            class="input-wrapper"
            v-if="!isLogin"
            :class="{ focused: codeFocus || loginForm.code }"
          >
            <span class="input-icon"><i class="el-icon-key"></i></span>
            <input
              type="text"
              v-model="loginForm.code"
              placeholder="邀请码（未开放自由注册时必填）"
              class="firefly-input"
              autocomplete="off"
              @focus="codeFocus = true"
              @blur="codeFocus = false"
            />
          </label>
        </transition>

        <!-- 记住状态与操作 -->
        <div class="form-options">
          <label class="checkbox-label">
            <input type="checkbox" v-model="remember" />
            <span class="checkbox-custom"></span>
            <span class="checkbox-text">保持登录状态</span>
          </label>
          <button
            type="button"
            class="link-ghost-btn"
            @click="clearLocalCache"
            title="清理本地历史凭据"
          >
            <i class="el-icon-refresh-left"></i> 清理凭据
          </button>
        </div>

        <!-- 错误提示 -->
        <transition name="fade">
          <div class="error-banner" v-if="errorMsg">
            <i class="el-icon-warning-outline"></i>
            <span>{{ errorMsg }}</span>
          </div>
        </transition>

        <!-- 提交按钮 -->
        <div class="submit-actions">
          <button type="submit" class="btn-firefly-submit" :disabled="loading">
            <i class="el-icon-loading" v-if="loading"></i>
            <i class="el-icon-right" v-else></i>
            <span>{{
              loading ? "处理中..." : isLogin ? "立 即 登 录" : "注 册 账 号"
            }}</span>
          </button>
        </div>
      </form>

      <!-- 底部辅助说明 -->
      <div class="firefly-footer">
        <div class="tip-line">
          初始管理员: <strong class="badge-text">admin</strong> &nbsp;|&nbsp;
          初始密码: <strong class="badge-text">admin</strong>
        </div>
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
      default: true
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
      usernameFocus: false,
      passwordFocus: false,
      codeFocus: false,
      loginForm: {
        username: "",
        password: "",
        code: ""
      }
    };
  },
  mounted() {
    this.$nextTick(() => {
      if (this.$refs.usernameInput && !this.loginForm.username) {
        this.$refs.usernameInput.focus();
      }
    });
  },
  methods: {
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
          try {
            const userToken =
              (res.data.data && res.data.data.accessToken) || "";
            const username = this.loginForm.username;
            if (userToken) {
              localStorage.removeItem(`pwa_banner_dismissed_${userToken}`);
            }
            if (username) {
              localStorage.removeItem(`pwa_banner_dismissed_${username}`);
              const userPrefix = `pwa_banner_dismissed_${username}:`;
              Object.keys(localStorage).forEach(k => {
                if (k.startsWith(userPrefix)) {
                  localStorage.removeItem(k);
                }
              });
            }
            localStorage.removeItem("pwa_banner_dismissed_anonymous");
          } catch (e) {
            // ignore
          }
          this.$message.success(this.isLogin ? "登录成功" : "注册成功");
          this.$emit("success", {
            data: res.data.data,
            remember: this.remember
          });
        } else {
          const msg =
            (res && res.data && res.data.errorMsg) || "操作失败，请重试";
          this.triggerShake(msg);
        }
      } catch (err) {
        const msg =
          (err &&
            err.response &&
            err.response.data &&
            err.response.data.errorMsg) ||
          "网络或服务异常，请稍后重试";
        this.triggerShake(msg);
      } finally {
        this.loading = false;
      }
    },
    clearLocalCache() {
      try {
        localStorage.removeItem("auth");
        localStorage.removeItem("userToken");
        sessionStorage.clear();
        this.$message.info("已清理本地登录凭据缓存");
        this.loginForm.username = "";
        this.loginForm.password = "";
        this.loginForm.code = "";
      } catch (e) {
        // ignore
      }
    }
  }
};
</script>

<style scoped>
.firefly-login-container {
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  width: 100vw;
  box-sizing: border-box;
  background: url("~@/assets/firefly.webp") no-repeat center center;
  background-size: cover;
  position: fixed;
  inset: 0;
  z-index: 2000;
  overflow-y: auto;
}

/* 顶部操作条 */
.top-nav-bar {
  position: absolute;
  top: max(16px, env(safe-area-inset-top));
  right: 20px;
  display: flex;
  gap: 10px;
  z-index: 10;
}

.nav-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 9999px;
  background: rgba(18, 22, 34, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #f1f5f9;
  font-size: 13px;
  font-weight: 500;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  cursor: pointer;
  transition: all 0.25s ease;
}

.nav-pill-btn:hover {
  background: rgba(18, 22, 34, 0.8);
  border-color: rgba(99, 242, 201, 0.6);
  color: #34d399;
  transform: translateY(-1px);
}

/* VisionOS 悬浮极光毛玻璃卡片核心样式 (From firefly-panel) */
.login-card {
  margin: 20px;
  width: 100%;
  max-width: 440px;
  border-radius: 28px !important;
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
  padding: 32px 30px 24px;

  /* 高透磨砂质感 */
  background: rgba(18, 22, 34, 0.65) !important;
  backdrop-filter: blur(32px) saturate(210%) !important;
  -webkit-backdrop-filter: blur(32px) saturate(210%) !important;

  /* 极光双层微光边框与悬浮高光深度投影 */
  border: 1px solid rgba(255, 255, 255, 0.22) !important;
  box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.55),
    0 0 50px -10px rgba(99, 242, 201, 0.25),
    0 0 35px -8px rgba(251, 191, 36, 0.18),
    inset 0 1.5px 2px rgba(255, 255, 255, 0.55),
    inset 0 -1px 2px rgba(255, 255, 255, 0.15) !important;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1) !important;
}

/* 极光环形边缘折射伪元素 */
.login-card::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: 28px;
  padding: 1.5px;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.7) 0%,
    rgba(99, 242, 201, 0.55) 30%,
    rgba(251, 191, 36, 0.45) 60%,
    rgba(167, 139, 250, 0.55) 100%
  );
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
  opacity: 0.85;
}

/* 标题设计：流萤薄荷青 ➔ 荧光冰绿晶莹渐变 */
.login-title {
  text-align: center;
  margin-bottom: 22px;
}

.firefly-glow-title {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(
    135deg,
    #a7f3d0 0%,
    #34d399 45%,
    #64f4ac 100%
  ) !important;
  -webkit-background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
  filter: drop-shadow(0 0 16px rgba(52, 211, 153, 0.55))
    drop-shadow(0 2px 10px rgba(0, 0, 0, 0.45));
  letter-spacing: 0.05em;
  display: inline-block;
  transition: filter 0.3s ease;
}

.firefly-glow-title:hover {
  filter: drop-shadow(0 0 24px rgba(100, 244, 172, 0.85))
    drop-shadow(0 2px 10px rgba(0, 0, 0, 0.5));
}

.subtitle-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: rgba(226, 232, 240, 0.75);
  font-weight: 400;
}

/* 模式切换器 */
.segmented-control {
  position: relative;
  display: flex;
  background: rgba(0, 0, 0, 0.32);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 4px;
  margin-bottom: 24px;
}

.tab-pill {
  position: absolute;
  top: 4px;
  bottom: 4px;
  left: 4px;
  width: calc(50% - 4px);
  background: linear-gradient(
    135deg,
    rgba(16, 185, 129, 0.85) 0%,
    rgba(5, 150, 105, 0.95) 100%
  );
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.4);
  transition: transform 0.28s cubic-bezier(0.2, 0.8, 0.2, 1);
  pointer-events: none;
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 0;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.65);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  z-index: 1;
  transition: color 0.2s ease;
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
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  padding: 0 14px;
  backdrop-filter: blur(10px);
  transition: all 0.25s ease;
  cursor: text;
}

.input-wrapper.focused,
.input-wrapper:focus-within {
  border-color: rgba(99, 242, 201, 0.75) !important;
  box-shadow: 0 0 16px rgba(99, 242, 201, 0.35) !important;
  background: rgba(255, 255, 255, 0.14) !important;
}

.input-icon {
  color: rgba(255, 255, 255, 0.65);
  font-size: 18px;
  margin-right: 10px;
  display: flex;
  align-items: center;
  pointer-events: none;
  user-select: none;
}

.firefly-input {
  flex: 1;
  height: 44px;
  background: transparent;
  border: none;
  outline: none;
  color: #ffffff;
  font-size: 14px;
  font-family: inherit;
}

.firefly-input::placeholder {
  color: rgba(255, 255, 255, 0.45);
  transition: opacity 0.2s ease;
}

.input-wrapper.focused .firefly-input::placeholder,
.firefly-input:focus::placeholder {
  opacity: 0;
}

.input-action-btn {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.65);
  font-size: 17px;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  transition: color 0.2s ease;
}

.input-action-btn:hover {
  color: #34d399;
}

/* 选项行 */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding: 2px 2px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(226, 232, 240, 0.85);
  cursor: pointer;
  user-select: none;
}

.checkbox-label input {
  display: none;
}

.checkbox-custom {
  width: 16px;
  height: 16px;
  border-radius: 5px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  display: inline-block;
  position: relative;
  transition: all 0.2s ease;
}

.checkbox-label input:checked + .checkbox-custom {
  background: #10b981;
  border-color: #34d399;
}

.checkbox-label input:checked + .checkbox-custom::after {
  content: "";
  position: absolute;
  left: 5px;
  top: 2px;
  width: 4px;
  height: 8px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.link-ghost-btn {
  background: transparent;
  border: none;
  color: rgba(167, 243, 208, 0.8);
  cursor: pointer;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s ease;
}

.link-ghost-btn:hover {
  color: #6ee7b7;
  text-decoration: underline;
}

/* 错误横条 */
.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(239, 68, 68, 0.2);
  border: 1px solid rgba(248, 113, 113, 0.4);
  color: #fca5a5;
  padding: 8px 14px;
  border-radius: 12px;
  font-size: 13px;
  backdrop-filter: blur(8px);
}

/* 极光微光登录按钮 (firefly-panel 规范) */
.btn-firefly-submit {
  width: 100%;
  height: 46px;
  border-radius: 14px !important;
  background: linear-gradient(
    135deg,
    #10b981 0%,
    #059669 50%,
    #0d9488 100%
  ) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.45) !important;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600 !important;
  letter-spacing: 0.08em;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-firefly-submit:hover:not(:disabled) {
  transform: translateY(-1.5px);
  box-shadow: 0 6px 22px rgba(16, 185, 129, 0.6) !important;
  background: linear-gradient(
    135deg,
    #34d399 0%,
    #10b981 50%,
    #059669 100%
  ) !important;
}

.btn-firefly-submit:active:not(:disabled) {
  transform: translateY(0.5px);
}

.btn-firefly-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 底部提示与版权致敬 */
.firefly-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.7);
}

.badge-text {
  color: #a7f3d0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.firefly-brand-link {
  color: #34d399;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s ease;
}

.firefly-brand-link:hover {
  color: #6ee7b7;
  text-decoration: underline;
}

/* 抖动动画 */
.shake-active {
  animation: shake 0.45s ease-in-out;
}

@keyframes shake {
  0%,
  100% {
    transform: translateX(0);
  }
  20%,
  60% {
    transform: translateX(-8px);
  }
  40%,
  80% {
    transform: translateX(8px);
  }
}

/* 淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
