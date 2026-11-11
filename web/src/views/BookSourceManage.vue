<template>
  <div class="book-source-manage-page" :class="{ 'theme-dark': isNight }">
    <div class="page-header">
      <div class="header-left">
        <el-button
          icon="el-icon-back"
          size="small"
          circle
          @click="goBack"
          title="返回书架"
        ></el-button>
        <span class="page-title">书源管理与体检中心</span>
        <el-tag
          size="small"
          :type="currentUser ? 'success' : 'info'"
          class="user-tag"
          v-if="currentUser"
        >
          <i class="el-icon-user"></i>
          {{ currentUser }} 专属空间
        </el-tag>
        <el-tag
          size="small"
          type="warning"
          class="user-tag clickable-tag"
          v-else
          @click="$store.commit('setShowLogin', true)"
          style="cursor: pointer;"
        >
          <i class="el-icon-user"></i> 未登录 (点击登录)
        </el-tag>
        <el-tag size="small" type="info" class="source-count-tag">
          共 {{ bookSourceList.length }} 个书源
        </el-tag>
      </div>
      <div class="header-right">
        <el-button
          type="primary"
          plain
          icon="el-icon-folder-opened"
          size="small"
          @click="openImportSourceDialog('file')"
        >
          从本地导入
        </el-button>
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="small"
          @click="openImportSourceDialog('file')"
        >
          导入书源
        </el-button>
        <el-button
          type="primary"
          icon="el-icon-odometer"
          size="small"
          :loading="isTestingSpeed"
          @click="startSpeedTest"
        >
          {{
            isTestingSpeed
              ? `体检测速中 (${testedCount}/${bookSourceList.length})`
              : "一键体检/测速"
          }}
        </el-button>
        <el-button
          type="success"
          plain
          icon="el-icon-top"
          size="small"
          :disabled="greenSourcesCount === 0"
          @click="prioritizeGreenSources"
        >
          置顶优质源 ({{ greenSourcesCount }})
        </el-button>
        <el-button
          type="warning"
          plain
          icon="el-icon-circle-close"
          size="small"
          :disabled="redSourcesCount === 0"
          @click="disableFailedSources"
        >
          一键禁用失效源 ({{ redSourcesCount }})
        </el-button>
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="small"
          :disabled="selectedSources.length === 0"
          @click="batchDeleteSources"
        >
          批量删除 (已选 {{ selectedSources.length }} 项)
        </el-button>
      </div>
    </div>

    <!-- 过滤与工具栏 -->
    <div class="toolbar-section">
      <div class="filter-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索书源名称或域名..."
          prefix-icon="el-icon-search"
          size="small"
          clearable
          class="search-input"
        ></el-input>
        <el-radio-group v-model="statusFilter" size="small" class="status-tabs">
          <el-radio-button label="all">
            全部 ({{ bookSourceList.length }})
          </el-radio-button>
          <el-radio-button label="green">
            🟢 优质 ({{ greenSourcesCount }})
          </el-radio-button>
          <el-radio-button label="yellow">
            🟡 缓慢 ({{ yellowSourcesCount }})
          </el-radio-button>
          <el-radio-button label="red">
            🔴 失效 ({{ redSourcesCount }})
          </el-radio-button>
          <el-radio-button label="enabled"> 已启用 </el-radio-button>
          <el-radio-button label="disabled"> 已禁用 </el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-right">
        <el-button size="mini" @click="selectAll">全选</el-button>
        <el-button size="mini" @click="invertSelection">反选</el-button>
        <el-button size="mini" @click="clearSelection">清空选择</el-button>
        <span class="selected-summary">
          已选 <b>{{ selectedSources.length }}</b> 项
        </span>
      </div>
    </div>

    <!-- 书源列表表格 -->
    <div class="table-container">
      <el-table
        ref="sourceTable"
        :data="pagedSources"
        v-loading="loadingSources"
        stripe
        row-key="bookSourceUrl"
        style="width: 100%;"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          type="selection"
          :reserve-selection="true"
          width="48"
          align="center"
        ></el-table-column>

        <el-table-column label="体检状态" width="120" align="center">
          <template slot-scope="scope">
            <el-tag
              v-if="scope.row.speedStatus === 'fast'"
              size="mini"
              type="success"
              effect="dark"
            >
              🟢 {{ scope.row.speedCost }}ms
            </el-tag>
            <el-tag
              v-else-if="scope.row.speedStatus === 'slow'"
              size="mini"
              type="warning"
              effect="dark"
            >
              🟡 {{ scope.row.speedCost }}ms
            </el-tag>
            <el-tag
              v-else-if="scope.row.speedStatus === 'failed'"
              size="mini"
              type="danger"
              effect="dark"
              :title="scope.row.speedError || '超时或异常'"
            >
              🔴 {{ scope.row.speedError || scope.row.speedCost + "ms" }}
            </el-tag>
            <el-tag v-else size="mini" type="info"> 未测速 </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="bookSourceName" label="书源名称" min-width="170">
          <template slot-scope="scope">
            <span class="source-name">{{ scope.row.bookSourceName }}</span>
          </template>
        </el-table-column>

        <el-table-column
          prop="bookSourceUrl"
          label="书源地址 / 域名"
          min-width="230"
          show-overflow-tooltip
        >
          <template slot-scope="scope">
            <a
              :href="scope.row.bookSourceUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="source-link"
            >
              {{ scope.row.bookSourceUrl }}
            </a>
          </template>
        </el-table-column>

        <el-table-column label="特性标签" width="130">
          <template slot-scope="scope">
            <el-tag size="mini" type="warning" v-if="isJsSource(scope.row)">
              @js
            </el-tag>
            <el-tag
              size="mini"
              type="info"
              v-if="isWvSource(scope.row)"
              style="margin-left: 4px;"
            >
              webview
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="启用状态" width="90" align="center">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.enabled"
              active-color="#13ce66"
              inactive-color="#dcdfe6"
              @change="toggleSourceEnabled(scope.row)"
            ></el-switch>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" align="center">
          <template slot-scope="scope">
            <el-button
              type="text"
              size="mini"
              icon="el-icon-video-play"
              :loading="scope.row.isSingleTesting"
              @click="testSingleSource(scope.row)"
            >
              测速
            </el-button>
            <el-button
              type="text"
              size="mini"
              style="color: #f56c6c;"
              icon="el-icon-delete"
              @click="deleteSingleSource(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-section">
      <el-pagination
        @current-change="currentPage = $event"
        @size-change="pageSize = $event"
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[20, 50, 100, 200]"
        layout="total, sizes, prev, pager, next"
        :total="filteredSources.length"
      ></el-pagination>
    </div>

    <!-- 导入书源弹窗 -->
    <el-dialog
      title="导入书源 (个人专属书源库)"
      :visible.sync="showImportDialog"
      width="600px"
      append-to-body
      custom-class="source-import-dialog"
    >
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 15px;"
        show-icon
      >
        <span>
          💡
          推荐通过<b>「预检沙箱」</b>进行并发测速与健康体检，自动剔除失效死源；对少量可信书源亦保留直接入库通道。
        </span>
      </el-alert>
      <el-tabs v-model="importTab">
        <el-tab-pane label="从本地导入" name="file">
          <input
            type="file"
            ref="localFileInput"
            style="display: none;"
            accept=".json,.txt"
            @change="handleLocalFileChange"
            @click.stop
          />
          <div
            class="local-import-dropzone"
            @click="triggerSelectLocalFile"
            @dragover.prevent
            @drop.prevent="handleLocalFileDrop"
          >
            <i
              class="el-icon-folder-opened"
              style="font-size: 42px; color: #409EFF; margin-bottom: 12px; display: block;"
            ></i>
            <div
              style="font-size: 14px; font-weight: bold; color: #303133; margin-bottom: 6px;"
            >
              点击选择本地书源文件，或将文件拖拽至此
            </div>
            <div style="font-size: 12px; color: #909399;">
              支持 UTF-8 编码的 .json 或 .txt 格式书源文件 (单书源或书源数组)
            </div>
          </div>
          <div
            v-if="localFileName"
            style="margin-top: 14px; padding: 10px 14px; background: #f0f9eb; border: 1px solid #e1f3d8; border-radius: 6px; font-size: 13px; color: #67c23a; display: flex; align-items: center; justify-content: space-between;"
          >
            <div>
              <i class="el-icon-document-checked"></i>
              <span>
                已就绪: <strong>{{ localFileName }}</strong> (检测到
                <strong>{{ localFileSources.length }}</strong> 个书源)</span
              >
            </div>
            <el-button
              type="text"
              size="small"
              style="color: #f56c6c; padding: 0;"
              @click.stop="clearLocalFile"
            >
              移除
            </el-button>
          </div>
          <div class="import-tab-footer">
            <el-button size="small" @click="showImportDialog = false"
              >取消</el-button
            >
            <el-button
              size="small"
              icon="el-icon-folder-opened"
              @click="triggerSelectLocalFile"
            >
              {{ localFileName ? "重新选择" : "选择本地文件" }}
            </el-button>
            <el-button
              type="info"
              plain
              size="small"
              icon="el-icon-upload2"
              :loading="importing"
              :disabled="!localFileSources || localFileSources.length === 0"
              @click="submitImportLocalFileDirect"
              title="跳过测速体检，直接将全部书源存入书库"
            >
              直接入库全部
            </el-button>
            <el-button
              type="primary"
              size="small"
              icon="el-icon-odometer"
              :loading="importing"
              :disabled="!localFileSources || localFileSources.length === 0"
              @click="importLocalFileToSandbox"
              title="载入前端内存沙箱并发测速体检，自动剔除死源"
            >
              ⚡ 进入预检沙箱 (推荐)
            </el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="网络远程链接" name="url">
          <el-input
            v-model="importUrl"
            type="textarea"
            :rows="3"
            placeholder="请输入书源 JSON 文件的 HTTP/HTTPS 网络地址"
          ></el-input>
          <div class="import-tab-footer">
            <el-button size="small" @click="showImportDialog = false"
              >取消</el-button
            >
            <el-button
              type="info"
              plain
              size="small"
              icon="el-icon-link"
              :loading="importing"
              @click="submitImportUrlDirect"
              title="跳过测速体检，直接将全部书源存入书库"
            >
              直接拉取入库
            </el-button>
            <el-button
              type="primary"
              size="small"
              icon="el-icon-odometer"
              :loading="importing"
              @click="importUrlToSandbox"
              title="拉取后载入前端内存沙箱并发测速体检，自动剔除死源"
            >
              ⚡ 预检拉取导入 (推荐)
            </el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="文本 / JSON 内容" name="text">
          <el-input
            v-model="importContent"
            type="textarea"
            :rows="6"
            placeholder="请在此粘贴书源 JSON 数据 (单个书源对象或书源数组)"
          ></el-input>
          <div class="import-tab-footer">
            <el-button size="small" @click="showImportDialog = false"
              >取消</el-button
            >
            <el-button
              type="info"
              plain
              size="small"
              icon="el-icon-document-add"
              :loading="importing"
              @click="submitImportTextDirect"
              title="跳过测速体检，直接将全部书源存入书库"
            >
              直接解析入库
            </el-button>
            <el-button
              type="primary"
              size="small"
              icon="el-icon-odometer"
              :loading="importing"
              @click="importTextToSandbox"
              title="解析后载入前端内存沙箱并发测速体检，自动剔除死源"
            >
              ⚡ 预检解析导入 (推荐)
            </el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="同步系统默认源" name="sync">
          <div
            style="padding: 10px 0; color: #606266; font-size: 14px; line-height: 1.6;"
          >
            <p>从服务器同步预设的默认书源到您的个人书源库。</p>
            <p style="color: #e6a23c; font-size: 13px;">
              * 提示：推荐载入预检沙箱体检后按需勾选入库，亦可直接覆盖同步。
            </p>
          </div>
          <div class="import-tab-footer">
            <el-button size="small" @click="showImportDialog = false"
              >取消</el-button
            >
            <el-button
              type="info"
              plain
              size="small"
              icon="el-icon-download"
              :loading="syncingDefault"
              @click="submitSyncDefaultDirect"
              title="跳过测速体检，直接从系统默认同步覆盖"
            >
              直接从系统默认同步
            </el-button>
            <el-button
              type="success"
              size="small"
              icon="el-icon-odometer"
              :loading="syncingDefault"
              @click="syncDefaultToSandbox"
              title="载入到预检沙箱进行测速体检"
            >
              ⚡ 同步并进入沙箱 (推荐)
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 书源导入预检暂存沙箱弹窗 -->
    <el-dialog
      title="书源导入预检暂存沙箱 (零死源入库)"
      :visible.sync="showSandboxDialog"
      width="1000px"
      top="4vh"
      :before-close="handleCloseSandbox"
      append-to-body
      custom-class="source-sandbox-dialog"
    >
      <!-- 沙箱顶部健康概览横幅 -->
      <div class="sandbox-summary-banner">
        <div class="banner-info">
          <div class="banner-title">
            <i class="el-icon-odometer"></i>
            <span>待导入书源暂存池 (共 {{ sandboxSources.length }} 个)</span>
          </div>
          <div class="banner-desc">
            内存隔离沙箱中并发测试连通性与响应速度，自动标记并勾选优质源、剔除超时死源。未勾选书源随弹窗关闭直接在内存中丢弃，零污染入库。
          </div>
        </div>
        <div class="banner-badges">
          <el-tag size="small" type="success" effect="dark">
            🟢 极速: {{ sandboxFastCount }}
          </el-tag>
          <el-tag size="small" type="warning" effect="dark">
            🟡 较慢: {{ sandboxSlowCount }}
          </el-tag>
          <el-tag size="small" type="danger" effect="dark">
            🔴 失效: {{ sandboxFailedCount }}
          </el-tag>
          <el-tag size="small" type="info">
            ⚪ 待测: {{ sandboxPendingCount }}
          </el-tag>
          <el-tag
            size="small"
            type="primary"
            effect="plain"
            class="selected-badge"
          >
            已勾选: {{ sandboxSelectedCount }} / {{ sandboxSources.length }}
          </el-tag>
        </div>
      </div>

      <!-- 测速进度条与控制 -->
      <div
        class="sandbox-progress-section"
        v-if="isSandboxTesting || sandboxTestedCount > 0"
      >
        <div class="progress-bar-wrap">
          <el-progress
            :percentage="sandboxProgressPercent"
            :status="
              isSandboxTesting
                ? ''
                : sandboxFailedCount > 0
                ? 'warning'
                : 'success'
            "
            :stroke-width="14"
          ></el-progress>
        </div>
        <div class="progress-actions">
          <span class="progress-status-text">
            {{
              isSandboxTesting
                ? `并发体检中 (${sandboxTestedCount}/${sandboxSources.length})`
                : `体检完成 (${sandboxTestedCount}/${sandboxSources.length})`
            }}
          </span>
          <el-button
            v-if="isSandboxTesting"
            size="mini"
            type="danger"
            plain
            icon="el-icon-video-pause"
            @click="stopSandboxSpeedTest"
          >
            停止体检
          </el-button>
          <el-button
            v-else
            size="mini"
            type="primary"
            plain
            icon="el-icon-refresh"
            @click="startSandboxSpeedTest(true)"
          >
            重新体检
          </el-button>
        </div>
      </div>

      <!-- 工具栏与筛选 -->
      <div class="sandbox-toolbar">
        <div class="sandbox-filter-left">
          <el-input
            v-model="sandboxSearchKeyword"
            placeholder="筛选沙箱书源名称/网址..."
            prefix-icon="el-icon-search"
            size="mini"
            clearable
            class="sandbox-search-input"
          ></el-input>
          <el-radio-group v-model="sandboxStatusFilter" size="mini">
            <el-radio-button label="all"
              >全部 ({{ sandboxSources.length }})</el-radio-button
            >
            <el-radio-button label="green"
              >🟢 优质 ({{ sandboxFastCount }})</el-radio-button
            >
            <el-radio-button label="yellow"
              >🟡 较慢 ({{ sandboxSlowCount }})</el-radio-button
            >
            <el-radio-button label="red"
              >🔴 失效 ({{ sandboxFailedCount }})</el-radio-button
            >
            <el-radio-button label="selected"
              >已勾选 ({{ sandboxSelectedCount }})</el-radio-button
            >
          </el-radio-group>
        </div>
        <div class="sandbox-filter-right">
          <el-button
            size="mini"
            type="success"
            plain
            @click="selectAvailableSandboxSources"
          >
            仅选可用源 (🟢+🟡)
          </el-button>
          <el-button
            size="mini"
            type="primary"
            plain
            @click="selectFastSandboxSources"
          >
            仅选极速源 (🟢)
          </el-button>
          <el-button size="mini" @click="selectAllSandboxSources(true)"
            >全选</el-button
          >
          <el-button size="mini" @click="invertSandboxSelection"
            >反选</el-button
          >
          <el-button size="mini" @click="selectAllSandboxSources(false)"
            >清空</el-button
          >
        </div>
      </div>

      <!-- 沙箱书源表格 -->
      <div class="sandbox-table-wrap">
        <el-table
          :data="pagedSandboxSources"
          stripe
          size="small"
          max-height="380"
          row-key="_sandboxUid"
          style="width: 100%;"
        >
          <el-table-column width="48" align="center">
            <template slot="header">
              <el-checkbox
                :indeterminate="isSandboxIndeterminate"
                v-model="isAllSandboxChecked"
                @change="toggleAllSandboxCheck"
              ></el-checkbox>
            </template>
            <template slot-scope="scope">
              <el-checkbox v-model="scope.row._sandboxChecked"></el-checkbox>
            </template>
          </el-table-column>

          <el-table-column label="体检状态" width="130" align="center">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.isTesting" size="mini" type="info">
                <i class="el-icon-loading"></i> 体检中
              </el-tag>
              <el-tag
                v-else-if="scope.row.speedStatus === 'fast'"
                size="mini"
                type="success"
                effect="dark"
              >
                🟢 {{ scope.row.speedCost }}ms
              </el-tag>
              <el-tag
                v-else-if="scope.row.speedStatus === 'slow'"
                size="mini"
                type="warning"
                effect="dark"
              >
                🟡 {{ scope.row.speedCost }}ms
              </el-tag>
              <el-tag
                v-else-if="scope.row.speedStatus === 'failed'"
                size="mini"
                type="danger"
                effect="dark"
                :title="scope.row.speedError || '超时或异常'"
              >
                🔴 {{ scope.row.speedError || scope.row.speedCost + "ms" }}
              </el-tag>
              <el-tag v-else size="mini" type="info">未体检</el-tag>
            </template>
          </el-table-column>

          <el-table-column
            prop="bookSourceName"
            label="书源名称"
            min-width="160"
          >
            <template slot-scope="scope">
              <span class="sandbox-source-name">{{
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
              <a
                :href="scope.row.bookSourceUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="source-link"
              >
                {{ scope.row.bookSourceUrl }}
              </a>
            </template>
          </el-table-column>

          <el-table-column label="特性标签" width="120">
            <template slot-scope="scope">
              <el-tag size="mini" type="warning" v-if="isJsSource(scope.row)">
                @js
              </el-tag>
              <el-tag
                size="mini"
                type="info"
                v-if="isWvSource(scope.row)"
                style="margin-left: 4px;"
              >
                webview
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" align="center">
            <template slot-scope="scope">
              <el-button
                type="text"
                size="mini"
                icon="el-icon-video-play"
                :loading="scope.row.isTesting"
                @click="testSingleSandboxSource(scope.row)"
              >
                测速
              </el-button>
              <el-button
                type="text"
                size="mini"
                style="color: #f56c6c;"
                icon="el-icon-delete"
                @click="removeSandboxSource(scope.row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 沙箱分页 -->
      <div class="sandbox-pagination-section">
        <el-pagination
          @current-change="sandboxCurrentPage = $event"
          @size-change="sandboxPageSize = $event"
          :current-page="sandboxCurrentPage"
          :page-size="sandboxPageSize"
          :page-sizes="[15, 30, 50, 100]"
          layout="total, sizes, prev, pager, next"
          :total="filteredSandboxSources.length"
        ></el-pagination>
      </div>

      <!-- 弹窗底部操作栏 -->
      <div slot="footer" class="sandbox-dialog-footer">
        <div class="footer-left-tip">
          <i class="el-icon-info"></i>
          <span
            >零污染保证：未勾选的书源随弹窗关闭直接在内存中丢弃，绝不写入书库。</span
          >
        </div>
        <div class="footer-right-buttons">
          <el-button size="small" @click="handleCloseSandbox"
            >放弃并关闭</el-button
          >
          <el-button
            size="small"
            type="info"
            plain
            :loading="sandboxImporting"
            @click="confirmDirectImportAll"
          >
            直接入库全部 ({{ sandboxSources.length }}项)
          </el-button>
          <el-button
            type="success"
            size="small"
            icon="el-icon-check"
            :loading="sandboxImporting"
            :disabled="sandboxSelectedCount === 0"
            @click="confirmImportSelected"
          >
            确认导入已选 ({{ sandboxSelectedCount }}) 个优质源
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Axios from "../plugins/axios";

export default {
  name: "BookSourceManage",
  data() {
    return {
      bookSourceList: [],
      loadingSources: false,
      searchKeyword: "",
      statusFilter: "all",
      currentPage: 1,
      pageSize: 50,
      selectedSources: [],
      isTestingSpeed: false,
      testedCount: 0,
      showImportDialog: false,
      importTab: "file",
      importUrl: "",
      importContent: "",
      localFileName: "",
      localFileSources: [],
      importing: false,
      syncingDefault: false,
      // 沙箱相关状态
      showSandboxDialog: false,
      sandboxSources: [],
      sandboxSearchKeyword: "",
      sandboxStatusFilter: "all",
      sandboxCurrentPage: 1,
      sandboxPageSize: 20,
      isSandboxTesting: false,
      sandboxTestedCount: 0,
      sandboxShouldStopTest: false,
      sandboxImporting: false
    };
  },
  computed: {
    isNight() {
      return this.$store.getters.isNight;
    },
    currentUser() {
      return (
        (this.$store.state.userInfo && this.$store.state.userInfo.username) ||
        this.$store.state.username ||
        ""
      );
    },
    api() {
      return this.$store.state.api;
    },
    filteredSources() {
      const keyword = (this.searchKeyword || "").trim().toLowerCase();
      return this.bookSourceList.filter(item => {
        if (!item) return false;
        // 1. 关键字过滤
        if (keyword) {
          const name = (item.bookSourceName || "").toLowerCase();
          const url = (item.bookSourceUrl || "").toLowerCase();
          if (!name.includes(keyword) && !url.includes(keyword)) {
            return false;
          }
        }
        // 2. 状态过滤
        if (this.statusFilter === "green") {
          return item.speedStatus === "fast";
        }
        if (this.statusFilter === "yellow") {
          return item.speedStatus === "slow";
        }
        if (this.statusFilter === "red") {
          return item.speedStatus === "failed";
        }
        if (this.statusFilter === "enabled") {
          return item.enabled !== false;
        }
        if (this.statusFilter === "disabled") {
          return item.enabled === false;
        }
        return true;
      });
    },
    pagedSources() {
      const start = (this.currentPage - 1) * this.pageSize;
      return this.filteredSources.slice(start, start + this.pageSize);
    },
    greenSourcesCount() {
      return this.bookSourceList.filter(s => s.speedStatus === "fast").length;
    },
    yellowSourcesCount() {
      return this.bookSourceList.filter(s => s.speedStatus === "slow").length;
    },
    redSourcesCount() {
      return this.bookSourceList.filter(s => s.speedStatus === "failed").length;
    },
    sandboxFastCount() {
      return this.sandboxSources.filter(s => s.speedStatus === "fast").length;
    },
    sandboxSlowCount() {
      return this.sandboxSources.filter(s => s.speedStatus === "slow").length;
    },
    sandboxFailedCount() {
      return this.sandboxSources.filter(s => s.speedStatus === "failed").length;
    },
    sandboxPendingCount() {
      return this.sandboxSources.filter(s => !s.speedStatus).length;
    },
    sandboxSelectedCount() {
      return this.sandboxSources.filter(s => s._sandboxChecked).length;
    },
    sandboxProgressPercent() {
      if (this.sandboxSources.length === 0) return 0;
      return Math.min(
        100,
        Math.round((this.sandboxTestedCount / this.sandboxSources.length) * 100)
      );
    },
    isAllSandboxChecked: {
      get() {
        return (
          this.sandboxSources.length > 0 &&
          this.sandboxSources.every(s => s._sandboxChecked)
        );
      },
      set(val) {
        this.toggleAllSandboxCheck(val);
      }
    },
    isSandboxIndeterminate() {
      return (
        this.sandboxSelectedCount > 0 &&
        this.sandboxSelectedCount < this.sandboxSources.length
      );
    },
    filteredSandboxSources() {
      const keyword = (this.sandboxSearchKeyword || "").trim().toLowerCase();
      return this.sandboxSources.filter(item => {
        if (!item) return false;
        if (keyword) {
          const name = (item.bookSourceName || "").toLowerCase();
          const url = (item.bookSourceUrl || "").toLowerCase();
          if (!name.includes(keyword) && !url.includes(keyword)) {
            return false;
          }
        }
        if (this.sandboxStatusFilter === "green") {
          return item.speedStatus === "fast";
        }
        if (this.sandboxStatusFilter === "yellow") {
          return item.speedStatus === "slow";
        }
        if (this.sandboxStatusFilter === "red") {
          return item.speedStatus === "failed";
        }
        if (this.sandboxStatusFilter === "selected") {
          return !!item._sandboxChecked;
        }
        return true;
      });
    },
    pagedSandboxSources() {
      const start = (this.sandboxCurrentPage - 1) * this.sandboxPageSize;
      return this.filteredSandboxSources.slice(
        start,
        start + this.sandboxPageSize
      );
    }
  },
  mounted() {
    this.fetchSources();
  },
  methods: {
    goBack() {
      this.$router.push("/");
    },
    openImportSourceDialog(tab = "file") {
      this.importUrl = "";
      this.importContent = "";
      this.localFileName = "";
      this.localFileSources = [];
      this.importTab = typeof tab === "string" ? tab : "file";
      this.showImportDialog = true;
    },
    triggerSelectLocalFile() {
      if (this.$refs.localFileInput) {
        this.$refs.localFileInput.click();
      }
    },
    handleLocalFileChange(event) {
      const file = event.target.files && event.target.files[0];
      if (!file) return;
      this.processLocalFile(file);
    },
    handleLocalFileDrop(event) {
      const file =
        event.dataTransfer &&
        event.dataTransfer.files &&
        event.dataTransfer.files[0];
      if (!file) return;
      this.processLocalFile(file);
    },
    processLocalFile(file) {
      if (!file.name.match(/\.(json|txt)$/i)) {
        this.$message.warning("请上传 .json 或 .txt 格式的书源文件");
        return;
      }
      const reader = new FileReader();
      reader.onload = e => {
        try {
          let text = e.target.result;
          if (typeof text === "string") {
            text = text.replace(/^\uFEFF/, "").trim();
          }
          const parsed = JSON.parse(text);
          const list = this.extractSourceList(parsed);
          if (!list || list.length === 0) {
            this.$message.warning(
              "文件中未解析到有效的书源数据，请确认是否为有效书源"
            );
            return;
          }
          this.localFileSources = list;
          this.localFileName = file.name;
          this.$message.success(
            `成功解析「${file.name}」，包含 ${list.length} 个书源`
          );
        } catch (err) {
          this.$message.error(
            "文件解析失败，无法解析为 JSON: " + (err && err.message)
          );
        } finally {
          if (this.$refs.localFileInput) {
            this.$refs.localFileInput.value = "";
          }
        }
      };
      reader.onerror = () => {
        this.$message.error("读取本地文件失败");
        if (this.$refs.localFileInput) {
          this.$refs.localFileInput.value = "";
        }
      };
      reader.readAsText(file, "utf-8");
    },
    clearLocalFile() {
      this.localFileName = "";
      this.localFileSources = [];
      if (this.$refs.localFileInput) {
        this.$refs.localFileInput.value = "";
      }
    },
    cleanSourceForSave(source) {
      const item = Object.assign({}, source);
      delete item._sandboxChecked;
      delete item._sandboxUid;
      delete item.speedStatus;
      delete item.speedCost;
      delete item.speedError;
      delete item.isTesting;
      delete item.isSingleTesting;
      return item;
    },
    loadIntoSandbox(sources) {
      if (!sources || sources.length === 0) {
        this.$message.warning("未检测到有效书源数据");
        return;
      }
      this.sandboxSources = sources.map((s, idx) => ({
        ...s,
        speedStatus: null,
        speedCost: null,
        speedError: null,
        isTesting: false,
        _sandboxChecked: true,
        _sandboxUid: `sb_${Date.now()}_${idx}_${Math.random()
          .toString(36)
          .substr(2, 6)}`
      }));
      this.sandboxSearchKeyword = "";
      this.sandboxStatusFilter = "all";
      this.sandboxCurrentPage = 1;
      this.showImportDialog = false;
      this.showSandboxDialog = true;
      this.$nextTick(() => {
        this.startSandboxSpeedTest();
      });
    },
    importLocalFileToSandbox() {
      if (!this.localFileSources || this.localFileSources.length === 0) {
        this.$message.warning("请先选择本地书源文件");
        return;
      }
      const list = [...this.localFileSources];
      this.clearLocalFile();
      this.loadIntoSandbox(list);
    },
    async submitImportLocalFileDirect() {
      if (!this.localFileSources || this.localFileSources.length === 0) {
        this.$message.warning("请先选择本地书源文件");
        return;
      }
      this.importing = true;
      try {
        const success = await this.saveImportedSources(this.localFileSources);
        if (success) {
          this.$message.success(
            `成功直接导入 ${this.localFileSources.length} 个书源！`
          );
          this.showImportDialog = false;
          this.clearLocalFile();
        }
      } catch (err) {
        this.$message.error("导入书源失败: " + err.toString());
      } finally {
        this.importing = false;
      }
    },
    submitImportLocalFile() {
      return this.importLocalFileToSandbox();
    },
    async fetchRemoteSourcesList(url) {
      let sourceList = null;
      try {
        const fetchRes = await Axios.post(
          this.api + "/readRemoteSourceFile",
          { url },
          { silent: true }
        );
        if (
          fetchRes &&
          fetchRes.data &&
          fetchRes.data.isSuccess &&
          Array.isArray(fetchRes.data.data)
        ) {
          sourceList = fetchRes.data.data;
        }
      } catch (e) {
        // ignore
      }

      if (!sourceList) {
        const directRes = await Axios.get(url);
        sourceList = directRes.data;
      }
      return this.extractSourceList(sourceList);
    },
    async importUrlToSandbox() {
      const url = (this.importUrl || "").trim();
      if (!url) {
        this.$message.error("请输入远程书源 URL 地址");
        return;
      }
      this.importing = true;
      try {
        const list = await this.fetchRemoteSourcesList(url);
        if (!list || list.length === 0) {
          this.$message.warning("未从远程地址解析到有效书源");
          return;
        }
        this.importUrl = "";
        this.loadIntoSandbox(list);
      } catch (err) {
        this.$message.error("拉取远程书源失败: " + err.toString());
      } finally {
        this.importing = false;
      }
    },
    async submitImportUrlDirect() {
      const url = (this.importUrl || "").trim();
      if (!url) {
        this.$message.error("请输入远程书源 URL 地址");
        return;
      }
      this.importing = true;
      try {
        const list = await this.fetchRemoteSourcesList(url);
        if (!list || list.length === 0) {
          this.$message.warning("未从远程地址解析到有效书源");
          return;
        }
        const success = await this.saveImportedSources(list);
        if (success) {
          this.$message.success(`成功直接导入 ${list.length} 个远程书源！`);
          this.showImportDialog = false;
          this.importUrl = "";
        }
      } catch (err) {
        this.$message.error("拉取远程书源失败: " + err.toString());
      } finally {
        this.importing = false;
      }
    },
    submitImportUrl() {
      return this.importUrlToSandbox();
    },
    importTextToSandbox() {
      const content = (this.importContent || "").trim();
      if (!content) {
        this.$message.error("请粘贴书源 JSON 数据");
        return;
      }
      try {
        const data = JSON.parse(content);
        const list = this.extractSourceList(data);
        if (!list || list.length === 0) {
          this.$message.warning("未解析到有效书源数据");
          return;
        }
        this.importContent = "";
        this.loadIntoSandbox(list);
      } catch (err) {
        this.$message.error("解析书源 JSON 失败: " + err.toString());
      }
    },
    async submitImportTextDirect() {
      const content = (this.importContent || "").trim();
      if (!content) {
        this.$message.error("请粘贴书源 JSON 数据");
        return;
      }
      this.importing = true;
      try {
        const data = JSON.parse(content);
        const list = this.extractSourceList(data);
        if (!list || list.length === 0) {
          this.$message.warning("未解析到有效书源数据");
          return;
        }
        const success = await this.saveImportedSources(list);
        if (success) {
          this.$message.success(`成功直接导入 ${list.length} 个书源！`);
          this.showImportDialog = false;
          this.importContent = "";
        }
      } catch (err) {
        this.$message.error("解析书源 JSON 失败: " + err.toString());
      } finally {
        this.importing = false;
      }
    },
    submitImportText() {
      return this.importTextToSandbox();
    },
    async syncDefaultToSandbox() {
      this.syncingDefault = true;
      try {
        const res = await Axios.post(
          this.api + "/syncAdminBookSources?preview=1"
        );
        if (
          res &&
          res.data &&
          res.data.isSuccess &&
          Array.isArray(res.data.data)
        ) {
          const list = this.extractSourceList(res.data.data);
          if (!list || list.length === 0) {
            this.$message.warning("系统默认源中未包含有效书源");
            return;
          }
          this.loadIntoSandbox(list);
        } else {
          this.$message.error(
            (res && res.data && res.data.errorMsg) || "获取系统默认书源失败"
          );
        }
      } catch (err) {
        this.$message.error("获取系统默认书源失败: " + err.toString());
      } finally {
        this.syncingDefault = false;
      }
    },
    async submitSyncDefaultDirect() {
      this.syncingDefault = true;
      try {
        const res = await Axios.post(this.api + "/syncAdminBookSources");
        if (res && res.data && res.data.isSuccess) {
          this.$message.success("成功从系统默认书源同步！");
          this.showImportDialog = false;
          await this.fetchSources();
        } else {
          this.$message.error(
            (res && res.data && res.data.errorMsg) || "同步系统默认书源失败"
          );
        }
      } catch (err) {
        this.$message.error("同步系统默认书源失败: " + err.toString());
      } finally {
        this.syncingDefault = false;
      }
    },
    submitSyncDefault() {
      return this.submitSyncDefaultDirect();
    },
    async startSandboxSpeedTest(forceAll = false) {
      if (this.isSandboxTesting) return;
      if (!this.sandboxSources || this.sandboxSources.length === 0) return;

      this.isSandboxTesting = true;
      this.sandboxShouldStopTest = false;
      this.sandboxTestedCount = 0;

      if (forceAll) {
        this.sandboxSources.forEach(s => {
          s.speedStatus = null;
          s.speedCost = null;
          s.speedError = null;
          s._sandboxChecked = true;
        });
      }

      const sourcesToTest = [...this.sandboxSources];
      const concurrency = 8;
      let currentIndex = 0;

      const worker = async () => {
        while (currentIndex < sourcesToTest.length) {
          if (this.sandboxShouldStopTest) break;
          const idx = currentIndex++;
          const source = sourcesToTest[idx];
          if (!source || !source.bookSourceUrl) {
            this.sandboxTestedCount++;
            continue;
          }
          await this.testSingleSandboxSource(source);
          this.sandboxTestedCount++;
        }
      };

      const workers = [];
      for (let i = 0; i < Math.min(concurrency, sourcesToTest.length); i++) {
        workers.push(worker());
      }
      await Promise.all(workers);

      this.isSandboxTesting = false;
      if (!this.sandboxShouldStopTest) {
        const fast = this.sandboxFastCount;
        const slow = this.sandboxSlowCount;
        const failed = this.sandboxFailedCount;
        this.$message.success(
          `沙箱体检完成：共检测 ${
            sourcesToTest.length
          } 个书源，已自动勾选 ${fast +
            slow} 个可用源（🟢 ${fast} + 🟡 ${slow}），自动剔除 ${failed} 个失效死源（🔴）`
        );
      }
    },
    async testSingleSandboxSource(source) {
      this.$set(source, "isTesting", true);
      const start = Date.now();
      try {
        const res = await Axios.post(
          this.api + "/testBookSourceSpeed",
          { url: source.bookSourceUrl },
          { timeout: 5000, silent: true }
        );
        const cost =
          res.data && res.data.data && typeof res.data.data.cost === "number"
            ? res.data.data.cost
            : Date.now() - start;
        const isOk = res.data && res.data.data && res.data.data.status === "ok";

        if (isOk) {
          if (cost < 1500) {
            this.$set(source, "speedStatus", "fast");
            this.$set(source, "_sandboxChecked", true);
          } else if (cost <= 4000) {
            this.$set(source, "speedStatus", "slow");
            this.$set(source, "_sandboxChecked", true);
          } else {
            this.$set(source, "speedStatus", "failed");
            this.$set(source, "_sandboxChecked", false);
            this.$set(source, "speedError", "响应超时 (>4s)");
          }
          this.$set(source, "speedCost", cost);
          if (cost <= 4000) {
            this.$set(source, "speedError", null);
          }
        } else {
          this.$set(source, "speedStatus", "failed");
          this.$set(source, "speedCost", cost);
          this.$set(
            source,
            "speedError",
            (res.data && res.data.errorMsg) || "连接失败"
          );
          this.$set(source, "_sandboxChecked", false);
        }
      } catch (e) {
        const cost = Date.now() - start;
        this.$set(source, "speedStatus", "failed");
        this.$set(source, "speedCost", cost);
        this.$set(source, "speedError", "超时/失败");
        this.$set(source, "_sandboxChecked", false);
      } finally {
        this.$set(source, "isTesting", false);
      }
    },
    stopSandboxSpeedTest() {
      this.sandboxShouldStopTest = true;
      this.isSandboxTesting = false;
      this.$message.info("已停止沙箱体检测速");
    },
    toggleAllSandboxCheck(val) {
      this.sandboxSources.forEach(s => {
        this.$set(s, "_sandboxChecked", !!val);
      });
    },
    selectAllSandboxSources(val) {
      this.sandboxSources.forEach(s => {
        this.$set(s, "_sandboxChecked", !!val);
      });
    },
    invertSandboxSelection() {
      this.sandboxSources.forEach(s => {
        this.$set(s, "_sandboxChecked", !s._sandboxChecked);
      });
    },
    selectAvailableSandboxSources() {
      let count = 0;
      this.sandboxSources.forEach(s => {
        const isGood = s.speedStatus === "fast" || s.speedStatus === "slow";
        this.$set(s, "_sandboxChecked", isGood);
        if (isGood) count++;
      });
      this.$message.success(`已勾选全部可用源 (🟢+🟡 共 ${count} 个)`);
    },
    selectFastSandboxSources() {
      let count = 0;
      this.sandboxSources.forEach(s => {
        const isFast = s.speedStatus === "fast";
        this.$set(s, "_sandboxChecked", isFast);
        if (isFast) count++;
      });
      this.$message.success(`已勾选全部极速源 (🟢 共 ${count} 个)`);
    },
    removeSandboxSource(source) {
      const idx = this.sandboxSources.indexOf(source);
      if (idx !== -1) {
        this.sandboxSources.splice(idx, 1);
      }
    },
    async confirmImportSelected() {
      const selected = this.sandboxSources.filter(s => s._sandboxChecked);
      if (selected.length === 0) {
        this.$message.warning("请至少勾选一个要导入的书源");
        return;
      }
      this.sandboxImporting = true;
      const cleanList = selected.map(s => this.cleanSourceForSave(s));
      const discardedCount = this.sandboxSources.length - selected.length;
      try {
        const success = await this.saveImportedSources(cleanList);
        if (success) {
          if (discardedCount > 0) {
            this.$notify({
              title: "零死源入库完成",
              message: `已入库 ${cleanList.length} 个优质书源，其余 ${discardedCount} 个失效/未选书源已在内存中丢弃`,
              type: "success",
              duration: 4000
            });
          } else {
            this.$message.success(`成功导入 ${cleanList.length} 个优质书源！`);
          }
          this.sandboxSources = [];
          this.showSandboxDialog = false;
        }
      } catch (err) {
        this.$message.error("保存书源失败: " + err.toString());
      } finally {
        this.sandboxImporting = false;
      }
    },
    confirmDirectImportAll() {
      if (!this.sandboxSources || this.sandboxSources.length === 0) return;
      this.$confirm(
        `确定要将沙箱中的全部 ${this.sandboxSources.length} 个书源直接入库吗？（包含失效或未测速源）`,
        "直接全部入库确认",
        {
          confirmButtonText: "确定入库",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).then(async () => {
        this.sandboxImporting = true;
        const cleanList = this.sandboxSources.map(s =>
          this.cleanSourceForSave(s)
        );
        try {
          const success = await this.saveImportedSources(cleanList);
          if (success) {
            this.$message.success(
              `成功直接导入全部 ${cleanList.length} 个书源！`
            );
            this.sandboxSources = [];
            this.showSandboxDialog = false;
          }
        } catch (err) {
          this.$message.error("直接入库失败: " + err.toString());
        } finally {
          this.sandboxImporting = false;
        }
      });
    },
    handleCloseSandbox(done) {
      if (this.isSandboxTesting) {
        this.stopSandboxSpeedTest();
      }
      const count = this.sandboxSources.length;
      this.sandboxSources = [];
      this.showSandboxDialog = false;
      if (count > 0) {
        this.$message.info("沙箱已关闭，未保存的书源已在内存中丢弃");
      }
      if (typeof done === "function") {
        done();
      }
    },
    extractSourceList(data) {
      if (typeof data === "string") {
        try {
          data = JSON.parse(data.replace(/^\uFEFF/, "").trim());
        } catch (e) {
          return [];
        }
      }
      let list = [];
      if (Array.isArray(data)) {
        for (const item of data) {
          if (typeof item === "string") {
            try {
              const parsed = JSON.parse(item.replace(/^\uFEFF/, "").trim());
              list = list.concat(this.extractSourceList(parsed));
            } catch (e) {
              // 忽略非 JSON 数据行
            }
          } else if (item && typeof item === "object") {
            if (
              item.bookSourceName ||
              item.bookSourceUrl ||
              item.ruleSearch ||
              item.ruleBookInfo ||
              item.ruleToc
            ) {
              list.push(item);
            } else if (
              Array.isArray(item.data) ||
              Array.isArray(item.sources) ||
              Array.isArray(item.list)
            ) {
              list = list.concat(this.extractSourceList(item));
            }
          }
        }
      } else if (data && typeof data === "object") {
        if (Array.isArray(data.data)) {
          list = this.extractSourceList(data.data);
        } else if (Array.isArray(data.sources)) {
          list = this.extractSourceList(data.sources);
        } else if (Array.isArray(data.list)) {
          list = this.extractSourceList(data.list);
        } else if (
          data.bookSourceName ||
          data.bookSourceUrl ||
          data.ruleSearch ||
          data.ruleBookInfo ||
          data.ruleToc
        ) {
          list = [data];
        }
      }
      return list.map(v => {
        const item = Object.assign({}, v);
        if (item.headerMap) {
          if (!item.header) {
            item.header =
              typeof item.headerMap === "string"
                ? item.headerMap
                : JSON.stringify(item.headerMap);
          }
          delete item.headerMap;
        }
        return item;
      });
    },
    async saveImportedSources(data) {
      let list = this.extractSourceList(data);
      if (list.length === 0) {
        this.$message.warning("未检测到有效书源数据");
        return false;
      }
      try {
        const res = await Axios.post(this.api + "/saveBookSources", list);
        if (res && res.data && res.data.isSuccess) {
          await this.fetchSources();
          return true;
        } else {
          this.$message.error(
            (res && res.data && res.data.errorMsg) || "保存书源失败"
          );
          return false;
        }
      } catch (err) {
        this.$message.error("保存书源请求失败: " + err.toString());
        return false;
      }
    },
    async fetchSources() {
      this.loadingSources = true;
      try {
        const res = await Axios.get(this.api + "/getBookSources");
        if (res.data && res.data.isSuccess) {
          const list = res.data.data || [];
          // 保留已有测速状态
          const oldMap = {};
          this.bookSourceList.forEach(s => {
            if (s.bookSourceUrl) {
              oldMap[s.bookSourceUrl] = {
                speedStatus: s.speedStatus,
                speedCost: s.speedCost,
                speedError: s.speedError
              };
            }
          });
          this.bookSourceList = list.map(item => {
            const old = oldMap[item.bookSourceUrl] || {};
            return {
              ...item,
              speedStatus: old.speedStatus || null,
              speedCost: old.speedCost || null,
              speedError: old.speedError || null,
              isSingleTesting: false
            };
          });
        }
      } catch (err) {
        this.$message.error("获取书源列表失败: " + err.toString());
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
    handleSelectionChange(selection) {
      this.selectedSources = selection || [];
    },
    selectAll() {
      if (!this.$refs.sourceTable) return;
      this.pagedSources.forEach(row => {
        this.$refs.sourceTable.toggleRowSelection(row, true);
      });
    },
    invertSelection() {
      if (!this.$refs.sourceTable) return;
      this.pagedSources.forEach(row => {
        this.$refs.sourceTable.toggleRowSelection(row);
      });
    },
    clearSelection() {
      if (this.$refs.sourceTable) {
        this.$refs.sourceTable.clearSelection();
      }
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
        `确认删除书源 [${source.bookSourceName}] 吗？此操作不可逆。`,
        "确认删除",
        {
          confirmButtonText: "确定删除",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).then(async () => {
        try {
          await Axios.post(this.api + "/deleteBookSources", [source]);
          this.$message.success("删除书源成功");
          await this.fetchSources();
        } catch (err) {
          this.$message.error("删除书源失败: " + err.toString());
        }
      });
    },
    batchDeleteSources() {
      const count = this.selectedSources.length;
      if (count === 0) {
        this.$message.warning("请先勾选需要删除的书源");
        return;
      }
      this.$confirm(
        `确定要删除选中的 ${count} 个书源吗？此操作不可逆。`,
        "批量删除确认",
        {
          confirmButtonText: "确定删除",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).then(async () => {
        const loading = this.$loading({
          lock: true,
          text: `正在批量删除 ${count} 个书源...`,
          spinner: "el-icon-loading",
          background: "rgba(0, 0, 0, 0.7)"
        });
        try {
          await Axios.post(
            this.api + "/deleteBookSources",
            this.selectedSources
          );
          this.$message.success(`成功删除选中的 ${count} 个书源`);
          this.selectedSources = [];
          await this.fetchSources();
        } catch (err) {
          this.$message.error("批量删除失败: " + err.toString());
        } finally {
          loading.close();
        }
      });
    },
    async testSingleSource(source) {
      this.$set(source, "isSingleTesting", true);
      const start = Date.now();
      try {
        const res = await Axios.post(
          this.api + "/testBookSourceSpeed",
          { url: source.bookSourceUrl },
          { timeout: 5000, silent: true }
        );
        const cost =
          res.data && res.data.data && typeof res.data.data.cost === "number"
            ? res.data.data.cost
            : Date.now() - start;
        const isOk = res.data && res.data.data && res.data.data.status === "ok";

        if (isOk) {
          if (cost < 1500) {
            this.$set(source, "speedStatus", "fast");
          } else if (cost <= 4000) {
            this.$set(source, "speedStatus", "slow");
          } else {
            this.$set(source, "speedStatus", "failed");
          }
          this.$set(source, "speedCost", cost);
          this.$set(source, "speedError", null);
        } else {
          this.$set(source, "speedStatus", "failed");
          this.$set(source, "speedCost", cost);
          this.$set(
            source,
            "speedError",
            (res.data && res.data.errorMsg) || "异常"
          );
        }
      } catch (e) {
        const cost = Date.now() - start;
        this.$set(source, "speedStatus", "failed");
        this.$set(source, "speedCost", cost);
        this.$set(source, "speedError", "超时/失败");
      } finally {
        this.$set(source, "isSingleTesting", false);
      }
    },
    async startSpeedTest() {
      if (this.isTestingSpeed) return;
      if (this.bookSourceList.length === 0) {
        this.$message.info("暂无可测速书源");
        return;
      }
      this.isTestingSpeed = true;
      this.testedCount = 0;

      const sourcesToTest = [...this.bookSourceList];
      const concurrency = 8;
      let currentIndex = 0;

      const worker = async () => {
        while (currentIndex < sourcesToTest.length) {
          const idx = currentIndex++;
          const source = sourcesToTest[idx];
          if (!source || !source.bookSourceUrl) {
            this.testedCount++;
            continue;
          }
          await this.testSingleSource(source);
          this.testedCount++;
        }
      };

      const workers = [];
      for (let i = 0; i < Math.min(concurrency, sourcesToTest.length); i++) {
        workers.push(worker());
      }
      await Promise.all(workers);

      this.isTestingSpeed = false;
      this.$message.success(
        `体检测速完成：优质源 ${this.greenSourcesCount} 个，缓慢源 ${this.yellowSourcesCount} 个，失效源 ${this.redSourcesCount} 个`
      );
    },
    async prioritizeGreenSources() {
      if (this.greenSourcesCount === 0) {
        this.$message.warning(
          "当前暂无测速通过的优质源，请先进行一键体检/测速"
        );
        return;
      }
      // 将绿色优质源排序到前列，黄色次之，其余在后
      const green = [];
      const yellow = [];
      const others = [];

      this.bookSourceList.forEach(s => {
        if (s.speedStatus === "fast") green.push(s);
        else if (s.speedStatus === "slow") yellow.push(s);
        else others.push(s);
      });

      // 优质源内部按延迟升序
      green.sort((a, b) => (a.speedCost || 0) - (b.speedCost || 0));
      yellow.sort((a, b) => (a.speedCost || 0) - (b.speedCost || 0));

      const reorderedList = [...green, ...yellow, ...others];
      this.bookSourceList = reorderedList;

      try {
        await Axios.post(this.api + "/saveBookSources?sort=1", reorderedList);
        this.$message.success(
          `已成功置顶 ${green.length} 个优质书源并保存顺序`
        );
      } catch (err) {
        this.$message.error("保存书源排序失败: " + err.toString());
      }
    },
    async disableFailedSources() {
      if (this.redSourcesCount === 0) {
        this.$message.info("当前没有检测到失效书源");
        return;
      }
      this.$confirm(
        `检测到 ${this.redSourcesCount} 个失效或超时书源，确定要将它们全部一键禁用吗？`,
        "一键禁用失效源",
        {
          confirmButtonText: "确定禁用",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).then(async () => {
        const failedSources = this.bookSourceList.filter(
          s => s.speedStatus === "failed" && s.enabled !== false
        );
        if (failedSources.length === 0) {
          this.$message.info("所有失效书源此前已被禁用");
          return;
        }
        failedSources.forEach(s => {
          s.enabled = false;
        });
        try {
          await Axios.post(this.api + "/saveBookSources", failedSources);
          this.$message.success(
            `已成功禁用 ${failedSources.length} 个失效书源`
          );
        } catch (err) {
          this.$message.error("批量禁用失效源失败: " + err.toString());
        }
      });
    }
  }
};
</script>

<style lang="stylus" scoped>
.book-source-manage-page
  padding 20px
  max-width 1300px
  margin 0 auto
  background #f9fafb
  min-height 100vh

.page-header
  display flex
  justify-content space-between
  align-items center
  flex-wrap wrap
  gap 12px
  background #fff
  padding 16px 20px
  border-radius 8px
  box-shadow 0 1px 3px rgba(0, 0, 0, 0.05)
  margin-bottom 16px

.header-left
  display flex
  align-items center
  gap 12px

.page-title
  font-size 18px
  font-weight bold
  color #1f2937

.header-right
  display flex
  align-items center
  flex-wrap wrap
  gap 10px

.toolbar-section
  display flex
  justify-content space-between
  align-items center
  flex-wrap wrap
  gap 12px
  background #fff
  padding 14px 20px
  border-radius 8px
  box-shadow 0 1px 3px rgba(0, 0, 0, 0.05)
  margin-bottom 16px

.filter-left
  display flex
  align-items center
  flex-wrap wrap
  gap 12px

.search-input
  width 240px

.filter-right
  display flex
  align-items center
  gap 8px

.selected-summary
  font-size 13px
  color #4b5563
  margin-left 6px
  b
    color #f56c6c

.table-container
  background #fff
  padding 16px
  border-radius 8px
  box-shadow 0 1px 3px rgba(0, 0, 0, 0.05)

.source-name
  font-weight 600
  color #1f2937

.source-link
  color #3b82f6
  text-decoration none
  &:hover
    text-decoration underline

.pagination-section
  display flex
  justify-content flex-end
  margin-top 16px
  padding 12px 16px
  background #fff
  border-radius 8px
  box-shadow 0 1px 3px rgba(0, 0, 0, 0.05)

.local-import-dropzone
  border 2px dashed #dcdfe6
  border-radius 8px
  padding 28px 16px
  text-align center
  cursor pointer
  background #fafafa
  transition all .2s
  *
    pointer-events none
  &:hover
    border-color #409EFF
    background #f4f8ff

.import-tab-footer
  margin-top 18px
  display flex
  justify-content flex-end
  align-items center
  gap 8px
  flex-wrap wrap

.source-sandbox-dialog
  >>> .el-dialog
    border-radius 12px
    overflow hidden
  >>> .el-dialog__body
    padding 14px 20px

.sandbox-summary-banner
  background #f0f7ff
  border 1px solid #d0e7ff
  border-radius 8px
  padding 12px 16px
  margin-bottom 12px
  display flex
  justify-content space-between
  align-items center
  flex-wrap wrap
  gap 10px

.banner-info
  .banner-title
    font-size 15px
    font-weight bold
    color #1e3a8a
    display flex
    align-items center
    gap 6px
  .banner-desc
    font-size 12px
    color #475569
    margin-top 4px

.banner-badges
  display flex
  gap 6px
  align-items center
  flex-wrap wrap

.selected-badge
  font-weight bold

.sandbox-progress-section
  background #fafafa
  border 1px solid #ebeef5
  border-radius 6px
  padding 10px 14px
  margin-bottom 12px
  display flex
  align-items center
  gap 16px

.progress-bar-wrap
  flex 1

.progress-actions
  display flex
  align-items center
  gap 10px

.progress-status-text
  font-size 12px
  color #606266
  font-weight 500
  white-space nowrap

.sandbox-toolbar
  display flex
  justify-content space-between
  align-items center
  flex-wrap wrap
  gap 10px
  margin-bottom 12px

.sandbox-filter-left
  display flex
  align-items center
  gap 10px
  flex-wrap wrap

.sandbox-search-input
  width 210px

.sandbox-filter-right
  display flex
  align-items center
  gap 6px
  flex-wrap wrap

.sandbox-table-wrap
  border 1px solid #ebeef5
  border-radius 6px
  overflow hidden

.sandbox-source-name
  font-weight 600
  color #1f2937

.sandbox-pagination-section
  display flex
  justify-content flex-end
  margin-top 10px

.sandbox-dialog-footer
  display flex
  justify-content space-between
  align-items center
  flex-wrap wrap
  gap 12px
  width 100%

.footer-left-tip
  display flex
  align-items center
  gap 6px
  font-size 12px
  color #64748b
  i
    color #0284c7

.footer-right-buttons
  display flex
  align-items center
  gap 8px

/* 暗色主题适配 */
.theme-dark
  background #0f172a
  color #f1f5f9
  .page-header, .toolbar-section, .table-container, .pagination-section
    background #1e293b
    box-shadow 0 1px 3px rgba(0, 0, 0, 0.3)
    border 1px solid rgba(255, 255, 255, 0.05)
  .page-title
    color #f1f5f9
  .source-name
    color #f1f5f9
  .selected-summary
    color #94a3b8
  .local-import-dropzone
    background #1e293b
    border-color rgba(255, 255, 255, 0.15)
    &:hover
      border-color #3b82f6
      background #253349
    div
      color #e2e8f0 !important
  .sandbox-summary-banner
    background #0f172a
    border-color #1e3a8a
    .banner-title
      color #60a5fa
    .banner-desc
      color #94a3b8
  .sandbox-progress-section
    background #0f172a
    border-color rgba(255, 255, 255, 0.1)
    .progress-status-text
      color #cbd5e1
  .sandbox-source-name
    color #f1f5f9
  .sandbox-table-wrap
    border-color rgba(255, 255, 255, 0.08)
  .footer-left-tip
    color #94a3b8
    i
      color #38bdf8
  >>> .el-dialog
    background #1e293b
    color #f1f5f9
    .el-dialog__title
      color #f1f5f9
    .el-dialog__header
      border-bottom 1px solid rgba(255, 255, 255, 0.08)
    .el-dialog__footer
      border-top 1px solid rgba(255, 255, 255, 0.08)
  >>> .el-table
    background #1e293b
    color #e2e8f0
    th.el-table__cell, tr.el-table__row
      background #1e293b
      border-bottom-color rgba(255, 255, 255, 0.08)
    .el-table__row--striped td.el-table__cell
      background #162032
    td.el-table__cell
      border-bottom-color rgba(255, 255, 255, 0.08)
  >>> .el-pagination
    .el-pagination__total, .btn-prev, .btn-next, .el-pager li
      background #1e293b !important
      color #94a3b8 !important
      border 1px solid rgba(255, 255, 255, 0.08)
    .el-pager li.active
      color #fff !important
      background #3b82f6 !important
</style>
