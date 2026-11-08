<template>
  <div class="admin-container" :class="isNight ? 'theme-dark' : 'theme-light'">
    <!-- 顶部状态栏导航 -->
    <header class="admin-header">
      <div class="header-left">
        <div class="brand-logo" @click="$router.push('/')">
          <i class="el-icon-reading"></i>
          <span class="brand-title">Reader</span>
          <span class="brand-badge">控制台 v1.0.3</span>
        </div>
      </div>
      <div class="header-right">
        <el-tag
          size="small"
          :type="isAdmin ? 'danger' : 'success'"
          effect="dark"
          class="user-badge"
        >
          <i class="el-icon-user"></i>
          {{ currentUser ? currentUser.username : "用户" }} ({{
            isAdmin ? "系统管理员" : "普通用户"
          }})
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
            v-for="item in authorizedNavItems"
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
              <div class="stat-icon bg-blue">
                <i class="el-icon-user-solid"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">
                  {{ userList.length }}
                  <span class="stat-unit"
                    >/ {{ systemConfig.userLimit }} 人</span
                  >
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
              <div class="stat-icon bg-green">
                <i class="el-icon-notebook-2"></i>
              </div>
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
              <div class="stat-icon bg-purple">
                <i class="el-icon-reading"></i>
              </div>
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
                style="width: 200px; margin-right: 10px;"
              ></el-input>
              <el-select
                v-model="sourceTypeFilter"
                size="small"
                placeholder="书源类型"
                style="width: 120px; margin-right: 10px;"
              >
                <el-option label="全部类型" value="all"></el-option>
                <el-option label="包含 @js" value="js"></el-option>
                <el-option label="支持 webview" value="webview"></el-option>
                <el-option label="标准纯净源" value="normal"></el-option>
              </el-select>
              <el-button
                type="success"
                size="small"
                icon="el-icon-connection"
                @click="syncAdminSources"
                title="将管理员配置的默认书源同步至当前账户"
              >
                同步管理员书源
              </el-button>
              <el-button
                type="primary"
                size="small"
                icon="el-icon-upload2"
                @click="openImportSourceDialog"
              >
                导入书源
              </el-button>
              <el-button
                type="info"
                size="small"
                icon="el-icon-first-aid-kit"
                :loading="checkingInvalidSources"
                @click="checkAndShowInvalidSources"
              >
                失效源检测
              </el-button>
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

        <!-- 3.5 书架设置（书籍管理、分组管理、导入书籍、浏览书仓、刷新缓存） -->
        <section v-if="activeTab === 'shelf'" class="tab-pane">
          <div class="pane-header-group">
            <div>
              <h2 class="pane-title" style="margin-bottom: 4px;">
                <i class="el-icon-reading"></i> 书架综合设置与维护
              </h2>
              <span class="pane-subtitle"
                >一站式管理个人书架：图书治理与换封面、分类分组维护、本地及书仓导书、全站缓存清理</span
              >
            </div>
            <div class="pane-actions">
              <el-button
                type="primary"
                size="small"
                icon="el-icon-upload2"
                @click="triggerUploadBook"
              >
                导入书籍
              </el-button>
              <el-button
                type="warning"
                size="small"
                icon="el-icon-refresh"
                @click="refreshShelfAndCache"
              >
                刷新缓存
              </el-button>
            </div>
          </div>

          <el-tabs v-model="shelfSubTab" type="border-card" class="shelf-tabs">
            <!-- 1. 书籍管理 -->
            <el-tab-pane name="books" label="书籍管理">
              <div
                style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;"
              >
                <span style="font-size: 13px; color: #64748b;">
                  当前书架共有 <strong>{{ shelfBooks.length }}</strong> 本书籍
                </span>
                <el-input
                  v-model="shelfSearchKey"
                  placeholder="搜索书名或作者..."
                  size="small"
                  clearable
                  prefix-icon="el-icon-search"
                  style="width: 220px;"
                ></el-input>
              </div>
              <el-table
                :data="filteredShelfBooks"
                stripe
                size="small"
                style="width: 100%;"
              >
                <el-table-column label="封面" width="80" align="center">
                  <template slot-scope="scope">
                    <img
                      :src="getBookCover(scope.row)"
                      style="width: 44px; height: 60px; object-fit: cover; border-radius: 4px; box-shadow: 0 1px 4px rgba(0,0,0,0.15);"
                      @error="e => (e.target.src = noCoverImage)"
                    />
                  </template>
                </el-table-column>
                <el-table-column prop="name" label="书名" min-width="140">
                  <template slot-scope="scope">
                    <div style="font-weight: bold; color: #1e293b;">
                      {{ scope.row.name }}
                    </div>
                    <div style="font-size: 11px; color: #94a3b8;">
                      {{
                        scope.row.originName || scope.row.origin || "未知来源"
                      }}
                    </div>
                  </template>
                </el-table-column>
                <el-table-column
                  prop="author"
                  label="作者"
                  width="120"
                ></el-table-column>
                <el-table-column label="分组" width="110">
                  <template slot-scope="scope">
                    <el-tag size="mini" type="info">{{
                      getGroupName(scope.row.group)
                    }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  label="最新章节"
                  min-width="160"
                  show-overflow-tooltip
                >
                  <template slot-scope="scope">
                    <span>{{ scope.row.latestChapterTitle || "-" }}</span>
                  </template>
                </el-table-column>
                <el-table-column
                  label="阅读进度"
                  min-width="140"
                  show-overflow-tooltip
                >
                  <template slot-scope="scope">
                    <span style="color: #64748b;">{{
                      scope.row.durChapterTitle || "未开始"
                    }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center">
                  <template slot-scope="scope">
                    <el-button
                      type="text"
                      size="mini"
                      icon="el-icon-picture"
                      style="color: #E6A23C;"
                      @click="openCoverPicker(scope.row)"
                    >
                      修改封面
                    </el-button>
                    <el-button
                      type="text"
                      size="mini"
                      icon="el-icon-top"
                      @click="topShelfBook(scope.row)"
                    >
                      置顶
                    </el-button>
                    <el-button
                      type="text"
                      size="mini"
                      icon="el-icon-delete"
                      style="color: #F56C6C;"
                      @click="removeShelfBookItem(scope.row)"
                    >
                      移除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- 2. 分组管理 -->
            <el-tab-pane name="groups" label="分组管理">
              <div
                style="margin-bottom: 14px; display: flex; justify-content: space-between; align-items: center;"
              >
                <span style="font-size: 13px; color: #64748b;"
                  >自定义书籍分组类别</span
                >
                <el-button
                  type="primary"
                  size="small"
                  icon="el-icon-plus"
                  @click="openAddGroupDialog"
                >
                  新建分组
                </el-button>
              </div>
              <el-table :data="bookGroupList" stripe size="small">
                <el-table-column
                  prop="groupName"
                  label="分组名称"
                  min-width="160"
                ></el-table-column>
                <el-table-column
                  prop="groupId"
                  label="分组ID"
                  width="100"
                  align="center"
                ></el-table-column>
                <el-table-column label="包含书籍数" width="120" align="center">
                  <template slot-scope="scope">
                    {{ getGroupBookCount(scope.row.groupId) }} 本
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="160" align="center">
                  <template slot-scope="scope">
                    <el-button
                      type="text"
                      size="mini"
                      icon="el-icon-edit"
                      @click="renameGroup(scope.row)"
                      >重命名</el-button
                    >
                    <el-button
                      type="text"
                      size="mini"
                      icon="el-icon-delete"
                      style="color: #F56C6C;"
                      @click="deleteGroup(scope.row)"
                      >删除</el-button
                    >
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- 3. 导入书籍 -->
            <el-tab-pane name="importBook" label="导入书籍">
              <div
                style="padding: 24px; text-align: center; background: #f8fafc; border-radius: 8px; border: 1px dashed #cbd5e1;"
              >
                <i
                  class="el-icon-upload"
                  style="font-size: 48px; color: #409EFF; margin-bottom: 12px;"
                ></i>
                <div
                  style="font-size: 16px; font-weight: bold; margin-bottom: 8px;"
                >
                  上传本地书籍文件
                </div>
                <div
                  style="font-size: 13px; color: #64748b; margin-bottom: 16px;"
                >
                  支持 .txt、.epub、.cbz
                  格式书籍，上传后将自动解析并加入您的书架
                </div>
                <input
                  type="file"
                  ref="shelfBookFileInput"
                  style="display: none;"
                  accept=".txt,.epub,.cbz"
                  @change="onShelfBookFileSelected"
                />
                <el-button
                  type="primary"
                  icon="el-icon-folder-opened"
                  @click="triggerUploadBook"
                >
                  选择书籍文件上传
                </el-button>
              </div>
            </el-tab-pane>

            <!-- 4. 浏览书仓 -->
            <el-tab-pane name="localStore" label="浏览书仓">
              <div
                style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;"
              >
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="font-size: 13px; color: #64748b;"
                    >当前书仓路径：</span
                  >
                  <el-tag size="small">{{ localStorePath }}</el-tag>
                  <el-button
                    v-if="localStorePath !== '/'"
                    type="text"
                    size="mini"
                    icon="el-icon-top"
                    @click="navigateLocalStoreUp"
                  >
                    上一级
                  </el-button>
                </div>
                <el-button
                  size="mini"
                  icon="el-icon-refresh"
                  @click="fetchLocalStoreFiles"
                  >刷新书仓</el-button
                >
              </div>
              <el-table
                :data="localStoreFiles"
                v-loading="loadingLocalStore"
                stripe
                size="small"
              >
                <el-table-column label="名称" min-width="200">
                  <template slot-scope="scope">
                    <i
                      :class="
                        scope.row.isDir ? 'el-icon-folder' : 'el-icon-document'
                      "
                      style="margin-right: 6px; color: #409EFF;"
                    ></i>
                    <span
                      :style="
                        scope.row.isDir
                          ? 'cursor: pointer; color: #409EFF; font-weight: bold;'
                          : ''
                      "
                      @click="
                        scope.row.isDir
                          ? enterLocalStoreFolder(scope.row.name)
                          : null
                      "
                    >
                      {{ scope.row.name }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="size" label="大小" width="120">
                  <template slot-scope="scope">
                    {{ scope.row.isDir ? "-" : formatFileSize(scope.row.size) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template slot-scope="scope">
                    <el-button
                      v-if="!scope.row.isDir"
                      type="text"
                      size="mini"
                      icon="el-icon-plus"
                      @click="importLocalStoreBook(scope.row)"
                    >
                      加入书架
                    </el-button>
                    <el-button
                      v-else
                      type="text"
                      size="mini"
                      @click="enterLocalStoreFolder(scope.row.name)"
                    >
                      打开目录
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- 5. 刷新缓存 -->
            <el-tab-pane name="cache" label="刷新缓存">
              <div style="padding: 16px; background: #fff; border-radius: 6px;">
                <h4 style="margin-top: 0; margin-bottom: 12px;">
                  缓存与数据同步选项
                </h4>
                <div style="display: flex; flex-direction: column; gap: 16px;">
                  <div
                    style="display: flex; justify-content: space-between; align-items: center; padding: 12px; background: #f8fafc; border-radius: 6px;"
                  >
                    <div>
                      <div style="font-weight: bold;">刷新书架书籍状态</div>
                      <div style="font-size: 12px; color: #64748b;">
                        从后端重新载入最新书架目录与进度
                      </div>
                    </div>
                    <el-button
                      size="small"
                      type="primary"
                      icon="el-icon-refresh"
                      @click="refreshShelfAndCache"
                      >立即刷新</el-button
                    >
                  </div>
                  <div
                    style="display: flex; justify-content: space-between; align-items: center; padding: 12px; background: #f8fafc; border-radius: 6px;"
                  >
                    <div>
                      <div style="font-weight: bold;">清空书源与目录缓存</div>
                      <div style="font-size: 12px; color: #64748b;">
                        清理已缓存的书源章节列表及搜索临时数据
                      </div>
                    </div>
                    <el-button
                      size="small"
                      type="warning"
                      icon="el-icon-delete"
                      @click="clearSourceAndCatalogCache"
                      >清理缓存</el-button
                    >
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
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
                'line-error':
                  line.includes('ERROR') || line.includes('Exception'),
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

        <!-- 5. 云同步与定时备份 (参考 Firefly-Panel 架构) -->
        <section v-if="activeTab === 'cloudSync'" class="tab-pane">
          <div class="pane-header-group">
            <div>
              <h2 class="pane-title" style="margin-bottom: 4px;">
                <i class="el-icon-cloudy"></i> 云同步与数据备份治理
              </h2>
              <span class="pane-subtitle"
                >支持将书源、书架、替换规则及全站数据自动增量同步与全量打包备份至
                GitHub 仓库与 WebDAV 云盘</span
              >
            </div>
            <div class="pane-actions">
              <el-button
                type="primary"
                size="small"
                icon="el-icon-check"
                :loading="savingCloudSync"
                @click="submitCloudSyncConfig"
              >
                保存云同步配置
              </el-button>
            </div>
          </div>

          <!-- 同步运行状态与立即执行操作面板 -->
          <div class="dashboard-box" style="margin-bottom: 20px;">
            <div class="box-header">
              <div class="box-title-group">
                <h3 class="box-title">
                  <i class="el-icon-odometer"></i> 同步状态与快速调度
                </h3>
              </div>
              <div class="header-tools">
                <el-button
                  size="small"
                  type="success"
                  icon="el-icon-refresh"
                  :loading="triggeringSync"
                  @click="manualTriggerSync"
                >
                  立即执行增量同步
                </el-button>
                <el-button
                  size="small"
                  type="warning"
                  icon="el-icon-box"
                  :loading="triggeringBackup"
                  @click="manualTriggerBackup"
                >
                  立即执行全量备份
                </el-button>
              </div>
            </div>

            <div class="cloud-status-grid">
              <div class="cloud-status-card">
                <div class="status-meta-title">增量同步状态</div>
                <div class="status-meta-time">
                  上次执行：{{
                    cloudSyncConfig.lastIncrementalSyncTime || "尚未执行"
                  }}
                </div>
                <div class="status-meta-detail">
                  <i class="el-icon-info"></i>
                  {{ cloudSyncConfig.lastIncrementalStatus || "无状态记录" }}
                </div>
              </div>
              <div class="cloud-status-card">
                <div class="status-meta-title">全量备份状态</div>
                <div class="status-meta-time">
                  上次执行：{{
                    cloudSyncConfig.lastFullBackupTime || "尚未执行"
                  }}
                </div>
                <div class="status-meta-detail">
                  <i class="el-icon-info"></i>
                  {{ cloudSyncConfig.lastFullStatus || "无状态记录" }}
                </div>
              </div>
            </div>
          </div>

          <!-- 云同步与定时策略配置表单 -->
          <div class="dashboard-box config-box">
            <el-form :model="cloudSyncConfig" label-width="140px" size="small">
              <h4 class="form-section-title">
                <i class="el-icon-timer"></i> 自动化策略与定时调度
              </h4>
              <el-row :gutter="20">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="开启云端自动同步">
                    <el-switch
                      v-model="cloudSyncConfig.enabled"
                      active-text="启用调度"
                      inactive-text="关闭"
                    ></el-switch>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="备份存储目标渠道">
                    <el-select
                      v-model="cloudSyncConfig.target"
                      style="width: 100%;"
                    >
                      <el-option
                        label="同时备份到 GitHub 与 WebDAV"
                        value="both"
                      ></el-option>
                      <el-option
                        label="仅备份到 GitHub 仓库"
                        value="github"
                      ></el-option>
                      <el-option
                        label="仅备份到 WebDAV 云盘"
                        value="webdav"
                      ></el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="增量同步间隔">
                    <el-select
                      v-model="cloudSyncConfig.incrementalIntervalMinutes"
                      style="width: 100%;"
                    >
                      <el-option label="每 15 分钟" :value="15"></el-option>
                      <el-option
                        label="每 30 分钟 (推荐)"
                        :value="30"
                      ></el-option>
                      <el-option label="每 1 小时" :value="60"></el-option>
                      <el-option label="每 6 小时" :value="360"></el-option>
                      <el-option label="每 12 小时" :value="720"></el-option>
                      <el-option
                        label="关闭增量定时 (仅手动)"
                        :value="0"
                      ></el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="全量备份间隔">
                    <el-select
                      v-model="cloudSyncConfig.fullBackupIntervalHours"
                      style="width: 100%;"
                    >
                      <el-option
                        label="每天打包全量备份 (推荐)"
                        :value="24"
                      ></el-option>
                      <el-option
                        label="每 3 天打包一次"
                        :value="72"
                      ></el-option>
                      <el-option label="每周打包一次" :value="168"></el-option>
                      <el-option
                        label="关闭全量定时 (仅手动)"
                        :value="0"
                      ></el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-divider></el-divider>

              <!-- GitHub 配置 -->
              <h4 class="form-section-title">
                <i class="el-icon-link"></i> GitHub 仓库存储配置
              </h4>
              <el-row :gutter="20">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="仓库所有者 (Owner)">
                    <el-input
                      v-model="cloudSyncConfig.githubOwner"
                      placeholder="例如: username 或 org"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="仓库名称 (Repo)">
                    <el-input
                      v-model="cloudSyncConfig.githubRepo"
                      placeholder="例如: reader-backup"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="备份分支 (Branch)">
                    <el-input
                      v-model="cloudSyncConfig.githubBranch"
                      placeholder="默认: main"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="远端存放目录">
                    <el-input
                      v-model="cloudSyncConfig.githubPath"
                      placeholder="默认: reader-backup"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :xs="24">
                  <el-form-item label="GitHub 访问令牌 (Token)">
                    <el-input
                      v-model="cloudSyncConfig.githubToken"
                      type="password"
                      show-password
                      placeholder="填入具备 repo 读写权限的 Personal Access Token (ghp_...)"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :xs="24">
                  <el-form-item label="备份加密密码 (强制加密)">
                    <el-input
                      v-model="cloudSyncConfig.encryptionPassword"
                      type="password"
                      show-password
                      placeholder="必须设置数据包加密密码！上传至 GitHub 的全量与增量包均将采用 AES-256-GCM 强加密保护"
                    ></el-input>
                    <div
                      style="font-size: 12px; color: #e6a23c; margin-top: 4px;"
                    >
                      <i class="el-icon-lock"></i>
                      强安全保障：哪怕仓库公开，未经密码授权也完全无法读取任何数据包。
                    </div>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-divider></el-divider>

              <!-- WebDAV 配置 -->
              <h4 class="form-section-title">
                <i class="el-icon-folder-opened"></i> WebDAV 云盘存储配置
              </h4>
              <el-row :gutter="20">
                <el-col :xs="24">
                  <el-form-item label="WebDAV 服务器地址">
                    <el-input
                      v-model="cloudSyncConfig.webdavUrl"
                      placeholder="例如: https://dav.example.com 或 坚果云地址"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="WebDAV 账号">
                    <el-input
                      v-model="cloudSyncConfig.webdavUsername"
                      placeholder="请输入 WebDAV 登录账号"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="WebDAV 密码 / 授权码">
                    <el-input
                      v-model="cloudSyncConfig.webdavPassword"
                      type="password"
                      show-password
                      placeholder="请输入应用密码或登录密码"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :xs="24">
                  <el-form-item label="远端备份根路径">
                    <el-input
                      v-model="cloudSyncConfig.webdavRemotePath"
                      placeholder="例如: /reader-backup"
                    ></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>
        </section>

        <!-- 6. 本地书仓维护 -->
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
    <el-dialog
      title="修改用户上限配额"
      :visible.sync="showQuotaDialog"
      width="400px"
    >
      <el-form label-width="110px" size="small">
        <el-form-item label="当前用户数">
          <span style="font-weight: bold; color: #3b82f6;"
            >{{ userList.length }} 人</span
          >
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
        <el-button @click="showQuotaDialog = false" size="small"
          >取消</el-button
        >
        <el-button type="primary" @click="saveQuotaAndClose" size="small"
          >保存配额</el-button
        >
      </div>
    </el-dialog>

    <!-- 弹窗 2：书源批量快捷控制 -->
    <el-dialog
      title="书源批量快捷控制"
      :visible.sync="showSourceBatchDialog"
      width="460px"
    >
      <p style="margin-top: 0; color: #64748b;">
        当前已配置 {{ bookSourceList.length }} 个书源，可执行一键批量操作：
      </p>
      <div style="display: flex; gap: 10px; margin-bottom: 20px;">
        <el-button
          type="success"
          size="small"
          icon="el-icon-check"
          @click="batchToggleSources(true)"
        >
          一键启用全部书源
        </el-button>
        <el-button
          type="warning"
          size="small"
          icon="el-icon-close"
          @click="batchToggleSources(false)"
        >
          一键禁用全部书源
        </el-button>
      </div>
      <el-button
        type="primary"
        size="small"
        icon="el-icon-d-arrow-right"
        @click="goToSourcesTab"
      >
        进入完整书源管理页面
      </el-button>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showSourceBatchDialog = false" size="small"
          >关闭</el-button
        >
      </div>
    </el-dialog>

    <!-- 弹窗 3：全局书架书籍管理 -->
    <el-dialog
      title="书架书籍整理"
      :visible.sync="showShelfDialog"
      width="560px"
    >
      <p style="margin-top: 0; color: #64748b;">
        当前共有 {{ shelfBooks.length }} 本书籍在书架上：
      </p>
      <el-table :data="shelfBooks" max-height="300" stripe size="small">
        <el-table-column
          prop="name"
          label="书名"
          min-width="140"
        ></el-table-column>
        <el-table-column
          prop="author"
          label="作者"
          width="100"
        ></el-table-column>
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
        <el-button @click="showShelfDialog = false" size="small"
          >完成</el-button
        >
      </div>
    </el-dialog>

    <!-- 弹窗：从在线书源选择并替换书封面 -->
    <el-dialog
      title="修改书籍封面"
      :visible.sync="showCoverPicker"
      width="560px"
      append-to-body
    >
      <div v-if="currentCoverBook">
        <div style="margin-bottom: 12px; font-size: 14px;">
          当前书籍：<strong style="color: #409EFF;">{{
            currentCoverBook.name
          }}</strong>
          ({{ currentCoverBook.author }})
        </div>
        <div style="margin-bottom: 15px; display: flex; gap: 8px;">
          <el-input
            v-model="customCoverInput"
            size="small"
            placeholder="自定义图片链接 http(s)://... 或从下方书源选择"
          ></el-input>
          <el-button
            type="primary"
            size="small"
            icon="el-icon-check"
            @click="applyCover(customCoverInput)"
          >
            直接应用
          </el-button>
        </div>
        <div
          style="font-size: 13px; color: #888; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;"
        >
          <span>各书源封面候选项 (点击即可设为新封面)：</span>
          <el-button
            type="text"
            size="mini"
            icon="el-icon-refresh"
            :loading="loadingCovers"
            @click="fetchBookCovers(currentCoverBook)"
          >
            重新拉取
          </el-button>
        </div>
        <div class="cover-grid" v-loading="loadingCovers">
          <div
            v-for="(item, idx) in candidateCovers"
            :key="item.coverUrl || idx"
            class="cover-card"
            :class="{
              active:
                (currentCoverBook.customCoverUrl ||
                  currentCoverBook.coverUrl) === item.coverUrl
            }"
            @click="applyCover(item.coverUrl)"
          >
            <img
              :src="getCover(item.coverUrl, true)"
              referrerpolicy="no-referrer"
              class="cover-img"
              @error="e => (e.target.src = noCoverImage)"
            />
            <div class="cover-source-tag">
              {{ item.originName || "未知书源" }}
            </div>
          </div>
          <div
            v-if="!loadingCovers && candidateCovers.length === 0"
            style="grid-column: 1 / -1; text-align: center; color: #999; padding: 20px 0;"
          >
            暂未从其他书源匹配到图片封面，请手动输入图片链接
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 弹窗：新建分组 -->
    <el-dialog
      title="新建书籍分组"
      :visible.sync="showAddGroupDialog"
      width="400px"
      append-to-body
    >
      <el-input
        v-model="newGroupName"
        placeholder="请输入分组名称"
        size="small"
      ></el-input>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showAddGroupDialog = false" size="small"
          >取消</el-button
        >
        <el-button type="primary" @click="saveNewGroup" size="small"
          >确定</el-button
        >
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
        <el-button type="primary" @click="submitAddUser" size="small"
          >确认添加</el-button
        >
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
        <el-button type="primary" @click="submitResetPwd" size="small"
          >确认修改</el-button
        >
      </div>
    </el-dialog>

    <!-- 弹窗 6：导入书源弹窗 -->
    <el-dialog
      title="导入书源"
      :visible.sync="showImportSourceDialog"
      width="520px"
    >
      <el-tabs v-model="importSourceTab">
        <el-tab-pane label="网络远程链接" name="url">
          <el-input
            v-model="importSourceUrl"
            type="textarea"
            :rows="3"
            placeholder="请输入书源 JSON 文件的 HTTP/HTTPS 网络地址"
          ></el-input>
          <div style="margin-top: 15px; text-align: right;">
            <el-button size="small" @click="showImportSourceDialog = false"
              >取消</el-button
            >
            <el-button
              type="primary"
              size="small"
              :loading="importingSource"
              @click="submitImportSourceUrl"
            >
              立即拉取并导入
            </el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="文本 / JSON 内容" name="text">
          <el-input
            v-model="importSourceContent"
            type="textarea"
            :rows="6"
            placeholder="请在此粘贴书源 JSON 数据 (单个书源或书源数组)"
          ></el-input>
          <div style="margin-top: 15px; text-align: right;">
            <el-button size="small" @click="showImportSourceDialog = false"
              >取消</el-button
            >
            <el-button
              type="primary"
              size="small"
              :loading="importingSource"
              @click="submitImportSourceText"
            >
              解析并导入
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 弹窗 7：失效书源诊断与清理 -->
    <el-dialog
      title="失效书源体检与批量治理"
      :visible.sync="showInvalidSourcesDialog"
      width="760px"
    >
      <el-alert
        title="体检原理说明"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      >
        <div style="font-size: 12px; line-height: 1.6; color: #475569;">
          <div>
            <b>① 书名实测扫描（推荐）</b
            >：模拟读者使用测试书名（如《斗罗大陆》）向各大书源网站发送并发搜索请求，若网站倒闭、超时无响应或结构报错，即精准判定失效。
          </div>
          <div>
            <b>② 系统运行记录</b
            >：读取后端服务在日常阅读、转码和搜索中自动捕获并记录的历史异常书源。
          </div>
        </div>
      </el-alert>

      <el-tabs v-model="invalidSourceMode">
        <el-tab-pane label="① 书名实测扫描" name="keyword">
          <div
            style="background: #f8fafc; padding: 14px 16px; border-radius: 6px; margin-bottom: 14px; border: 1px solid #e2e8f0;"
          >
            <el-row :gutter="12" type="flex" align="middle">
              <el-col :span="8">
                <el-input
                  v-model="checkSourceKeyword"
                  size="small"
                  placeholder="测试搜索书名"
                >
                  <template slot="prepend">测试书名</template>
                </el-input>
              </el-col>
              <el-col :span="6">
                <el-input
                  v-model.number="checkSourceTimeout"
                  size="small"
                  type="number"
                  placeholder="超时(毫秒)"
                >
                  <template slot="prepend">超时</template>
                  <template slot="append">ms</template>
                </el-input>
              </el-col>
              <el-col :span="5">
                <el-input
                  v-model.number="checkSourceConcurrent"
                  size="small"
                  type="number"
                  placeholder="并发数"
                >
                  <template slot="prepend">并发</template>
                </el-input>
              </el-col>
              <el-col :span="5" style="text-align: right;">
                <el-button
                  v-if="!isScanningSources"
                  type="primary"
                  size="small"
                  icon="el-icon-video-play"
                  @click="startKeywordSourceScan"
                >
                  开始体检
                </el-button>
                <el-button
                  v-else
                  type="danger"
                  size="small"
                  icon="el-icon-video-pause"
                  @click="stopKeywordSourceScan"
                >
                  停止检测
                </el-button>
              </el-col>
            </el-row>
            <div
              v-if="isScanningSources || scanSourceProgress > 0"
              style="margin-top: 10px;"
            >
              <el-progress
                :percentage="scanSourcePercentage"
                :status="
                  isScanningSources
                    ? undefined
                    : invalidSourcesList.length > 0
                    ? 'exception'
                    : 'success'
                "
              ></el-progress>
              <div
                style="font-size: 12px; color: #64748b; margin-top: 4px; display: flex; justify-content: space-between;"
              >
                <span
                  >正在扫描：{{ scanSourceProgress }} /
                  {{ bookSourceList.length }} 个书源</span
                >
                <span
                  v-if="invalidSourcesList.length > 0"
                  style="color: #ef4444; font-weight: bold;"
                >
                  已发现 {{ invalidSourcesList.length }} 个失效书源
                </span>
                <span v-else style="color: #10b981;">暂未发现异常书源</span>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="② 系统运行异常记录" name="runtime">
          <div
            style="margin-bottom: 14px; display: flex; justify-content: space-between; align-items: center;"
          >
            <span style="font-size: 13px; color: #64748b;">
              直接调取后端在运行过程中自动捕捉的超时或异常源记录
            </span>
            <el-button
              type="primary"
              size="small"
              icon="el-icon-refresh"
              :loading="checkingInvalidSources"
              @click="loadRuntimeInvalidSources"
            >
              拉取后端故障记录
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>

      <div
        style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;"
      >
        <span style="font-size: 13px; font-weight: bold; color: #334155;">
          失效书源清单 ({{ invalidSourcesList.length }})
        </span>
        <el-button
          v-if="invalidSourcesList.length > 0"
          type="danger"
          size="mini"
          icon="el-icon-delete"
          @click="cleanupAllInvalidSources"
        >
          一键清理所有失效源
        </el-button>
      </div>

      <el-table :data="invalidSourcesList" max-height="340" stripe size="small">
        <el-table-column
          prop="bookSourceName"
          label="书源名称"
          min-width="140"
        ></el-table-column>
        <el-table-column
          prop="error"
          label="失效具体原因"
          min-width="240"
          show-overflow-tooltip
        >
          <template slot-scope="scope">
            <span style="color: #ef4444;">{{ scope.row.error }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template slot-scope="scope">
            <el-button
              type="text"
              size="mini"
              style="color: #F56C6C;"
              @click="deleteSingleInvalidSource(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showInvalidSourcesDialog = false" size="small"
          >关闭</el-button
        >
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Axios from "../plugins/axios";
import { LimitResquest } from "../plugins/helper";
import eventBus from "../plugins/eventBus";

export default {
  name: "AdminView",
  data() {
    return {
      activeTab: "overview",
      navMenuItems: [
        {
          index: "overview",
          title: "系统状态总览",
          icon: "el-icon-data-analysis"
        },
        { index: "users", title: "用户权限治理", icon: "el-icon-user-solid" },
        { index: "sources", title: "书源健康管理", icon: "el-icon-collection" },
        { index: "cloudSync", title: "云同步与备份", icon: "el-icon-cloudy" },
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
      shelfSubTab: "books",
      shelfSearchKey: "",
      bookGroupList: [],
      showAddGroupDialog: false,
      newGroupName: "",
      localStorePath: "/",
      localStoreFiles: [],
      loadingLocalStore: false,
      showCoverPicker: false,
      currentCoverBook: null,
      customCoverInput: "",
      candidateCovers: [],
      loadingCovers: false,
      noCoverImage: require("../assets/imgs/noCover.jpeg"),
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
        version: "1.0.2"
      },
      savingCloudSync: false,
      triggeringSync: false,
      triggeringBackup: false,
      cloudSyncConfig: {
        enabled: false,
        target: "both",
        incrementalIntervalMinutes: 30,
        fullBackupIntervalHours: 24,
        githubOwner: "",
        githubRepo: "",
        githubBranch: "main",
        githubPath: "reader-backup",
        githubToken: "",
        encryptionPassword: "",
        webdavUrl: "",
        webdavUsername: "",
        webdavPassword: "",
        webdavRemotePath: "/reader-backup",
        lastIncrementalSyncTime: "",
        lastIncrementalStatus: "",
        lastFullBackupTime: "",
        lastFullStatus: ""
      },
      showImportSourceDialog: false,
      importSourceTab: "url",
      importSourceUrl: "",
      importSourceContent: "",
      importingSource: false,
      showInvalidSourcesDialog: false,
      checkingInvalidSources: false,
      invalidSourcesList: [],
      invalidSourceMode: "keyword",
      checkSourceKeyword: "斗罗大陆",
      checkSourceTimeout: 5000,
      checkSourceConcurrent: 16,
      isScanningSources: false,
      scanSourceProgress: 0,
      scanSourceLimitHandler: null
    };
  },
  computed: {
    api() {
      return this.$store.getters.api;
    },
    isAdmin() {
      return !!(
        this.currentUser &&
        (this.currentUser.is_admin ||
          this.currentUser.isAdmin ||
          this.currentUser.username === "admin")
      );
    },
    authorizedNavItems() {
      if (this.isAdmin) {
        return this.navMenuItems;
      }
      return [
        { index: "sources", title: "书源管理", icon: "el-icon-collection" },
        { index: "shelf", title: "书架设置", icon: "el-icon-reading" }
      ];
    },
    filteredShelfBooks() {
      if (!this.shelfSearchKey) return this.shelfBooks;
      const k = this.shelfSearchKey.toLowerCase();
      return this.shelfBooks.filter(
        b =>
          (b.name && b.name.toLowerCase().includes(k)) ||
          (b.author && b.author.toLowerCase().includes(k))
      );
    },
    scanSourcePercentage() {
      if (!this.bookSourceList || this.bookSourceList.length === 0) return 0;
      const pct = Math.round(
        (this.scanSourceProgress / this.bookSourceList.length) * 100
      );
      return Math.min(pct, 100);
    },
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
      } else if (index === "cloudSync") {
        this.fetchCloudSyncConfig();
      }
    },
    refreshAllData() {
      this.fetchUsers();
      this.fetchSources();
      this.fetchSystemConfig();
      this.fetchCloudSyncConfig();
      this.$message.success("数据与系统状态已全部刷新");
    },
    async fetchSystemConfig() {
      try {
        const res = await Axios.get(this.api + "/getSystemConfig");
        if (res.data.isSuccess && res.data.data) {
          this.systemConfig = Object.assign(
            {},
            this.systemConfig,
            res.data.data
          );
        }
      } catch (err) {
        //
      }
    },
    async fetchCloudSyncConfig() {
      try {
        const res = await Axios.get(this.api + "/getCloudSyncConfig");
        if (res.data.isSuccess && res.data.data) {
          this.cloudSyncConfig = Object.assign(
            {},
            this.cloudSyncConfig,
            res.data.data
          );
        }
      } catch (err) {
        //
      }
    },
    async submitCloudSyncConfig() {
      this.savingCloudSync = true;
      try {
        const res = await Axios.post(
          this.api + "/saveCloudSyncConfig",
          this.cloudSyncConfig
        );
        if (res.data.isSuccess) {
          this.$message.success("云同步配置保存成功！");
          this.cloudSyncConfig = Object.assign(
            {},
            this.cloudSyncConfig,
            res.data.data
          );
        } else {
          this.$message.error(res.data.errorMsg || "保存云同步配置失败");
        }
      } catch (err) {
        this.$message.error("保存云同步配置发生异常");
      } finally {
        this.savingCloudSync = false;
      }
    },
    async manualTriggerSync() {
      this.triggeringSync = true;
      try {
        const res = await Axios.post(this.api + "/triggerCloudSync");
        if (res.data.isSuccess) {
          this.$message.success(`增量同步执行完成：${res.data.data}`);
          this.fetchCloudSyncConfig();
        } else {
          this.$message.error(res.data.errorMsg || "增量同步失败");
        }
      } catch (err) {
        this.$message.error("触发增量同步发生网络异常");
      } finally {
        this.triggeringSync = false;
      }
    },
    async manualTriggerBackup() {
      this.triggeringBackup = true;
      try {
        const res = await Axios.post(this.api + "/triggerCloudBackup");
        if (res.data.isSuccess) {
          this.$message.success(`全量备份打包上传成功：${res.data.data}`);
          this.fetchCloudSyncConfig();
        } else {
          this.$message.error(res.data.errorMsg || "全量备份失败");
        }
      } catch (err) {
        this.$message.error("触发全量备份发生网络异常");
      } finally {
        this.triggeringBackup = false;
      }
    },
    async submitSystemConfig() {
      this.savingConfig = true;
      try {
        const res = await Axios.post(
          this.api + "/saveSystemConfig",
          this.systemConfig
        );
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
      const statusText = this.systemConfig.allowRegister
        ? "已开启自主注册"
        : "已暂停自主注册";
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
          await Axios.post(this.api + "/deleteBook", { url: book.bookUrl });
          this.$message.success("书籍已移除");
          const idx = this.shelfBooks.findIndex(
            b => b.bookUrl === book.bookUrl
          );
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
      this.$confirm(
        `确定要一键${actionText}全部 ${this.bookSourceList.length} 个书源吗？`,
        "批量操作",
        {
          type: "warning"
        }
      ).then(async () => {
        try {
          const updatedList = this.bookSourceList.map(s => {
            s.enabled = enabled;
            return s;
          });
          await Axios.post(this.api + "/saveBookSources", updatedList);
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
        const res = await Axios.get(this.api + "/getUserList");
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
        const res = await Axios.get(this.api + "/getBookSources");
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
        await Axios.post(this.api + "/saveBookSource", source);
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
          await Axios.post(this.api + "/deleteBookSources", [source]);
          this.$message.success("删除书源成功");
          this.fetchSources();
        } catch (err) {
          this.$message.error("删除书源失败");
        }
      });
    },
    openImportSourceDialog() {
      this.importSourceUrl = "";
      this.importSourceContent = "";
      this.showImportSourceDialog = true;
    },
    async submitImportSourceUrl() {
      if (!this.importSourceUrl) {
        this.$message.error("请输入远程书源 URL 地址");
        return;
      }
      this.importingSource = true;
      try {
        const res = await Axios.get(this.importSourceUrl);
        const data = res.data;
        await this.saveImportedSources(data);
        this.showImportSourceDialog = false;
      } catch (err) {
        this.$message.error("拉取远程书源失败: " + err.toString());
      } finally {
        this.importingSource = false;
      }
    },
    async submitImportSourceText() {
      if (!this.importSourceContent) {
        this.$message.error("请粘贴书源 JSON 数据");
        return;
      }
      this.importingSource = true;
      try {
        const data = JSON.parse(this.importSourceContent);
        await this.saveImportedSources(data);
        this.showImportSourceDialog = false;
      } catch (err) {
        this.$message.error("解析书源 JSON 失败: " + err.toString());
      } finally {
        this.importingSource = false;
      }
    },
    async saveImportedSources(data) {
      let list = Array.isArray(data) ? data : [data];
      if (list.length === 0) {
        this.$message.warning("未检测到有效书源数据");
        return;
      }
      const res = await Axios.post(this.api + "/saveBookSources", list);
      if (res.data.isSuccess) {
        this.$message.success(`成功导入 ${list.length} 个书源！`);
        this.fetchSources();
      } else {
        this.$message.error(res.data.errorMsg || "保存书源失败");
      }
    },
    checkAndShowInvalidSources() {
      this.showInvalidSourcesDialog = true;
    },
    startKeywordSourceScan() {
      if (!this.checkSourceKeyword) {
        this.$message.error("请输入用于体检测试的书籍名称");
        return;
      }
      if (!this.bookSourceList || this.bookSourceList.length === 0) {
        this.$message.warning("当前系统暂无已配置的书源");
        return;
      }
      this.isScanningSources = true;
      this.scanSourceProgress = 0;
      this.invalidSourcesList = [];

      const concurrent = Math.max(1, this.checkSourceConcurrent || 16);
      const timeout = Math.max(1000, this.checkSourceTimeout || 5000);
      const total = this.bookSourceList.length;

      const limitFunc = LimitResquest(concurrent, handler => {
        this.scanSourceProgress = handler.requestCount;
        if (handler.isEnd()) {
          this.isScanningSources = false;
          this.$message.success(
            `体检完成！共扫描 ${total} 个书源，发现 ${this.invalidSourcesList.length} 个失效书源`
          );
        }
      });
      this.scanSourceLimitHandler = limitFunc;

      this.bookSourceList.forEach(source => {
        limitFunc(async () => {
          try {
            const res = await Axios.post(
              "/searchBook",
              {
                key: this.checkSourceKeyword,
                bookSourceUrl: source.bookSourceUrl
              },
              {
                timeout: timeout,
                silent: true
              }
            );
            if (!res.data.isSuccess) {
              const errText = res.data.errorMsg || "解析失败或未返回有效内容";
              this.invalidSourcesList.push({
                bookSourceUrl: source.bookSourceUrl,
                bookSourceName: source.bookSourceName || source.bookSourceUrl,
                error: `[接口报错] ${errText}`
              });
            }
          } catch (err) {
            const errStr = (err && err.toString()) || "";
            let reason = "网站无响应或访问故障";
            if (errStr.includes("timeout")) {
              reason = `响应超时 (> ${timeout}ms)`;
            } else if (errStr.includes("Network Error")) {
              reason = "网络中断或域名无法连接";
            } else if (errStr.includes("404")) {
              reason = "HTTP 404 (页面不存在或源失效)";
            } else if (
              errStr.includes("500") ||
              errStr.includes("502") ||
              errStr.includes("503")
            ) {
              reason = "HTTP 5xx (目标源服务器内部崩溃)";
            } else if (errStr) {
              reason = errStr;
            }
            this.invalidSourcesList.push({
              bookSourceUrl: source.bookSourceUrl,
              bookSourceName: source.bookSourceName || source.bookSourceUrl,
              error: reason
            });
          }
        });
      });
    },
    stopKeywordSourceScan() {
      if (this.scanSourceLimitHandler && this.scanSourceLimitHandler.cancel) {
        this.scanSourceLimitHandler.cancel();
      }
      this.isScanningSources = false;
      this.$message.info("已停止书源体检");
    },
    async loadRuntimeInvalidSources() {
      this.checkingInvalidSources = true;
      try {
        const res = await Axios.post(this.api + "/getInvalidBookSources");
        if (res.data.isSuccess) {
          const rawMap = res.data.data || {};
          const list = [];
          for (let url in rawMap) {
            const item = rawMap[url];
            const matched = this.bookSourceList.find(
              s => s.bookSourceUrl === url
            );
            list.push({
              bookSourceUrl: url,
              bookSourceName: (matched && matched.bookSourceName) || url,
              error: item.error || "运行异常: 超时或格式错误"
            });
          }
          this.invalidSourcesList = list;
          this.$message.success(`已调取到 ${list.length} 个后端故障记录源`);
        } else {
          this.$message.error(res.data.errorMsg || "获取后端故障源失败");
        }
      } catch (err) {
        this.$message.error("读取后端故障源异常: " + err.toString());
      } finally {
        this.checkingInvalidSources = false;
      }
    },
    async deleteSingleInvalidSource(row) {
      try {
        await Axios.post(this.api + "/deleteBookSources", [
          { bookSourceUrl: row.bookSourceUrl }
        ]);
        this.$message.success("已移除失效源");
        this.invalidSourcesList = this.invalidSourcesList.filter(
          s => s.bookSourceUrl !== row.bookSourceUrl
        );
        this.fetchSources();
      } catch (err) {
        this.$message.error("移除失败");
      }
    },
    async cleanupAllInvalidSources() {
      if (this.invalidSourcesList.length === 0) return;
      this.$confirm(
        `确认要清理全部 ${this.invalidSourcesList.length} 个失效书源吗？`,
        "批量清理",
        {
          type: "warning"
        }
      ).then(async () => {
        try {
          const payload = this.invalidSourcesList.map(s => ({
            bookSourceUrl: s.bookSourceUrl
          }));
          await Axios.post(this.api + "/deleteBookSources", payload);
          this.$message.success("失效书源已全部清理完毕");
          this.invalidSourcesList = [];
          this.showInvalidSourcesDialog = false;
          this.fetchSources();
        } catch (err) {
          this.$message.error("清理异常");
        }
      });
    },
    async fetchLogs() {
      try {
        const res = await Axios.get(this.api + "/getSystemLogs", {
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
        const res = await Axios.post(this.api + "/clearSystemCache");
        if (res.data.isSuccess) {
          this.$message.success(
            res.data.data || "失效音频与解析缓存已全部清空"
          );
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
        const res = await Axios.post(this.api + "/addUser", this.newUserForm);
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
        const res = await Axios.post(this.api + "/resetPassword", {
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
        await Axios.post(this.api + "/updateUser", user);
        this.$message.success("权限更新成功");
      } catch (err) {
        this.$message.error("权限更新失败");
      }
    },
    async syncAdminSources() {
      try {
        const res = await Axios.post(this.api + "/syncAdminBookSources");
        if (res.data.isSuccess) {
          const count = res.data.data ? res.data.data.length : 0;
          this.$message.success(`已同步管理员书源，共载入 ${count} 个书源！`);
          this.fetchSources();
        } else {
          this.$message.error(res.data.errorMsg || "同步失败");
        }
      } catch (err) {
        this.$message.error("同步管理员书源异常: " + err.toString());
      }
    },
    async fetchShelfBooks() {
      try {
        const res = await Axios.get(this.api + "/getBookshelf");
        if (res.data.isSuccess) {
          this.$store.commit("setShelfBooks", res.data.data);
        }
      } catch (e) {
        //
      }
    },
    async fetchBookGroups() {
      try {
        const res = await Axios.get(this.api + "/getBookGroups");
        if (res.data.isSuccess) {
          this.bookGroupList = res.data.data || [];
        }
      } catch (e) {
        //
      }
    },
    getGroupName(groupId) {
      if (typeof groupId === "undefined" || groupId === 0) return "未分组";
      const g = this.bookGroupList.find(x => x.groupId === groupId);
      return g ? g.groupName : "未分组";
    },
    getGroupBookCount(groupId) {
      return this.shelfBooks.filter(b => b.group === groupId).length;
    },
    getBookCover(book) {
      return book.customCoverUrl || book.coverUrl || this.noCoverImage;
    },
    openAddGroupDialog() {
      this.newGroupName = "";
      this.showAddGroupDialog = true;
    },
    async saveNewGroup() {
      if (!this.newGroupName.trim()) {
        this.$message.error("请输入分组名称");
        return;
      }
      try {
        const res = await Axios.post(this.api + "/saveBookGroup", {
          groupName: this.newGroupName.trim()
        });
        if (res.data.isSuccess) {
          this.$message.success("新建分组成功");
          this.showAddGroupDialog = false;
          this.fetchBookGroups();
        } else {
          this.$message.error(res.data.errorMsg || "新建分组失败");
        }
      } catch (e) {
        this.$message.error("新建分组异常: " + e.toString());
      }
    },
    renameGroup(group) {
      this.$prompt("请输入新分组名称:", "重命名分组", {
        inputValue: group.groupName,
        confirmButtonText: "确定",
        cancelButtonText: "取消"
      }).then(async ({ value }) => {
        if (!value || !value.trim()) return;
        try {
          const res = await Axios.post(this.api + "/saveBookGroup", {
            groupId: group.groupId,
            groupName: value.trim()
          });
          if (res.data.isSuccess) {
            this.$message.success("分组重命名成功");
            this.fetchBookGroups();
          }
        } catch (e) {
          this.$message.error("重命名失败");
        }
      });
    },
    deleteGroup(group) {
      this.$confirm(
        `确认删除分组【${group.groupName}】吗？分组内的书籍不会被删除`,
        "确认删除",
        {
          type: "warning"
        }
      ).then(async () => {
        try {
          const res = await Axios.post(this.api + "/deleteBookGroup", {
            groupId: group.groupId
          });
          if (res.data.isSuccess) {
            this.$message.success("分组已删除");
            this.fetchBookGroups();
          }
        } catch (e) {
          this.$message.error("删除分组失败");
        }
      });
    },
    async topShelfBook(book) {
      try {
        const minOrder = this.shelfBooks.reduce(
          (min, b) => Math.min(min, b.order || 0),
          0
        );
        const updated = Object.assign({}, book, { order: minOrder - 1 });
        await Axios.post(this.api + "/saveBook", updated);
        this.$message.success(`书籍《${book.name}》已置顶`);
        this.fetchShelfBooks();
      } catch (e) {
        this.$message.error("置顶失败");
      }
    },
    openCoverPicker(book) {
      this.currentCoverBook = book;
      this.customCoverInput = book.customCoverUrl || "";
      this.candidateCovers = [];
      this.showCoverPicker = true;
      this.fetchBookCovers(book);
    },
    fetchBookCovers(book) {
      if (!book || !book.name) return;
      if (this.coverEventSource) {
        try {
          this.coverEventSource.close();
        } catch (e) {
          // ignore
        }
        this.coverEventSource = null;
      }
      this.loadingCovers = true;
      this.candidateCovers = [];
      const seen = new Set();
      const currentCover = book.customCoverUrl || book.coverUrl;
      if (currentCover) {
        this.candidateCovers.push({
          url: currentCover,
          coverUrl: currentCover,
          sourceName: "当前封面",
          originName: "当前封面"
        });
        seen.add(currentCover);
      }
      if (book.coverUrl && !seen.has(book.coverUrl)) {
        this.candidateCovers.push({
          url: book.coverUrl,
          coverUrl: book.coverUrl,
          sourceName: "原书源封面",
          originName: "原书源封面"
        });
        seen.add(book.coverUrl);
      }
      const buildURL = require("axios/lib/helpers/buildURL");
      const params = {
        name: book.name,
        author: book.author || "",
        searchSize: 15
      };
      if (this.$store.state.token) {
        params.accessToken = this.$store.state.token;
      }
      const sseUrl = buildURL(this.api + "/searchBookSourceSSE", params);
      try {
        const es = new EventSource(sseUrl);
        this.coverEventSource = es;
        es.addEventListener("message", e => {
          try {
            const res = JSON.parse(e.data);
            if (res.data && Array.isArray(res.data)) {
              res.data.forEach(item => {
                if (item.coverUrl && !seen.has(item.coverUrl)) {
                  seen.add(item.coverUrl);
                  const name = item.originName || item.origin || "在线书源";
                  this.candidateCovers.push({
                    url: item.coverUrl,
                    coverUrl: item.coverUrl,
                    sourceName: name,
                    originName: name
                  });
                }
              });
            }
          } catch (err) {
            // ignore JSON parse error
          }
        });
        const stopSSE = () => {
          this.loadingCovers = false;
          if (this.coverEventSource) {
            try {
              this.coverEventSource.close();
            } catch (e) {
              // ignore
            }
            this.coverEventSource = null;
          }
        };
        es.addEventListener("end", stopSSE);
        es.addEventListener("error", stopSSE);
        setTimeout(stopSSE, 10000);
      } catch (err) {
        this.loadingCovers = false;
      }
    },
    async applyCover(coverUrl) {
      if (!coverUrl) {
        this.$message.error("请选择或输入有效的图片封面链接");
        return;
      }
      try {
        const updated = Object.assign({}, this.currentCoverBook, {
          customCoverUrl: coverUrl
        });
        const res = await Axios.post(this.api + "/saveBook", updated);
        if (res.data.isSuccess) {
          this.$message.success("书籍封面已更新成功！");
          this.showCoverPicker = false;
          this.fetchShelfBooks();
        } else {
          this.$message.error(res.data.errorMsg || "保存封面失败");
        }
      } catch (e) {
        this.$message.error("保存封面异常: " + e.toString());
      }
    },
    triggerUploadBook() {
      if (this.$refs.shelfBookFileInput) {
        this.$refs.shelfBookFileInput.click();
      }
    },
    async onShelfBookFileSelected(e) {
      const file = e.target.files && e.target.files[0];
      if (!file) return;
      const loading = this.$loading({
        text: `正在上传解析《${file.name}》...`
      });
      try {
        const formData = new FormData();
        formData.append("file", file);
        const res = await Axios.post(this.api + "/uploadFile", formData, {
          headers: { "Content-Type": "multipart/form-data" }
        });
        if (res.data.isSuccess) {
          this.$message.success("书籍上传解析成功，已加入书架！");
          this.fetchShelfBooks();
        } else {
          this.$message.error(res.data.errorMsg || "上传失败");
        }
      } catch (err) {
        this.$message.error("上传发生异常: " + err.toString());
      } finally {
        loading.close();
        e.target.value = "";
      }
    },
    async fetchLocalStoreFiles() {
      this.loadingLocalStore = true;
      try {
        const res = await Axios.get(this.api + "/getLocalStoreFileList", {
          params: { path: this.localStorePath }
        });
        if (res.data.isSuccess) {
          this.localStoreFiles = res.data.data || [];
        } else {
          this.$message.error(res.data.errorMsg || "读取书仓失败");
        }
      } catch (e) {
        this.$message.error("读取书仓异常");
      } finally {
        this.loadingLocalStore = false;
      }
    },
    enterLocalStoreFolder(folder) {
      const p = this.localStorePath === "/" ? "" : this.localStorePath;
      this.localStorePath = p + "/" + folder;
      this.fetchLocalStoreFiles();
    },
    navigateLocalStoreUp() {
      const parts = this.localStorePath.split("/").filter(Boolean);
      parts.pop();
      this.localStorePath = "/" + parts.join("/");
      this.fetchLocalStoreFiles();
    },
    async importLocalStoreBook(item) {
      try {
        const filePath =
          (this.localStorePath === "/" ? "" : this.localStorePath) +
          "/" +
          item.name;
        const res = await Axios.post(this.api + "/importFromLocalPathPreview", {
          path: filePath
        });
        if (res.data.isSuccess) {
          this.$message.success(`《${item.name}》已成功加入书架！`);
          this.fetchShelfBooks();
        } else {
          this.$message.error(res.data.errorMsg || "加入书架失败");
        }
      } catch (e) {
        this.$message.error("加入书架异常: " + e.toString());
      }
    },
    formatFileSize(bytes) {
      if (!bytes || bytes <= 0) return "0 B";
      const k = 1024;
      const sizes = ["B", "KB", "MB", "GB"];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return (bytes / Math.pow(k, i)).toFixed(1) + " " + sizes[i];
    },
    refreshShelfAndCache() {
      this.fetchShelfBooks();
      this.$message.success("书架书籍状态已刷新！");
    },
    async clearSourceAndCatalogCache() {
      try {
        const res = await Axios.post(this.api + "/clearCache");
        if (res.data.isSuccess) {
          this.$message.success("缓存已清空");
        } else {
          this.$message.error(res.data.errorMsg || "清理失败");
        }
      } catch (e) {
        this.$message.error("清理异常");
      }
    },
    deleteUser(user) {
      this.$confirm(`确认删除用户 [${user.username}] 吗？`, "确认删除", {
        type: "warning"
      }).then(async () => {
        try {
          const res = await Axios.post(this.api + "/deleteUsers", [
            user.username
          ]);
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

/* 云同步与备份专属样式 */
.cloud-status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.cloud-status-card {
  padding: 16px;
  border-radius: 10px;
  background: rgba(241, 245, 249, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.theme-dark .cloud-status-card {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.06);
}

.status-meta-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}

.theme-dark .status-meta-title {
  color: #f1f5f9;
}

.status-meta-time {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.status-meta-detail {
  font-size: 13px;
  color: #0284c7;
  word-break: break-all;
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

.cover-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(105px, 1fr));
  gap: 12px;
  max-height: 380px;
  overflow-y: auto;
  padding: 4px;
}
.cover-card {
  border: 2px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  text-align: center;
  transition: all 0.2s ease;
  background: #f8fafc;
  padding: 4px;
}
.cover-card:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}
.cover-card.active {
  border-color: #67c23a;
  background: rgba(103, 194, 58, 0.08);
}
.cover-card .cover-img {
  width: 100%;
  height: 130px;
  object-fit: cover;
  border-radius: 4px;
  display: block;
}
.cover-card .cover-source-tag {
  font-size: 11px;
  color: #666;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
