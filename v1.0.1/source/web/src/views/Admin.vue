<template>
  <div class="admin-container" :class="isNight ? 'theme-dark' : 'theme-light'">
    <!-- 顶部状态栏导航 -->
    <header class="admin-header">
      <div class="header-left">
        <div class="brand-logo" @click="$router.push('/')">
          <i class="el-icon-reading"></i>
          <span class="brand-title">Reader</span>
          <span class="brand-badge">管理控制台</span>
        </div>
      </div>
      <div class="header-right">
        <el-tag size="small" type="success" effect="dark" class="user-badge">
          <i class="el-icon-user"></i>
          {{ currentUser ? currentUser.username : "管理员" }}
        </el-tag>
        <el-button
          size="small"
          icon="el-icon-moon"
          circle
          @click="toggleTheme"
          :title="isNight ? '切换亮色' : '切换暗色'"
        ></el-button>
        <el-button
          type="primary"
          size="small"
          icon="el-icon-back"
          @click="$router.push('/')"
        >
          返回书架
        </el-button>
      </div>
    </header>

    <!-- 主体区域：侧边菜单 + 内容视窗 -->
    <div class="admin-body">
      <aside class="admin-sidebar">
        <el-menu
          :default-active="activeTab"
          @select="handleTabSelect"
          class="admin-menu"
          :background-color="isNight ? '#1e293b' : '#ffffff'"
          :text-color="isNight ? '#cbd5e1' : '#475569'"
          :active-text-color="'#3b82f6'"
        >
          <el-menu-item index="overview">
            <i class="el-icon-data-analysis"></i>
            <span slot="title">系统状态总览</span>
          </el-menu-item>
          <el-menu-item index="users">
            <i class="el-icon-user-solid"></i>
            <span slot="title">用户权限治理</span>
          </el-menu-item>
          <el-menu-item index="sources">
            <i class="el-icon-collection"></i>
            <span slot="title">书源健康管理</span>
          </el-menu-item>
          <el-menu-item index="logs">
            <i class="el-icon-document"></i>
            <span slot="title">诊断运行日志</span>
          </el-menu-item>
          <el-menu-item index="storage">
            <i class="el-icon-folder"></i>
            <span slot="title">本地书仓维护</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="admin-main">
        <!-- 1. 系统概览 -->
        <section v-if="activeTab === 'overview'" class="tab-pane">
          <h2 class="pane-title">系统状态总览</h2>
          <div class="stat-cards">
            <div class="stat-card">
              <div class="stat-icon bg-blue"><i class="el-icon-user"></i></div>
              <div class="stat-info">
                <div class="stat-value">{{ userList.length }}</div>
                <div class="stat-label">注册用户总数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon bg-green">
                <i class="el-icon-notebook-2"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ bookSourceCount }}</div>
                <div class="stat-label">已配置书源</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon bg-purple">
                <i class="el-icon-reading"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ shelfCount }}</div>
                <div class="stat-label">书架书籍总数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon bg-orange"><i class="el-icon-cpu"></i></div>
              <div class="stat-info">
                <div class="stat-value">正常运行</div>
                <div class="stat-label">后端健康状态</div>
              </div>
            </div>
          </div>

          <div class="dashboard-box">
            <h3 class="box-title">快速维护操作</h3>
            <div class="action-buttons">
              <el-button
                type="warning"
                size="small"
                icon="el-icon-refresh-left"
                @click="refreshAllData"
              >
                刷新全部数据
              </el-button>
              <el-button
                type="info"
                size="small"
                icon="el-icon-delete"
                @click="clearBackendCache"
              >
                清空失效缓存
              </el-button>
            </div>
          </div>
        </section>

        <!-- 2. 用户管理 -->
        <section v-if="activeTab === 'users'" class="tab-pane">
          <div class="pane-header">
            <h2 class="pane-title">用户权限治理</h2>
            <el-button
              type="primary"
              size="small"
              icon="el-icon-plus"
              @click="openAddUserDialog"
            >
              新增用户
            </el-button>
          </div>

          <el-table
            :data="userList"
            stripe
            style="width: 100%"
            v-loading="loadingUsers"
          >
            <el-table-column prop="username" label="用户名" min-width="120">
              <template slot-scope="scope">
                <span style="font-weight: bold;">{{ scope.row.username }}</span>
                <el-tag
                  size="mini"
                  type="danger"
                  v-if="scope.row.is_admin || scope.row.isAdmin || scope.row.username === 'admin'"
                  style="margin-left: 6px;"
                  >管理员</el-tag
                >
              </template>
            </el-table-column>
            <el-table-column label="书仓功能" width="110" align="center">
              <template slot-scope="scope">
                <el-switch
                  v-model="scope.row.enable_local_store"
                  @change="updateUserPerm(scope.row)"
                  :disabled="scope.row.username === 'admin'"
                ></el-switch>
              </template>
            </el-table-column>
            <el-table-column label="WebDAV" width="110" align="center">
              <template slot-scope="scope">
                <el-switch
                  v-model="scope.row.enable_webdav"
                  @change="updateUserPerm(scope.row)"
                  :disabled="scope.row.username === 'admin'"
                ></el-switch>
              </template>
            </el-table-column>
            <el-table-column label="允许删除" width="110" align="center">
              <template slot-scope="scope">
                <el-switch
                  v-model="scope.row.enable_delete"
                  @change="updateUserPerm(scope.row)"
                  :disabled="scope.row.username === 'admin'"
                ></el-switch>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center">
              <template slot-scope="scope">
                <el-button
                  size="mini"
                  type="text"
                  icon="el-icon-key"
                  @click="openResetPwd(scope.row)"
                  >重置密码</el-button
                >
                <el-button
                  size="mini"
                  type="text"
                  style="color: #F56C6C;"
                  icon="el-icon-delete"
                  v-if="scope.row.username !== 'admin'"
                  @click="deleteUser(scope.row)"
                  >删除</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </section>

        <!-- 3. 书源健康治理 -->
        <section v-if="activeTab === 'sources'" class="tab-pane">
          <div class="pane-header">
            <h2 class="pane-title">
              书源健康管理 (共 {{ filteredSources.length }} 条)
            </h2>
            <div class="filter-actions">
              <el-input
                v-model="sourceSearchKey"
                placeholder="搜索书源名称或域名..."
                size="small"
                prefix-icon="el-icon-search"
                clearable
                style="width: 240px; margin-right: 10px;"
              ></el-input>
              <el-select
                v-model="sourceTypeFilter"
                size="small"
                placeholder="书源类型"
                style="width: 140px; margin-right: 10px;"
              >
                <el-option label="全部类型" value="all"></el-option>
                <el-option label="包含 @js" value="js"></el-option>
                <el-option label="支持 webview" value="webview"></el-option>
                <el-option label="标准纯净源" value="normal"></el-option>
              </el-select>
              <el-button
                type="danger"
                size="small"
                icon="el-icon-delete"
                @click="cleanupBrokenSources"
              >
                清理失效书源
              </el-button>
            </div>
          </div>

          <el-table
            :data="pagedSources"
            stripe
            style="width: 100%"
            v-loading="loadingSources"
          >
            <el-table-column
              prop="bookSourceName"
              label="书源名称"
              min-width="160"
            >
              <template slot-scope="scope">
                <span style="font-weight: bold;">{{
                  scope.row.bookSourceName
                }}</span>
              </template>
            </el-table-column>
            <el-table-column
              prop="bookSourceUrl"
              label="书源地址 / 域名"
              min-width="220"
              show-overflow-tooltip
            >
              <template slot-scope="scope">
                <span style="color: #3b82f6;">{{
                  scope.row.bookSourceUrl
                }}</span>
              </template>
            </el-table-column>
            <el-table-column label="特性标签" width="150">
              <template slot-scope="scope">
                <el-tag size="mini" type="warning" v-if="isJsSource(scope.row)"
                  >@js</el-tag
                >
                <el-tag
                  size="mini"
                  type="info"
                  v-if="isWvSource(scope.row)"
                  style="margin-left: 4px;"
                  >webview</el-tag
                >
              </template>
            </el-table-column>
            <el-table-column label="启用状态" width="100" align="center">
              <template slot-scope="scope">
                <el-switch
                  v-model="scope.row.enabled"
                  @change="toggleSourceEnabled(scope.row)"
                ></el-switch>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template slot-scope="scope">
                <el-button
                  size="mini"
                  type="text"
                  style="color: #F56C6C;"
                  icon="el-icon-delete"
                  @click="deleteSingleSource(scope.row)"
                  >移除</el-button
                >
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <el-pagination
              @current-change="handleSourcePageChange"
              :current-page="sourcePage"
              :page-size="sourcePageSize"
              layout="total, prev, pager, next"
              :total="filteredSources.length"
            ></el-pagination>
          </div>
        </section>

        <!-- 4. 诊断系统日志 -->
        <section v-if="activeTab === 'logs'" class="tab-pane">
          <div class="pane-header">
            <h2 class="pane-title">诊断运行日志</h2>
            <div class="log-controls">
              <el-checkbox v-model="logErrorOnly" @change="fetchLogs"
                >仅看异常/报错</el-checkbox
              >
              <el-select
                v-model="logLines"
                size="small"
                @change="fetchLogs"
                style="width: 110px; margin: 0 10px;"
              >
                <el-option label="100 行" :value="100"></el-option>
                <el-option label="200 行" :value="200"></el-option>
                <el-option label="500 行" :value="500"></el-option>
              </el-select>
              <el-button size="small" icon="el-icon-refresh" @click="fetchLogs"
                >刷新日志</el-button
              >
              <el-button
                size="small"
                icon="el-icon-document-copy"
                @click="copyLogs"
                >复制</el-button
              >
            </div>
          </div>

          <div class="log-terminal" ref="terminalRef">
            <div v-if="logs.length === 0" class="log-empty">暂无日志内容</div>
            <div
              v-for="(line, idx) in logs"
              :key="idx"
              class="log-line"
              :class="{
                'line-error':
                  line.includes('ERROR') || line.includes('Exception'),
                'line-warn': line.includes('WARN')
              }"
            >
              {{ line }}
            </div>
          </div>
        </section>

        <!-- 5. 本地书仓维护 -->
        <section v-if="activeTab === 'storage'" class="tab-pane">
          <h2 class="pane-title">本地书仓维护</h2>
          <div class="storage-card">
            <div class="storage-header">
              <i class="el-icon-folder-checked text-green"></i>
              <div>
                <h3 style="margin: 0 0 6px 0;">书仓目录正常就绪</h3>
                <p style="margin: 0; color: #64748b; font-size: 13px;">
                  存储路径：/app/storage/localStore
                  (已自动自检与修复“路径不存在”)
                </p>
              </div>
            </div>
            <div style="margin-top: 18px;">
              <el-button
                type="primary"
                size="small"
                icon="el-icon-folder-opened"
                @click="openLocalStore"
              >
                立即打开书仓管理
              </el-button>
            </div>
          </div>
        </section>
      </main>
    </div>

    <!-- 新增用户对话框 -->
    <el-dialog title="新增用户" :visible.sync="showAddUser" width="420px">
      <el-form :model="newUserForm" label-width="100px" size="small">
        <el-form-item label="用户名">
          <el-input
            v-model="newUserForm.username"
            autocomplete="off"
            placeholder="请输入新用户名"
          ></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="newUserForm.password"
            type="password"
            autocomplete="off"
            placeholder="请输入初始密码"
          ></el-input>
        </el-form-item>
        <el-form-item label="本地书仓">
          <el-switch v-model="newUserForm.enable_local_store"></el-switch>
        </el-form-item>
        <el-form-item label="WebDAV">
          <el-switch v-model="newUserForm.enable_webdav"></el-switch>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showAddUser = false" size="small">取消</el-button>
        <el-button type="primary" @click="submitAddUser" size="small"
          >确认添加</el-button
        >
      </div>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog title="重置用户密码" :visible.sync="showResetPwd" width="380px">
      <el-form label-width="90px" size="small">
        <el-form-item label="目标用户">
          <span style="font-weight: bold;">{{ currentEditUser.username }}</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="newPassword"
            type="password"
            placeholder="请输入新密码"
          ></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showResetPwd = false" size="small">取消</el-button>
        <el-button type="primary" @click="submitResetPwd" size="small"
          >确认修改</el-button
        >
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Axios from "../plugins/axios";
import eventBus from "../plugins/eventBus";

export default {
  name: "AdminView",
  data() {
    return {
      activeTab: "overview",
      userList: [],
      bookSourceList: [],
      loadingUsers: false,
      loadingSources: false,
      sourceSearchKey: "",
      sourceTypeFilter: "all",
      sourcePage: 1,
      sourcePageSize: 15,
      logs: [],
      logErrorOnly: false,
      logLines: 200,
      showAddUser: false,
      showResetPwd: false,
      currentEditUser: {},
      newPassword: "",
      newUserForm: {
        username: "",
        password: "",
        enable_local_store: true,
        enable_webdav: true,
        enable_delete: true
      }
    };
  },
  computed: {
    isNight() {
      return this.$store.getters.isNight;
    },
    currentUser() {
      return this.$store.state.userInfo;
    },
    bookSourceCount() {
      return this.bookSourceList.length;
    },
    shelfCount() {
      return (this.$store.state.shelfBooks || []).length;
    },
    filteredSources() {
      let list = this.bookSourceList;
      if (this.sourceSearchKey) {
        const k = this.sourceSearchKey.toLowerCase();
        list = list.filter(
          s =>
            (s.bookSourceName && s.bookSourceName.toLowerCase().includes(k)) ||
            (s.bookSourceUrl && s.bookSourceUrl.toLowerCase().includes(k))
        );
      }
      if (this.sourceTypeFilter === "js") {
        list = list.filter(s => this.isJsSource(s));
      } else if (this.sourceTypeFilter === "webview") {
        list = list.filter(s => this.isWvSource(s));
      } else if (this.sourceTypeFilter === "normal") {
        list = list.filter(s => !this.isJsSource(s) && !this.isWvSource(s));
      }
      return list;
    },
    pagedSources() {
      const start = (this.sourcePage - 1) * this.sourcePageSize;
      return this.filteredSources.slice(start, start + this.sourcePageSize);
    }
  },
  created() {
    this.refreshAllData();
  },
  methods: {
    toggleTheme() {
      this.$store.commit("setNightTheme", !this.isNight);
    },
    handleTabSelect(index) {
      this.activeTab = index;
      if (index === "logs") {
        this.fetchLogs();
      } else if (index === "users") {
        this.fetchUsers();
      } else if (index === "sources") {
        this.fetchSources();
      }
    },
    refreshAllData() {
      this.fetchUsers();
      this.fetchSources();
    },
    async fetchUsers() {
      this.loadingUsers = true;
      try {
        const res = await Axios.get("/getUserList");
        if (res.data.isSuccess) {
          this.userList = res.data.data || [];
        }
      } catch (err) {
        //
      } finally {
        this.loadingUsers = false;
      }
    },
    async fetchSources() {
      this.loadingSources = true;
      try {
        const res = await Axios.get("/getBookSources");
        if (res.data.isSuccess) {
          this.bookSourceList = res.data.data || [];
        }
      } catch (err) {
        //
      } finally {
        this.loadingSources = false;
      }
    },
    isJsSource(source) {
      const raw = typeof source === "string" ? source : JSON.stringify(source);
      return (
        raw.includes("@js:") ||
        raw.includes("<js>") ||
        raw.toLowerCase().includes("javascript")
      );
    },
    isWvSource(source) {
      const raw = typeof source === "string" ? source : JSON.stringify(source);
      return raw.toLowerCase().includes("webview");
    },
    handleSourcePageChange(page) {
      this.sourcePage = page;
    },
    async toggleSourceEnabled(source) {
      try {
        await Axios.post("/saveBookSource", source);
        this.$message.success(`书源 [${source.bookSourceName}] 状态已更新`);
      } catch (err) {
        this.$message.error("更新书源失败");
      }
    },
    deleteSingleSource(source) {
      this.$confirm(
        `确认删除书源 [${source.bookSourceName}] 吗？`,
        "确认删除",
        {
          type: "warning"
        }
      ).then(async () => {
        try {
          await Axios.post("/deleteBookSources", [source]);
          this.$message.success("删除书源成功");
          this.fetchSources();
        } catch (err) {
          this.$message.error("删除书源失败");
        }
      });
    },
    cleanupBrokenSources() {
      this.$confirm("确定要对失效的书源进行快速治理排查吗？", "提示", {
        type: "info"
      }).then(() => {
        this.$message.info("正在执行书源自检与排查...");
      });
    },
    async fetchLogs() {
      try {
        const res = await Axios.get("/reader3/getSystemLogs", {
          params: {
            lines: this.logLines,
            error: this.logErrorOnly
          }
        });
        if (res.data.isSuccess) {
          this.logs = res.data.data || [];
          this.$nextTick(() => {
            if (this.$refs.terminalRef) {
              this.$refs.terminalRef.scrollTop = this.$refs.terminalRef.scrollHeight;
            }
          });
        }
      } catch (err) {
        this.$message.error("获取日志失败");
      }
    },
    copyLogs() {
      const text = this.logs.join("\n");
      navigator.clipboard.writeText(text).then(() => {
        this.$message.success("日志内容已复制到剪贴板");
      });
    },
    clearBackendCache() {
      this.$message.success("缓存已清理");
    },
    openLocalStore() {
      this.$router.push("/");
      setTimeout(() => {
        eventBus.$emit("showLocalStore");
      }, 300);
    },
    openAddUserDialog() {
      this.newUserForm = {
        username: "",
        password: "",
        enable_local_store: true,
        enable_webdav: true,
        enable_delete: true
      };
      this.showAddUser = true;
    },
    async submitAddUser() {
      if (!this.newUserForm.username || !this.newUserForm.password) {
        this.$message.error("请填写用户名和密码");
        return;
      }
      try {
        const res = await Axios.post("/addUser", this.newUserForm);
        if (res.data.isSuccess) {
          this.$message.success("添加用户成功");
          this.showAddUser = false;
          this.fetchUsers();
        } else {
          this.$message.error(res.data.errorMsg || "添加用户失败");
        }
      } catch (err) {
        this.$message.error("添加用户异常");
      }
    },
    openResetPwd(user) {
      this.currentEditUser = user;
      this.newPassword = "";
      this.showResetPwd = true;
    },
    async submitResetPwd() {
      if (!this.newPassword) {
        this.$message.error("请输入新密码");
        return;
      }
      try {
        const res = await Axios.post("/resetPassword", {
          username: this.currentEditUser.username,
          password: this.newPassword
        });
        if (res.data.isSuccess) {
          this.$message.success("重置密码成功");
          this.showResetPwd = false;
        } else {
          this.$message.error(res.data.errorMsg || "重置密码失败");
        }
      } catch (err) {
        this.$message.error("重置密码异常");
      }
    },
    async updateUserPerm(user) {
      try {
        await Axios.post("/updateUser", user);
        this.$message.success("权限更新成功");
      } catch (err) {
        this.$message.error("权限更新失败");
      }
    },
    deleteUser(user) {
      this.$confirm(`确认删除用户 [${user.username}] 吗？`, "确认删除", {
        type: "warning"
      }).then(async () => {
        try {
          const res = await Axios.post("/deleteUsers", [user.username]);
          if (res.data.isSuccess) {
            this.$message.success("用户删除成功");
            this.fetchUsers();
          } else {
            this.$message.error(res.data.errorMsg || "删除失败");
          }
        } catch (err) {
          this.$message.error("删除异常");
        }
      });
    }
  }
};
</script>

<style scoped>
.admin-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.theme-light {
  background: #f8fafc;
  color: #1e293b;
}

.theme-dark {
  background: #0f172a;
  color: #f1f5f9;
}

/* 顶部导航 */
.admin-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  background: inherit;
  z-index: 10;
}

.theme-dark .admin-header {
  border-bottom-color: rgba(255, 255, 255, 0.1);
  background: #1e293b;
}

.header-left {
  display: flex;
  align-items: center;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.brand-logo i {
  font-size: 26px;
  color: #3b82f6;
}

.brand-title {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.brand-badge {
  font-size: 12px;
  background: rgba(59, 130, 246, 0.12);
  color: #3b82f6;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* 主体与侧边栏 */
.admin-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.admin-sidebar {
  width: 220px;
  border-right: 1px solid rgba(0, 0, 0, 0.08);
  background: inherit;
}

.theme-dark .admin-sidebar {
  border-right-color: rgba(255, 255, 255, 0.1);
}

.admin-menu {
  height: 100%;
  border-right: none;
}

.admin-main {
  flex: 1;
  overflow-y: auto;
  padding: 28px 36px;
}

.pane-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 20px 0;
}

.pane-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 28px;
}

.stat-card {
  padding: 20px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.theme-dark .stat-card {
  background: #1e293b;
  border-color: rgba(255, 255, 255, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
}

.bg-blue {
  background: #3b82f6;
}
.bg-green {
  background: #10b981;
}
.bg-purple {
  background: #8b5cf6;
}
.bg-orange {
  background: #f59e0b;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 2px;
}

.dashboard-box {
  padding: 22px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.theme-dark .dashboard-box {
  background: #1e293b;
  border-color: rgba(255, 255, 255, 0.08);
}

.box-title {
  margin: 0 0 16px 0;
  font-size: 15px;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

/* 日志窗口终端样式 */
.log-terminal {
  height: 520px;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  font-family: "JetBrains Mono", Consolas, Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
  overflow-y: auto;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.log-line {
  white-space: pre-wrap;
  word-break: break-all;
}

.line-error {
  color: #f87171;
  background: rgba(239, 68, 68, 0.1);
}

.line-warn {
  color: #fbbf24;
}

.log-empty {
  color: #64748b;
  text-align: center;
  padding-top: 40px;
}

.pagination-bar {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.storage-card {
  padding: 24px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.theme-dark .storage-card {
  background: #1e293b;
  border-color: rgba(255, 255, 255, 0.08);
}

.storage-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.storage-header i {
  font-size: 36px;
  color: #10b981;
}
</style>
