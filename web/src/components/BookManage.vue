<template>
  <el-dialog
    title="书架管理"
    :visible.sync="show"
    :width="dialogWidth"
    :top="dialogTop"
    :fullscreen="$store.state.miniInterface"
    :class="
      isWebApp && !$store.getters.isNight ? 'status-bar-light-bg-dialog' : ''
    "
    v-if="$store.getters.isNormalPage"
    :before-close="cancel"
  >
    <div class="custom-dialog-title" slot="title">
      <span class="el-dialog__title"
        >书架管理
        <span class="float-right small-tip">❗️只能缓存文本内容</span>
      </span>
    </div>
    <div class="source-container table-container">
      <el-table
        :data="bookList"
        :height="dialogContentHeight"
        @selection-change="manageBookSelection = $event"
      >
        <el-table-column
          type="selection"
          width="25"
          :selectable="isBookSelectable"
          :fixed="$store.state.miniInterface"
        >
        </el-table-column>
        <el-table-column
          property="name"
          label="书名名"
          min-width="100"
          :fixed="$store.state.miniInterface"
        >
          <template slot-scope="scope">
            <el-button
              class="text-button"
              size="medium"
              type="text"
              @click="showBookInfo(scope.row)"
              >{{ scope.row.name }}</el-button
            >
          </template>
        </el-table-column>
        <el-table-column
          property="author"
          label="作者"
          min-width="100"
        ></el-table-column>
        <el-table-column label="分组" min-width="120">
          <template slot-scope="scope">
            {{ renderBookGroup(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="章节" min-width="120">
          <template slot-scope="scope">
            <span>共 {{ scope.row.totalChapterNum }} 章</span><br />
            <span v-if="scope.row.origin !== 'loc_book'">
              服务器缓存： {{ scope.row.cachedChapterCount || 0 }} 章 <br
            /></span>
            <span>浏览器缓存： {{ scope.row.localCacheCount }} 章</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140px">
          <template slot-scope="scope">
            <el-button
              class="text-button"
              size="medium"
              type="text"
              @click="editBook(scope.row)"
              >编辑</el-button
            >
            <el-button
              class="text-button"
              size="medium"
              type="text"
              style="margin-left: 0; color: #E6A23C;"
              title="在线从各书源匹配并修改封面"
              @click="openCoverPicker(scope.row)"
              >封面</el-button
            >
            <el-button
              class="text-button"
              size="medium"
              type="text"
              style="margin-left: 0"
              @click="setBookGroup(scope.row)"
              >分组</el-button
            >
            <el-dropdown @command="cacheBook(scope.row, $event)">
              <el-button class="text-button" type="text" size="medium">
                <span v-if="isCaching(scope.row)">
                  <i class="el-icon-loading"></i> 缓存中
                </span>
                <span v-else>
                  缓存<i class="el-icon-arrow-down el-icon--right"></i>
                </span>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item
                  v-if="scope.row.origin !== 'loc_book'"
                  command="cacheBookSSE"
                  >缓存到服务器</el-dropdown-item
                >
                <el-dropdown-item command="cacheBookLocal"
                  >缓存到浏览器</el-dropdown-item
                >
                <el-dropdown-item
                  v-if="scope.row.origin !== 'loc_book'"
                  command="deleteBookCache"
                  >删除服务器缓存</el-dropdown-item
                >
                <el-dropdown-item command="deleteBookLocalCache"
                  >删除浏览器缓存</el-dropdown-item
                >
              </el-dropdown-menu>
            </el-dropdown>
            <el-dropdown @command="exportBook(scope.row, $event)">
              <el-button class="text-button" type="text" size="medium">
                导出<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="txt">导出为TXT</el-dropdown-item>
                <el-dropdown-item command="epub">导出为Epub</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button
        type="primary"
        size="medium"
        class="float-left"
        @click="deleteBookList"
        >批量删除</el-button
      >
      <el-dropdown class="float-left" @command="addBookGroupMulti">
        <el-button type="primary" size="medium">
          批量添加分组<i class="el-icon-arrow-down el-icon--right"></i>
        </el-button>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item
            v-for="(bookGroup, index) in bookGroupList"
            :key="'bookGroup-' + index"
            :command="bookGroup"
            >{{ bookGroup.groupName }}</el-dropdown-item
          >
        </el-dropdown-menu>
      </el-dropdown>
      <el-dropdown class="float-left" @command="removeBookGroupMulti">
        <el-button type="primary" size="medium">
          批量移除分组<i class="el-icon-arrow-down el-icon--right"></i>
        </el-button>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item
            v-for="(bookGroup, index) in bookGroupList"
            :key="'bookGroup-' + index"
            :command="bookGroup"
            >{{ bookGroup.groupName }}</el-dropdown-item
          >
        </el-dropdown-menu>
      </el-dropdown>
      <span class="check-tip">已选择 {{ manageBookSelection.length }} 个</span>
      <el-button size="medium" @click="cancel">取消</el-button>
    </div>

    <!-- 弹窗：从在线书源选择并替换书封面 -->
    <el-dialog
      title="修改书籍封面"
      :visible.sync="showCoverPicker"
      width="560px"
      append-to-body
      :before-close="() => (showCoverPicker = false)"
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
          <span>在线书源封面候选项 (点击即可设为新封面)：</span>
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
            v-for="(cover, idx) in candidateCovers"
            :key="cover.url || idx"
            class="cover-card"
            :class="{
              active:
                currentCoverBook.customCoverUrl === cover.url ||
                currentCoverBook.coverUrl === cover.url
            }"
            @click="applyCover(cover.url)"
          >
            <img
              :src="getCover(cover.url, true)"
              referrerpolicy="no-referrer"
              class="cover-img"
              @error="onCoverImgError($event, cover)"
            />
            <div class="cover-source-tag" :title="cover.sourceName">
              {{ cover.sourceName }}
            </div>
          </div>
          <div
            v-if="!loadingCovers && candidateCovers.length === 0"
            style="grid-column: 1/-1; text-align: center; color: #999; padding: 20px;"
          >
            暂未从书源匹配到高清封面，您可以直接在上方输入图片链接
          </div>
        </div>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script>
import { mapGetters } from "vuex";
import Axios from "../plugins/axios";
import eventBus from "../plugins/eventBus";
const buildURL = require("axios/lib/helpers/buildURL");
import { LimitResquest } from "../plugins/helper";

export default {
  model: {
    prop: "show",
    event: "setShow"
  },
  name: "BookManage",
  data() {
    return {
      bookList: [],
      manageBookSelection: [],
      showCoverPicker: false,
      currentCoverBook: null,
      customCoverInput: "",
      loadingCovers: false,
      candidateCovers: []
    };
  },
  props: ["show"],
  computed: {
    ...mapGetters(["dialogWidth", "dialogTop", "dialogContentHeight"]),
    bookGroupList() {
      return this.$store.state.bookGroupList.filter(v => v.groupId > 0);
    },
    cachingBookList: {
      get() {
        return this.$store.state.cachingBookList;
      },
      set(val) {
        this.$store.commit("setCachingBookList", val);
      }
    },
    cachingBookMap() {
      const map = {};
      this.cachingBookList.map(v => {
        map[v.bookUrl] = true;
      });
      return map;
    }
  },
  created() {
    window.cacheEventSource = window.cacheEventSource || {};
    window.cacheRequestHandle = window.cacheRequestHandle || {};
    window.bookManageComp = this;
  },
  watch: {
    show(isVisible) {
      if (isVisible) {
        this.loadBookCacheInfo();
      } else {
        this.manageBookSelection = [];
      }
    }
  },
  methods: {
    cancel() {
      this.$emit("setShow", false);
    },
    formatTableField(row, column, cellValue) {
      switch (column.property) {
        default:
          return cellValue;
      }
    },
    isBookSelectable() {
      return true;
    },
    async loadBookCacheInfo() {
      return Axios.get(this.api + "/getShelfBookWithCacheInfo").then(
        res => {
          if (res.data.isSuccess) {
            this.computeCachedCata(res.data.data).then(v => {
              this.bookList = v;
            });
          }
        },
        error => {
          this.$message.error(
            "获取书架信息失败 " + (error && error.toString())
          );
        }
      );
    },
    async deleteBookList() {
      if (!this.manageBookSelection.length) {
        this.$message.error("请选择需要删除的书籍");
        return;
      }
      const res = await this.$confirm("确认要删除所选择的书籍吗?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      Axios.post(this.api + "/deleteBooks", this.manageBookSelection).then(
        res => {
          if (res.data.isSuccess) {
            this.manageBookSelection = [];
            this.$message.success("删除书籍成功");
            this.loadBookCacheInfo();
            this.$root.$children[0].loadBookShelf();
          }
        },
        error => {
          this.$message.error("删除书籍失败 " + (error && error.toString()));
        }
      );
    },
    async addBookGroupMulti(bookGroup) {
      return this.operateBookGroupMulti(bookGroup, true);
    },
    async removeBookGroupMulti(bookGroup) {
      return this.operateBookGroupMulti(bookGroup);
    },
    async operateBookGroupMulti(bookGroup, isAdd) {
      const operate = isAdd ? "添加" : "移除";
      if (!this.manageBookSelection.length) {
        this.$message.error("请选择需要" + operate + "分组的书籍");
        return;
      }
      const res = await this.$confirm(
        isAdd
          ? `确认要将所选择的书籍添加到${bookGroup.groupName}分组吗?`
          : `确认要将所选择的书籍从${bookGroup.groupName}分组中移除吗?`,
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      Axios.post(
        this.api + (isAdd ? "/addBookGroupMulti" : "/removeBookGroupMulti"),
        {
          groupId: bookGroup.groupId,
          bookList: this.manageBookSelection
        }
      ).then(
        res => {
          if (res.data.isSuccess) {
            this.$message.success("操作成功");
            this.loadBookCacheInfo();
            this.$root.$children[0].loadBookShelf();
          }
        },
        error => {
          this.$message.error("操作失败 " + (error && error.toString()));
        }
      );
    },
    renderBookGroup(book) {
      const groups = [];
      this.$store.state.bookGroupList.forEach(v => {
        if (v.groupId > 0 && (v.groupId & book.group) !== 0) {
          groups.push(v.groupName);
        }
      });
      return groups.join(" ");
    },
    showBookInfo(book) {
      eventBus.$emit("showBookInfoDialog", book);
    },
    editBook(book) {
      eventBus.$emit("editBook", book, false, () => {
        this.loadBookCacheInfo();
      });
    },
    openCoverPicker(book) {
      this.currentCoverBook = book;
      this.customCoverInput = book.customCoverUrl || book.coverUrl || "";
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

      const seen = new Set();
      const currentCover = book.customCoverUrl || book.coverUrl;
      if (currentCover) {
        seen.add(currentCover);
        this.candidateCovers.push({
          url: currentCover,
          coverUrl: currentCover,
          sourceName: "当前封面",
          originName: "当前封面"
        });
      }
      if (book.coverUrl && !seen.has(book.coverUrl)) {
        seen.add(book.coverUrl);
        this.candidateCovers.push({
          url: book.coverUrl,
          coverUrl: book.coverUrl,
          sourceName: book.originName || "原书源封面",
          originName: book.originName || "原书源封面"
        });
      }

      // 1. 先尝试快速读取后端已缓存的书源
      if (book.bookUrl) {
        Axios.post(this.api + "/getAvailableBookSource", {
          url: book.bookUrl,
          refresh: 0
        })
          .then(res => {
            if (
              res.data &&
              res.data.isSuccess &&
              Array.isArray(res.data.data)
            ) {
              for (const item of res.data.data) {
                if (
                  item.coverUrl &&
                  item.coverUrl.startsWith("http") &&
                  !seen.has(item.coverUrl)
                ) {
                  seen.add(item.coverUrl);
                  const name = item.originName || item.origin || "本地源";
                  this.candidateCovers.push({
                    url: item.coverUrl,
                    coverUrl: item.coverUrl,
                    sourceName: name,
                    originName: name
                  });
                  if (this.candidateCovers.length >= 10) {
                    stopSSE();
                    break;
                  }
                }
              }
            }
          })
          .catch(() => {});
      }

      if (this.candidateCovers.length >= 10) {
        this.loadingCovers = false;
        return;
      }

      // 2. 启动 SSE 在线全网书源实时扫描拉取最新封面
      const buildURL = require("axios/lib/helpers/buildURL");
      const params = {
        name: book.name,
        author: book.author || "",
        url: book.bookUrl || "",
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
              for (const item of res.data) {
                if (
                  item.coverUrl &&
                  item.coverUrl.startsWith("http") &&
                  !seen.has(item.coverUrl)
                ) {
                  seen.add(item.coverUrl);
                  const name = item.originName || item.origin || "在线书源";
                  this.candidateCovers.push({
                    url: item.coverUrl,
                    coverUrl: item.coverUrl,
                    sourceName: name,
                    originName: name
                  });
                  if (this.candidateCovers.length >= 10) {
                    stopSSE();
                    break;
                  }
                }
              }
            }
          } catch (err) {
            // ignore
          }
        });
        es.addEventListener("end", stopSSE);
        es.addEventListener("error", stopSSE);
        setTimeout(stopSSE, 10000);
      } catch (err) {
        this.loadingCovers = false;
      }
    },
    onCoverImgError(e) {
      if (e && e.target) {
        // 防止由于外部图床防盗链或链接失效导致卡片闪烁消失；优雅降级为本地默认封面，保持卡片和书源名称可见
        e.target.src = require("@/assets/imgs/noCover.jpeg");
      }
    },
    applyCover(coverUrl) {
      if (!coverUrl) {
        this.$message.error("请选择或输入封面链接");
        return;
      }
      const updatedBook = Object.assign({}, this.currentCoverBook, {
        customCoverUrl: coverUrl,
        coverUrl: coverUrl
      });
      Axios.post(this.api + "/saveBook", updatedBook).then(
        res => {
          if (res.data.isSuccess) {
            this.$message.success("书籍封面已更新成功！");
            this.showCoverPicker = false;
            this.currentCoverBook.customCoverUrl = coverUrl;
            this.currentCoverBook.coverUrl = coverUrl;
            this.loadBookCacheInfo();
            this.$root.$children[0].loadBookShelf();
          } else {
            this.$message.error(res.data.errorMsg || "保存封面失败");
          }
        },
        err => {
          this.$message.error("保存封面异常: " + err.toString());
        }
      );
    },
    setBookGroup(book) {
      this.$store.commit("setShowBookInfo", book);
      eventBus.$emit("showBookGroupDialog", true);
    },
    isCaching(book) {
      if (this.cachingBookMap[book.bookUrl]) return true;
      if (window.cacheEventSource && window.cacheEventSource[book.bookUrl])
        return true;
      if (window.cacheRequestHandle && window.cacheRequestHandle[book.bookUrl])
        return true;
      return false;
    },
    cacheBook(book, command) {
      this[command](book);
    },
    async cacheBookSSE(book) {
      const tryClose = () => {
        try {
          if (
            window.cacheEventSource[book.bookUrl] &&
            window.cacheEventSource[book.bookUrl].readyState !=
              window.cacheEventSource[book.bookUrl].CLOSED
          ) {
            window.cacheEventSource[book.bookUrl].close();
          }
          window.cacheEventSource[book.bookUrl] = null;
          delete window.cacheEventSource[book.bookUrl];
          const index = this.cachingBookList.findIndex(
            v => v.bookUrl === book.bookUrl
          );
          this.cachingBookList.splice(index, 1);
          this.cachingBookList = [].concat(this.cachingBookList);
        } catch (error) {
          //
        }
      };
      if (this.isCaching(book)) {
        // 取消缓存
        this.$message.info("已取消缓存");
        if (window.cacheEventSource[book.bookUrl]) {
          tryClose();
        }
        return;
      }

      const params = {
        url: book.bookUrl,
        refresh: 0
      };

      const url = buildURL(this.api + "/cacheBookSSE", params);

      tryClose();

      this.cachingBookList = this.cachingBookList.concat([book]);
      window.cacheEventSource[book.bookUrl] = new EventSource(url, {
        withCredentials: true
      });
      window.cacheEventSource[book.bookUrl].addEventListener("error", e => {
        tryClose();
        try {
          if (e.data) {
            const result = JSON.parse(e.data);
            if (result && result.errorMsg) {
              this.$message.error(result.errorMsg);
            }
          }
        } catch (error) {
          //
        }
      });
      window.cacheEventSource[book.bookUrl].addEventListener("end", e => {
        this.$message.info(book.name + "缓存到服务器完成");
        tryClose();
        try {
          if (e.data) {
            // const result = JSON.parse(e.data);
            // console.log(result);
          }
        } catch (error) {
          //
        }
      });
      window.cacheEventSource[book.bookUrl].addEventListener("message", e => {
        try {
          if (e.data) {
            const result = JSON.parse(e.data);
            if (result && result.cachedCount) {
              const index = this.bookList.findIndex(
                v => v.bookUrl === book.bookUrl
              );
              this.$set(this.bookList, index, {
                ...book,
                cachedChapterCount: result.cachedCount
              });
            }
          }
        } catch (error) {
          //
        }
      });
    },
    cacheBookLocal(book) {
      if (this.isCaching(book)) {
        // 取消缓存
        this.$message.info("已取消缓存");
        if (window.cacheRequestHandle[book.bookUrl]) {
          window.cacheRequestHandle[book.bookUrl].cancel();
        }
        return;
      }
      let isComputing = false;
      const computeCache = () => {
        this.computeCachedCata([book]).then(bookList => {
          isComputing = false;
          const index = this.bookList.findIndex(
            v => v.bookUrl === book.bookUrl
          );
          this.$set(this.bookList, index, {
            ...book,
            localCacheCount: bookList[0].localCacheCount
          });
        });
      };
      this.cachingHandler = LimitResquest(2, handler => {
        if (!isComputing) {
          isComputing = true;
          computeCache();
        }
        if (handler.isEnd()) {
          this.$message.success("缓存到浏览器完成");
          computeCache();
        }
      });
      for (let i = 0; i < book.totalChapterNum; i++) {
        this.cachingHandler(() => {
          return this.$root.$children[0].getBookContent(
            i,
            {
              timeout: 30000,
              silent: true
            },
            false,
            true,
            book
          );
        });
      }
    },
    exportBook(book, type) {
      const url = buildURL(this.api + "/exportBook", {
        url: book.bookUrl,
        isEpub: type === "epub" ? 1 : 0
      });

      window.open(url, "_target");
    },
    computeCachedCata(bookList, returnCacheMap) {
      const cachePrefixMap = {};
      bookList.forEach(book => {
        cachePrefixMap[book.bookUrl] = {
          key:
            "localCache@" +
            book.name +
            "_" +
            book.author +
            "@" +
            book.bookUrl +
            "@chapterContent-",
          map: {}
        };
      });
      return window.$cacheStorage
        .iterate(function(value, key) {
          for (const bookUrl in cachePrefixMap) {
            if (key.startsWith(cachePrefixMap[bookUrl].key)) {
              try {
                let index = parseInt(
                  key.replace(cachePrefixMap[bookUrl].key, "")
                );
                cachePrefixMap[bookUrl].map[index] = true;
              } catch (error) {
                //
                // console.error(error);
              }
              break;
            }
          }
        })
        .then(() => {
          if (returnCacheMap) return cachePrefixMap;
          return bookList.map(v => {
            const cacheMap = cachePrefixMap[v.bookUrl].map;
            v.localCacheCount = Object.keys(cacheMap).length;
            return v;
          });
        })
        .catch(function() {
          if (returnCacheMap) return cachePrefixMap;
          return bookList.map(v => {
            v.localCacheCount = 0;
            return v;
          });
        });
    },
    async deleteBookCache(book) {
      const res = await this.$confirm(
        `确认要删除服务器上《${book.name}》的缓存章节吗?`,
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      Axios.post(this.api + "/deleteBookCache", {
        bookUrl: book.bookUrl
      }).then(
        res => {
          if (res.data.isSuccess) {
            this.$message.success("删除服务器缓存成功");
            this.loadBookCacheInfo();
          }
        },
        error => {
          this.$message.error(
            "删除服务器缓存失败 " + (error && error.toString())
          );
        }
      );
    },
    async deleteBookLocalCache(book) {
      const res = await this.$confirm(
        `确认要删除浏览器中《${book.name}》的缓存章节吗?`,
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      this.computeCachedCata([book], true)
        .then(list => {
          const op = [];
          for (const index in list[book.bookUrl].map) {
            const cacheKey = list[book.bookUrl].key + index;
            op.push(window.$cacheStorage.removeItem(cacheKey));
          }
          return Promise.all(op);
        })
        .then(() => {
          this.$message.success("删除浏览器缓存成功");
          this.computeCachedCata([].concat(this.bookList)).then(v => {
            this.bookList = v;
          });
        });
    }
  }
};
</script>
<style lang="stylus" scoped>
.float-right {
  float: right;
}
.small-tip {
  font-size: 14px;
  margin-right: 10px;
}
.dialog-footer {
  .float-left {
    margin-right: 5px;
  }
}
.source-container {
  .text-button {
    padding: 3px 5px;
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

  &:hover {
    border-color: #409EFF;
    transform: translateY(-2px);
    box-shadow: 0 4px 10px rgba(0,0,0,0.1);
  }
  &.active {
    border-color: #67C23A;
    background: rgba(103, 194, 58, 0.08);
  }
  .cover-img {
    width: 100%;
    height: 130px;
    object-fit: cover;
    border-radius: 4px;
    display: block;
  }
  .cover-source-tag {
    font-size: 11px;
    color: #666;
    margin-top: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
