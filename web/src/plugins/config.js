import body_0 from "../assets/imgs/themes/body_0.png";
import content_0 from "../assets/imgs/themes/content_0.png";
import popup_0 from "../assets/imgs/themes/popup_0.png";
import body_1 from "../assets/imgs/themes/body_1.png";
import content_1 from "../assets/imgs/themes/content_1.png";
import popup_1 from "../assets/imgs/themes/popup_1.png";
import body_2 from "../assets/imgs/themes/body_2.png";
import content_2 from "../assets/imgs/themes/content_2.png";
import popup_2 from "../assets/imgs/themes/popup_2.png";
import body_3 from "../assets/imgs/themes/body_3.png";
import content_3 from "../assets/imgs/themes/content_3.png";
import popup_3 from "../assets/imgs/themes/popup_3.png";
import body_5 from "../assets/imgs/themes/body_5.png";
import content_5 from "../assets/imgs/themes/content_5.png";
import popup_5 from "../assets/imgs/themes/popup_5.png";
import body_6 from "../assets/imgs/themes/body_6.png";
import content_6 from "../assets/imgs/themes/content_6.png";
// import popup_6 from "../assets/imgs/themes/popup_6.png";

const defaultDayConfig = {
  configDefaultType: "白天默认",
  name: "内置白天",
  theme: 0,
  font: 0,
  chineseFont: "简体",
  fontSize: 18,
  fontWeight: 400,
  fontColor: "#262626",
  bodyColor: "#eadfca",
  contentColor: "#fff",
  popupColor: "#ede7da",
  themeType: "day",
  readMethod: "上下滑动",
  clickMethod: "自动",
  animateMSTime: 300, // 翻页动画时长
  readWidth: 800,
  lineHeight: 1.8, // 行高
  paragraphSpace: 0.2, // 段间距
  autoReadingMethod: "像素滚动",
  autoReadingPixel: 1,
  autoReadingLineTime: 1000,
  pageMode: "自适应",
  selectionAction: "操作弹窗"
};
const defaultNightConfig = {
  configDefaultType: "黑夜默认",
  name: "内置黑夜",
  theme: 6,
  font: 0,
  chineseFont: "简体",
  fontSize: 18,
  fontWeight: 400,
  fontColor: "#666666",
  bodyColor: "#121212",
  contentColor: "#171717",
  popupColor: "#121212",
  themeType: "night",
  readMethod: "上下滑动",
  clickMethod: "自动",
  animateMSTime: 300, // 翻页动画时长
  readWidth: 800,
  lineHeight: 1.8, // 行高
  paragraphSpace: 0.2, // 段间距
  autoReadingMethod: "像素滚动",
  autoReadingPixel: 1,
  autoReadingLineTime: 1000,
  pageMode: "自适应",
  selectionAction: "操作弹窗"
};
const settings = {
  shelfConfig: {
    showBookGroup: -1
  },
  searchConfig: {
    searchType: "multi",
    bookSourceGroup: "",
    bookSourceUrl: "",
    concurrentCount: 24
  },
  customConfigList: [defaultDayConfig, defaultNightConfig],
  config: {
    ...defaultDayConfig,
    customConfig: "内置白天",
    autoTheme: true, // 自动切换主题
    pageType: "正常"
  },
  speechVoiceConfig: {
    voiceName: "",
    speechRate: 1,
    speechPitch: 1
  },
  defaultNightTheme: 6,
  themes: [
    {
      body: "url(" + body_0 + ") repeat",
      content: "url(" + content_0 + ") repeat",
      popup: "url(" + popup_0 + ") repeat"
    },
    {
      body: "url(" + body_1 + ") repeat",
      content: "url(" + content_1 + ") repeat",
      popup: "url(" + popup_1 + ") repeat"
    },
    {
      body: "url(" + body_2 + ") repeat",
      content: "url(" + content_2 + ") repeat",
      popup: "url(" + popup_2 + ") repeat"
    },
    {
      body: "url(" + body_3 + ") repeat",
      content: "url(" + content_3 + ") repeat",
      popup: "url(" + popup_3 + ") repeat"
    },
    {
      body: "#ebcece repeat",
      content: "#f5e4e4 repeat",
      popup: "#faeceb repeat"
    },
    {
      body: "url(" + body_5 + ") repeat",
      content: "url(" + content_5 + ") repeat",
      popup: "url(" + popup_5 + ") repeat"
    },
    {
      body: "url(" + body_6 + ") repeat",
      content: "url(" + content_6 + ") repeat",
      popup: "#121212"
    },
    {
      body: "#f7f7f7 repeat",
      content: "#fff repeat",
      popup: "#f7f7f7 repeat"
    }
  ],
  fonts: [
    {
      fontFamily:
        'custom-system, -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "WenQuanYi Micro Hei", sans-serif'
    },
    // 黑体
    {
      fontFamily:
        'custom-ht, reader-ht, "PingFang SC", "Hiragino Sans GB", "Heiti SC", "Microsoft YaHei", SimHei, sans-serif'
    },
    // 楷体
    {
      fontFamily:
        '"LXGW WenKai", custom-kt, reader-kt, "Kaiti SC", STKaiti, "KaiTi", "KaiTi_GB2312", "DFKai-SB", serif'
    },
    // 宋体
    {
      fontFamily:
        '"Noto Serif SC", custom-st, reader-st, "Songti SC", STSong, "Source Han Serif SC", "Noto Serif CJK SC", "SimSun", serif'
    },
    // 仿宋
    {
      fontFamily:
        'custom-fs, reader-fs, STFangsong, FangSong, FangSong_GB2312, "LXGW WenKai", "Songti SC", serif'
    },
    // 自定义字体
    {
      fontFamily:
        'custom-user, -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif'
    }
  ]
};
export const errorTypeList = [
  "UnknownHostException",
  "ConnectException: Failed to connect",
  "SocketException: Connection reset",
  "SSLHandshakeException",
  "responseCode: 307",
  "responseCode: 400",
  "responseCode: 403",
  "responseCode: 404",
  "responseCode: 500",
  "responseCode: 502",
  "responseCode: 503",
  "responseCode: 504",
  "responseCode: 513"
];
export const defaultReplaceRule = {
  name: "",
  pattern: "",
  replacement: "",
  scope: "",
  isRegex: false,
  isEnabled: true
};
export const defaultBookmark = {
  bookName: "",
  bookAuthor: "",
  chapterIndex: 0,
  chapterPos: 0,
  chapterName: "",
  bookText: "",
  content: ""
};
export const syncConfigFiled = Object.keys(defaultDayConfig).concat([
  "contentBGImg"
]);
export const customFonts = [
  "custom-system",
  "custom-ht",
  "custom-kt",
  "custom-st",
  "custom-fs",
  "custom-user"
];
export default settings;
