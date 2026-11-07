<template>
  <div class="admin-container" :class="isNight ? 'theme-dark' : 'theme-light'">
    <!-- 顶部状态栏导航 -->
    <header class="admin-header">
      <div class="header-left">
        <div class="brand-logo" @click="$router.push('/')">
          <i class="el-icon-reading"></i>
          <span class="brand-title">Reader</span>
          <span class="brand-badge">控制台 v1.0.1</span>
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
        <div class="sidebar-nav-list">
          <div
            v-for="item in navMenuItems"
            :key="item.index"
            class="sidebar-nav-item"
            :class="{ active: activeTab === item.index }"
            @click="handleTabSelect(item.index)"
          >
            <i :class="item.icon" class="nav-icon"></i>
            <span class="nav-title">{{ item.title }}</span>
          </div>
        </div>
      </aside>

      <main class="admin-main">
        <!-- 1. 系统概览与配置管理 -->
        <section v-if="activeTab === 'overview'" class="tab-pane">
          <h2 class="pane-title">系统状态总览与快速控制</h2>
          
          <!-- 4个核心指标卡片（支持点击互动与快捷修改） -->
          <div class="stat-cards">
            <!-- 1. 用户总数与配额卡片 -->
            <div
              class="stat-card clickable-card"
              @click="openQuotaDialog"
              title="点击调整用户上限配额或快速加人"
            >
              <div class="stat-icon bg-blue"><i class="el-icon-user-solid"></i></div>
              <div class="stat-info">
                <div class="stat-value">
                  {{ userList.length }}
                  <span class="stat-unit">/ {{ systemConfig.userLimit }} 人</span>
                </div>
                <div class="stat-label">
                  注册用户配额 <i class="el-icon-edit-outline"></i>
                </div>
              </div>
              <div class="card-action-tag">修改配额</div>
            </div>

            <!-- 2. 书源总数卡片 -->
            <div
              class="stat-card clickable-card"
              @click="openSourceBatchDialog"
              title="点击进行书源批量操作与健康治理"
            >
              <div class="stat-icon bg-green"><i class="el-icon-notebook-2"></i></div>
              <div class="stat-info">
                <div class="stat-value">
                  {{ bookSourceCount }}
                  <span class="stat-unit">个可用</span>
                </div>
                <div class="stat-label">
                  已配置书源 <i class="el-icon-s-operation"></i>
                </div>
              </div>
              <div class="card-action-tag">书源批量治理</div>
            </div>

            <!-- 3. 书架总数卡片 -->
            <div
              class="stat-card clickable-card"
              @click="openShelfDialog"
              title="点击查看与清理全局书架图书"
            >
              <div class="stat-icon bg-purple"><i class="el-icon-reading"></i></div>
              <div class="stat-info">
                <div class="stat-value">
                  {{ shelfCount }}
                  <span class="stat-unit">本图书</span>
                </div>
                <div class="stat-label">
                  书架书籍总数 <i class="el-icon-folder-opened"></i>
                </div>
              </div>
              <div class="card-action-tag">书架整理</div>
            </div>

            <!-- 4. 注册准入与系统状态卡片 -->
            <div
              class="stat-card clickable-card"
              @click="quickToggleRegister"
              title="点击一键切换用户自主注册开关"
            >
              <div class="stat-icon bg-orange"><i class="el-icon-cpu"></i></div>
              <div class="stat-info">
                <div class="stat-value">
                  {{ systemConfig.allowRegister ? "开放注册" : "暂停注册" }}
                </div>
                <div class="stat-label">
                  注册准入策略 <i class="el-icon-switch-button"></i>
                </div>
              </div>
              <div class="card-action-tag">
                {{ systemConfig.allowRegister ? "点击暂停" : "点击开放" }}
              </div>
            </div>
          </div>

          <!-- 系统参数可编辑配置面板 (管理员可在线修改并保存) -->
          <div class="dashboard-box config-box">
            <div class="box-header">
              <div class="box-title-group">
                <h3 class="box-title">
                  <i class="el-icon-s-tools"></i> 核心系统运行参数在线配置
                </h3>
                <span class="box-subtitle"
                  >管理员可直接编辑以下业务策略，点击右侧按钮立即保存生效</span
                >
              </div>
              <el-button
                type="primary"
                size="small"
                icon="el-icon-check"
                :loading="savingConfig"
                @click="submitSystemConfig"
              >
                保存全局系统配置
              </el-button>
            </div>

            <el-form
              :model="systemConfig"
              label-width="140px"
              size="small"
              class="config-form"
            >
              <div class="form-section-title">
                <i class="el-icon-user"></i> 用户准入与配额策略
              </div>
              <div class="form-row-grid">
                <el-form-item label="开放自主注册">
                  <el-switch
                    v-model="systemConfig.allowRegister"
                    active-text="允许注册"
                    inactive-text="关闭注册"
                  ></el-switch>
                </el-form-item>
                <el-form-item label="注册专属邀请码">
                  <el-input
                    v-model="systemConfig.inviteCode"
                    placeholder="留空表示注册无需邀请码"
                    clearable
                  ></el-input>
                </el-form-item>
                <el-form-item label="系统总用户上限">
                  <el-input-number
                    v-model="systemConfig.userLimit"
                    :min="1"
                    :max="500"
                    style="width: 100%;"
                  ></el-input-number>
                </el-form-item>
                <el-form-item label="单用户书籍上限">
                  <el-input-number
                    v-model="systemConfig.userBookLimit"
                    :min="10"
                    :max="1000"
                    style="width: 100%;"
                  ></el-input-number>
                </el-form-item>
              </div>

              <div class="form-section-title" style="margin-top: 16px;">
                <i class="el-icon-headset"></i> 阅读检索与微软听书参数
              </div>
              <div class="form-row-grid">
                <el-form-item label="默认听书音色">
                  <el-select
                    v-model="systemConfig.defaultVoice"
                    placeholder="请选择默认朗读音色"
                    style="width: 100%;"
                  >
                    <el-option
                      label="微软 晓晓 (女声 / 沉浸通用)"
                      value="zh-CN-XiaoxiaoNeural"
                    ></el-option>
                    <el-option
                      label="微软 云希 (男声 / 影视解说)"
                      value="zh-CN-YunxiNeural"
                    ></el-option>
                    <el-option
                      label="微软 云扬 (男声 / 专业播音)"
                      value="zh-CN-YunjianNeural"
                    ></el-option>
                    <el-option
                      label="微软 晓依 (女声 / 抒情轻柔)"
                      value="zh-CN-XiaoyiNeural"
                    ></el-option>
                  </el-select>
                </el-form-item>
                <el-form-item label="多源并发搜索数">
                  <el-slider
                    v-model="systemConfig.concurrentSearchCount"
                    :min="8"
                    :max="48"
                    :step="4"
                    show-input
                  ></el-slider>
                </el-form-item>
                <el-form-item label="章节内容缓存">
                  <el-switch
                    v-model="systemConfig.cacheChapterContent"
                    active-text="开启缓存"
                    inactive-text="实时拉取"
                  ></el-switch>
                </el-form-item>
              </div>
            </el-form>
          </div>

          <!-- 快速运维工具箱 -->
          <div class="dashboard-box action-toolbox">
            <h3 class="box-title">
              <i class="el-icon-magic-stick"></i> 快捷维护运维工具箱
            </h3>
            <div class="action-buttons">
              <el-button
                type="warning"
                size="small"
                icon="el-icon-refresh-left"
                @click="refreshAllData"
              >
                刷新全部运行指标
              </el-button>
              <el-button
                type="danger"
                size="small"
                icon="el-icon-delete-solid"
                @click="clearSystemCacheFiles"
              >
                一键清理音频与解析失效缓存
              </el-button>
              <el-button
                type="success"
                size="small"
                icon="el-icon-document-add"
                @click="openAddUserDialog"
              >
                快捷新增用户
              </el-button>
              <el-button
                type="info"
                size="small"
                icon="el-icon-folder"
                @click="openLocalStore"
              >
                进入本地书仓目录
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
                  v-if="
                    scope.row.is_admin ||
                      scope.row.isAdmin ||
                      scope.row.username === 'admin'
                  "
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
                style="width: 220px; margin-right: 10px;"
              ></el-input>
              <el-select
                v-model="sourceTypeFilter"
                size="small"
                placeholder="书源类型"
                style="width: 130px; margin-right: 10px;"
              >
                <el-option label="全部类型" value="all"></el-option>
                <el-option label="包含 @js" value="js"></el-option>
                <el-option label="支持 webview" value="webview"></el-option>
                <el-option label="标准纯净源" value="normal"></el-option>
              </el-select>
              <el-button
                type="success"
                size="small"
                icon="el-icon-check"
                @click="batchToggleSources(true)"
              >
                全部启用
              </el-button>
              <el-button
                type="warning"
                size="small"
                icon="el-icon-close"
                @click="batchToggleSources(false)"
              >
                全部禁用
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
              <el-switch
                v-model="logErrorOnly"
                active-text="仅看异常"
                @change="fetchLogs"
                style="margin-right: 14px;"
              ></el-switch>
              <el-select
                v-model="logLines"
                size="small"
                @change="fetchLogs"
                style="width: 110px; margin-right: 14px;"
              >
                <el-option label="最新 50 行" :value="50"></el-option>
                <el-option label="最新 100 行" :value="100"></el-option>
                <el-option label="最新 200 行" :value="200"></el-option>
              </el-select>
              <el-button
                size="small"
                icon="el-icon-refresh"
                @click="fetchLogs"
                style="margin-right: 10px;"
                >刷新日志</el-button
              >
              <el-button
                size="small"
                type="primary"
                icon="el-icon-document-copy"
                @click="copyLogs"
                >复制日志</el-button
              >
            </div>
          </div>

          <div class="terminal-box" ref="terminalRef">
            <div
              v-for="(line, idx) in logs"
              :key="idx"
              class="log-line"
              :class="{
                'line-error': line.includes('ERROR') || line.includes('Exception'),
                'line-warn': line.includes('WARN')
              }"
            >
              {{ line }}
            </div>
            <div v-if="logs.length === 0" class="log-empty">
              暂无运行日志输出
            </div>
          </div>
        </section>

        <!-- 5. 本地书仓维护 -->
        <section v-if="activeTab === 'storage'" class="tab-pane">
          <h2 class="pane-title">本地书仓维护</h2>
          <div class="storage-card">
            <div class="storage-header">
              <i class="el-icon-folder-opened"></i>
              <div>
                <h3 style="margin: 0 0 6px 0;">书仓自动挂载就绪</h3>
                <p style="margin: 0; color: #64748b; font-size: 14px;">
                  系统已自动创建 storage/localStore 目录，放书即读
                </p>
              </div>
            </div>
            <div style="margin-top: 20px;">
              <el-button
                type="primary"
                size="small"
                icon="el-icon-folder"
                @click="openLocalStore"
              >
                打开本地书仓视图
              </el-button>
            </div>
          </div>
        </section>
      </main>
    </div>

    <!-- 弹窗 1：用户上限配额快速修改 -->
    <el-dialog title="修改用户上限配额" :visible.sync="showQuotaDialog" width="400px">
      <el-form label-width="110px" size="small">
        <el-form-item label="当前用户数">
          <span style="font-weight: bold; color: #3b82f6;">{{ userList.length }} 人</span>
        </el-form-item>
        <el-form-item label="系统用户配额">
          <el-input-number
            v-model="systemConfig.userLimit"
            :min="1"
            :max="500"
            style="width: 100%;"
          ></el-input-number>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showQuotaDialog = false" size="small">取消</el-button>
        <el-button type="primary" @click="saveQuotaAndClose" size="small">保存配额</el-button>
      </div>
    </el-dialog>

    <!-- 弹窗 2：书源批量快捷控制 -->
    <el-dialog title="书源批量快捷控制" :visible.sync="showSourceBatchDialog" width="460px">
      <p style="margin-top: 0; color: #64748b;">
        当前已配置 {{ bookSourceList.length }} 个书源，可执行一键批量操作：
      </p>
      <div style="display: flex; gap: 10px; margin-bottom: 20px;">
        <el-button type="success" size="small" icon="el-icon-check" @click="batchToggleSources(true)">
          一键启用全部书源
        </el-button>
        <el-button type="warning" size="small" icon="el-icon-close" @click="batchToggleSources(false)">
          一键禁用全部书源
        </el-button>
      </div>
      <el-button type="primary" size="small" icon="el-icon-d-arrow-right" @click="goToSourcesTab">
        进入完整书源管理页面
      </el-button>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showSourceBatchDialog = false" size="small">关闭</el-button>
      </div>
    </el-dialog>

    <!-- 弹窗 3：全局书架书籍管理 -->
    <el-dialog title="书架书籍整理" :visible.sync="showShelfDialog" width="560px">
      <p style="margin-top: 0; color: #64748b;">
        当前共有 {{ shelfBooks.length }} 本书籍在书架上：
      </p>
      <el-table :data="shelfBooks" max-height="300" stripe size="small">
        <el-table-column prop="name" label="书名" min-width="140"></el-table-column>
        <el-table-column prop="author" label="作者" width="100"></el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template slot-scope="scope">
            <el-button
              type="text"
              size="mini"
              style="color: #F56C6C;"
              @click="removeShelfBookItem(scope.row)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showShelfDialog = false" size="small">完成</el-button>
      </div>
    </el-dialog>

    <!-- 弹窗 4：新增用户对话框 -->
    <el-dialog title="新增用户" :visible.sync="showAddUser" width="420px">
      <el-form :model="newUserForm" label-width="100px" size="small">
        <el-form-item label="用户名">
          <el-input
            v-model="newUserForm.username"
            autocomplete="off"
            placeholder="请输入新用户名 (≥3位)"
          ></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="newUserForm.password"
            type="password"
            autocomplete="off"
            placeholder="请输入初始密码 (≥4位)"
          ></el-input>
        </el-form-item>
        <el-form-item label="管理员权限">
          <el-switch v-model="newUserForm.isAdmin"></el-switch>
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
        <el-button type="primary" @click="submitAddUser" size="small">确认添加</el-button>
      </div>
    </el-dialog>

    <!-- 弹窗 5：重置用户密码对话框 -->
    <el-dialog title="重置用户密码" :visible.sync="showResetPwd" width="380px">
      <el-form label-width="90px" size="small">
        <el-form-item label="目标用户">
          <span style="font-weight: bold;">{{ currentEditUser.username }}</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="newPassword"
            type="password"
            placeholder="请输入新密码 (不少于4位)"
          ></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showResetPwd = false" size="small">取消</el-button>
        <el-button type="primary" @click="submitResetPwd" size="small">确认修改</el-button>
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
      navMenuItems: [
        { index: "overview", title: "系统状态总览", icon: "el-icon-data-analysis" },
        { index: "users", title: "用户权限治理", icon: "el-icon-user-solid" },
        { index: "sources", title: "书源健康管理", icon: "el-icon-collection" },
        { index: "logs", title: "诊断运行日志", icon: "el-icon-document" },
        { index: "storage", title: "本地书仓维护", icon: "el-icon-folder" }
      ],
      userList: [],
      bookSourceList: [],
      loadingUsers: false,
      loadingSources: false,
      sourceSearchKey: "",
      sourceTypeFilter: "all",
      sourcePage: 1,
      sourcePageSize: 15,
      logs: [],
      logLines: 50,
      logErrorOnly: false,
      showAddUser: false,
      showResetPwd: false,
      showQuotaDialog: false,
      showSourceBatchDialog: false,
      showShelfDialog: false,
      savingConfig: false,
      currentEditUser: {},
      newPassword: "",
      newUserForm: {
        username: "",
        password: "",
        isAdmin: false,
        enable_local_store: true,
        enable_webdav: true
      },
      systemConfig: {
        allowRegister: true,
        inviteCode: "",
        userLimit: 50,
        userBookLimit: 200,
        cacheChapterContent: false,
        defaultVoice: "zh-CN-XiaoxiaoNeural",
        concurrentSearchCount: 24,
        version: "1.0.1"
      }
    };
  },
  computed: {
    isNight() {
      return this.$store.getters.isNight;
    },
    currentUser() {
      return this.$store.state.userInfo || {};
    },
    bookSourceCount() {
      return this.bookSourceList.length;
    },
    shelfCount() {
      return this.shelfBooks.length;
    },
    shelfBooks() {
      return this.$store.state.shelfBooks || [];
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
    this.fetchSystemConfig();
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
      } else if (index === "overview") {
        this.fetchSystemConfig();
      }
    },
    refreshAllData() {
      this.fetchUsers();
      this.fetchSources();
      this.fetchSystemConfig();
      this.$message.success("数据与系统状态已全部刷新");
    },
    async fetchSystemConfig() {
      try {
        const res = await Axios.get("/getSystemConfig");
        if (res.data.isSuccess && res.data.data) {
          this.systemConfig = Object.assign({}, this.systemConfig, res.data.data);
        }
      } catch (err) {
        //
      }
    },
    async submitSystemConfig() {
      this.savingConfig = true;
      try {
        const res = await Axios.post("/saveSystemConfig", this.systemConfig);
        if (res.data.isSuccess) {
          this.$message.success("系统全局配置保存成功！");
        } else {
          this.$message.error(res.data.errorMsg || "保存失败");
        }
      } catch (err) {
        this.$message.error("保存配置发生异常");
      } finally {
        this.savingConfig = false;
      }
    },
    async quickToggleRegister() {
      this.systemConfig.allowRegister = !this.systemConfig.allowRegister;
      await this.submitSystemConfig();
      const statusText = this.systemConfig.allowRegister ? "已开启自主注册" : "已暂停自主注册";
      this.$message.info(`系统准入策略变更：${statusText}`);
    },
    openQuotaDialog() {
      this.showQuotaDialog = true;
    },
    async saveQuotaAndClose() {
      await this.submitSystemConfig();
      this.showQuotaDialog = false;
    },
    openSourceBatchDialog() {
      this.showSourceBatchDialog = true;
    },
    goToSourcesTab() {
      this.showSourceBatchDialog = false;
      this.activeTab = "sources";
      this.fetchSources();
    },
    openShelfDialog() {
      this.showShelfDialog = true;
    },
    async removeShelfBookItem(book) {
      this.$confirm(`确认从书架移除书籍《${book.name}》吗？`, "确认移除", {
        type: "warning"
      }).then(async () => {
        try {
          await Axios.post("/deleteShelfBook", { url: book.bookUrl });
          this.$message.success("书籍已移除");
          const idx = this.shelfBooks.findIndex(b => b.bookUrl === book.bookUrl);
          if (idx !== -1) {
            this.shelfBooks.splice(idx, 1);
          }
        } catch (err) {
          this.$message.error("移除书籍失败");
        }
      });
    },
    async batchToggleSources(enabled) {
      const actionText = enabled ? "启用" : "禁用";
      this.$confirm(`确定要一键${actionText}全部 ${this.bookSourceList.length} 个书源吗？`, "批量操作", {
        type: "warning"
      }).then(async () => {
        try {
          const updatedList = this.bookSourceList.map(s => {
            s.enabled = enabled;
            return s;
          });
          await Axios.post("/saveBookSources", updatedList);
          this.$message.success(`已一键${actionText}全部书源`);
          this.fetchSources();
        } catch (err) {
          this.$message.error(`批量${actionText}书源失败`);
        }
      });
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
    async clearSystemCacheFiles() {
      try {
        const res = await Axios.post("/clearSystemCache");
        if (res.data.isSuccess) {
          this.$message.success(res.data.data || "失效音频与解析缓存已全部清空");
        } else {
          this.$message.error(res.data.errorMsg || "清理缓存失败");
        }
      } catch (err) {
        this.$message.error("清理缓存请求失败");
      }
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
        isAdmin: false,
        enable_local_store: true,
        enable_webdav: true
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
            this.$message.success("删除用户成功");
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
  box-sizing: border-box;
}

.theme-dark .admin-sidebar {
  border-right-color: rgba(255, 255, 255, 0.1);
}

.sidebar-nav-list {
  display: flex;
  flex-direction: column;
  padding: 14px 10px;
  gap: 6px;
}

.sidebar-nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 46px;
  padding: 0 16px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  user-select: none;
}

.theme-light .sidebar-nav-item {
  color: #475569;
}

.theme-dark .sidebar-nav-item {
  color: #94a3b8;
}

.sidebar-nav-item:hover {
  background: rgba(59, 130, 246, 0.08);
  color: #3b82f6;
}

.sidebar-nav-item.active {
  background: #3b82f6;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.sidebar-nav-item .nav-icon {
  font-size: 18px;
}

.sidebar-nav-item .nav-title {
  white-space: nowrap;
}

.admin-main {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
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
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  margin-bottom: 24px;
}

.stat-card {
  padding: 18px 20px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
  position: relative;
  transition: all 0.2s ease;
}

.clickable-card {
  cursor: pointer;
}

.clickable-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  border-color: rgba(59, 130, 246, 0.4);
}

.theme-dark .stat-card {
  background: #1e293b;
  border-color: rgba(255, 255, 255, 0.08);
}

.theme-dark .clickable-card:hover {
  border-color: rgba(59, 130, 246, 0.6);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
}

.card-action-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  font-weight: 500;
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
  flex-shrink: 0;
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
  line-height: 1.2;
}

.stat-unit {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  margin-left: 2px;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.theme-dark .stat-label,
.theme-dark .stat-unit {
  color: #94a3b8;
}

/* 控制台表单与盒子 */
.dashboard-box {
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
  padding: 24px;
  margin-bottom: 24px;
}

.theme-dark .dashboard-box {
  background: #1e293b;
  border-color: rgba(255, 255, 255, 0.08);
}

.box-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.theme-dark .box-header {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.box-title-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.box-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}

.box-subtitle {
  font-size: 12px;
  color: #64748b;
}

.theme-dark .box-subtitle {
  color: #94a3b8;
}

.form-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #3b82f6;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.form-row-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px 24px;
}

.action-toolbox .action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
}

/* 终端日志视窗 */
.log-controls {
  display: flex;
  align-items: center;
}

.terminal-box {
  background: #090d16;
  color: #e2e8f0;
  border-radius: 12px;
  padding: 16px 20px;
  font-family: "SF Mono", Monaco, Menlo, Consolas, monospace;
  font-size: 12.5px;
  line-height: 1.6;
  height: 480px;
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
  background: rgba(255, 255, 255, 0.85);
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

/* 移动端与自适应优化 */
@media (max-width: 768px) {
  .admin-body {
    flex-direction: column;
  }
  .admin-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  }
  .sidebar-nav-list {
    flex-direction: row;
    overflow-x: auto;
    padding: 8px;
  }
  .sidebar-nav-item {
    height: 38px;
    padding: 0 12px;
  }
  .admin-main {
    padding: 16px;
  }
}
</style>
