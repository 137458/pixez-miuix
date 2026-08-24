# 更新日志 (Changelog)

本项目所有版本变更记录。GitHub Release 保留用户简明摘要，详细内容记录于本文件。

---

## [未发布]

### 新增

- 接入 Android 16 (API 36) 实时动态胶囊下载通知（Rich Ongoing Notifications），支持状态栏实时展示多页插画与漫画批量下载进度。
- 接入 Android 15 (API 35) 私密空间（Private Space）兼容与 `FileProvider` 安全沙盒文件共享机制。
- 接入 Android 16 现代化零权限照片选择器（Photo Picker）架构。
- 接入 Android 14+ / 15+ 桌面快捷捷径（App Shortcuts）与深度链接路由，支持长按桌面图标快速直达日榜推荐、插画搜索、下载管理与浏览历史。
- 接入 Android 14+ 系统级分享面板自定义动作（`ChooserAction` 快速复制）。
- 接入 Android 14+ / 15+ 10-bit Display P3 广色域色彩渲染模式（`wideColorGamut`），充分发挥现代 OLED 与 HyperOS 屏幕高色域表现力。
- 集成 `androidx.profileinstaller` 运行时编译安装器，利用 Baseline Profiles 自动触发 DEX AOT 预编译提升启动与滑动流畅度。
- 接入 Android 12+ 官方启动屏幕（Splash Screen）API，消除冷启动闪屏。
- 接入 Android 13+ 每应用独立语言偏好（Per-App Language Preferences）与动态取色自适应图标。
- 评论区支持 Pixiv 官方 40 枚表情包解析与快捷输入面板。
- 接入跨平台系统存储目录选择器（`rememberDirectoryPicker`），Android 端直接调用系统 SAF 文件选择器（`OpenDocumentTree`），彻底替代手动填写路径。
- 实现 Android 桌面小部件 Provider（`PixEzAppWidgetProvider`），支持在主屏幕展示每日推荐与榜单插画并点击直达。
- 插画详情页接入横向联动切换容器（`HorizontalPager`），开启「滑动切换作品」设置后支持左右手势流畅滑动切换关联作品。

### 优化

- 重构设置中心层级结构，收敛为 4 大标准分类，消除“保存设置”与“下载设置”、“跨适配设置”与“布局设置”等重复分散页面，整合为统一清晰的「下载与保存」及「界面与布局」设置。
- 开启 Android 15 16 KB 内存页对齐与未压缩 JNI 原生库打包配置（`useLegacyPackaging = false`），保障新型内核设备高性能运行。
- 配置 Coil 3 全局 25% 内存缓存池（MemoryCache）与请求头重用，消除长列表滑动重复网络请求与对象分配开销。
- 全量 Lazy 列表与瀑布流卡片接入 Compose `contentType` 槽位复用机制，大幅提升长列表滑动流畅度。
- 记忆化插画卡片 NSFW 遮罩、宽高比与 AI 标识计算，降低高速滑动重组耗时。
- 移除背景着色器 60fps 硬编码帧率限制，全面适配 Xiaomi HyperOS 90Hz/120Hz/144Hz 高刷屏幕原生 VSync 渲染。
- 开启 Android 硬件加速与大内存堆栈（largeHeap）配置。
- 全面完成二级菜单与子页面多语言本地化支持，涵盖账号信息编辑、动态与关注设置、交互与屏蔽设置、跨适配与布局设置、主题与色板弹窗、数据导入导出、下载管理与历史记录、公告看板、评论互动、画师详情与作品详情等全部子页面的硬编码文本收敛。
- 扩充 `AppStrings` 国际化字典并在 11 种语言（简体中文、繁体中文、英语、日语、韩语、俄语、西班牙语、土耳其语、印尼语、菲律宾语、德语）下全量补齐定义。
- 增强电脑端与跨平台登录凭证智能解析，自动清洗两端引号、空格、剥离 `Bearer ` 前缀与 JSON 结构体，支持无缝粘贴 `pixiv://...` 回调链接或授权 Code。
- OAuthClient 引入 PKCE `code_verifier` 历史记录回退重试机制，解决桌面端多页面授权或重复点击导致的 verifier 覆盖失效问题。
- 增强 `PixivHttpClient` 针对 OAuth 400 错误的详细反序列化与解析，精准提取服务端提示。
- 优化桌面端应用启动命令行参数自动解析登录流程。

### 修复

- 修复 Android 14/15/16 原生预测性返回手势（Predictive Back）被底层回调阻断的问题，接入 `androidPredictiveBackAnimatable` 还原丝滑物理跟手缩放动画。
- 修复插画瀑布流网格（`IllustStaggeredGrid`）未响应横屏列数配置以及固定列数错误应用自适应宽度的问题，实现横竖屏自适应与固定列数精准匹配。
- 修复插画详情页画质读取逻辑，为漫画作品正确应用「漫画详情页画质」独立配置。
- 补齐收藏与下载的自动联动，支持「收藏后自动保存」、「保存后自动收藏」、「收藏后关注画师」与「收藏自动打标签」。
- 接入 Android 原生「显示模式 (60Hz/高刷/系统默认)」切换与根页面「再次返回退出应用」拦截器。
- 修复各作品列表流中「H 是不行的 (R-18过滤)」设置未生效的问题。
- 修复插画详情页未自动写入浏览历史导致本地历史无效的问题，完善 `HistoryRepository` 实时记录与降序时间排列。

- 修复 iOS / macOS (Kotlin/Native) 跨平台编译错误，补全 `BrowserLauncher`、`Platform`、`RuntimeShaderCompat`、`DataExportFileHelper`、`IllustClipboard`、`IllustSaver` 与 `IllustShare` 的原生实际声明（`actual`）。
- 新增跨平台 `String.format` 扩展实现，替代 Native 端缺失的 JVM `java.lang.String.format`。
- 收敛全平台协程调度器，解决 Native 端缺少 `Dispatchers.IO` 的访问限制问题。

---

## [v0.9.108.1] - 2026-08-18

### 新增

- 登录界面新增直接输入 Access Token 快速登录功能，方便跨平台授权与登录凭证同步。
- 全面完善 11 种语言本地化支持（简体中文、繁体中文、英语、日语、韩语、俄语、西班牙语、土耳其语、印尼语、菲律宾语、德语），补齐所有设置界面的多语言翻译。

### 修复

- 修复 Android 端页面返回异常退出至桌面以及 HyperOS 预测性返回手势动画未生效的问题。
- 修复 Android 端在横竖屏旋转切换时触发 Activity 重建并导致页面重新刷新的问题。
- 修复平板端与宽屏设备无法正确切换悬浮底栏的问题，支持在左侧 MIUIX NavigationRail 侧边栏与居中悬浮底栏间无缝切换。
- 修复桌面端与平板端宽屏模式下残留 Material 3 控件的问题，全面迁移至 MIUIX 官方 NavigationRail 侧边栏与超椭圆组件规范。
- 修复桌面端完成登录后未能自动跳转回主界面的问题。

---

## [v0.9.108] - 2026-08-18

### 新增

- 首页推荐 (Hello)、关注动态 (New)、插画与画师搜索 (Search)、排行榜 (Ranking)、相关推荐 (Related)、插画/漫画系列详情 (Series)、画师主页作品与收藏 (UserDetail)、作品评论区 (Comments) 以及好P友/粉丝列表全量支持触底自动流式预加载。
- 全部列表页面接入 MIUIX 官方下拉刷新与顶部手动刷新按钮。
- 新增项目架构全景百科文档（`Code_Wiki.md`）、UI 设计与组件规范（`MIUIX_Spec.md`）及全局待办清单（`Global_TODO.md`）。

### 优化

- 悬浮胶囊底栏增加最大宽度限制（320dp ~ 540dp）并在平板与桌面大屏设备上居中对齐，消除全屏拉伸失真。
- 瀑布流在大屏和宽屏设备上自适应 3 到 7 列布局，单列内容容器居中限宽（760dp）。
- 升级推荐用户页为触底自动平滑加载。

### 修复

- 修复长评作品评论区无法加载历史评论的问题。
- 修复画师关注列表和好P友/粉丝列表仅加载首批数据的问题。
- 修复超过 30 话的连载插画/漫画系列详情页无法加载完整章节的问题。
- 修复 Android 端 Release 发行包构建校验异常。



---

## [v0.9.105] - 2026-08-17

### 新增

- PixEz MIUIX (Compose Multiplatform + Xiaomi HyperOS / MIUIX) 重构首发版发布。
- 支持 IosLiquidGlass 悬浮液态玻璃底栏（高斯模糊基底、动态高光拾取、色散折射透镜层）与阻尼手势拖拽。
- 支持 Android 13+ 原生 `RuntimeShader` 与 Desktop Skia `RuntimeEffect` 运行时 GLSL 动态流光背景。
- 全量采用 Xiaomi HyperOS (MIUIX) 官方组件体系（Squircle 超椭圆圆角卡片、`SuperDialog` 弹窗、色彩系统）。
- 支持免代理 SNI 混淆直连引擎与 OAuth2 PKCE 自动鉴权与 Token 无感刷新。
- 支持插画/漫画多图轮播浏览与动图 (Ugoira) 原生帧播放。
- 支持 Pixivision 官方特辑沉浸式图文阅读器。
- 支持作品 ID、画师 ID、标签及 AI 作品过滤等多重屏蔽引擎。
- 支持多任务并发下载队列与本地 MediaStore 相册同步。
