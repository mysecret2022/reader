/* eslint-disable no-console */
import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import "./plugins/element.js";
import store from "./plugins/vuex.js";
import "./plugins/md5.js";
import { registerServiceWorker } from "./registerServiceWorker";
import noCover from "./assets/imgs/noCover.jpeg";
import noImage from "./assets/imgs/noImage.png";
import VueLazyload from "vue-lazyload";
import { jsonEncode } from "./plugins/safe-json-stringify";
import localforage from "localforage";

try {
  // 设置全局错误收集
  if (window.location.href.indexOf("errorAlert") > 0) {
    window.errorAlert = true;
  }
  window.onerror = function(event, source, lineno, colno, error) {
    if (window.errorAlert) {
      window.alert(
        jsonEncode({
          event: event,
          source,
          lineno,
          colno,
          error: error
        })
      );
    }
  };
  window.addEventListener("unhandledrejection", e => {
    if (window.errorAlert) {
      window.alert(jsonEncode(e));
    }
  });

  Vue.config.errorHandler = e => {
    if (window.errorAlert) {
      window.alert(jsonEncode(e));
    }
  };

  window.$cacheStorage = localforage.createInstance({
    name: "cacheStorage"
  });

  registerServiceWorker();

  // 监听 PWA 安装事件 beforeinstallprompt
  window.deferredPrompt = window.__deferredPrompt || null;
  if (window.deferredPrompt) {
    store.commit("setPwaInstallPrompt", window.deferredPrompt);
  }
  window.addEventListener("beforeinstallprompt", e => {
    e.preventDefault();
    window.deferredPrompt = e;
    window.__deferredPrompt = e;
    store.commit("setPwaInstallPrompt", e);
  });
  window.addEventListener("pwa-prompt-available", e => {
    if (e && e.detail) {
      window.deferredPrompt = e.detail;
      window.__deferredPrompt = e.detail;
      store.commit("setPwaInstallPrompt", e.detail);
    }
  });
  window.addEventListener("appinstalled", () => {
    window.deferredPrompt = null;
    window.__deferredPrompt = null;
    store.commit("setPwaInstallPrompt", null);
  });

  Vue.config.productionTip = false;

  Vue.use(VueLazyload, {
    observer: true
  });

  Vue.mixin({
    computed: {
      api() {
        return this.$store.getters.api;
      },
      isWebApp() {
        return window.navigator.standalone;
      },
      isPWA() {
        return ["fullscreen", "standalone", "minimal-ui"].some(
          displayMode =>
            window.matchMedia("(display-mode: " + displayMode + ")").matches
        );
      },
      canInstallPwa() {
        return (
          Boolean(this.$store.state.pwaInstallPrompt) &&
          !this.isPWA &&
          !this.isWebApp
        );
      },
      isNightTheme() {
        return this.$store.getters.isNight;
      },
      currentUserName() {
        return this.$store.getters.currentUserName;
      }
    },
    methods: {
      async installPwa() {
        const promptEvent =
          this.$store.state.pwaInstallPrompt ||
          window.deferredPrompt ||
          window.__deferredPrompt;
        if (!promptEvent) {
          this.$message.info("暂未检测到满足安装条件，或当前环境已安装应用");
          return;
        }
        try {
          if (typeof promptEvent.prompt === "function") {
            await promptEvent.prompt();
            if (promptEvent.userChoice) {
              const choiceResult = await promptEvent.userChoice;
              if (choiceResult && choiceResult.outcome === "accepted") {
                this.$message.success("正在添加到手机桌面...");
                this.$store.commit("setPwaInstallPrompt", null);
                window.deferredPrompt = null;
                window.__deferredPrompt = null;
              } else {
                this.$message.info("已取消安装");
              }
            }
          } else {
            this.$message.info(
              "当前浏览器不支持直接唤起安装，请通过浏览器菜单【添加到主屏幕】进行安装"
            );
          }
        } catch (err) {
          console.error("PWA install error:", err);
          this.$message.error("唤起安装失败: " + (err.message || err));
        }
      },
      getImagePath(url, useSW) {
        if (
          url &&
          (url.startsWith("http://") ||
            url.startsWith("https://") ||
            url.startsWith("//"))
        ) {
          if (useSW && window.serviceWorkerReady) {
            return url;
          }
          return this.api + "/cover?path=" + url;
        }
        if (!url) return false;
        // 默认是接口服务器上的资源
        return this.$store.getters.apiRoot + url;
      },
      getCover(coverUrl, normal, useSW) {
        coverUrl = this.getImagePath(coverUrl, useSW);
        if (coverUrl) {
          return normal
            ? coverUrl
            : {
                src: coverUrl,
                error: noCover
              };
        }
        return noCover;
      },
      getImage(imageUrl, normal, useSW) {
        imageUrl = this.getImagePath(imageUrl, useSW);
        if (imageUrl) {
          return normal
            ? imageUrl
            : {
                src: imageUrl,
                error: noCover
              };
        }
        return noImage;
      }
    }
  });

  new Vue({
    router,
    store,
    render: h => h(App)
  }).$mount("#app");
} catch (error) {
  alert(error.stack);
}
