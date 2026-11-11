<template>
  <div class="book-source-manage-page">
    <div class="page-header">
      <div class="header-left">
        <el-button
          icon="el-icon-back"
          size="small"
          circle
          @click="goBack"
          title="返回"
        ></el-button>
        <span class="page-title">书源管理与体检中心</span>
        <el-tag size="small" type="info" class="source-count-tag">
          共 {{ bookSourceList.length }} 个书源
        </el-tag>
      </div>
      <div class="header-right">
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
      testedCount: 0
    };
  },
  computed: {
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
    }
  },
  mounted() {
    this.fetchSources();
  },
  methods: {
    goBack() {
      if (window.history.length > 1) {
        this.$router.go(-1);
      } else {
        this.$router.push("/admin");
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
          this.fetchSources();
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
</style>
