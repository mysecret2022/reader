<template>
  <div
    class="chapter-wrapper"
    :style="bodyTheme"
    :class="{
      night: isNight,
      day: !isNight,
      'mini-interface': $store.state.miniInterface,
      'suppress-callout':
        $store.getters.config &&
        $store.getters.config.selectionAction === '操作弹窗'
    }"
    ref="chapterWrapperRef"
  >
    <div class="tool-bar" :style="leftBarTheme">
      <div class="tools">
        <el-popover
          placement="right"
          :width="popperWidth"
          trigger="click"
          :visible-arrow="false"
          v-model="popBookShelfVisible"
          popper-class="popper-component"
        >
          <BookShelf
            ref="popBookShelf"
            class="popup"
            :visible="popBookShelfVisible"
            @changeBook="changeBook"
            @toShelf="toShelf"
          />
          <div class="tool-icon" slot="reference">
            <div class="iconfont">
              &#58892;
            </div>
            <div class="icon-text">书架</div>
          </div>
        </el-popover>
        <el-popover
          placement="right"
          :width="popperWidth"
          trigger="click"
          :visible-arrow="false"
          v-model="popBookSourceVisible"
          popper-class="popper-component"
        >
          <BookSource
            ref="popBookSource"
            class="popup"
            :visible="popBookSourceVisible"
            @changeBookSource="changeBookSource()"
            @close="popBookSourceVisible = false"
          />

          <div class="tool-icon" slot="reference">
            <div class="tool-el-icon">
              <i class="el-icon-menu"></i>
            </div>
            <div class="icon-text">书源</div>
          </div>
        </el-popover>
        <el-popover
          placement="right"
          :width="popperWidth"
          trigger="click"
          :visible-arrow="false"
          v-model="popCataVisible"
          popper-class="popper-component"
        >
          <PopCata
            @getContent="getContent"
            ref="popCata"
            class="popup"
            @refresh="refreshCatalog"
            :visible="popCataVisible"
            @close="popCataVisible = false"
            @openCache="openOfflineCacheFromCatalog"
          />

          <div class="tool-icon" slot="reference">
            <div class="iconfont">
              &#58905;
            </div>
            <div class="icon-text">目录</div>
          </div>
        </el-popover>
        <el-popover
          placement="right"
          :width="popperWidth"
          trigger="click"
          :visible-arrow="false"
          v-model="readSettingsVisible"
          popper-class="popper-component"
        >
          <ReadSettings
            class="popup"
            :visible="readSettingsVisible"
            @close="readSettingsVisible = false"
            @showClickZone="showClickZone = true"
            @readMethodChange="beforeReadMethodChange"
          />

          <div class="tool-icon" slot="reference">
            <div class="iconfont">
              &#58971;
            </div>
            <div class="icon-text">设置</div>
          </div>
        </el-popover>
        <div
          class="tool-icon"
          @click="toShelf"
          :style="$store.state.miniInterface ? { order: -1 } : {}"
        >
          <div class="iconfont">
            &#58920;
          </div>
          <div class="icon-text">首页</div>
        </div>
        <div
          class="tool-icon"
          @click="toTop(0)"
          v-if="!$store.state.miniInterface"
        >
          <div class="iconfont">
            &#58914;
          </div>
          <div class="icon-text">顶部</div>
        </div>
        <div
          class="tool-icon"
          @click="toBottom(0)"
          v-if="!$store.state.miniInterface"
        >
          <div class="iconfont">
            &#58915;
          </div>
          <div class="icon-text">底部</div>
        </div>
      </div>
    </div>
    <div class="read-bar" :style="rightBarTheme">
      <div class="float-btn-zone">
        <div class="float-left-btn-zone">
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="showBookmarkDialog"
          >
            <i class="el-icon-collection-tag"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="showSearchBookContentDialog"
          >
            <i class="el-icon-search"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="showReadingBookInfo"
          >
            <i class="el-icon-info"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="toTop(0)"
            v-if="$store.state.miniInterface"
          >
            <i class="el-icon-top"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="toBottom(0)"
            v-if="$store.state.miniInterface"
          >
            <i class="el-icon-bottom"></i>
          </div>
        </div>
        <div class="float-right-btn-zone">
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="refreshContent"
          >
            <i class="el-icon-refresh-right"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="toggleAutoReading()"
            v-if="!isEpub && !isCarToon && !isAudio"
          >
            <i class="el-icon-view"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="showReadBar = !showReadBar"
            v-if="speechAvalable && !isEpub && !isCarToon && !isAudio"
          >
            <i class="el-icon-headset"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            title="离线缓存"
            @click="openOfflineCacheDialog"
            v-if="!isEpub && !isCarToon && !isAudio"
          >
            <i class="el-icon-download"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="toogleNight"
          >
            <i class="el-icon-moon" v-if="!isNight"></i>
            <i class="el-icon-sunny" v-else></i>
          </div>
        </div>
      </div>
      <div class="progress" v-if="$store.state.miniInterface && !isAudio">
        <div class="progress-bar">
          <el-slider
            v-model="currentPage"
            :min="1"
            :max="totalPages"
            :show-tooltip="false"
            @change="showPage"
            @input="progressValue = $event"
          ></el-slider>
        </div>
        <span class="progress-tip">{{ formatProgressTip() }}</span>
      </div>
      <div class="tools">
        <div class="tool-icon progress-text" @click="openOfflineCacheDialog">
          <span v-if="$store.state.miniInterface">阅读进度: </span>
          {{ readingProgress }}
          <i
            class="el-icon-download"
            style="margin-left: 4px; font-size: 13px;"
            title="离线缓存"
          ></i>
        </div>
        <div
          class="tool-icon"
          @click="toLastChapter()"
          :style="$store.state.miniInterface ? { order: -1 } : {}"
        >
          <div class="iconfont">
            &#58920;
          </div>
          <span v-if="$store.state.miniInterface">上一章</span>
        </div>
        <div class="tool-icon" @click="toNextChapter()">
          <span v-if="$store.state.miniInterface">下一章</span>
          <div class="iconfont">
            &#58913;
          </div>
        </div>
      </div>
    </div>
    <div class="read-bar" :style="readBarTheme">
      <div class="reader-bar-inner">
        <div class="operate-bar">
          <div class="close-btn" @click="exitRead">
            <i class="el-icon-close"></i>
          </div>
          <div class="center">
            <span class="ctrl-btn" @click="speechPrev">上一段</span>
            <span class="play-pause-btn" @click="toggleSpeech">
              <i
                class="el-icon-video-pause"
                :style="popupAbsoluteBtnStyle"
                v-if="speechSpeaking"
              ></i>
              <i
                class="el-icon-video-play"
                :style="popupAbsoluteBtnStyle"
                v-else
              ></i>
            </span>
            <span class="ctrl-btn" @click="speechNext">下一段</span>
          </div>
          <div
            class="collapse-btn"
            @click="showSpeechConfig = !showSpeechConfig"
          >
            <i class="el-icon-bottom" v-if="showSpeechConfig"></i>
            <i class="el-icon-top" v-else></i>
          </div>
        </div>
        <div class="setting-item" v-if="showSpeechConfig">
          <div class="setting-title">语音库</div>
          <div class="setting-value">
            <div class="voice-list">
              <el-radio-group
                v-model="voiceName"
                size="small"
                class="radio-group"
              >
                <el-radio-button
                  class="radio-button"
                  :label="voice.name"
                  :key="index"
                  v-for="(voice, index) in voiceList"
                ></el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </div>
        <div class="setting-item" v-if="showSpeechConfig">
          <div class="setting-title">语音设置</div>
          <div class="setting-value">
            <div class="progress">
              <span class="progress-tip">语速</span>
              <div class="progress-bar">
                <el-slider
                  v-model="speechRate"
                  :min="0.5"
                  :max="2"
                  :step="0.1"
                  :show-tooltip="false"
                  @change="changeSpeechRate"
                ></el-slider>
              </div>
              <span class="setting-btn" @click="changeSpeechRate(1)">重置</span>
            </div>
            <div class="progress">
              <span class="progress-tip">语调</span>
              <div class="progress-bar">
                <el-slider
                  v-model="speechPitch"
                  :min="0"
                  :max="2"
                  :step="0.1"
                  :show-tooltip="false"
                  @change="changeSpeechPitch"
                ></el-slider>
              </div>
              <span class="setting-btn" @click="changeSpeechPitch(1)"
                >重置</span
              >
            </div>
            <div class="progress">
              <span class="progress-tip">定时</span>
              <div class="progress-bar">
                <el-slider
                  v-model="speechMinutes"
                  :min="0"
                  :max="180"
                  :step="1"
                  :show-tooltip="false"
                  @change="changeSpeechMinutes"
                ></el-slider>
              </div>
              <span class="setting-btn">{{ speechMinutes }}分钟</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      class="chapter"
      ref="content"
      :class="chapterClass"
      :style="chapterTheme"
    >
      <div
        class="click-zone"
        v-if="showClickZone"
        :style="!isSlideRead ? { position: 'fixed' } : {}"
      >
        <div :style="showPrevPageStyle"><span>点击前一页</span></div>
        <div :style="showMenuZoneStyle"><span>点击显示菜单</span></div>
        <div :style="showNextPageStyle"><span>点击后一页</span></div>
        <div class="close-btn" @click="showClickZone = false">关闭</div>
      </div>
      <div class="top-bar" ref="top">
        {{ $store.state.miniInterface ? title : "" }}
      </div>
      <div
        class="content"
        ref="contentRef"
        @touchstart="handleTouchStart"
        @touchmove="handleTouchMove"
        @touchend="handleTouchEnd"
        @touchcancel="handleTouchCancel"
        @mousedown="handleContentMouseDown"
        @mouseup="handleMouseUp"
        @click="handlerClick"
        @contextmenu="handleContextMenu"
      >
        <div class="content-inner" v-if="show">
          <Content
            class="book-content"
            :title="title"
            :content="content"
            :showContent="show"
            :error="error"
            :style="contentStyle"
            :showChapterList="showChapterList"
            :isScrollRead="isScrollRead"
            ref="bookContentRef"
            @prevChapter="toLastChapter"
            @nextChapter="toNextChapter"
            @updateProgress="saveReadingPosition"
            @iframeLoad="$emit('iframeLoad')"
            @contentChange="computePages()"
            @epubClick="eventHandler"
            @epubLocationChange="epubLocationChangeHandler"
            @epubClickHash="epubClickHash"
            @epubKeydown="keydownHandler($event, true)"
            @refreshContent="refreshContent"
            @changeBookSource="popBookSourceVisible = true"
          />
        </div>
        <!-- 仿真翻页 3D 纸张拟真翻页图层 -->
        <div class="simulation-flip-container" v-if="isSimulationFlipping">
          <div class="simulation-page-sheet" :style="sheetTransformStyle">
            <div class="sheet-face sheet-front" :style="sheetFrontStyle">
              <div
                class="sheet-content"
                :style="{
                  transform: `translateX(${flipperOffset}px)`
                }"
                v-html="flipperHTML"
              ></div>
              <div
                class="sheet-shadow"
                :style="{
                  opacity: sheetShadowOpacity,
                  background: isNight
                    ? 'linear-gradient(to right, rgba(0,0,0,0) 20%, rgba(0,0,0,0.85) 100%)'
                    : 'linear-gradient(to right, rgba(0,0,0,0) 20%, rgba(0,0,0,0.38) 100%)'
                }"
              ></div>
            </div>
            <div class="sheet-face sheet-back" :style="sheetBackStyle">
              <div
                class="sheet-shadow"
                :style="{
                  opacity: sheetShadowOpacity,
                  background: isNight
                    ? 'linear-gradient(to left, rgba(0,0,0,0) 20%, rgba(0,0,0,0.85) 100%)'
                    : 'linear-gradient(to left, rgba(0,0,0,0) 20%, rgba(0,0,0,0.38) 100%)'
                }"
              ></div>
            </div>
          </div>
          <div
            class="under-page-shadow"
            :style="{
              opacity: underShadowOpacity,
              background: isNight
                ? 'linear-gradient(to right, rgba(0,0,0,0.7) 0%, rgba(0,0,0,0) 100%)'
                : 'linear-gradient(to right, rgba(0,0,0,0.25) 0%, rgba(0,0,0,0) 100%)'
            }"
          ></div>
        </div>
      </div>
      <div class="bottom-bar" ref="bottom">
        <span v-if="isSlideRead">{{
          `第${currentPage}/${totalPages}页 ${readingProgress}`
        }}</span>
        <span v-if="isSlideRead">{{ timeStr }}</span>
        <span
          class="bottom-btn"
          v-if="show && !isSlideRead && !error && !isScrollRead"
          @click="toNextChapter()"
          >加载下一章</span
        >
      </div>

      <!-- 仿安卓侧滑返回浮标指示器 (左/右全屏边缘手势) -->
      <transition name="edge-back-indicator">
        <div
          class="edge-back-bubble"
          :class="edgeSwipeDirection"
          v-if="edgeBackIndicatorVisible"
          :style="{
            top: edgeBackIndicatorY + 'px',
            transform: `translateY(-50%) scale(${edgeBackScale})`
          }"
        >
          <div
            class="edge-back-arrow"
            :class="{ active: edgeBackMoveDistance >= 60 }"
          >
            <i
              :class="
                edgeSwipeDirection === 'right'
                  ? 'el-icon-arrow-right'
                  : 'el-icon-arrow-left'
              "
            ></i>
          </div>
        </div>
      </transition>

      <!-- “再次滑动返回书架”防误触确认胶囊提示 -->
      <transition name="edge-toast-fade">
        <div class="edge-back-toast" v-if="edgeBackToastVisible">
          <i class="el-icon-arrow-left"></i> 再次滑动返回书架
        </div>
      </transition>

      <!-- 选中文本高亮图层 (彻底解决 iOS/安卓原生系统菜单弹出的问题，同时完美保留文字选区高亮) -->
      <div
        v-for="(rect, idx) in selectionHighlightRects"
        :key="'selection-hl-' + idx"
        class="custom-selection-highlight"
        :style="{
          position: 'fixed',
          top: rect.top + 'px',
          left: rect.left + 'px',
          width: rect.width + 'px',
          height: rect.height + 'px'
        }"
      ></div>

      <!-- 选中文字快捷操作气泡弹窗 (替代系统原生菜单，支持复制、书签、过滤) -->
      <transition name="text-action-fade">
        <div
          ref="textActionMenuRef"
          class="text-action-popup"
          :class="{ 'is-below': textActionMenuPos.placeBelow }"
          v-if="textActionMenuVisible"
          :style="{
            top: textActionMenuPos.top + 'px',
            left: textActionMenuPos.left + 'px'
          }"
          @click.stop
        >
          <div
            class="text-action-btn"
            @click.stop.prevent="handleActionCopy"
            @touchend.stop.prevent="handleActionCopy"
          >
            <i class="el-icon-document-copy"></i>
            <span>复制</span>
          </div>
          <div class="text-action-divider"></div>
          <div
            class="text-action-btn"
            @click.stop.prevent="handleActionAddBookmark"
            @touchend.stop.prevent="handleActionAddBookmark"
          >
            <i class="el-icon-collection-tag"></i>
            <span>添加书签</span>
          </div>
          <div class="text-action-divider"></div>
          <div
            class="text-action-btn"
            @click.stop.prevent="handleActionAddFilter"
            @touchend.stop.prevent="handleActionAddFilter"
          >
            <i class="el-icon-scissors"></i>
            <span>添加过滤规则</span>
          </div>
        </div>
      </transition>
    </div>

    <!-- 离线缓存管理弹窗 -->
    <el-dialog
      title="离线缓存管理"
      :visible.sync="showOfflineCacheDialog"
      :width="$store.state.miniInterface ? '92%' : '480px'"
      custom-class="offline-cache-dialog"
      append-to-body
      :close-on-click-modal="!isCachingContent"
    >
      <div class="offline-cache-content" v-if="$store.getters.readingBook">
        <!-- 统计状态卡片 -->
        <div class="cache-status-card">
          <div class="status-header">
            <span class="book-name-tag">{{
              $store.getters.readingBook.name
            }}</span>
            <span class="cache-size-text" v-if="cachedStats.sizeText"
              >已占用存储：{{ cachedStats.sizeText }}</span
            >
          </div>
          <div class="status-progress-info">
            <span
              >已缓存在手机：<strong>{{ cachedStats.cachedCount }}</strong> /
              {{ catalog.length }} 章</span
            >
            <span class="status-percent">{{ cachedPercent }}%</span>
          </div>
          <el-progress
            :percentage="cachedPercent"
            :stroke-width="8"
            :show-text="false"
            :status="cachedPercent === 100 ? 'success' : undefined"
          ></el-progress>
        </div>

        <!-- 正在缓存中的动态进度 -->
        <div class="active-caching-box" v-if="isCachingContent">
          <div class="active-caching-header">
            <span><i class="el-icon-loading"></i> {{ cachingContentTip }}</span>
            <el-button type="danger" size="mini" plain @click="cancelCaching"
              >停止缓存</el-button
            >
          </div>
          <el-progress
            :percentage="cachingStepPercent"
            :stroke-width="6"
            color="#409EFF"
          ></el-progress>
        </div>

        <!-- 批量缓存操作区 -->
        <div class="cache-actions-section" v-show="!isCachingContent">
          <div class="section-title">批量缓存后续章节至手机存储：</div>
          <div class="cache-btns-grid">
            <el-button
              type="primary"
              plain
              size="small"
              icon="el-icon-download"
              @click="cacheChapterContent(20)"
            >
              缓存后 20 章
            </el-button>
            <el-button
              type="primary"
              plain
              size="small"
              icon="el-icon-download"
              @click="cacheChapterContent(50)"
            >
              缓存后 50 章
            </el-button>
            <el-button
              type="primary"
              plain
              size="small"
              icon="el-icon-download"
              @click="cacheChapterContent(100)"
            >
              缓存后 100 章
            </el-button>
            <el-button
              type="success"
              size="small"
              icon="el-icon-folder-checked"
              @click="cacheChapterContent(true)"
            >
              缓存全本书籍
            </el-button>
          </div>
        </div>

        <!-- 缓存清理区 -->
        <div
          class="cache-footer-actions"
          v-if="cachedStats.cachedCount > 0 && !isCachingContent"
        >
          <el-button
            type="text"
            size="small"
            style="color: #F56C6C;"
            icon="el-icon-delete"
            @click="clearCurrentBookCache"
          >
            清空本书离线缓存 (释放手机存储空间)
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import PopCata from "../components/PopCatalog.vue";
import ReadSettings from "../components/ReadSettings.vue";
import BookSource from "../components/BookSource.vue";
import BookShelf from "../components/BookShelf.vue";
import Content from "../components/Content.vue";
import Axios from "../plugins/axios";
import jump from "../plugins/jump";
import Animate from "../plugins/animate";
import { setCache, getCache } from "../plugins/cache";
import { simplized, traditionalized } from "../plugins/chinese";
import {
  LimitResquest,
  networkFirstRequest,
  editDistance,
  formatSize
} from "../plugins/helper";
import { defaultReplaceRule, defaultBookmark } from "../plugins/config.js";
import eventBus from "../plugins/eventBus";
// eslint-disable-next-line no-useless-escape
const symboRegex = /[\u2000-\u206F\u2E00-\u2E7F\\'!"#$%&\(\)*+,-\./:;<=>?@\[\]^_`{\|}~，。？《》；：、«]/g;

export default {
  components: {
    PopCata,
    BookSource,
    BookShelf,
    Content,
    ReadSettings
  },
  mounted() {
    window.readerPage = this;
    this.speechAvalable = true;
    this.fetchVoiceList();
    if (
      window.speechSynthesis &&
      window.speechSynthesis.onvoiceschanged !== undefined
    ) {
      window.speechSynthesis.onvoiceschanged = this.fetchVoiceList;
    }
    window.addEventListener("unload", this.saveReadingPosition);
    document.addEventListener("selectionchange", this.handleSelectionChange);
    window.addEventListener("mouseup", this.handleGlobalMouseUp);
    eventBus.$on("showSearchContent", data => {
      if (this._inactive) {
        return;
      }
      if (this.chapterIndex === data.chapterIndex) {
        this.showMatchKeyword(data);
        return;
      }
      if (this.isScrollRead) {
        this.scrollStartChapterIndex = data.chapterIndex;
        this.computeShowChapterList().then(() => {
          this.showMatchKeyword(data);
        });
        return;
      }
      this.$once("showContent", () => {
        this.showMatchKeyword(data);
      });
      this.getContent(data.chapterIndex);
    });
    eventBus.$on("showBookmark", bookmark => {
      if (this._inactive) {
        return;
      }
      // console.log(this.chapterIndex, bookmark);
      if (this.chapterIndex === bookmark.chapterIndex) {
        this.showBookmark(bookmark);
        return;
      }
      if (this.isScrollRead) {
        this.scrollStartChapterIndex = bookmark.chapterIndex;
        this.computeShowChapterList().then(() => {
          this.showBookmark(bookmark);
        });
        return;
      }
      this.$once("showContent", () => {
        this.showBookmark(bookmark);
      });
      this.getContent(bookmark.chapterIndex);
    });
  },
  activated() {
    this.init();
    window.addEventListener("keydown", this.keydownHandler);
    document.addEventListener("selectionchange", this.handleSelectionChange);
    window.addEventListener("mouseup", this.handleGlobalMouseUp);
    if (this.title) {
      document.title =
        this.$store.getters.readingBook.name + " - " + this.title;
    } else {
      document.title = this.$store.getters.readingBook.name;
    }
    this.formatTime();
    this.computeBookCacheStats();
    this.timer = setInterval(() => {
      this.formatTime();
      this.computeBookCacheStats();
    }, 5000);
    this.unwatchFn = this.$store.watch(
      state => state.config,
      () => {
        this.$nextTick(() => {
          this.computePages(() => {
            if (this.currentPage > this.totalPages) {
              this.showPage(this.totalPages, 0);
            }
          });
        });
      },
      {
        deep: true
      }
    );
    window.addEventListener("scroll", this.scrollHandler);
    try {
      this.releaseWakeLockFn = this.wakeLock();
    } catch (e) {
      //
    }
    this.$Lazyload.$on("loaded", this.lazyloadHandler);
    // iOS 前后台切换与来电恢复后的 DOM 朗读高亮即时对齐
    this.visibilityChangeHandler = () => {
      if (
        document.visibilityState === "visible" &&
        this.speechSpeaking &&
        this.speechParagraphIndex >= 0
      ) {
        const paragraphs = this.getAllParagraphs();
        const curP = paragraphs[this.speechParagraphIndex];
        if (curP) {
          this.showParagraph(curP, true);
        }
      }
    };
    document.addEventListener("visibilitychange", this.visibilityChangeHandler);
  },
  deactivated() {
    this.stopSpeech();
    this.saveBookProgress();
    this.startSavePosition = false;
    this.lastReadingBook = this.$store.getters.readingBook;
    this.timer && clearInterval(this.timer);
    window.removeEventListener("keydown", this.keydownHandler);
    window.removeEventListener("scroll", this.scrollHandler);
    this.unwatchFn && this.unwatchFn();
    this.releaseWakeLockFn && this.releaseWakeLockFn();
    this.$Lazyload.$off("loaded", this.lazyloadHandler);
    if (this.visibilityChangeHandler) {
      document.removeEventListener(
        "visibilitychange",
        this.visibilityChangeHandler
      );
    }
    document.removeEventListener("selectionchange", this.handleSelectionChange);
    window.removeEventListener("mouseup", this.handleGlobalMouseUp);
    this.isMouseDown = false;
    this.justSelectedOnPC = false;
    if (this.selectionChangeTimer) {
      clearTimeout(this.selectionChangeTimer);
      this.selectionChangeTimer = null;
    }
  },
  beforeDestroy() {
    document.removeEventListener("selectionchange", this.handleSelectionChange);
    window.removeEventListener("mouseup", this.handleGlobalMouseUp);
    this.isMouseDown = false;
    this.justSelectedOnPC = false;
    if (this.selectionChangeTimer) {
      clearTimeout(this.selectionChangeTimer);
      this.selectionChangeTimer = null;
    }
  },
  watch: {
    textActionMenuVisible(val) {
      if (val) {
        // 延迟绑定全局点击关闭监听，彻底避开同一次抬手手势派发的微任务合成 pointerdown 事件，杜绝瞬间误关
        setTimeout(() => {
          if (!this.textActionMenuVisible) return;
          const closeHandler = e => {
            if (
              this.$refs.textActionMenuRef &&
              this.$refs.textActionMenuRef.contains(e.target)
            ) {
              return;
            }
            this.closeTextActionMenu(true);
            document.removeEventListener("pointerdown", closeHandler, true);
          };
          document.addEventListener("pointerdown", closeHandler, true);
        }, 220);
      }
    },
    chapterName(to) {
      this.title = to;
    },
    content() {
      this.contentStyle = {};
      this.transformX = 0;
      this.currentPage = 1;
      this.$nextTick(() => {
        this.computePages();
        this.saveReadingPosition();
      });
      if (this.isEpub) {
        this.$once("iframeLoad", () => {
          this.computePages();
        });
      }
    },
    readSettingsVisible(visible) {
      if (!visible) {
        //
      }
    },
    title(title) {
      if (title) {
        document.title = this.$store.getters.readingBook.name + " - " + title;
      } else {
        document.title = this.$store.getters.readingBook.name;
      }
    },
    isSlideRead(val) {
      if (!val) {
        this.contentStyle = {};
        this.transformX = 0;
      }
      this.$nextTick(() => {
        this.computePages(() => {
          if (this.currentParagraph) {
            this.showParagraph(this.currentParagraph, true);
          } else {
            this.showPage(this.currentPage, 0);
          }
        });
      });
    },
    isScrollRead(val) {
      if (val) {
        this.scrollStartChapterIndex = this.chapterIndex;
        this.computeShowChapterList();
      }
    },
    windowSize() {
      this.$nextTick(() => {
        this.computePages(() => {
          this.showPage(this.currentPage, 0);
        });
      });
    },
    loginAuth(val) {
      if (val) {
        this.init(true);
      }
    },
    showReadBar(val) {
      if (val) {
        this.showToolBar = false;
      }
    },
    readingBook(val, oldVal) {
      if (val.bookUrl !== oldVal.bookUrl) {
        this.startSavePosition = false;
        this.autoShowPosition();
      }
    },
    currentPage(val, oldVal) {
      if (val !== oldVal) {
        this.checkReadingProgress();
      }
    },
    filterRules() {
      if (this.isScrollRead) {
        //
        this.computeShowChapterList();
      } else {
        this.content = this.filterContent(this.content);
      }
    },
    chineseFont() {
      this.title = this.filterContent(this.title);
      this.content = this.filterContent(this.content);
      this.computeShowChapterList();
    }
  },
  data() {
    return {
      title: "",
      content: "",
      error: false,
      popCataVisible: false,
      readSettingsVisible: false,
      popBookSourceVisible: false,
      popBookShelfVisible: false,
      showToolBar: true,
      book: null,
      show: false,
      contentStyle: {},
      currentPage: 1,
      totalPages: 1,
      transformX: 0,
      transforming: false,
      showLastPage: false,
      showClickZone: false,
      timeStr: "",
      progressValue: 1,

      speechAvalable: false,
      showReadBar: false,
      voiceList: [],
      speechSpeaking: false,
      showSpeechConfig: true,
      speechParagraphIndex: -1,

      currentParagraph: null,

      startSavePosition: false,

      showCacheContentZone: false,
      isCachingContent: false,
      cachingContentTip: "",
      showOfflineCacheDialog: false,
      cachedStats: {
        cachedCount: 0,
        totalBytes: 0,
        sizeText: "0 B",
        cachedMap: {}
      },
      totalCachingTarget: 0,
      currentCachingProgress: 0,
      isSilentPreloading: false,
      hasPreloadedThisChapter: false,
      singlePageDwellTimer: null,

      autoReading: false,
      showChapterList: [],

      scrollStartChapterIndex: 0,
      showNextChapterSize: 1,
      showPrevChapterSize: 0,

      speechMinutes: 0,
      speechEndTime: 0,

      // 仿安卓侧滑返回与防误触机制 (支持左侧与右侧双边缘)
      isEdgeSwipe: false,
      edgeSwipeDirection: "left", // "left" 或 "right"
      edgeBackIndicatorVisible: false,
      edgeBackIndicatorY: 300,
      edgeBackMoveDistance: 0,
      edgeBackToastVisible: false,
      lastEdgeBackTriggerTime: 0,
      edgeBackToastTimer: null,

      // 自定义选中文本快捷菜单
      textActionMenuVisible: false,
      selectedActionText: "",
      textActionMenuPos: {
        top: 100,
        left: 150,
        placeBelow: false
      },
      isTouchActive: false,
      selectionChangeTimer: null,

      // 仿真翻页 3D 纸张翻折状态
      isSimulationFlipping: false,
      flipAngle: 0,
      flipperHTML: "",
      flipperOffset: 0,
      flipDirection: "next",

      // 文本选区高亮与弹窗关闭防误触冷冻期
      selectionHighlightRects: [],
      dismissCooldown: 0,
      isDismissingMenu: false,
      isTextSelecting: false,

      // PC 鼠标文本选择状态跟踪
      isMouseDown: false,
      justSelectedOnPC: false
    };
  },
  computed: {
    isMobile() {
      if (typeof navigator === "undefined") return false;
      const ua = navigator.userAgent || "";
      const isIOS =
        /iPhone|iPad|iPod/i.test(ua) ||
        (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
      return (
        isIOS || /Android|webOS|BlackBerry|IEMobile|Opera Mini|Mobile/i.test(ua)
      );
    },
    cachedPercent() {
      if (!this.catalog || !this.catalog.length) return 0;
      const count = this.cachedStats.cachedCount || 0;
      return Math.min(100, Math.round((count / this.catalog.length) * 100));
    },
    cachingStepPercent() {
      if (!this.totalCachingTarget) return 0;
      return Math.min(
        100,
        Math.round(
          (this.currentCachingProgress / this.totalCachingTarget) * 100
        )
      );
    },
    edgeBackScale() {
      // 随着手势拉动距离 0~60px 平滑放大
      const progress = Math.min(Math.max(this.edgeBackMoveDistance / 60, 0), 1);
      return (0.6 + progress * 0.4).toFixed(2);
    },
    readingBook() {
      return this.$store.getters.readingBook || {};
    },
    catalog() {
      return (this.$store.getters.readingBook || {}).catalog || [];
    },
    chapterIndex() {
      return ((this.$store.getters.readingBook || {}).index || 0) | 0;
    },
    windowSize() {
      return this.$store.state.windowSize;
    },
    config() {
      return this.$store.getters.config;
    },
    theme() {
      return this.config.theme;
    },
    animateMSTime() {
      return this.config.animateMSTime;
    },
    isNight() {
      return this.$store.getters.isNight;
    },
    bodyTheme() {
      return {
        background: this.$store.getters.currentThemeConfig.body
      };
    },
    isSlideRead() {
      return this.autoReading ||
        this.showReadBar ||
        this.isEpub ||
        this.isCarToon ||
        this.isAudio
        ? false
        : this.$store.getters.isSlideRead;
    },
    isSimulationRead() {
      return this.isSlideRead && this.config.readMethod === "仿真翻页";
    },
    sheetTransformStyle() {
      return {
        transform: `rotateY(${this.flipAngle}deg)`,
        WebkitTransform: `rotateY(${this.flipAngle}deg)`
      };
    },
    sheetFrontStyle() {
      return {
        background: this.$store.getters.currentThemeConfig.body
      };
    },
    sheetBackStyle() {
      return {
        background: this.$store.getters.currentThemeConfig.body,
        transform: "rotateY(180deg)",
        WebkitTransform: "rotateY(180deg)"
      };
    },
    sheetShadowOpacity() {
      const rad = Math.abs(this.flipAngle) * (Math.PI / 180);
      return Math.sin(rad) * 0.65;
    },
    underShadowOpacity() {
      const rad = Math.abs(this.flipAngle) * (Math.PI / 180);
      return Math.sin(rad) * 0.45;
    },
    isScrollRead() {
      return (
        !this.isEpub &&
        !this.isAudio &&
        !this.isSlideRead &&
        (this.config.readMethod === "上下滚动" ||
          this.config.readMethod === "上下滚动2")
      );
    },
    chapterClass() {
      return this.isSlideRead
        ? "slide-reader"
        : this.isEpub
        ? "epub"
        : this.isCarToon
        ? "cartoon"
        : this.isAudio
        ? "audio"
        : "";
    },
    chapterTheme() {
      let readingStyle = this.showReadBar
        ? { paddingBottom: (this.showSpeechConfig ? 280 : 80) + "px" }
        : {};
      if (typeof this.$store.getters.currentThemeConfig.content === "string") {
        return {
          ...readingStyle,
          background: this.$store.getters.currentThemeConfig.content,
          width: this.readWidth
        };
      } else {
        return {
          ...readingStyle,
          ...this.$store.getters.currentThemeConfig.content,
          width: this.readWidth
        };
      }
    },
    leftBarTheme() {
      return {
        background: this.$store.getters.currentThemeConfig.popup,
        marginLeft: this.$store.state.miniInterface
          ? 0
          : -(this.readWidthConfig / 2 + 68) + "px",
        display:
          this.$store.state.miniInterface && !this.showToolBar
            ? "none"
            : "block"
      };
    },
    rightBarTheme() {
      return {
        background: this.$store.getters.currentThemeConfig.popupPure,
        marginRight: this.$store.state.miniInterface
          ? 0
          : -(this.readWidthConfig / 2 + 52) + "px",
        display:
          this.$store.state.miniInterface && !this.showToolBar
            ? "none"
            : "block"
      };
    },
    readBarTheme() {
      return {
        background: this.$store.getters.currentThemeConfig.popupPure,
        marginRight: this.$store.state.miniInterface
          ? 0
          : -(this.readWidthConfig / 2) + "px",
        zIndex: 200,
        display: this.speechAvalable && this.showReadBar ? "block" : "none",
        width: this.$store.state.miniInterface ? "100vw" : "500px"
      };
    },
    readWidth() {
      if (!this.$store.state.miniInterface) {
        return this.readWidthConfig - 130 + "px";
      } else {
        return this.windowSize.width + "px";
      }
    },
    readWidthConfig() {
      var width = this.$store.getters.config.readWidth;
      while (width > this.$store.state.windowSize.width - 140) {
        width -= 20;
      }
      return width;
    },
    popperWidth() {
      if (!this.$store.state.miniInterface) {
        return this.readWidthConfig - 33;
      } else {
        return this.windowSize.width - 33;
      }
    },
    readingProgress() {
      if (this.catalog && this.catalog.length) {
        return (
          parseInt(((this.chapterIndex + 1) * 100) / this.catalog.length) + "%"
        );
      } else {
        return "";
      }
    },
    showPrevPageStyle() {
      if (this.isSlideRead) {
        // 左半部
        return {
          left: 0,
          top: 0,
          bottom: 0,
          right: this.windowSize.width / 2 + "px",
          background: "#43987324",
          paddingRight: this.windowSize.width * 0.2 + "px"
        };
      } else {
        // 上半部
        return {
          left: 0,
          top: 0,
          right: 0,
          bottom: this.windowSize.height / 2 + "px",
          background: "#43987324"
        };
      }
    },
    showMenuZoneStyle() {
      return {
        top: this.windowSize.height * 0.3 + "px",
        bottom: this.windowSize.height * 0.3 + "px",
        left: this.windowSize.width * 0.3 + "px",
        right: this.windowSize.width * 0.3 + "px",
        background: "#636060",
        zIndex: 10
      };
    },
    showNextPageStyle() {
      if (this.isSlideRead) {
        // 右半部
        return {
          right: 0,
          top: 0,
          bottom: 0,
          left: this.windowSize.width / 2 + "px",
          background: "#6b1a7324",
          paddingLeft: this.windowSize.width * 0.2 + "px"
        };
      } else {
        // 下半部
        return {
          left: 0,
          bottom: 0,
          right: 0,
          top: this.windowSize.height / 2 + "px",
          background: "#6b1a7324"
        };
      }
    },
    loginAuth() {
      return this.$store.state.loginAuth;
    },
    filterRules() {
      return this.$store.state.filterRules;
    },
    themeBtnStyle() {
      // if (this.$store.getters.isNight) {
      //   return {
      //     background: "#f7f7f7"
      //   };
      // } else {
      //   return {
      //     background: "#222"
      //   };
      // }
      return {
        background: this.$store.getters.currentThemeConfig.popupPure
      };
    },
    popupAbsoluteBtnStyle() {
      return {
        background: this.$store.getters.currentThemeConfig.popupPure
      };
    },
    voiceName: {
      get() {
        return this.$store.state.speechVoiceConfig.voiceName;
      },
      set(val) {
        if (val !== this.$store.state.speechVoiceConfig.voiceName) {
          if (this.speechSpeaking) {
            this.restartSpeech();
          }
        }
        this.$store.commit("setSpeechVoiceConfig", {
          ...this.$store.state.speechVoiceConfig,
          voiceName: val
        });
      }
    },
    speechRate: {
      get() {
        return this.$store.state.speechVoiceConfig.speechRate;
      },
      set(val) {
        if (val !== this.$store.state.speechVoiceConfig.speechRate) {
          if (this.speechSpeaking) {
            this.restartSpeech();
          }
        }
        this.$store.commit("setSpeechVoiceConfig", {
          ...this.$store.state.speechVoiceConfig,
          speechRate: val
        });
      }
    },
    speechPitch: {
      get() {
        return this.$store.state.speechVoiceConfig.speechPitch;
      },
      set(val) {
        if (val !== this.$store.state.speechVoiceConfig.speechPitch) {
          if (this.speechSpeaking) {
            this.restartSpeech();
          }
        }
        this.$store.commit("setSpeechVoiceConfig", {
          ...this.$store.state.speechVoiceConfig,
          speechPitch: val
        });
      }
    },
    isCarToon() {
      return (
        !this.error &&
        !this.isEpub &&
        !this.isCbz &&
        (this.content || "").indexOf("<img") >= 0
      );
    },
    isAudio() {
      return !this.error && this.$store.getters.readingBook.type === 1;
    },
    isEpub() {
      return (
        !this.error &&
        this.$store.getters.readingBook.bookUrl.toLowerCase().endsWith(".epub")
      );
    },
    isCbz() {
      return (
        !this.error &&
        this.$store.getters.readingBook.bookUrl.toLowerCase().endsWith(".cbz")
      );
    },
    scrollOffset() {
      // 两行 + 两个段间距
      return (
        this.$store.getters.config.fontSize *
          this.$store.getters.config.lineHeight *
          2 +
        this.$store.getters.config.fontSize *
          this.$store.getters.config.paragraphSpace *
          2
      );
    },
    formatedTitle() {
      return this.formatChinese(this.title);
    },
    chineseFont() {
      return this.config.chineseFont;
    }
  },
  methods: {
    init(refresh) {
      if (this.$store.getters.readingBook) {
        if (
          refresh ||
          !this.lastReadingBook ||
          this.lastReadingBook.bookUrl !==
            this.$store.getters.readingBook.bookUrl
        ) {
          this.title = "";
          this.show = false;
          this.loading = this.$loading({
            target: this.$refs.content,
            lock: true,
            text: "正在获取内容",
            spinner: "el-icon-loading",
            background: "rgba(0,0,0,0)"
          });
          this.lastReadingBook = this.$store.getters.readingBook;
          // 跳转记住的位置
          this.autoShowPosition();
          this.loadCatalog(false, true);
        } else {
          if (this.isScrollRead) {
            this.scrollStartChapterIndex = this.chapterIndex;
            this.showPrevChapterSize = 0;
            this.computeShowChapterList().then(() => {
              this.autoShowPosition(true);
            });
          } else if (this.isEpub) {
            // 跳转记住的位置
            this.autoShowPosition(true);
          } else {
            this.startSavePosition = true;
          }
          setTimeout(() => {
            // console.log("setReadingBook", this.lastReadingBook);
            this.$store.commit("setReadingBook", this.lastReadingBook);
          }, 100);
        }
      } else {
        this.$message.error("请在书架选择书籍");
      }
    },
    changeBook(book) {
      this.$message.info("换书成功");
      this.popBookShelfVisible = false;
      this.show = false;
      this.$store.commit("setReadingBook", book);
      this.loadCatalog(true, true);
    },
    changeBookSource() {
      this.popBookSourceVisible = false;
      this.show = false;
      this.tryRefresh = false;
      if (this.$route.query && this.$route.query.search) {
        this.$router.replace({ path: "/reader" }).catch(() => {});
      }
      // 换源后直接加载新源目录并直接阅读
      this.loadCatalog(false, true);
    },
    loadCatalog(refresh, init) {
      if (!this.api) {
        setTimeout(() => {
          if (this.loadCatalog) {
            this.loadCatalog(refresh);
          }
        }, 1000);
        return;
      }
      this.getCatalog(refresh).then(
        res => {
          if (res && res.data && res.data.isSuccess) {
            var book = Object.assign({}, this.$store.getters.readingBook);
            book.catalog = res.data.data;
            this.$store.commit("setReadingBook", book);
            this.$emit("loadCatalog");
            var index = book.index || 0;
            this.getContent(index);
          } else {
            if (init) {
              this.title = "";
              this.content =
                "获取章节目录失败！\n" +
                (res && res.data ? res.data.errorMsg : "");
              this.error = true;
              this.show = true;
              this.$emit("showContent");
            }
            if (this.loading) {
              this.loading.close();
            }
          }
        },
        error => {
          if (this.loading) {
            this.loading.close();
          }
          this.$message.error(
            "获取书籍目录列表 " + (error && error.toString())
          );
        }
      );
    },
    getCatalog(refresh) {
      if (
        !this.$store.getters.readingBook ||
        !this.$store.getters.readingBook.bookUrl
      ) {
        return Promise.resolve({
          data: { isSuccess: false, errorMsg: "未选择书籍" }
        });
      }
      const params = {
        url: this.$store.getters.readingBook.bookUrl,
        refresh: refresh ? 1 : 0
      };
      if (this.$route.query.search) {
        // 来自搜索结果，请求需要带上 书源链接
        params.bookSourceUrl = this.$store.getters.readingBook.origin;
      }
      return networkFirstRequest(
        () => Axios.post(this.api + "/getChapterList", params),
        this.$store.getters.readingBook.name +
          "_" +
          this.$store.getters.readingBook.author +
          "@" +
          this.$store.getters.readingBook.bookUrl +
          "@chapterList"
      );
    },
    refreshCatalog() {
      return this.loadCatalog(true);
    },
    getBookContent(chapterIndex, options, refresh, cache) {
      return this.$root.$children[0].getBookContent(
        chapterIndex,
        options,
        refresh,
        cache
      );
    },
    refreshContent() {
      this.getContent(this.$store.getters.readingBook.index, true);
    },
    getContent(index, refresh) {
      //展示进度条
      this.show = false;
      if (!this.loading || !this.loading.visible) {
        this.loading = this.$loading({
          target: this.$refs.content,
          lock: true,
          text: refresh ? "正在刷新内容" : "正在获取内容",
          spinner: "el-icon-loading",
          background: "rgba(0,0,0,0)"
        });
      }
      if (this.loadingSafetyTimer) {
        clearTimeout(this.loadingSafetyTimer);
        this.loadingSafetyTimer = null;
      }
      this.loadingSafetyTimer = setTimeout(() => {
        if (this.loading && typeof this.loading.close === "function") {
          this.loading.close();
        }
        if (!this.show) {
          this.show = true;
          this.error = true;
          this.content = "获取章节内容超时，建议尝试换源后重试！";
          this.$message.warning("获取章节内容超时，建议切换书源");
        }
      }, 15000);

      let bookUrl = this.$store.getters.readingBook.bookUrl;
      this.hasPreloadedThisChapter = false;
      this.isSilentPreloading = false;
      if (this.singlePageDwellTimer) {
        clearTimeout(this.singlePageDwellTimer);
        this.singlePageDwellTimer = null;
      }
      try {
        // 保存阅读进度
        let book = { ...this.$store.getters.readingBook };
        book.index = index;
        this.$store.commit("setReadingBook", book);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      }
      //强制滚回顶层
      this.toTop(0);
      // 如果超出目录范围，尝试刷新目录
      if (!this.$store.getters.readingBook.catalog[index]) {
        if (this.loadingSafetyTimer) {
          clearTimeout(this.loadingSafetyTimer);
          this.loadingSafetyTimer = null;
        }
        if (this.tryRefresh) {
          this.tryRefresh = false;
          this.content = "获取章节内容失败，请更新目录！";
          this.error = true;
          this.show = true;
          this.$emit("showContent");
          if (this.loading && typeof this.loading.close === "function") {
            this.loading.close();
          }
        } else {
          this.tryRefresh = true;
          this.refreshCatalog();
        }
        return;
      }
      //let chapterUrl = this.$store.getters.readingBook.catalog[index].url;
      let chapterName = this.$store.getters.readingBook.catalog[index].title;
      let chapterIndex = this.$store.getters.readingBook.catalog[index].index;
      this.title = chapterName;
      const now = new Date().getTime();
      this.getBookContent(chapterIndex, {}, refresh)
        .then(
          res => {
            if (
              bookUrl !== this.$store.getters.readingBook.bookUrl ||
              index !== this.$store.getters.readingBook.index
            ) {
              // 已经换书或者换章节了
              return;
            }
            if (now + 100 > new Date().getTime()) {
              // 不超过 100ms 假定为获取缓存，此时发送进度保存请求
              this.saveBookProgress();
            }
            try {
              if (res.data.isSuccess) {
                let data = res.data.data;
                this.content = this.filterContent(data);
                this.addChapterContentToCache({
                  bookUrl,
                  index: index,
                  title: chapterName,
                  content: res.data.data,
                  error: false
                });
                this.error = false;
                this.show = true;
                this.$emit("showContent");
                if (this.singlePageDwellTimer) {
                  clearTimeout(this.singlePageDwellTimer);
                  this.singlePageDwellTimer = null;
                }
                // 若为单页内容（或用户持续在该章节阅读），停留阅读超过 6 秒后，判定已阅读 1/3 并触发预读后续 1 章
                this.singlePageDwellTimer = setTimeout(() => {
                  this.checkReadingProgress();
                }, 6000);
              } else {
                this.content = "获取章节内容失败！\n" + res.data.errorMsg;
                this.addChapterContentToCache({
                  bookUrl,
                  index: index,
                  title: chapterName,
                  content: "获取章节内容失败！\n" + res.data.errorMsg,
                  error: true
                });
                this.error = true;
                this.show = true;
                this.$emit("showContent");
              }
            } catch (err) {
              this.content = "解析章节内容失败！\n" + err.toString();
              this.error = true;
              this.show = true;
              this.$emit("showContent");
            }
            if (this.isScrollRead) {
              this.computeShowChapterList();
            }
          },
          error => {
            if (
              bookUrl !== this.$store.getters.readingBook.bookUrl ||
              index !== this.$store.getters.readingBook.index
            ) {
              // 已经换书或者换章节了
              return;
            }
            this.content = "获取章节内容失败！\n" + (error && error.toString());
            this.addChapterContentToCache({
              bookUrl,
              index: index,
              title: chapterName,
              content: "获取章节内容失败！\n" + (error && error.toString()),
              error: true
            });
            this.error = true;
            this.show = true;
            this.$emit("showContent");
            this.$message.error(
              "获取章节内容失败 " + (error && error.toString())
            );
            if (this.isScrollRead) {
              this.computeShowChapterList();
            }
          }
        )
        .finally(() => {
          if (this.loadingSafetyTimer) {
            clearTimeout(this.loadingSafetyTimer);
            this.loadingSafetyTimer = null;
          }
          if (this.loading && typeof this.loading.close === "function") {
            this.loading.close();
          }
        });
    },
    filterContent(content) {
      if (this.isEpub || this.isAudio) {
        return content;
      }
      if (!content) {
        return content;
      }
      try {
        this.filterRules.forEach(rule => {
          if (
            typeof rule.isEnabled !== "undefined" &&
            rule.isEnabled === false
          ) {
            return;
          }
          const scope = rule.scope.split(";");
          if (
            scope[0] === "*" ||
            scope[0] === this.$store.getters.readingBook.name
          ) {
            if (
              scope.length == 1 ||
              (scope.length > 1 &&
                scope[1] === this.$store.getters.readingBook.bookUrl)
            ) {
              if (rule.isRegex) {
                content = content.replace(
                  new RegExp(rule.pattern, "ig"),
                  rule.replacement
                );
              } else {
                content = content.replace(rule.pattern, rule.replacement);
              }
            }
          }
        });
      } catch (error) {
        //
      }
      content.replace(/\\n+/g, "\n");
      content = this.formatChinese(content);
      return content;
    },
    loadShowChapter(index, refresh) {
      if (
        !refresh &&
        this.chapterContentCache &&
        this.chapterContentCache.chapters[index] &&
        !this.chapterContentCache.chapters[index].error
      ) {
        if (
          index >= this.chapterIndex - this.showPrevChapterSize &&
          index <= this.chapterIndex + this.showNextChapterSize
        ) {
          this.computeShowChapterList();
        }
        return Promise.resolve();
      }
      let bookUrl = this.$store.getters.readingBook.bookUrl;
      if (!this.$store.getters.readingBook.catalog) {
        return new Promise(resolve => {
          this.$once("loadCatalog", () => {
            this.loadShowChapter(index, refresh).then(resolve);
          });
        });
      }
      // 如果超出目录范围，尝试刷新目录
      if (!this.$store.getters.readingBook.catalog[index]) {
        return Promise.reject("章节不存在");
      }
      let chapterName = this.$store.getters.readingBook.catalog[index].title;
      let chapterIndex = this.$store.getters.readingBook.catalog[index].index;
      return this.getBookContent(chapterIndex, {}, refresh, true).then(
        res => {
          if (res.data.isSuccess) {
            this.addChapterContentToCache({
              bookUrl,
              index: index,
              title: chapterName,
              content: res.data.data,
              error: false
            });
          } else {
            this.addChapterContentToCache({
              bookUrl,
              index: index,
              title: chapterName,
              content: "获取章节内容失败！\n" + res.data.errorMsg,
              error: true
            });
          }
        },
        error => {
          this.addChapterContentToCache({
            bookUrl,
            index: index,
            title: chapterName,
            content: "获取章节内容失败！\n" + (error && error.toString()),
            error: true
          });
          throw error;
        }
      );
    },
    addChapterContentToCache(chapter) {
      if (
        !this.chapterContentCache ||
        this.chapterContentCache.bookUrl !== this.readingBook.bookUrl
      ) {
        this.chapterContentCache = {
          bookUrl: this.readingBook.bookUrl,
          chapters: {}
        };
      }
      if (
        typeof this.chapterContentCache.chapters[chapter.index] ===
          "undefined" || // 没有缓存
        !chapter.error || // 当前内容正确
        this.chapterContentCache.chapters[chapter.index].error // 缓存内容错误
      ) {
        // 查询是否卷名
        chapter.isVolume = !!(this.readingBook.catalog[chapter.index] || {})
          .isVolume;
        this.chapterContentCache.chapters[chapter.index] = chapter;
      }
    },
    computeShowChapterList(reset) {
      if (!this.chapterContentCache) {
        return new Promise(resolve => {
          setTimeout(() => {
            this.computeShowChapterList(reset).then(resolve);
          }, 10);
        });
      }
      if (!this.isScrollRead) {
        return Promise.resolve();
      }
      const list = [];
      let startIndex = this.scrollStartChapterIndex || this.chapterIndex;
      if (this.config.readMethod === "上下滚动2") {
        startIndex = this.chapterIndex - this.showPrevChapterSize;
      }
      const waitPromise = [];
      for (
        let i = startIndex;
        i <= this.chapterIndex + this.showNextChapterSize;
        i++
      ) {
        if (!this.chapterContentCache.chapters[i]) {
          waitPromise.push(this.loadShowChapter(i));
          continue;
        }
        list.push({
          ...this.chapterContentCache.chapters[i],
          content: this.filterContent(
            this.chapterContentCache.chapters[i].content
          )
        });
      }
      if (waitPromise.length) {
        return Promise.all(waitPromise).then(() => {
          this.computeShowChapterList(reset);
        });
      }
      this.saveReadingPosition();
      // 暂停记录位置
      this.startSavePosition = false;
      // 记录当前章节
      this.showChapterList = list;
      this.$nextTick(() => {
        this.computePages(() => {
          if (reset) {
            // 切换上下章节，滚动到顶部
            this.toTop(0);
            this.startSavePosition = true;
          } else if (this.config.readMethod === "上下滚动2") {
            this.autoShowPosition(true);
          } else {
            this.startSavePosition = true;
          }
        });
      });
    },
    saveBookProgress() {
      return Axios.post(
        this.api + "/saveBookProgress",
        {
          url: this.$store.getters.readingBook.bookUrl,
          index: this.chapterIndex
        },
        {
          silent: true
        }
      ).catch(() => {});
    },
    toTop(interval) {
      if (this.$store.state.miniInterface) {
        this.scrollContent(
          -(document.documentElement.scrollTop || document.body.scrollTop),
          interval
        );
      } else {
        jump(this.$refs.top, { duration: interval });
      }
    },
    toBottom(interval) {
      jump(this.$refs.bottom, { duration: interval });
    },
    toNextChapter(onError) {
      if (
        !this.$store.getters.readingBook ||
        !this.$store.getters.readingBook.bookUrl ||
        !this.$store.getters.readingBook.catalog
      ) {
        onError && onError();
        return;
      }
      let index = this.$store.getters.readingBook.index;
      index++;
      if (
        typeof this.$store.getters.readingBook.catalog[index] !== "undefined"
      ) {
        if (this.isScrollRead) {
          this.scrollStartChapterIndex = index;
          this.computeShowChapterList(true);
          return;
        }
        this.getContent(index);
      } else {
        onError && onError();
        this.$message.error("本章是最后一章");
      }
    },
    toLastChapter(onError) {
      if (
        !this.$store.getters.readingBook ||
        !this.$store.getters.readingBook.bookUrl ||
        !this.$store.getters.readingBook.catalog
      ) {
        onError && onError();
        return;
      }
      let index = this.$store.getters.readingBook.index;
      index--;
      if (
        typeof this.$store.getters.readingBook.catalog[index] !== "undefined"
      ) {
        if (this.isScrollRead) {
          this.scrollStartChapterIndex = index;
          this.computeShowChapterList(true);
          return;
        }
        this.getContent(index);
      } else {
        this.$message.error("本章是第一章");
        onError && onError();
      }
    },
    toShelf() {
      this.$router.push("/");
    },
    computePages(cb) {
      if (!this.$refs.bookContentRef || !this.$refs.bookContentRef.$el) {
        setTimeout(() => {
          this.computePages(cb);
        }, 30);
        return;
      }
      if (this.isSlideRead) {
        this.totalPages = Math.ceil(
          this.$refs.bookContentRef.$el.scrollWidth /
            (this.windowSize.width - 16)
        );
      } else {
        this.totalPages = Math.ceil(
          this.$refs.bookContentRef.$el.scrollHeight /
            (this.windowSize.height - this.scrollOffset)
        );
      }
      if (this.showLastPage) {
        this.showPage(this.totalPages, 0);
        this.showLastPage = false;
      }
      cb && cb();
    },
    nextPage(moveX) {
      if (!this.show) {
        return;
      }
      if (this.transforming || this.isSimulationFlipping) {
        return;
      }
      if (this.isSlideRead) {
        if (this.currentPage < this.totalPages) {
          if (this.isSimulationRead) {
            this.flipPage("next");
            return;
          }
          if (typeof moveX === "undefined") {
            this.transformX =
              -(this.windowSize.width - 16) * (this.currentPage - 1);
          }
          this.currentPage += 1;
          this.transforming = true;
          this.transform(
            typeof moveX === "undefined"
              ? -(this.windowSize.width - 16)
              : moveX,
            this.animateMSTime
          );
        } else {
          this.toNextChapter(() => {
            if (typeof moveX !== "undefined") {
              // 没有下一章，但是已经做了动画，恢复
              this.showPage(this.currentPage, 0);
            }
          });
        }
      } else {
        if (
          (document.documentElement.scrollTop || document.body.scrollTop) +
            this.windowSize.height <
          document.documentElement.scrollHeight
        ) {
          this.currentPage += 1;
          const moveY = this.windowSize.height - this.scrollOffset;
          this.transforming = true;
          this.scrollContent(moveY, this.animateMSTime);
        } else {
          this.currentPage = 1;
          this.toNextChapter();
        }
      }
    },
    prevPage(moveX) {
      if (!this.show) {
        return;
      }
      if (this.transforming || this.isSimulationFlipping) {
        return;
      }
      if (this.isSlideRead) {
        if (this.currentPage > 1) {
          if (this.isSimulationRead) {
            this.flipPage("prev");
            return;
          }
          if (typeof moveX === "undefined") {
            this.transformX =
              -(this.windowSize.width - 16) * (this.currentPage - 1);
          }
          this.currentPage -= 1;
          this.transforming = true;
          this.transform(
            typeof moveX === "undefined" ? this.windowSize.width - 16 : moveX,
            this.animateMSTime
          );
        } else {
          this.showLastPage = true;
          this.toLastChapter(() => {
            if (typeof moveX !== "undefined") {
              // 没有下一章，但是已经做了动画，恢复
              this.showPage(this.currentPage, 0);
            }
          });
        }
      } else {
        if (
          (document.documentElement.scrollTop || document.body.scrollTop) > 0
        ) {
          this.currentPage -= 1;
          const moveY = -this.windowSize.height + this.scrollOffset;
          this.transforming = true;
          this.scrollContent(moveY, this.animateMSTime);
        } else {
          this.currentPage = 1;
          this.toLastChapter();
        }
      }
    },
    flipPage(direction) {
      if (this.transforming || this.isSimulationFlipping) {
        return;
      }
      if (!this.$refs.bookContentRef || !this.$refs.bookContentRef.$el) {
        return;
      }
      const pageWidth = this.windowSize.width - 16;
      this.transforming = true;
      this.isSimulationFlipping = true;
      this.flipDirection = direction;

      if (direction === "next") {
        this.flipperHTML = this.$refs.bookContentRef.$el.outerHTML;
        this.flipperOffset = -(pageWidth * (this.currentPage - 1));

        this.currentPage += 1;
        this.transformX = -(pageWidth * (this.currentPage - 1));
        this.contentStyle = {
          transform: `translateX(${this.transformX}px)`
        };

        const duration = this.animateMSTime || 360;
        const timing = Animate.Utils.makeEaseInOut(
          Animate.Timings.power.bind(null, 3)
        );

        new Animate({
          duration: duration,
          timing: timing,
          draw: progress => {
            this.flipAngle = -progress * 180;
          },
          onEnd: () => {
            this.flipAngle = 0;
            this.isSimulationFlipping = false;
            this.flipperHTML = "";
            this.transforming = false;
            setTimeout(this.saveReadingPosition, 50);
          }
        });
      } else {
        this.flipperHTML = this.$refs.bookContentRef.$el.outerHTML;
        this.currentPage -= 1;
        this.transformX = -(pageWidth * (this.currentPage - 1));
        this.flipperOffset = this.transformX;

        const oldX = -(pageWidth * this.currentPage);
        this.contentStyle = {
          transform: `translateX(${oldX}px)`
        };

        this.flipAngle = -180;
        const duration = this.animateMSTime || 360;
        const timing = Animate.Utils.makeEaseInOut(
          Animate.Timings.power.bind(null, 3)
        );

        new Animate({
          duration: duration,
          timing: timing,
          draw: progress => {
            this.flipAngle = -180 + progress * 180;
          },
          onEnd: () => {
            this.flipAngle = 0;
            this.contentStyle = {
              transform: `translateX(${this.transformX}px)`
            };
            this.isSimulationFlipping = false;
            this.flipperHTML = "";
            this.transforming = false;
            setTimeout(this.saveReadingPosition, 50);
          }
        });
      }
    },
    showPage(page, duration) {
      if (!this.show) {
        return;
      }
      this.currentPage = Math.min(page, this.totalPages);
      if (this.isSlideRead) {
        const moveX =
          -(this.windowSize.width - 16) * (this.currentPage - 1) -
          this.transformX;
        this.transform(
          moveX,
          typeof duration === "undefined" ? this.animateMSTime : duration
        );
      } else {
        const moveY =
          (this.windowSize.height - 10) * (this.currentPage - 1) -
          (document.documentElement.scrollTop || document.body.scrollTop);
        this.scrollContent(
          moveY,
          typeof duration === "undefined" ? this.animateMSTime : duration
        );
      }
    },
    transform(moveX, duration) {
      const onEnd = () => {
        this.contentStyle = {
          transform: `translateX(${this.transformX + moveX}px)`
        };
        this.transformX += moveX;
        this.transforming = false;
        // 保存进度
        setTimeout(this.saveReadingPosition, duration);
      };
      if (!duration) {
        onEnd();
        return;
      }
      const timing = Animate.Utils.makeEaseInOut(
        Animate.Timings.power.bind(null, 3)
      );

      new Animate({
        duration: duration || 500,
        timing: timing,
        draw: progress => {
          this.contentStyle = {
            transform: `translateX(${this.transformX + moveX * progress}px)`
          };
        },
        onEnd
      });
    },
    scrollContent(moveY, duration, isAccurate) {
      // console.log("scrollContent", moveY);
      const lastScrollTop = isAccurate
        ? 0
        : document.documentElement.scrollTop || document.body.scrollTop;
      const onEnd = () => {
        document.documentElement.scrollTop = lastScrollTop + moveY;
        document.body.scrollTop = lastScrollTop + moveY;
        this.transforming = false;
        // 保存进度
        setTimeout(this.saveReadingPosition, duration);
      };
      if (!duration) {
        onEnd();
        return;
      }
      const timing = Animate.Utils.makeEaseInOut(
        Animate.Timings.power.bind(null, 3)
      );

      new Animate({
        duration: duration || 500,
        timing: timing,
        draw: progress => {
          document.documentElement.scrollTop = lastScrollTop + moveY * progress;
          document.body.scrollTop = lastScrollTop + moveY * progress;
        },
        onEnd
      });
    },
    handlerClick(e) {
      if (this.justSelectedOnPC) {
        this.justSelectedOnPC = false;
        this.ignoreNextClick = true;
        return;
      }
      if (this.textActionMenuVisible) {
        this.closeTextActionMenu(true);
        this.ignoreNextClick = true;
        return;
      }
      if (this.dismissCooldown && Date.now() < this.dismissCooldown) {
        this.ignoreNextClick = true;
        return;
      }
      if (this.isEpub) {
        return;
      }
      if (!this.lastTouch && !this.ignoreNextClick) {
        this.eventHandler(e);
      }
      this.ignoreNextClick = false;
    },
    handleContextMenu(e) {
      if (
        this.$store.getters.config &&
        this.$store.getters.config.selectionAction === "操作弹窗"
      ) {
        e.preventDefault();
      }
    },
    handleTouchStart(e) {
      this.isTouchActive = true;
      this.isTextSelecting = false;
      if (this.textActionMenuVisible) {
        if (
          this.$refs.textActionMenuRef &&
          this.$refs.textActionMenuRef.contains(e.target)
        ) {
          return;
        }
        this.closeTextActionMenu(true);
        this.isDismissingMenu = true;
        this.lastSelection = null;
        return;
      }
      if (this.dismissCooldown && Date.now() < this.dismissCooldown) {
        this.lastSelection = null;
        return;
      }
      this.lastSelection = this.checkSelection();
      if (this.lastSelection) {
        this.isTextSelecting = true;
        return;
      }
      if (this.isAudio) {
        return;
      }
      if (this.isEpub) {
        return;
      }
      // e.preventDefault();
      // e.stopPropagation();
      this.lastTouch = false;
      this.lastMoveX = false;
      this.isEdgeSwipe = false;
      this.edgeBackMoveDistance = 0;

      if (e.touches && e.touches[0]) {
        this.lastTouch = e.touches[0];
        const touchX = e.touches[0].clientX;
        const touchY = e.touches[0].clientY;

        // 判定屏幕左边缘（距离左边界 <= 28px）或右边缘（距离右边界 <= 28px）：进入仿安卓侧滑手势 (需在“特殊模式”中开启“边缘识别”)
        if (
          this.$store.state.config &&
          this.$store.state.config.edgeSwipeBack
        ) {
          const screenWidth =
            window.innerWidth || document.documentElement.clientWidth || 375;
          if (touchX <= 28) {
            this.isEdgeSwipe = true;
            this.edgeSwipeDirection = "left";
            this.edgeBackIndicatorY = touchY;
          } else if (touchX >= screenWidth - 28) {
            this.isEdgeSwipe = true;
            this.edgeSwipeDirection = "right";
            this.edgeBackIndicatorY = touchY;
          }
        }
      }
    },
    handleTouchMove(e) {
      // 1. 如果已处于文本划选状态，绝对不要移动正文视口，防止屏幕文字乱跳
      if (this.isTextSelecting) {
        this.lastMoveX = 0;
        return;
      }
      // 2. 实时检测当前是否有选区正在产生
      const sel = window.getSelection ? window.getSelection() : null;
      if (sel && !sel.isCollapsed && sel.toString().trim()) {
        this.isTextSelecting = true;
        this.lastMoveX = 0;
        return;
      }
      if (this.checkSelection()) {
        this.isTextSelecting = true;
        this.lastMoveX = 0;
        return;
      }
      if (e.touches && e.touches[0] && this.lastTouch) {
        this.lastMoveY = e.touches[0].clientY - this.lastTouch.clientY;
        const moveX = e.touches[0].clientX - this.lastTouch.clientX;

        // 如果是从左边缘或右边缘起步的侧滑手势
        if (this.isEdgeSwipe) {
          const isLeftValid = this.edgeSwipeDirection === "left" && moveX > 0;
          const isRightValid = this.edgeSwipeDirection === "right" && moveX < 0;

          if (isLeftValid || isRightValid) {
            e.preventDefault();
            e.stopPropagation();
            this.edgeBackMoveDistance = Math.abs(moveX);
            this.edgeBackIndicatorVisible = true;
            this.edgeBackIndicatorY = e.touches[0].clientY;
            return;
          }
        }

        if (this.isSlideRead) {
          e.preventDefault();
          e.stopPropagation();
          if (!this.isSimulationRead) {
            this.contentStyle = {
              transform: `translateX(${this.transformX + moveX}px)`
            };
          }
          this.lastMoveX = moveX;
        }
      }
    },
    handleTouchCancel() {
      this.isTouchActive = false;
      this.isTextSelecting = false;
      if (this.isDismissingMenu) {
        this.isDismissingMenu = false;
        this.lastSelection = null;
        return;
      }
      if (this.dismissCooldown && Date.now() < this.dismissCooldown) {
        this.lastSelection = null;
        return;
      }
      setTimeout(() => {
        this.checkAndTriggerSelection();
      }, 80);
    },
    handleContentMouseDown(e) {
      if (e.button === 0) {
        this.isMouseDown = true;
        this.justSelectedOnPC = false;
      }
    },
    handleMouseUp() {
      // PC 鼠标抬起事件统一由全局 handleGlobalMouseUp 处理
    },
    handleGlobalMouseUp(e) {
      this.isMouseDown = false;
      if (this.isMobile || this.isTouchActive) {
        return;
      }
      if (
        e &&
        this.$refs.textActionMenuRef &&
        this.$refs.textActionMenuRef.contains(e.target)
      ) {
        return;
      }
      if (this.isDismissingMenu) {
        this.isDismissingMenu = false;
        this.lastSelection = null;
        return;
      }
      if (this.dismissCooldown && Date.now() < this.dismissCooldown) {
        this.lastSelection = null;
        return;
      }
      const sel = window.getSelection ? window.getSelection() : null;
      if (!sel || sel.isCollapsed || sel.rangeCount === 0) {
        return;
      }
      const text = sel.toString().trim();
      if (!text) {
        return;
      }
      const range = sel.getRangeAt(0);
      const container =
        this.$refs.contentRef ||
        (this.$el && this.$el.querySelector(".content"));
      if (container && range.commonAncestorContainer) {
        const node = range.commonAncestorContainer;
        const elem = node.nodeType === 1 ? node : node.parentNode;
        if (elem && !container.contains(elem)) {
          return;
        }
      }
      this.justSelectedOnPC = true;
      this.checkAndTriggerSelection();
    },
    handleTouchEnd() {
      this.isTouchActive = false;
      if (this.isDismissingMenu) {
        this.isDismissingMenu = false;
        this.lastSelection = null;
        this.isTextSelecting = false;
        return;
      }
      if (this.dismissCooldown && Date.now() < this.dismissCooldown) {
        this.lastSelection = null;
        this.isTextSelecting = false;
        return;
      }

      // 核心拦截：如果处于划选文本状态，或者当前选区有文本，绝对禁止触发翻页！
      const currentSel = window.getSelection ? window.getSelection() : null;
      const hasSelectedText =
        this.isTextSelecting ||
        (currentSel &&
          !currentSel.isCollapsed &&
          currentSel.toString().trim()) ||
        this.checkSelection();

      if (hasSelectedText) {
        this.isTextSelecting = false;
        this.lastMoveX = 0;
        this.lastTouch = false;
        // 如果在划选初探时产生微小位移，立即复位回当前页，避免视口偏移
        if (this.isSlideRead && !this.isSimulationRead) {
          this.contentStyle = {
            transform: `translateX(${this.transformX}px)`
          };
        }
        setTimeout(() => {
          this.checkAndTriggerSelection();
        }, 80);
        return;
      }

      // 处理边缘侧滑返回手势
      if (this.isEdgeSwipe) {
        const triggered = this.edgeBackMoveDistance >= 60;
        this.edgeBackIndicatorVisible = false;
        this.isEdgeSwipe = false;
        this.edgeBackMoveDistance = 0;

        if (triggered) {
          const now = Date.now();
          // 如果在 2.5 秒内再次触发了边缘返回
          if (now - this.lastEdgeBackTriggerTime < 2500) {
            this.edgeBackToastVisible = false;
            this.toShelf();
            return;
          }
          // 第一次触发：弹出安卓经典的“再次滑动返回书架”防误触确认提示
          this.lastEdgeBackTriggerTime = now;
          this.edgeBackToastVisible = true;
          if (this.edgeBackToastTimer) {
            clearTimeout(this.edgeBackToastTimer);
          }
          this.edgeBackToastTimer = setTimeout(() => {
            this.edgeBackToastVisible = false;
          }, 2200);
          return;
        }
      }

      if (this.lastMoveX) {
        if (this.isSimulationRead) {
          const moveX = this.lastMoveX;
          this.lastMoveX = 0;
          if (moveX < -30) {
            this.nextPage();
          } else if (moveX > 30) {
            this.prevPage();
          }
          return;
        }
        this.transformX += this.lastMoveX;
        if (this.lastMoveX > 0) {
          // 上一页
          this.prevPage(this.windowSize.width - 16 - this.lastMoveX);
        } else {
          // 下一页
          this.nextPage(-(this.windowSize.width - 16) - this.lastMoveX);
        }
      } else if (Math.abs(this.lastMoveY) <= 3 && this.lastTouch) {
        this.eventHandler(this.lastTouch);
      }
      setTimeout(() => {
        this.lastTouch = false;
        this.lastMoveX = false;
        this.lastMoveY = false;
      }, 300);
    },
    epubClickHash(rect) {
      if (typeof rect.top !== "undefined") {
        this.scrollContent(
          rect.top -
            (this.$store.state.miniInterface
              ? this.getFirstParagraphPos().bottom
              : 0) -
            (window.webAppDistance | 0) -
            (this.$store.state.safeArea.top | 0),
          0,
          true
        );
      }
    },
    epubLocationChangeHandler(url) {
      function getPathname(path) {
        const a = document.createElement("a");
        a.href = path;
        return decodeURIComponent(a.pathname);
      }
      url = getPathname(url);
      // 判断是否跳转了其他章节
      const currentChapter = this.catalog[this.chapterIndex];
      if (currentChapter) {
        const chapterPrefix = this.content.replace(currentChapter.url, "");
        const iframeUrlPath = url.replace(chapterPrefix, "");
        let newChapterIndex = -1;
        for (let i = 0; i < this.catalog.length; i++) {
          if (this.catalog[i].url === iframeUrlPath) {
            newChapterIndex = i;
            break;
          }
        }
        if (newChapterIndex >= 0) {
          let book = { ...this.$store.getters.readingBook };
          book.index = newChapterIndex;
          this.$store.commit("setReadingBook", book);
          this.title = this.$store.getters.readingBook.catalog[
            newChapterIndex
          ].title;
        }
      }
    },
    eventHandler(point) {
      // console.log(point);
      if (this.checkSelection(true)) {
        // 选择文本
        this.ignoreNextClick = true;
        return;
      }
      if (
        this.popBookSourceVisible ||
        this.popBookShelfVisible ||
        this.popCataVisible ||
        this.readSettingsVisible
      ) {
        if (this.isEpub) {
          this.popBookSourceVisible = false;
          this.popBookShelfVisible = false;
          this.popCataVisible = false;
          this.readSettingsVisible = false;
        }
        return;
      }
      if (this.isAudio) {
        // 音频
        // 点击中部区域显示菜单
        if (!this.showReadBar) {
          this.showToolBar = !this.showToolBar;
        }
        return;
      }
      if (this.autoReading) {
        this.showToolBar = !this.showToolBar;
        return;
      }
      // 根据点击位置判断操作
      const midX = this.windowSize.width / 2;
      const midY = this.windowSize.height / 2;
      if (this.isEpub) {
        point.clientY =
          point.clientY +
          45 -
          (document.documentElement.scrollTop || document.body.scrollTop);
      }
      if (
        Math.abs(point.clientY - midY) <= this.windowSize.height * 0.2 &&
        Math.abs(point.clientX - midX) <= this.windowSize.width * 0.2
      ) {
        // 点击中部区域显示菜单
        if (!this.showReadBar) {
          this.showToolBar = !this.showToolBar;
        }
      } else if (this.$store.getters.config.clickMethod === "下一页") {
        // 全屏点击下一页
        this.showToolBar = false;
        this.nextPage();
        return;
      } else if (this.$store.getters.config.clickMethod === "不翻页") {
        // 全屏点击不翻页
        this.showToolBar = !this.showToolBar;
        return;
      } else if (this.isSlideRead) {
        if (point.clientX > midX) {
          // 点击右侧，下一页
          this.showToolBar = false;
          this.nextPage();
        } else if (point.clientX < midX) {
          // 点击左侧，上一页
          this.showToolBar = false;
          this.prevPage();
        }
      } else {
        if (point.clientY > midY) {
          // 点击下部，下一页
          this.showToolBar = false;
          this.nextPage();
        } else if (point.clientY < midY) {
          // 点击上部，上一页
          this.showToolBar = false;
          this.prevPage();
        }
      }
    },
    keydownHandler(event, force) {
      // console.log("keyup", event);
      if (
        this.popBookSourceVisible ||
        this.popBookShelfVisible ||
        this.popCataVisible ||
        this.readSettingsVisible ||
        this.showTextFilterPrompting
      ) {
        return;
      }
      if (!force && document.activeElement !== document.body) {
        return;
      }
      if (this.isAudio) {
        return;
      }
      const keyCodeMap = {
        37: "ArrowLeft",
        38: "ArrowUp",
        39: "ArrowRight",
        40: "ArrowDown",
        27: "Escape"
      };
      const eventKey = event.key || keyCodeMap[event.keyCode];
      switch (eventKey) {
        case "ArrowLeft":
          event.preventDefault && event.preventDefault();
          event.stopPropagation && event.stopPropagation();
          this.showToolBar = false;
          if (this.isSlideRead) {
            this.prevPage();
          } else {
            this.toLastChapter();
          }
          break;
        case "ArrowRight":
          event.preventDefault && event.preventDefault();
          event.stopPropagation && event.stopPropagation();
          this.showToolBar = false;
          if (this.isSlideRead) {
            this.nextPage();
          } else {
            this.toNextChapter();
          }
          break;
        case "ArrowUp":
          event.preventDefault && event.preventDefault();
          event.stopPropagation && event.stopPropagation();
          this.showToolBar = false;
          this.prevPage();
          break;
        case "ArrowDown":
          event.preventDefault && event.preventDefault();
          event.stopPropagation && event.stopPropagation();
          this.showToolBar = false;
          this.nextPage();
          break;
        case "Escape":
          this.toShelf();
          break;
      }
    },
    formatProgressTip(value) {
      return `第 ${value || this.progressValue}/${this.totalPages} 页`;
    },
    formatTime() {
      const now = new Date();
      const pad = v => (v >= 10 ? "" + v : "0" + v);
      this.timeStr = pad(now.getHours()) + ":" + pad(now.getMinutes());
    },
    checkSelection(show) {
      let text = "";
      let sel = null;
      if (window.getSelection) {
        sel = window.getSelection();
        text = sel.toString();
      } else if (document.selection && document.selection.type != "Control") {
        text = document.selection.createRange().text;
      }
      if (text && show) {
        setTimeout(() => {
          if (
            this.$store.getters.config.selectionAction === "过滤弹窗" ||
            this.$store.getters.config.selectionAction === "操作弹窗"
          ) {
            const currentSel = window.getSelection
              ? window.getSelection()
              : sel;
            const currentText = currentSel ? currentSel.toString() : text;
            this.showTextOperate(currentText || text, currentSel || sel);
          }
        }, 80);
      }
      return text;
    },
    showTextOperate(text, sel) {
      if (!text || !text.trim()) {
        return;
      }
      this.selectedActionText = text.trim();

      // 计算气泡弹窗在选区正上方/正下方的位置
      let left = window.innerWidth / 2;
      let top = window.innerHeight / 2;
      let placeBelow = false;

      if (sel && sel.rangeCount > 0) {
        try {
          const range = sel.getRangeAt(0);
          const rect = range.getBoundingClientRect();
          if (rect && rect.width) {
            left = rect.left + rect.width / 2;
            // 尽量放置在选中文本上方 10px，如果太靠顶则放在选区下方 10px
            if (rect.top >= 55) {
              top = rect.top - 10;
              placeBelow = false;
            } else {
              top = rect.bottom + 10;
              placeBelow = true;
            }
          }
          if (this.isMobile || this.isTouchActive) {
            // 提取选区的所有行选框矩形，作为高亮层展示在正文上方
            const clientRects = Array.from(
              range.getClientRects ? range.getClientRects() : [rect]
            );
            this.selectionHighlightRects = clientRects
              .filter(r => r.width > 0 && r.height > 0)
              .map(r => ({
                top: r.top,
                left: r.left,
                width: r.width,
                height: r.height
              }));

            // 提取完坐标后立即清空原生选区，彻底阻断 iOS Safari / 安卓长按呼出的原生上下文菜单（拷贝/查询/翻译/搜索网页）
            if (sel.removeAllRanges) {
              sel.removeAllRanges();
            }
          } else {
            this.selectionHighlightRects = [];
          }
        } catch (e) {
          // ignore
        }
      }

      // 边缘安全避让，防止超出屏幕
      left = Math.max(135, Math.min(window.innerWidth - 135, left));
      top = Math.max(20, Math.min(window.innerHeight - 50, top));

      this.textActionMenuPos = { top, left, placeBelow };
      this.textActionMenuVisible = true;
    },
    closeTextActionMenu(clearHighlight = true) {
      this.textActionMenuVisible = false;
      this.lastSelection = null;
      this.isTextSelecting = false;
      if (clearHighlight) {
        this.selectionHighlightRects = [];
      }
      if (this.selectionChangeTimer) {
        clearTimeout(this.selectionChangeTimer);
        this.selectionChangeTimer = null;
      }
      if (this.isMobile || this.isTouchActive) {
        if (window.getSelection) {
          try {
            window.getSelection().removeAllRanges();
          } catch (err) {
            // ignore
          }
        }
      }
      this.dismissCooldown = Date.now() + 450;
    },
    handleSelectionChange() {
      if (this.dismissCooldown && Date.now() < this.dismissCooldown) {
        return;
      }
      if (this.isDismissingMenu) {
        return;
      }
      if (
        !this.$store.getters.config ||
        (this.$store.getters.config.selectionAction !== "过滤弹窗" &&
          this.$store.getters.config.selectionAction !== "操作弹窗")
      ) {
        return;
      }
      // 核心防线：手指触屏拖拽或 PC 鼠标按住划选中，只标记选词状态，绝对不要提前清空选区，也不要提前弹窗打断！
      if (this.isTouchActive || this.isMouseDown) {
        if (this.isTouchActive) {
          this.isTextSelecting = true;
          this.lastMoveX = 0;
        }
        return;
      }

      const sel = window.getSelection ? window.getSelection() : null;
      if (!sel || sel.isCollapsed || sel.rangeCount === 0) {
        return;
      }
      const text = sel.toString().trim();
      if (!text) {
        return;
      }

      // 校验选区是否属于阅读正文区域，避免设置、目录、搜索等外部选区误触发
      const range = sel.getRangeAt(0);
      const container =
        this.$refs.contentRef ||
        (this.$el && this.$el.querySelector(".content"));
      if (container && range.commonAncestorContainer) {
        const node = range.commonAncestorContainer;
        const elem = node.nodeType === 1 ? node : node.parentNode;
        if (elem && !container.contains(elem)) {
          return;
        }
      }

      if (this.selectionChangeTimer) {
        clearTimeout(this.selectionChangeTimer);
      }
      // 手指抬起或 PC 鼠标划选防抖
      this.selectionChangeTimer = setTimeout(() => {
        this.checkAndTriggerSelection();
      }, 80);
    },
    checkAndTriggerSelection() {
      if (this.dismissCooldown && Date.now() < this.dismissCooldown) {
        return;
      }
      if (this.isDismissingMenu) {
        return;
      }
      if (
        !this.$store.getters.config ||
        (this.$store.getters.config.selectionAction !== "过滤弹窗" &&
          this.$store.getters.config.selectionAction !== "操作弹窗")
      ) {
        return;
      }
      const sel = window.getSelection ? window.getSelection() : null;
      if (!sel || sel.isCollapsed || sel.rangeCount === 0) {
        return;
      }
      const text = sel.toString().trim();
      if (!text) {
        return;
      }
      const range = sel.getRangeAt(0);
      const container =
        this.$refs.contentRef ||
        (this.$el && this.$el.querySelector(".content"));
      if (container && range.commonAncestorContainer) {
        const node = range.commonAncestorContainer;
        const elem = node.nodeType === 1 ? node : node.parentNode;
        if (elem && !container.contains(elem)) {
          return;
        }
      }
      this.showTextOperate(text, sel);
    },
    handleActionCopy() {
      const text = this.selectedActionText;
      this.closeTextActionMenu(true);
      if (!text) return;
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(
          () => {
            this.$message.success("已复制到剪贴板");
          },
          () => {
            this.fallbackCopy(text);
          }
        );
      } else {
        this.fallbackCopy(text);
      }
    },
    fallbackCopy(text) {
      const input = document.createElement("textarea");
      input.value = text;
      input.setAttribute("readonly", "");
      input.style.position = "fixed";
      input.style.top = "-9999px";
      input.style.left = "-9999px";
      input.style.opacity = "0";
      input.style.pointerEvents = "none";
      document.body.appendChild(input);
      input.select();
      input.setSelectionRange(0, 99999);
      try {
        document.execCommand("copy");
        this.$message.success("已复制到剪贴板");
      } catch (err) {
        this.$message.error("复制失败，请重试");
      }
      document.body.removeChild(input);
    },
    handleActionAddBookmark() {
      const text = this.selectedActionText;
      this.closeTextActionMenu(true);
      if (text) {
        this.showAddBookmark(text);
      }
    },
    handleActionAddFilter() {
      const text = this.selectedActionText;
      this.closeTextActionMenu(true);
      if (text) {
        this.showTextFilterPrompt(text);
      }
    },
    async showTextFilterPrompt(text) {
      if (this.showTextFilterPrompting) {
        return;
      }
      if (!text.replace(/^\s+/, "").replace(/\s+$/, "")) {
        return;
      }

      const replaceRule = Object.assign({}, defaultReplaceRule, {
        name: "文本替换",
        pattern: text,
        replacement: "",
        isRegex: false,
        isEnabled: true,
        scope:
          this.$store.getters.readingBook.name +
          ";" +
          this.$store.getters.readingBook.bookUrl
      });
      this.showTextFilterPrompting = true;
      eventBus.$emit("showReplaceRuleForm", replaceRule, true, () => {
        this.showTextFilterPrompting = false;
      });
      // const h = this.$createElement;
      // const bgColor = this.isNight ? "#121212" : "#eee";
      // const preEle = h(
      //   "pre",
      //   {
      //     key: "" + new Date().getTime(),
      //     attrs: {
      //       contenteditable: "true"
      //     },
      //     style: `margin-top: 10px;background: ${bgColor};padding: 10px;border: 1px solid ${bgColor};border-radius: 5px;white-space: pre-wrap;word-wrap: break-word;word-break: break-all;`
      //   },
      //   text
      // );
      // const result = await this.$prompt(
      //   h("div", null, [
      //     h("p", null, "是否要将下列文字替换为输入内容:"),
      //     preEle
      //   ]),
      //   "操作确认",
      //   {
      //     inputPlaceholder: "留空为过滤"
      //   }
      // ).catch(() => {});
      // if (result && result.action === "confirm") {
      //   text = ((preEle.elm || {}).innerText || "")
      //     .replace(/^\s+/, "")
      //     .replace(/\s+$/, "");
      //   if (text) {
      //     this.$store.commit("addFilterRule", {
      //       name: "文本替换",
      //       pattern: text,
      //       replacement: result.value || "",
      //       isRegex: false,
      //       isEnabled: true,
      //       scope:
      //         this.$store.getters.readingBook.name +
      //         ";" +
      //         this.$store.getters.readingBook.bookUrl
      //     });
      //   } else {
      //     this.$message.error("过滤内容为空!");
      //   }
      // }
      // this.showTextFilterPrompting = false;
    },
    async showAddBookmark(text) {
      if (this.showAddBookmarking) {
        return;
      }
      let pureText = text ? text.replace(/^\s+/, "").replace(/\s+$/, "") : "";
      if (!pureText) {
        return;
      }
      const paragraph = this.getContentMatchParagraph(pureText, 1, 0.6);
      if (!paragraph) {
        this.$message.error("未能定位选中文本所在的段落");
        return;
      }

      // 忠实呈现用户选择：书签内容严格只保存并展示用户实际划选的高亮片段，彻底移除补全拼接后续段落的旧逻辑
      const bookmark = Object.assign({}, defaultBookmark, {
        bookName: this.$store.getters.readingBook.name,
        bookAuthor: this.$store.getters.readingBook.author,
        chapterIndex: this.chapterIndex,
        chapterPos: this.currentPage,
        chapterName: this.title,
        bookText: pureText,
        content: ""
      });
      this.showAddBookmarking = true;
      eventBus.$emit("showBookmarkForm", bookmark, true, () => {
        this.showAddBookmarking = false;
      });
    },
    toogleNight() {
      if (this.isNight) {
        this.$store.commit("setNightTheme", false);
      } else {
        this.$store.commit("setNightTheme", true);
      }
    },
    fetchVoiceList() {
      let localVoices = [];
      if (window.speechSynthesis && window.speechSynthesis.getVoices) {
        // 本地系统语音仅保留：中文普通话、粤语、英语各精选语音，其他外语一概过滤
        const allVoices = window.speechSynthesis.getVoices();
        const filtered = allVoices.filter(v => {
          const l = (v.lang || "").toLowerCase();
          // 中文普通话/国语
          if (
            l.startsWith("zh-cn") ||
            l.startsWith("zh_cn") ||
            l === "zh" ||
            l.startsWith("zh-hans")
          )
            return true;
          // 粤语 (香港/广东)
          if (
            l.startsWith("zh-hk") ||
            l.startsWith("zh_hk") ||
            l.startsWith("zh-yue") ||
            l.startsWith("yue")
          )
            return true;
          // 英语 (精选美式或英式)
          if (
            l.startsWith("en-us") ||
            l.startsWith("en_us") ||
            l.startsWith("en-gb") ||
            l.startsWith("en_gb") ||
            l === "en"
          )
            return true;
          return false;
        });

        // 细分归类：中文优先、粤语次之、英语其后，并根据语音名称去重（解决 iOS/Safari 等系统上同一名称存在默认版与增强版的重复项）
        const zhMap = new Map();
        const yueMap = new Map();
        const enMap = new Map();

        filtered.forEach(v => {
          const l = (v.lang || "").toLowerCase();
          const cleanName = (v.name || "").trim();
          if (
            l.startsWith("zh-hk") ||
            l.startsWith("zh_hk") ||
            l.startsWith("zh-yue") ||
            l.startsWith("yue")
          ) {
            // 若同一名称存在多条（如普通版和增强版/高品质版），优先保留包含 enhanced/compact 或非 compact 的优质语音
            if (
              !yueMap.has(cleanName) ||
              v.name.includes("Enhanced") ||
              v.name.includes("增强")
            ) {
              yueMap.set(cleanName, v);
            }
          } else if (l.startsWith("en")) {
            if (
              !enMap.has(cleanName) ||
              v.name.includes("Enhanced") ||
              v.name.includes("增强")
            ) {
              enMap.set(cleanName, v);
            }
          } else {
            if (
              !zhMap.has(cleanName) ||
              v.name.includes("Enhanced") ||
              v.name.includes("增强")
            ) {
              zhMap.set(cleanName, v);
            }
          }
        });

        const zhList = Array.from(zhMap.values());
        const yueList = Array.from(yueMap.values());
        const enList = Array.from(enMap.values());

        // 合并精选本地语音 (英语只保留前1~2个代表语音，避免英文语音占满)
        const selectedLocal = [...zhList, ...yueList, ...enList.slice(0, 2)];
        localVoices = selectedLocal.map(v => {
          let label = v.name;
          const l = (v.lang || "").toLowerCase();
          if (
            l.startsWith("zh-hk") ||
            l.startsWith("zh_hk") ||
            l.startsWith("zh-yue") ||
            l.startsWith("yue")
          ) {
            label += " (本地粤语)";
          } else if (l.startsWith("en")) {
            label += " (本地英语)";
          } else {
            label += " (本地中文)";
          }
          return {
            name: label,
            nativeVoice: v,
            isEdge: false,
            lang: v.lang
          };
        });
      }

      // 本地高质量系统语音
      this.voiceList = localVoices;
      if (!this.voiceName && this.voiceList.length) {
        this.voiceName = this.voiceList[0].name;
      }
    },
    changeSpeechRate(rate) {
      this.speechRate = rate;
      if (this.speechSpeaking) {
        this.restartSpeech();
      }
    },
    changeSpeechPitch(pitch) {
      this.speechPitch = pitch;
      if (this.speechSpeaking) {
        this.restartSpeech();
      }
    },
    changeSpeechMinutes(minute) {
      this.speechMinutes = minute;
      if (minute) {
        this.speechEndTime = new Date().getTime() + minute * 60 * 1000;
      } else {
        this.speechEndTime = 0;
      }
    },
    getAllParagraphs() {
      if (!this.$refs.bookContentRef || !this.$refs.bookContentRef.$el) {
        return [];
      }
      const list = this.$refs.bookContentRef.$el.querySelectorAll(
        ".reading-chapter h3, .reading-chapter p, h3, p"
      );
      return Array.from(list).filter(
        el => el && el.innerText && el.innerText.trim().length > 0
      );
    },
    startSpeech(targetIndex) {
      if (this.error) {
        return;
      }
      if (!this.voiceName) {
        if (this.voiceList.length) {
          this.voiceName = this.voiceList[0].name;
        } else {
          return;
        }
      }
      const voice =
        this.voiceList.find(v => v.name === this.voiceName) ||
        this.voiceList[0];
      if (!voice) {
        return;
      }

      if (this.speechEndTime > 0 && new Date().getTime() > this.speechEndTime) {
        this.$message.info("定时关闭朗读");
        this.stopSpeech();
        return;
      }

      const paragraphs = this.getAllParagraphs();
      if (!paragraphs.length) {
        return;
      }

      let idx =
        typeof targetIndex === "number"
          ? targetIndex
          : this.speechParagraphIndex;
      if (idx < 0 || idx >= paragraphs.length) {
        const currentInView = this.getCurrentParagraph();
        idx = paragraphs.indexOf(currentInView);
        if (idx < 0) idx = 0;
      }

      this.speechParagraphIndex = idx;
      const paragraph = paragraphs[idx];

      paragraphs.forEach(p => {
        if (p !== paragraph && p.className.includes("reading")) {
          p.className = p.className.replace(/\breading\b/g, "").trim();
        }
      });
      paragraph.className = "reading";
      this.showParagraph(paragraph, true);
      this.speechSpeaking = true;
      this.skipAutoNext = false;

      if (window.speechSynthesis && window.speechSynthesis.speaking) {
        window.speechSynthesis.cancel();
      }
      this.utterance = new SpeechSynthesisUtterance(paragraph.innerText);

      this.utterance.onstart = () => {
        this.speechSpeaking = true;
        this.skipAutoNext = false;
      };
      this.utterance.onend = () => {
        if (!this.skipAutoNext) {
          this.speechNext();
        } else {
          this.skipAutoNext = false;
          this.speechSpeaking = false;
        }
      };
      this.utterance.onerror = () => {
        this.speechSpeaking = false;
      };
      if (voice.nativeVoice) {
        this.utterance.voice = voice.nativeVoice;
      }
      this.utterance.pitch = this.speechPitch;
      this.utterance.rate = this.speechRate;
      window.speechSynthesis.speak(this.utterance);
    },
    stopSpeech() {
      try {
        this.skipAutoNext = true;
        this.speechSpeaking = false;
        if (window.speechSynthesis) {
          window.speechSynthesis.cancel();
        }
        const paragraphs = this.getAllParagraphs();
        paragraphs.forEach(p => {
          if (p.className.includes("reading")) {
            p.className = p.className.replace(/\breading\b/g, "").trim();
          }
        });
      } catch (error) {
        //
      }
    },
    restartSpeech() {
      this.stopSpeech();
      setTimeout(() => {
        this.startSpeech(this.speechParagraphIndex);
      }, 100);
    },
    toggleSpeech() {
      this.speechSpeaking ? this.stopSpeech() : this.startSpeech();
    },
    speechPrev() {
      this.stopSpeech();
      if (this.speechParagraphIndex > 0) {
        this.speechParagraphIndex--;
        this.startSpeech(this.speechParagraphIndex);
      } else {
        this.$once("showContent", () => {
          setTimeout(() => {
            const newList = this.getAllParagraphs();
            this.speechParagraphIndex = Math.max(0, newList.length - 1);
            this.startSpeech(this.speechParagraphIndex);
          }, 300);
        });
        this.toLastChapter();
      }
    },
    speechNext() {
      this.stopSpeech();
      const list = this.getAllParagraphs();
      if (this.speechParagraphIndex + 1 < list.length) {
        this.speechParagraphIndex++;
        this.startSpeech(this.speechParagraphIndex);
      } else {
        this.$once("showContent", () => {
          setTimeout(() => {
            this.speechParagraphIndex = 0;
            this.startSpeech(0);
          }, 300);
        });
        this.toNextChapter();
      }
    },
    getCurrentParagraph() {
      const readingEle = this.$refs.bookContentRef.$el.querySelectorAll(
        ".reading"
      );
      let currentParagraph = null;
      if (!readingEle.length) {
        // 没有正在读的段落，遍历找到当前页面的第一段
        const list = this.$refs.bookContentRef.$el.querySelectorAll("h3,p");
        for (let i = 0; i < list.length; i++) {
          const elePos = list[i].getBoundingClientRect();
          if (this.isSlideRead) {
            // 段尾出现在视野里
            if (elePos.right > 0) {
              currentParagraph = list[i];
              break;
            }
          } else {
            // 段尾出现在视野里
            if (
              elePos.bottom >
              30 +
                20 +
                (window.webAppDistance | 0) +
                (this.$store.state.safeArea.top | 0)
            ) {
              currentParagraph = list[i];
              break;
            }
          }
        }
      } else {
        currentParagraph = readingEle[0];
      }
      return currentParagraph;
    },
    getPrevParagraph() {
      const current = this.getCurrentParagraph();
      const list = this.$refs.bookContentRef.$el.querySelectorAll("h3,p");
      for (let i = 0; i < list.length; i++) {
        if (i > 0 && current === list[i]) {
          return list[i - 1];
        }
      }
      return null;
    },
    getNextParagraph() {
      const current = this.getCurrentParagraph();
      const list = this.$refs.bookContentRef.$el.querySelectorAll("h3,p");
      for (let i = 0; i < list.length; i++) {
        if (current === list[i]) {
          return list[i + 1];
        }
      }
      return null;
    },
    exitRead() {
      this.stopSpeech();
      const current = this.getCurrentParagraph();
      this.showReadBar = false;
      this.showParagraph(current);
    },
    showParagraph(paragraph, scroll) {
      if (!paragraph) {
        return;
      }
      if (this.isSlideRead) {
        // 跳转位置
        this.$nextTick(() => {
          const pos = paragraph.getBoundingClientRect();
          if (pos.left > this.windowSize.width - 16) {
            this.showPage(
              Math.round(pos.left / (this.windowSize.width - 16)) + 1,
              0
            );
          }
        });
      } else if (scroll) {
        // 跳转位置
        this.$nextTick(() => {
          const pos = paragraph.getBoundingClientRect();
          this.scrollContent(
            pos.top -
              (this.$store.state.miniInterface
                ? this.getFirstParagraphPos().bottom
                : 0) -
              (window.webAppDistance | 0) -
              (this.$store.state.safeArea.top | 0),
            0
          );
        });
      }
    },
    getFirstParagraphPos() {
      return this.$refs.top.getBoundingClientRect();
    },
    scrollHandler() {
      const scrollTop =
        document.documentElement.scrollTop || document.body.scrollTop;
      if (!this.isSlideRead) {
        this.currentPage = Math.round(
          (scrollTop + this.windowSize.height) /
            (this.windowSize.height - this.scrollOffset)
        );
      }
      if (this.isScrollRead) {
        const lastScrollTop = this.lastScrollTop || 0;
        if (lastScrollTop > 0 && scrollTop == 0) {
          // 往上滚动到顶
          // if (!this.preCaching) {
          //   this.preCaching = true;
          //   const prevIndex = this.showChapterList[0].index - 1;
          //   if (prevIndex > 0) {
          //     this.showPrevChapterSize = this.chapterIndex - prevIndex;
          //     this.loadShowChapter(prevIndex).then(() => {
          //       setTimeout(() => {
          //         this.preCaching = false;
          //       }, 3000);
          //     });
          //   }
          // }
        } else if (
          scrollTop >
          document.documentElement.scrollHeight - 2 * this.windowSize.height // 倒数第三页
        ) {
          // 往下滚动到 倒数第三页
          if (!this.preCaching && this.startSavePosition) {
            this.preCaching = true;
            let nextIndex = this.chapterIndex + 1;
            if (this.showChapterList.length) {
              nextIndex =
                this.showChapterList[this.showChapterList.length - 1].index + 1;
            }
            this.showNextChapterSize = nextIndex - this.chapterIndex;
            // console.log("到底部了，加载下一章");
            this.loadShowChapter(nextIndex)
              .then(() => {
                this.computeShowChapterList();
                this.preCaching = false;
              })
              .catch(() => {
                this.preCaching = false;
              });
          }
        }
      }
      if (this.isScrollRead) {
        this.checkReadingProgress();
      }
      this.lastScrollTop = scrollTop;
      this.scrollTimer && clearTimeout(this.scrollTimer);
      this.scrollTimer = setTimeout(this.saveReadingPosition, 100);
    },
    beforeReadMethodChange() {
      this.currentParagraph = this.getCurrentParagraph();
    },
    // 只会在进入的时候调用
    showPosition(pos, callback) {
      if (this.isAudio) {
        // seek
        if (!this.$refs.bookContentRef) {
          setTimeout(() => {
            this.showPosition(pos, callback);
          }, 10);
          return;
        }
        this.$refs.bookContentRef.ensureSeekTime(pos);
      } else if (this.isEpub || this.isCarToon) {
        // 跳转
        this.scrollContent(pos, 0, true);
        if (this.isEpub) {
          this.$once("iframeLoad", () => {
            this.scrollContent(pos, 0, true);
            callback && callback();
          });
        }
      } else {
        if (!this.$refs.bookContentRef) {
          setTimeout(() => {
            this.showPosition(pos, callback);
          }, 10);
          return;
        }
        const list = this.$refs.bookContentRef.$el.querySelectorAll(
          ".reading-chapter h3,p"
        );
        for (let i = 0; i < list.length; i++) {
          if (
            list[i].dataset &&
            typeof list[i].dataset.pos !== "undefined" &&
            +list[i].dataset.pos >= pos
          ) {
            this.showParagraph(list[i], true);
            break;
          }
        }
        callback && callback();
      }
    },
    saveReadingPosition() {
      try {
        if (this.error || !this.startSavePosition) {
          return;
        }
        let position = 0;
        if (this.isAudio) {
          position = this.$refs.bookContentRef
            ? this.$refs.bookContentRef.currentTime
            : 0;
        } else if (this.isEpub || this.isCarToon) {
          position =
            document.documentElement.scrollTop || document.body.scrollTop;
        } else {
          // 更新当前章节 和 当前段落
          if (this.preCaching) {
            return;
          }
          this.currentParagraph = this.getCurrentParagraph();
          if (this.currentParagraph) {
            // 找到最近的 .chapter-content
            let currentChapter = this.currentParagraph;
            while (currentChapter.className.indexOf("chapter-content") < 0) {
              currentChapter = currentChapter.parentNode;
              if (currentChapter === this.$refs.bookContentRef.$el) {
                break;
              }
            }
            if (currentChapter) {
              if (
                currentChapter.dataset &&
                typeof currentChapter.dataset.index !== "undefined"
              ) {
                const chapterIndex = +currentChapter.dataset.index;
                if (chapterIndex != this.$store.getters.readingBook.index) {
                  let book = { ...this.$store.getters.readingBook };
                  book.index = chapterIndex;
                  this.$store.commit("setReadingBook", book);
                  // 保存阅读进度
                  this.saveBookProgress();
                  this.title = this.$store.getters.readingBook.catalog[
                    chapterIndex
                  ].title;
                }
              }
              position = currentChapter.innerText.indexOf(
                this.currentParagraph.innerText
              );
            }
          }
        }
        setCache(
          "bookChapterProgress@" +
            this.$store.getters.readingBook.name +
            "_" +
            this.$store.getters.readingBook.author,
          position
        );
      } catch (error) {
        //
      }
    },
    autoShowPosition(immediate) {
      const handler = () => {
        setTimeout(() => {
          this.startSavePosition = true;
        }, 2000);
        if (this.error) {
          return;
        }
        const lastPosition = getCache(
          "bookChapterProgress@" +
            this.$store.getters.readingBook.name +
            "_" +
            this.$store.getters.readingBook.author
        );
        if (lastPosition && +lastPosition) {
          this.$nextTick(() => {
            this.showPosition(+lastPosition, () => {
              this.startSavePosition = true;
            });
          });
        }
      };
      if (immediate) {
        handler();
      } else {
        this.$once("showContent", handler);
      }
    },
    wakeLock() {
      if ("WakeLock" in window && "request" in window.WakeLock) {
        let wakeLock = null;
        const requestWakeLock = () => {
          const controller = new AbortController();
          const signal = controller.signal;
          window.WakeLock.request("screen", { signal }).catch(e => {
            if (e.name === "AbortError") {
              // console.log("Wake Lock was aborted");
            } else {
              // console.error(`${e.name}, ${e.message}`);
            }
          });
          // console.log("Wake Lock is active");
          return controller;
        };

        wakeLock = requestWakeLock();

        const handleVisibilityChange = () => {
          if (wakeLock !== null && document.visibilityState === "visible") {
            wakeLock = requestWakeLock();
          }
        };

        document.addEventListener("visibilitychange", handleVisibilityChange);
        document.addEventListener("fullscreenchange", handleVisibilityChange);
        return () => {
          if (wakeLock != null) {
            wakeLock.abort();
            wakeLock = null;
          }
          document.removeEventListener(
            "visibilitychange",
            handleVisibilityChange
          );
          document.removeEventListener(
            "fullscreenchange",
            handleVisibilityChange
          );
        };
      } else if ("wakeLock" in navigator && "request" in navigator.wakeLock) {
        let wakeLock = null;
        const requestWakeLock = async () => {
          try {
            wakeLock = await navigator.wakeLock.request("screen");
            wakeLock.addEventListener("release", () => {
              // console.log("Wake Lock was released");
            });
            // console.log("Wake Lock is active");
          } catch (e) {
            // console.error(`${e.name}, ${e.message}`);
          }
        };
        requestWakeLock();
        const handleVisibilityChange = () => {
          if (wakeLock !== null && document.visibilityState === "visible") {
            requestWakeLock();
          }
        };
        document.addEventListener("visibilitychange", handleVisibilityChange);
        document.addEventListener("fullscreenchange", handleVisibilityChange);
        return () => {
          if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
          }
          document.removeEventListener(
            "visibilitychange",
            handleVisibilityChange
          );
          document.removeEventListener(
            "fullscreenchange",
            handleVisibilityChange
          );
        };
      }
    },
    lazyloadHandler() {
      if (!this.isAudio) {
        this.computePages();
      }
    },
    openOfflineCacheDialog() {
      this.showOfflineCacheDialog = true;
      this.computeBookCacheStats();
    },
    openOfflineCacheFromCatalog() {
      this.popCataVisible = false;
      this.openOfflineCacheDialog();
    },
    async computeBookCacheStats() {
      const book = this.$store.getters.readingBook;
      if (!book || !book.bookUrl || !window.$cacheStorage) {
        this.cachedStats = {
          cachedCount: 0,
          totalBytes: 0,
          sizeText: "0 B",
          cachedMap: {}
        };
        return;
      }
      const cachePrefix =
        "localCache@" +
        book.name +
        "_" +
        book.author +
        "@" +
        book.bookUrl +
        "@chapterContent-";
      const cachedMap = {};
      let totalBytes = 0;
      let count = 0;
      try {
        await window.$cacheStorage.iterate((value, key) => {
          if (key && key.startsWith(cachePrefix)) {
            const idxStr = key.replace(cachePrefix, "");
            const idx = parseInt(idxStr);
            if (!isNaN(idx)) {
              cachedMap[idx] = true;
              count++;
              if (value && typeof value === "object") {
                const str = JSON.stringify(value);
                totalBytes += str.length * 2;
              } else if (typeof value === "string") {
                totalBytes += value.length * 2;
              }
            }
          }
        });
        this.cachedStats = {
          cachedCount: count,
          totalBytes: totalBytes,
          sizeText: formatSize(totalBytes),
          cachedMap: cachedMap
        };
      } catch (e) {
        // ignore
      }
    },
    checkReadingProgress() {
      if (this.hasPreloadedThisChapter) return;
      const book = this.$store.getters.readingBook;
      if (!book || !book.catalog || !book.catalog.length) return;
      const currentIdx =
        typeof book.index === "number" ? book.index : this.chapterIndex;
      if (currentIdx >= book.catalog.length - 1) return;

      let reachedOneThird = false;
      if (this.isScrollRead) {
        const scrollTop =
          document.documentElement.scrollTop || document.body.scrollTop;
        const scrollableHeight =
          document.documentElement.scrollHeight - this.windowSize.height;
        const progress =
          scrollableHeight > 0 ? scrollTop / scrollableHeight : 0;
        if (progress >= 0.33) {
          reachedOneThird = true;
        }
      } else {
        if (this.totalPages > 1) {
          // 当翻过当前章节前 1/3 的页数时触发（例如 3 页翻到第 2 页，或 6 页翻到第 3 页）
          const progress = (this.currentPage - 1) / this.totalPages;
          if (
            progress >= 0.33 ||
            this.currentPage >= Math.ceil(this.totalPages / 3.0)
          ) {
            reachedOneThird = true;
          }
        } else {
          // 只有 1 页的短章节，停留达到设定时间后判定已读 1/3
          reachedOneThird = true;
        }
      }

      if (reachedOneThird) {
        this.hasPreloadedThisChapter = true;
        this.triggerSilentPreload(currentIdx);
      }
    },
    triggerSilentPreload(currentIndex) {
      if (this.isSilentPreloading) return;
      const book = this.$store.getters.readingBook;
      if (!book || !book.bookUrl || !this.catalog || !this.catalog.length)
        return;

      // 严格只预读后续 1 章
      const nextIdx = currentIndex + 1;
      if (nextIdx >= this.catalog.length) return;
      const chapter = this.catalog[nextIdx];
      if (!chapter) return;

      const cacheKey =
        "localCache@" +
        book.name +
        "_" +
        book.author +
        "@" +
        book.bookUrl +
        "@chapterContent-" +
        chapter.index;

      if (window.$cacheStorage) {
        window.$cacheStorage
          .getItem(cacheKey)
          .then(cached => {
            // 若后续 1 章已经在手机存储中，直接跳过，零网络开销
            if (cached) return;

            this.isSilentPreloading = true;
            this.getBookContent(
              chapter.index,
              { timeout: 18000, silent: true },
              false,
              true
            )
              .then(res => {
                this.isSilentPreloading = false;
                if (res && res.data && res.data.isSuccess) {
                  this.computeBookCacheStats();
                  if (
                    this.$refs.popCata &&
                    this.$refs.popCata.computeCachedCata
                  ) {
                    this.$refs.popCata.computeCachedCata();
                  }
                }
              })
              .catch(() => {
                this.isSilentPreloading = false;
              });
          })
          .catch(() => {
            this.isSilentPreloading = false;
          });
      }
    },
    cacheChapterContent(cacheCount) {
      if (!this.catalog || !this.catalog.length) {
        this.$message.warning("尚未加载目录，无法执行离线缓存");
        return;
      }
      let cacheChapterList = [];
      const currentIdx =
        this.chapterIndex >= 0
          ? this.chapterIndex
          : (this.$store.getters.readingBook &&
              this.$store.getters.readingBook.index) ||
            0;
      if (cacheCount === true) {
        cacheChapterList = this.catalog.slice(currentIdx + 1);
        if (!cacheChapterList.length) {
          cacheChapterList = [...this.catalog];
        }
      } else {
        const start = currentIdx + 1;
        const end = Math.min(this.catalog.length, start + cacheCount);
        cacheChapterList = this.catalog.slice(start, end);
      }
      if (!cacheChapterList.length) {
        this.$message.info("后续没有更多章节需要缓存");
        return;
      }

      const uncachedList = cacheChapterList.filter(
        v => !this.cachedStats.cachedMap[v.index]
      );
      if (!uncachedList.length) {
        this.$message.success("所选章节已全部保存在手机存储中，无需重复下载！");
        return;
      }

      this.isCachingContent = true;
      this.totalCachingTarget = uncachedList.length;
      this.currentCachingProgress = 0;
      this.cachingContentTip = `正在缓存：0 / ${uncachedList.length} 章`;

      this.cachingHandler = LimitResquest(3, handler => {
        this.currentCachingProgress = handler.requestCount;
        this.cachingContentTip = `正在缓存：${handler.requestCount} / ${uncachedList.length} 章`;
        if (handler.isEnd()) {
          this.$message.success("批量离线缓存下载完成！");
          this.isCachingContent = false;
          this.cachingContentTip = "";
          this.computeBookCacheStats();
          if (this.$refs.popCata && this.$refs.popCata.computeCachedCata) {
            this.$refs.popCata.computeCachedCata();
          }
        }
      });

      uncachedList.forEach(v => {
        this.cachingHandler(() => {
          return this.getBookContent(
            v.index,
            {
              timeout: 25000,
              silent: true
            },
            false,
            true
          );
        });
      });
    },
    cancelCaching() {
      if (this.cachingHandler && this.cachingHandler.cancel) {
        this.cachingHandler.cancel();
        this.isCachingContent = false;
        this.cachingContentTip = "";
        this.$message.info("已停止缓存");
        this.computeBookCacheStats();
      }
    },
    async clearCurrentBookCache() {
      const confirmed = await this.$confirm(
        "确定要清空本书在手机浏览器中的全部离线缓存吗？（清空后释放存储，但离线阅读需重新下载）",
        "提示",
        {
          confirmButtonText: "确定清空",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).catch(() => false);
      if (!confirmed) return;

      const book = this.$store.getters.readingBook;
      if (!book || !book.bookUrl || !window.$cacheStorage) return;
      const cachePrefix =
        "localCache@" +
        book.name +
        "_" +
        book.author +
        "@" +
        book.bookUrl +
        "@chapterContent-";
      const keysToRemove = [];
      await window.$cacheStorage.iterate((value, key) => {
        if (key && key.startsWith(cachePrefix)) {
          keysToRemove.push(key);
        }
      });
      await Promise.all(
        keysToRemove.map(k => window.$cacheStorage.removeItem(k))
      );
      this.$message.success(
        `已成功清空本书离线缓存，共清理 ${keysToRemove.length} 个章节`
      );
      await this.computeBookCacheStats();
      if (this.$refs.popCata && this.$refs.popCata.computeCachedCata) {
        this.$refs.popCata.computeCachedCata();
      }
    },
    startAutoReading() {
      this.showToolBar = false;
      this.autoReading = true;
      this.autoRead();
    },
    autoRead() {
      if (!this.autoReading) {
        return;
      }
      if (this.showToolBar) {
        this.autoReadingTimer = setTimeout(() => {
          this.autoRead();
        }, 300);
        return;
      }
      if (this.config.autoReadingMethod === "像素滚动") {
        this.autoReadByPixel();
        return;
      }
      const current = this.getCurrentParagraph();
      const next = this.getNextParagraph();
      if (next) {
        current.className = "reading";
        next.className = "";
        // 计算当前段落
        let delayTime = this.config.autoReadingLineTime;
        try {
          const currentPos = current.getBoundingClientRect();
          delayTime =
            delayTime *
            Math.ceil(
              currentPos.height / this.config.fontSize / this.config.lineHeight
            );
        } catch (error) {
          //
        }
        // console.log(delayTime, next);
        this.autoReadingTimer = setTimeout(() => {
          current.className = "";
          next.className = "reading";
          this.showParagraph(next, true);

          setTimeout(() => {
            this.autoRead();
          }, 32);
        }, delayTime);
      } else {
        // 下一章
        this.$once("showContent", () => {
          setTimeout(() => {
            this.autoRead();
          }, 100);
        });
        this.toNextChapter(() => {
          this.autoReading = false;
        });
      }
    },
    autoReadByPixel() {
      if (!this.autoReading) {
        return;
      }
      if (this.showToolBar) {
        this.autoReadingTimer = setTimeout(() => {
          this.autoRead();
        }, 300);
        return;
      }
      if (this.config.autoReadingMethod !== "像素滚动") {
        this.autoRead();
        return;
      }
      const scrollTop =
        document.documentElement.scrollTop || document.body.scrollTop;
      if (
        scrollTop + this.windowSize.height <
        document.documentElement.scrollHeight
      ) {
        // console.log(delayTime, next);
        this.autoReadingTimer = setTimeout(() => {
          // 滚动
          this.scrollContent(this.config.autoReadingPixel, 0);
          this.autoReadByPixel();
        }, this.config.autoReadingLineTime);
      } else {
        // 下一章
        this.$once("showContent", () => {
          setTimeout(() => {
            this.autoReadByPixel();
          }, 100);
        });
        this.toNextChapter(() => {
          this.autoReading = false;
        });
      }
    },
    stopAutoReading() {
      if (this.autoReadingTimer) {
        clearInterval(this.autoReadingTimer);
      }
      this.autoReading = false;
      const current = this.getCurrentParagraph();
      current.className = "";
    },
    toggleAutoReading() {
      if (this.autoReading) {
        this.stopAutoReading();
      } else {
        this.startAutoReading();
      }
    },
    showReadingBookInfo() {
      let book = { ...this.$store.getters.readingBook };
      const shelfBook = this.$store.getters.shelfBooks.find(
        v => v.bookUrl === book.bookUrl
      );
      book = Object.assign(book, shelfBook || {});
      eventBus.$emit("showBookInfoDialog", book);
    },
    formatChinese(text) {
      if (this.isEpub || this.isAudio || this.isCbz || this.isCarToon) {
        return text;
      }
      if (this.config.chineseFont === "简体") {
        return simplized(text);
      } else {
        return traditionalized(text);
      }
    },
    showSearchBookContentDialog() {
      let book = { ...this.$store.getters.readingBook };
      const shelfBook = this.$store.getters.shelfBooks.find(
        v => v.bookUrl === book.bookUrl
      );
      book = Object.assign(book, shelfBook || {});
      eventBus.$emit("showSearchBookContentDialog", book);
    },
    showMatchKeyword(data) {
      if (this._inactive) {
        return;
      }
      if (!this.$refs.bookContentRef) {
        setTimeout(() => {
          this.showMatchKeyword(data);
        }, 10);
        return;
      }
      try {
        const list = this.$refs.bookContentRef.$el.querySelectorAll(
          ".reading-chapter h3,p"
        );
        let matchCount = 0;
        for (let i = 0; i < list.length; i++) {
          const pContent = list[i].innerText;
          let startIndex = -1;
          let isFound = false;
          // eslint-disable-next-line no-constant-condition
          while (true) {
            startIndex = pContent.indexOf(data.query, startIndex + 1);
            if (startIndex >= 0) {
              matchCount++;
              if (matchCount === data.resultCountWithinChapter + 1) {
                isFound = true;
                this.showParagraph(list[i], true);
                break;
              }
            } else {
              break;
            }
          }
          if (isFound) {
            break;
          }
        }
      } catch (error) {
        // console.error(error);
      }
    },
    getParagraphListInView() {
      // 获取视口内的所有段落
      const list = this.$refs.bookContentRef.$el.querySelectorAll("h3,p");
      const paragraphList = [];
      for (let i = 0; i < list.length; i++) {
        const elePos = list[i].getBoundingClientRect();
        if (this.isSlideRead) {
          // 段尾出现在视野里
          if (elePos.right > 0 && elePos.left > 0) {
            paragraphList.push(list[i]);
          }
        } else {
          // 段尾出现在视野里
          if (
            elePos.bottom >
              30 +
                20 +
                (window.webAppDistance | 0) +
                (this.$store.state.safeArea.top | 0) &&
            elePos.bottom < this.windowSize.height
          ) {
            paragraphList.push(list[i]);
          }
        }
      }
      return paragraphList;
    },
    showBookmarkDialog() {
      let book = { ...this.$store.getters.readingBook };
      const shelfBook = this.$store.getters.shelfBooks.find(
        v => v.bookUrl === book.bookUrl
      );
      book = Object.assign(book, shelfBook || {});
      eventBus.$emit("showBookmarkDialog", book);
    },
    getContentMatchParagraph(text, distance, minDistance) {
      if (!text || !text.trim()) return null;
      const cleanTarget = text.replace(symboRegex, "").trim();
      if (!cleanTarget) return null;

      try {
        const list = this.$refs.bookContentRef.$el.querySelectorAll(
          ".reading-chapter h3,p"
        );
        if (!list || !list.length) return null;

        // 1. 优先精准子串/包含匹配：只要某段落包含用户划选的文字，直接命中该段落
        for (let i = 0; i < list.length; i++) {
          const rawText = list[i].innerText || "";
          if (rawText.includes(text.trim())) {
            return list[i];
          }
          const cleanParagraph = rawText.replace(symboRegex, "").trim();
          if (cleanParagraph && cleanParagraph.includes(cleanTarget)) {
            return list[i];
          }
        }

        // 2. 备选多段模糊比对 (兼容旧格式或跨段书签)
        distance = distance || 0.7;
        let paragraphList = text
          .replace(/\\n+/g, "\n")
          .split(/\n+/)
          .map(v => v.replace(symboRegex, "").trim())
          .filter(v => v);

        let paragraph = null;
        for (let i = 0; i < list.length; i++) {
          let isMatch = true;
          let pos = 0;
          let startPos = i;
          for (let j = 0; j < paragraphList.length; j++) {
            // 过滤所有字符
            let content = null;
            while (i + pos < list.length) {
              content = list[i + pos].innerText.replace(symboRegex, "").trim();
              if (!content.length) {
                pos++;
                startPos++;
              } else {
                break;
              }
            }
            if (!content) {
              // 说明没找到有内容的段落，终止匹配
              isMatch = false;
              break;
            }
            const paragraphDistance = editDistance(content, paragraphList[j]);
            if (paragraphDistance < distance) {
              isMatch = false;
              break;
            } else {
              pos++;
            }
          }
          if (isMatch) {
            paragraph = list[startPos];
            break;
          }
        }
        if (paragraph) {
          return paragraph;
        }
        if (minDistance && distance - 0.1 >= minDistance) {
          return this.getContentMatchParagraph(
            text,
            distance - 0.1,
            minDistance
          );
        }
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      }
      return null;
    },
    showContentMatchParagraph(content) {
      if (this._inactive) {
        return;
      }
      const paragraph = this.getContentMatchParagraph(content, 1, 0.6);
      if (paragraph) {
        this.showParagraph(paragraph, true);
      } else {
        this.$message.error("无法定位内容所在段落");
      }
    },
    showBookmark(bookmark) {
      if (this._inactive) {
        return;
      }
      if (!this.$refs.bookContentRef) {
        setTimeout(() => {
          this.showBookmark(bookmark);
        }, 10);
        return;
      }
      this.showContentMatchParagraph(bookmark.bookText);
    }
  }
};
</script>

<style lang="stylus" scoped>
>>>.popper-component {
  margin-left: 10px;
}

.chapter-wrapper {
  padding: 0;
  flex-direction: column;
  align-items: center;

  >>>.no-point {
    pointer-events: none;
  }

  .tool-bar {
    position: fixed;
    top: 0;
    padding-top: 0;
    padding-top: constant(safe-area-inset-top) !important;
    padding-top: env(safe-area-inset-top) !important;
    left: 50%;
    z-index: 2001;

    .tools {
      display: flex;
      flex-direction: column;

      .tool-icon {
        font-size: 18px;
        width: 58px;
        height: 48px;
        text-align: center;
        padding-top: 12px;
        cursor: pointer;
        outline: none;

        .iconfont {
          font-family: iconfont;
          width: 16px;
          font-size: 16px;
          margin: 0 auto;
          height: 22px;
          line-height: 22px;
          vertical-align: middle;
        }

        .tool-el-icon {
          font-size: 18px;
          line-height: 22px;
          height: 22px;

          i {
            line-height: 22px;
          }
        }

        .icon-text {
          font-size: 12px;
        }
      }
    }
  }

  .read-bar {
    position: fixed;
    bottom: 0;
    right: 50%;
    z-index: 100;

    .progress {
      padding: 10px 36px;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .progress-bar {
        flex: 1;
        padding: 0 10px;
      }

      .progress-tip {
        font-size: 14px;
        margin-left: 5px;
      }
    }

    .cache-content-zone {
      padding: 10px 36px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 14px;
      position: absolute;
      right: 55px;
      width: 300px;
      background: inherit;

      .cache-content-btn {
        cursor: pointer;
      }
    }

    .float-left-btn-zone {
      position: absolute;
      bottom: 155px;
      left: 4px;
      right: auto;
      display: flex;
      flex-direction: column;

      .float-btn {
        line-height: 32px;
        width: 36px;
        height: 36px;
        border-radius: 100%;
        display: block;
        cursor: pointer;
        text-align: center;
        vertical-align: middle;
        pointer-events: all;
        margin-top: 20px;

        .el-icon-top, .el-icon-bottom, .el-icon-info, .el-icon-search, .el-icon-collection-tag {
          line-height: 36px;
        }
      }
    }

    .float-right-btn-zone {
      position: absolute;
      bottom: 155px;
      left: 4px;
      right: auto;
      display: flex;
      flex-direction: column;

      .float-btn {
        line-height: 32px;
        width: 36px;
        height: 36px;
        border-radius: 100%;
        display: block;
        cursor: pointer;
        text-align: center;
        vertical-align: middle;
        pointer-events: all;
        margin-top: 20px;

        .el-icon-refresh-right, .el-icon-headset, .el-icon-view {
          line-height: 36px;
        }
        .el-icon-moon {
          color: #121212;
          line-height: 34px;
        }
        .el-icon-sunny {
          color: #666;
          line-height: 34px;
        }
      }
    }

    .tools {
      display: flex;
      flex-direction: column;

      .tool-icon {
        font-size: 18px;
        width: 42px;
        height: 31px;
        padding-top: 12px;
        text-align: center;
        align-items: center;
        cursor: pointer;
        outline: none;
        margin-top: -1px;

        &.progress-text {
          font-size: 16px;
        }

        .iconfont {
          font-family: iconfont;
          width: 16px;
          height: 16px;
          font-size: 16px;
          margin: 0 auto 6px;
        }
      }
    }

    .reader-bar-inner {
      display: flex;
      flex-direction: column;
      padding-bottom: 10px;
      padding-bottom: calc(10px + constant(safe-area-inset-top));
      padding-bottom: calc(10px + env(safe-area-inset-top));
      padding-left: 5px;
      padding-right: 5px;

      .operate-bar {
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        padding: 10px 10px 0 10px;
        align-items: center;

        .close-btn, .collapse-btn {
          font-size: 22px;
          height: 35px;
          cursor: pointer;
        }

        .center {
          span {
            display: inline-block;
            cursor: pointer;
          }
          .play-pause-btn {
            font-size: 50px;
            margin-top: -40px;
            i {
              border-radius: 100%;
            }
          }
          .ctrl-btn {
            margin: 0px 15px;
          }
        }
      }

      .setting-item {
        display: flex;
        flex-direction: column;
        padding: 5px 10px;

        .setting-title {
          font-size: 14px;
        }

        .setting-btn {
          font-size: 14px;
          cursor: pointer;
          display: inline-block;
          margin-left: 5px;
        }

        .voice-list {
          display: flex;
          flex-direction: row;
          overflow-x: auto;
          padding: 5px 10px;

          .radio-group {
            white-space: nowrap;

            .radio-button {
              margin-right: 10px;

              .el-radio-button__inner {
                border-radius: 4px 4px 4px 4px;
              }
            }
          }
        }

        .progress {
          padding: 5px 10px;

          .progress-tip {
            margin-left: 0;
            margin-right: 5px;
          }
        }
      }
    }
  }

  .chapter-bar {
    .el-breadcrumb {
      .item {
        font-size: 14px;
        color: #606266;
      }
    }
  }

  .chapter {
    font-family: 'Microsoft YaHei', PingFangSC-Regular, HelveticaNeue-Light, 'Helvetica Neue Light', sans-serif;
    text-align: left;
    padding: 0 65px;
    min-height: 100vh;
    min-height: calc(var(--vh, 1vh) * 100);
    width: 670px;
    margin: 0 auto;
    background-size: cover;
    position: relative;

    >>>.el-icon-loading {
      font-size: 36px;
      color: #B5B5B5;
    }

    >>>.el-loading-text {
      font-weight: 500;
      color: #B5B5B5;
    }

    .click-zone {
      position: absolute;
      z-index: 120;
      top: 0;
      bottom: 0;
      left: 0;
      right: 0;
      background: #333;
      opacity: 0.8;
      color: #fff;
      font-size: 14px;
      pointer-events: none;

      div {
        position: absolute;
        text-align: center;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .close-btn {
        left: 0;
        right: 0;
        bottom: 20px;
        height: 45px;
        line-height: 45px;
        z-index: 10;
        padding: 0;
        cursor: pointer;
        pointer-events: all;
      }
    }

    .content {
      font-size: 18px;
      line-height: 1.8;
      overflow: hidden;
      font-family: 'Microsoft YaHei', PingFangSC-Regular, HelveticaNeue-Light, 'Helvetica Neue Light', sans-serif;

      .content-inner {
        min-height: calc(var(--vh, 1vh) * 80);
        padding-bottom: 25px;
        box-sizing: border-box;
      }
    }

    .bottom-bar, .top-bar {
      box-sizing: border-box;
    }
    .top-bar {
      height: 44px;
      padding: 10px;
    }
    .bottom-bar {
      width: 100%;
      text-align: center;
      padding-bottom: 30px;
      .bottom-btn {
        font-size: 14px;
        cursor: pointer;
        display: inline-block;
        margin: 0 auto;
        padding: 10px 40px;
        width: 80%;
        box-sizing: border-box;
      }
    }
  }

  .chapter.audio {
    .top-bar, .bottom-bar {
      display: none;
    }
    .content-inner {
      height: calc(var(--vh, 1vh) * 100);
      margin-top: 0 !important;
      padding-top: 0 !important;
      padding-bottom: 0 !important;
      display: flex;
      align-items: center;
    }
  }
}

.day {
  >>>.popup {
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12), 0 0 6px rgba(0, 0, 0, 0.04);
  }

  >>>.tool-icon {
    border: 1px solid rgba(0, 0, 0, 0.1);
    margin-top: -1px;
    color: #000;

    .icon-text {
      color: rgba(0, 0, 0, 0.4);
    }
  }

  >>>.progress-tip {
    color: rgba(0, 0, 0, 0.4);
  }

  >>>.cache-content-zone {
    color: rgba(0, 0, 0, 0.4);
  }

  >>>.float-left-btn-zone {
    color: #121212;
  }

  >>>.float-right-btn-zone {
    color: #121212;
  }

  >>>.reader-bar-inner {
    color: #121212;

    .setting-title {
      color: rgba(0, 0, 0, 0.8);
    }

    .setting-value {
      color: rgba(0, 0, 0, 0.4);
    }
  }

  >>>.chapter {
    border: 1px solid #d8d8d8;
    color: #262626;
  }

  .bottom-bar, .top-bar {
    color: rgba(0, 0, 0, 0.4);
  }

  >>>.el-slider__runway {
    background-color: #fff;
  }

  >>>.play-pause-btn {
    color: #409EFF;
  }
}

.night {
  >>>.popup {
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.48), 0 0 6px rgba(0, 0, 0, 0.16);
  }

  >>>.tool-icon {
    border: 1px solid #444;
    margin-top: -1px;
    color: #666;

    .icon-text {
      color: #666;
    }
  }

  >>>.progress-tip {
    color: #666;
  }

  >>>.cache-content-zone {
    color: #666;
  }

  >>>.float-left-btn-zone {
    color: #666;
  }

  >>>.float-right-btn-zone {
    color: #666;
  }

  >>>.reader-bar-inner {
    color: #666;
  }

  >>>.chapter {
    border: 1px solid #444;
    color: #666;
  }

  >>>.popper__arrow {
    background: #666;
  }

  .bottom-bar, .top-bar {
    color: #666;
  }

  >>>.el-slider__runway {
    background-color: #282828;
  }
  >>>.el-slider__bar {
    background-color: #185798;
  }
  >>>.el-slider__button {
    border: 2px solid #185798;
    background-color: #282828;
  }
  >>>.play-pause-btn {
    color: #185798;
  }
}

.chapter-wrapper {
  .read-bar {
    .float-btn-zone {
      position: absolute;
      bottom: 135px;
      left: 4px;

      .float-left-btn-zone {
        position: relative;
        left: auto;
        bottom: auto;
      }

      .float-right-btn-zone {
        position: relative;
        left: auto;
        bottom: auto;
        margin-bottom: 20px;
      }
    }

  }
}

.chapter-wrapper.mini-interface {
  padding: 0;
  position: relative;
  height: 100%;

  .tool-bar {
    left: 0;
    width: 100vw;
    margin-left: 0 !important;

    .tools {
      flex-direction: row;
      justify-content: space-around;
      .tool-icon {
        border: none;
      }
    }
  }

  .read-bar {
    right: 0;
    width: 100vw;
    margin-right: 0 !important;

    .cache-content-zone {
      position: relative;
      width: auto;
      right: 0;
      background: inherit;
    }

    .float-btn-zone {
      position: static;
      bottom: 0;
      left: 0;
    }

    .float-left-btn-zone {
      position: absolute;
      right: auto;
      left: 20px;
      bottom: 135px;
    }

    .float-right-btn-zone {
      position: absolute;
      left: auto;
      right: 20px;
      bottom: 135px;
    }

    .tools {
      flex-direction: row;
      justify-content: space-around;
      padding: 0 15px;
      height: 45px;
      .tool-icon {
        border: none;
        width: auto;
        padding: 0;
        height: 45px;
        line-height: 45px;
        .iconfont {
          display: inline-block;
        }
        span {
          vertical-align: middle;
        }
      }
    }
  }

  .chapter {
    width: 100vw !important;
    padding: 0 16px;
    box-sizing: border-box;
    border: none;
    text-align: justify;
    position: relative;

    .top-bar {
      position: fixed;
      top: 0;
      left: 0;
      width: 100vw;
      z-index: 50;
      background: inherit;
      height: 30px;
      height: calc(30px + constant(safe-area-inset-top));
      height: calc(30px + env(safe-area-inset-top));
      padding: 6px 16px;
      padding-top: calc(6px + constant(safe-area-inset-top));
      padding-top: calc(6px + env(safe-area-inset-top));
      font-size: 12px;
    }

    .content-inner {
      margin-top: 30px;
      margin-top: calc(30px + constant(safe-area-inset-top));
      margin-top: calc(30px + env(safe-area-inset-top));
      padding-top: 15px;
      padding-bottom: 15px;
    }
  }

  .chapter.cartoon {
    padding: 0;

    .content-inner {
      padding-top: 1px;
    }
  }

  .chapter.slide-reader {
    padding: 0;
    height: 100%;

    .bottom-bar {
      height: 24px;
      position: absolute;
      bottom: 0;
      padding: 0 16px;
      padding-bottom: 6px;
      display: flex;
      justify-content: space-between;
      font-size: 12px;
    }

    .top-bar {
      position: relative;
    }

    .content {
      position: absolute;
      overflow: visible;
      top: 30px;
      top: calc(30px + constant(safe-area-inset-top));
      top: calc(30px + env(safe-area-inset-top));
      bottom: 24px;
    }

    .content-inner {
      margin: 0 16px;
      overflow: hidden;
      text-align: justify;
      padding: 0;
      height: 100%;
    }

    .book-content {
      height: 100%;
      -webkit-columns: calc(100vw - 32px) 1;
      -webkit-column-gap: 32px;
      columns: calc(100vw - 16px) 1;
      column-gap: 16px;
    }
  }
}
.chapter-wrapper.mini-interface::-webkit-scrollbar {
  width: 0 !important;
}
</style>
<style lang="stylus">
.voice-list {
  .el-radio-button__inner {
    border-radius: 4px !important;
    border-left: 1px solid #DCDFE6;
    box-shadow: none;
  }
}
.night-theme {
  .voice-list {
    .el-radio-button {
      box-shadow: none !important;
    }
    .el-radio-button__inner {
      background-color: #bbb;
      border-color: #bbb;
    }
    .el-radio-button__inner:hover {
      color: #185798;
    }
    .el-radio-button__orig-radio:checked+.el-radio-button__inner {
      background-color: #185798;
      border-color: #185798;
      color: #fff;
      box-shadow: none;
    }
  }
}
.kindle-page {
  .day {
    .tool-icon {
      border: 1px solid #fefefefe;

      .icon-text {
        color: #444;
      }
    }

    .progress-tip {
      color: #444;
    }

    .cache-content-zone {
      color: #444;
    }

    .reader-bar-inner {

      .setting-title {
        color: rgba(0, 0, 0, 0.8);
      }

      .setting-value {
        color: #444;
      }
    }

    .bottom-bar, .top-bar {
      color: #444;
    }
  }
}

/* 仿安卓侧滑返回浮标动画与样式 */
.edge-back-bubble {
  position: fixed;
  left: 0;
  z-index: 9999;
  width: 44px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-top-right-radius: 30px;
  border-bottom-right-radius: 30px;
  box-shadow: 2px 2px 10px rgba(0, 0, 0, 0.25);
  pointer-events: none;
  transition: transform 0.1s ease-out;

  &.right {
    left: auto;
    right: 0;
    border-top-right-radius: 0;
    border-bottom-right-radius: 0;
    border-top-left-radius: 30px;
    border-bottom-left-radius: 30px;
    box-shadow: -2px 2px 10px rgba(0, 0, 0, 0.25);

    .edge-back-arrow {
      transform: translateX(4px);

      &.active {
        transform: translateX(0) scale(1.15);
      }
    }
  }

  .edge-back-arrow {
    color: rgba(255, 255, 255, 0.7);
    font-size: 20px;
    font-weight: bold;
    transform: translateX(-4px);
    transition: all 0.15s ease;

    &.active {
      color: #409EFF;
      transform: translateX(0) scale(1.15);
    }
  }
}

.edge-back-indicator-enter-active,
.edge-back-indicator-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.edge-back-indicator-enter,
.edge-back-indicator-leave-to {
  opacity: 0;
  transform: translateY(-50%) scale(0.5);
}

/* “再次滑动返回书架”防误触确认胶囊提示 */
.edge-back-toast {
  position: fixed;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10000;
  background: rgba(0, 0, 0, 0.78);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  color: #fff;
  padding: 8px 18px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
  pointer-events: none;
  display: flex;
  align-items: center;
  gap: 6px;

  i {
    font-size: 15px;
    color: #409EFF;
  }
}

.edge-toast-fade-enter-active,
.edge-toast-fade-leave-active {
  transition: opacity 0.25s, transform 0.25s;
}

.edge-toast-fade-enter,
.edge-toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, 15px);
}

/* 屏蔽 iOS/移动端原生文本选择弹窗，但完整保留文本高亮与游标选择 */
.chapter-wrapper.suppress-callout {
  &, * {
    -webkit-touch-callout: none !important;
  }
}

/* 选中文字快捷操作气泡弹窗 (复制、添加书签、添加过滤规则) */
.text-action-popup {
  position: fixed;
  z-index: 10001;
  display: flex;
  align-items: center;
  height: 38px;
  padding: 0 4px;
  background: rgba(30, 30, 32, 0.92);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.35);
  transform: translate(-50%, -100%);
  user-select: none;
  -webkit-user-select: none;

  &.is-below {
    transform: translate(-50%, 0);
  }

  .text-action-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    height: 28px;
    padding: 0 10px;
    font-size: 13px;
    color: #ffffff;
    border-radius: 14px;
    cursor: pointer;
    white-space: nowrap;
    transition: background 0.15s, opacity 0.15s;

    i {
      font-size: 14px;
      color: #409eff;
    }

    &:active {
      background: rgba(255, 255, 255, 0.15);
      opacity: 0.85;
    }
  }

  .text-action-divider {
    width: 1px;
    height: 14px;
    background: rgba(255, 255, 255, 0.2);
    margin: 0 2px;
  }
}

.text-action-fade-enter-active,
.text-action-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.text-action-fade-enter,
.text-action-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -100%) scale(0.9);

  &.is-below {
    transform: translate(-50%, 0) scale(0.9);
  }
}

/* 选中文本高亮层 */
.custom-selection-highlight {
  position: fixed;
  background: rgba(64, 158, 255, 0.35);
  border-radius: 2px;
  pointer-events: none;
  z-index: 9998;
  mix-blend-mode: multiply;
}
.night .custom-selection-highlight {
  background: rgba(64, 158, 255, 0.48);
  mix-blend-mode: screen;
}

/* 仿真翻页 3D 纸张拟真翻页图层 */
.simulation-flip-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  perspective: 1600px;
  -webkit-perspective: 1600px;
  pointer-events: none;
  z-index: 20;
  overflow: hidden;
}

.simulation-page-sheet {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  -webkit-transform-style: preserve-3d;
  transform-origin: left center;
  -webkit-transform-origin: left center;
  will-change: transform;
}

.sheet-face {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  box-shadow: 0 0 25px rgba(0, 0, 0, 0.3);
}

.sheet-front {
  z-index: 2;
}

.sheet-back {
  z-index: 1;
}

.sheet-content {
  width: 100%;
  height: 100%;
  pointer-events: none;

  .content-inner {
    margin: 0 16px;
    height: 100%;
  }

  .book-content {
    height: 100%;
    -webkit-columns: calc(100vw - 16px) 1;
    columns: calc(100vw - 16px) 1;
    column-gap: 16px;
  }
}

.sheet-shadow {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.under-page-shadow {
  position: absolute;
  top: 0;
  left: 0;
  width: 35%;
  height: 100%;
  pointer-events: none;
  z-index: 15;
}

.offline-cache-dialog {
  border-radius: 12px !important;
  overflow: hidden;

  .el-dialog__header {
    padding: 16px 20px 12px;
    border-bottom: 1px solid #f0f2f5;

    .el-dialog__title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .el-dialog__body {
    padding: 16px 20px 20px;
  }
}

.offline-cache-content {
  .cache-status-card {
    background: #f7f9fc;
    border-radius: 10px;
    padding: 14px 16px;
    margin-bottom: 18px;
    border: 1px solid #ebeef5;

    .status-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;

      .book-name-tag {
        font-weight: 600;
        font-size: 15px;
        color: #303133;
        max-width: 65%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .cache-size-text {
        font-size: 12px;
        color: #909399;
      }
    }

    .status-progress-info {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      color: #606266;
      margin-bottom: 8px;

      strong {
        color: #409EFF;
        font-size: 14px;
      }

      .status-percent {
        font-weight: 600;
        color: #67C23A;
      }
    }
  }

  .active-caching-box {
    background: #ecf5ff;
    border: 1px solid #d9ecff;
    border-radius: 8px;
    padding: 12px 14px;
    margin-bottom: 18px;

    .active-caching-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      font-size: 13px;
      color: #409EFF;
      font-weight: 500;
    }
  }

  .cache-actions-section {
    margin-bottom: 12px;

    .section-title {
      font-size: 13px;
      color: #606266;
      margin-bottom: 10px;
      font-weight: 500;
    }

    .cache-btns-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;

      .el-button {
        margin-left: 0 !important;
        width: 100%;
      }
    }
  }

  .cache-footer-actions {
    text-align: center;
    padding-top: 10px;
    border-top: 1px dashed #ebeef5;
  }
}

.night-theme, .night {
  .offline-cache-dialog {
    background: #21252b !important;

    .el-dialog__header {
      border-bottom-color: #2c313a;

      .el-dialog__title {
        color: #d7dae0;
      }

      .el-dialog__close {
        color: #abb2bf;
      }
    }

    .cache-status-card {
      background: #1e2227;
      border-color: #2c313a;

      .status-header {
        .book-name-tag {
          color: #e5c07b;
        }

        .cache-size-text {
          color: #5c6370;
        }
      }

      .status-progress-info {
        color: #abb2bf;
      }
    }

    .active-caching-box {
      background: #282c34;
      border-color: #3e4451;
    }

    .cache-actions-section .section-title {
      color: #abb2bf;
    }

    .cache-footer-actions {
      border-top-color: #2c313a;
    }
  }
}
</style>
