# PixEz MIUIX · Code Wiki

> 本文档是 PixEz MIUIX 跨平台客户端的架构与代码全景手册，覆盖 `compose-miuix` 核心架构、数据仓库、网络引擎、数据库持久化与 UI 组件体系。

---

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 项目整体架构](#2-项目整体架构)
  - [2.1 分层架构图](#21-分层架构图)
  - [2.2 核心设计特征](#22-核心设计特征)
- [3. 目录与包结构详解](#3-目录与包结构详解)
- [4. 核心模块职责详解](#4-核心模块职责详解)
  - [4.1 UI 表现层 (UI Layer)](#41-ui-表现层-ui-layer)
  - [4.2 数据仓库层 (Repository Layer)](#42-数据仓库层-repository-layer)
  - [4.3 网络引擎层 (Network Layer)](#43-网络引擎层-network-layer)
  - [4.4 本地持久化与数据库层 (Persistence Layer)](#44-本地持久化与数据库层-persistence-layer)
  - [4.5 跨平台适配层 (Platform Layer)](#45-跨平台适配层-platform-layer)
- [5. 关键类与接口说明](#5-关键类与接口说明)
- [6. 核心数据流与典型调用链](#6-核心数据流与典型调用链)
  - [6.1 OAuth 认证与 Token 自动刷新流程](#61-oauth-认证与-token-自动刷新流程)
  - [6.2 列表触底流式加载（Infinite Scrolling）调用链](#62-列表触底流式加载infinite-scrolling调用链)
  - [6.3 多重黑名单与屏蔽过滤机制](#63-多重黑名单与屏蔽过滤机制)
  - [6.4 下载任务队列调度与相册同步](#64-下载任务队列调度与相册同步)
- [7. 项目构建与运行方式](#7-项目构建与运行方式)

---

## 1. 项目概述

| 项目元信息 | 说明 |
|---|---|
| 应用名称 | PixEz MIUIX |
| 包名 / Bundle ID | `com.perol.pixez` |
| 技术栈 | Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP) |
| UI 设计规范 | Xiaomi HyperOS / MIUIX (`top.yukonga.miuix.kmp`) |
| 目标平台 | Android (API 24+)、Desktop (Windows / macOS / Linux) |
| 异步网络 | Ktor Client 3.x + Kotlinx.Serialization |
| 本地数据库 | SQLDelight 2.x (SQLite 多库架构) |
| 图片管线 | Coil 3 跨平台渲染引擎 |
| 路由架构 | Decompose / Compose 状态驱动路由 |

**定位**：针对 Pixiv 平台打造的现代跨平台开源客户端，以 Xiaomi HyperOS (MIUIX) 通透、灵动的交互规范为核心，具备免代理直连、流式分页浏览、动图播放、多重屏蔽引擎与后台下载队列管理等功能。

---

## 2. 项目整体架构

### 2.1 分层架构图

```
┌─────────────────────────────────────────────────────────┐
│              Platform Entry (应用启动入口)               │
│    Android (MainActivity)   │   Desktop (main.kt)       │
└────────────────────────────┬────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────┐
│               RootContent / App Navigation              │
│       Decompose 栈导航 · 全局状态路由 · 主题与多语言注入    │
└────────────────────────────┬────────────────────────────┘
                             │ 驱动
                             ▼
┌─────────────────────────────────────────────────────────┐
│                   UI Screens (页面层)                    │
│  Hello · New · Search · Ranking · Spotlight · UserDetail │
│  IllustDetail · Comments · History · DownloadTask · etc. │
└────────────────────────────┬────────────────────────────┘
                             │ 调用
                             ▼
┌──────────────────┐  ┌──────────────────┐  ┌─────────────┐
│  MIUIX Components│  │ LiquidGlass / FX │  │ LocalStrings│
│  Card · Grid     │  │ RuntimeShader    │  │ (多语言体系) │
│  PullToRefresh   │  │ Backdrop Blur    │  │ AppStrings  │
└──────────────────┘  └──────────────────┘  └─────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                 Repository (数据仓库层)                  │
│  IllustRepository · SearchRepository · UserRepository   │
│  AccountRepository · BookmarkRepository · BanRepository │
│  DownloadRepository · HistoryRepository · SettingsRepo  │
└──────────────┬───────────────────────────┬──────────────┘
               │ 依赖                      │ 依赖
               ▼                           ▼
┌──────────────────────────────┐ ┌─────────────────────────┐
│      Network (网络引擎层)     │ │ Persistence (本地持久化) │
│  Ktor Client · OAuthClient   │ │ SQLDelight 多数据库架构  │
│  TokenRefreshPlugin · Direct │ │ Multiplatform Settings  │
└──────────────────────────────┘ └─────────────────────────┘
               │                           │
               └─────────────┬─────────────┘
                             ▼
┌─────────────────────────────────────────────────────────┐
│                  Platform (跨平台抽象层)                 │
│  DriverFactory · IllustSaver · IllustShare · Clipboard   │
└─────────────────────────────────────────────────────────┘
```

### 2.2 核心设计特征

1. **严格的单向数据流与清晰分层**：
   页面层 (`Screens`) 仅依赖数据仓库 (`Repositories`)，仓库层协调远程 API 与本地数据库，严禁 UI 直接跨层调用底层网络或数据库。
2. **纯粹的 MIUIX 原生规范**：
   全量采用 `top.yukonga.miuix.kmp` 组件体系，完全移除并严防 Material 3 控件泄漏。
3. **响应式与大屏适配内置**：
   瀑布流布局根据屏幕宽度自适应 2 到 7 列，悬浮底栏与内容容器在平板/桌面端自动限宽居中。
4. **统一并发与异常容错**：
   全工程挂起函数采用 `suspendRunCatchingNonCancel`，确保业务异常被捕获的同时保留协程取消语义。

---

## 3. 目录与包结构详解

```text
compose-miuix/
├── composeApp/                                # 各平台入口
│   ├── src/androidMain/                       # Android 专属配置与入口
│   └── src/desktopMain/                       # 桌面端启动主函数与打包
└── shared/                                    # 跨平台共享模块
    └── src/commonMain/
        ├── composeResources/                  # 共享图标与资源
        ├── sqldelight/com/perol/pixez/shared/ # SQLDelight 数据库架构定义
        │   ├── account/                       # 账号持久化表
        │   ├── ban/                           # 屏蔽 ID 与标签表
        │   ├── history/                       # 浏览历史表
        │   └── task/                          # 下载任务表
        └── kotlin/com/perol/pixez/shared/
            ├── data/
            │   ├── local/                     # SQLDelight 驱动与数据库封装
            │   ├── model/                     # 强类型数据模型与序列化实体
            │   ├── repository/                # 业务数据仓库（14 个核心 Repo）
            │   └── settings/                  # Multiplatform Settings 配置管理
            ├── network/                       # Ktor HTTP 客户端、OAuth 与插件
            ├── platform/                      # 平台 Expect/Actual 声明
            └── ui/
                ├── components/                # 通用 UI 组件与 MIUIX 封装
                ├── effect/                    # 液态玻璃与动态着色器
                ├── i18n/                      # 多语言国际化体系
                ├── navigation/                # 路由导航定义
                ├── screens/                   # 全部 41 个业务界面
                ├── theme/                     # MIUIX 主题配置
                └── utils/                     # 挂起与协程辅助工具
```

---

## 4. 核心模块职责详解

### 4.1 UI 表现层 (UI Layer)
- **`screens/`**：包含首页推荐（`HelloScreen`）、关注动态（`NewScreen`）、搜索（`SearchScreen`）、排行榜（`RankingScreen`）、插画详情（`IllustDetailScreen`）、画师主页（`UserDetailScreen`）等完整业务页。
- **设置架构 (Settings Architecture)**：
  - `SettingsScreen`：主设置中枢，统一聚合 5 大高内聚业务分组（账号、界面、画质保存、浏览交互、过滤屏蔽、系统数据），精简入口层级。
  - `ShieldScreen`（统一过滤与屏蔽中心）：集中整合分级过滤（`hIsNotAllow`）、敏感内容模糊遮罩（`nsfwMask`）、AI 生成内容控制与标识（`banAIIllust`、`feedAIBadge`、官方 AI 设置）及多重本地黑名单库（标签、画师、作品）。
  - `DownloadSettingScreen`（下载与联动中心）：集中管理保存目录、命名格式及全部 5 项收藏与保存双向联动逻辑（`defaultPrivateLike`、`autoTagWhenStar`、`followAfterStar`、`saveAfterStar`、`starAfterSave`）。
  - `QualitySettingScreen`（画质中心）：独立管理列表缩略图、插画详情原图与漫画阅读画质档位。
  - `LayoutSettingScreen`（布局中心）：管理自适应多列阈值、悬浮底栏开关与 Android 屏幕高刷新率模式切换。
  - `InteractionSettingScreen`（交互中心）：管理双击返回退出与手势滑动切换作品。
- **`components/`**：
  - `IllustStaggeredGrid`：具备动态多列、触底预加载、加载指示器与错误重试的全功能插画瀑布流。
  - `FloatingBottomBar`：集成 Liquid Glass 模糊滤镜、弹性拖拽手势与大屏居中限制的悬浮底栏。
  - `PixivAsyncImage`：封装 Coil 3 并内置 Referer 防盗链请求头的跨平台异步图片组件。

### 4.2 数据仓库层 (Repository Layer)
- **`IllustRepository`**：负责作品详情、推荐、榜单、关注流、相关作品、评论及 Pixivision 特辑的获取与缓存。
- **`SearchRepository`**：负责插画/漫画/画师关键词检索、热门标签与搜索建议。
- **`UserRepository`**：负责画师个人信息、作品集、收藏列表、关注/粉丝列表及推荐画师。
- **`AccountRepository`**：封装 OAuth 登录、PKCE 授权码兑换、密码/邮箱编辑与本地凭证持久化。
- **`BanRepository`**：维护作品 ID 屏蔽表、画师 ID 屏蔽表与屏蔽标签表，提供内存缓存与高性能检索。
- **`DownloadRepository` & `DownloadHistoryRepository`**：负责多任务并发下载调度、任务状态持久化与断点续传。

### 4.3 网络引擎层 (Network Layer)
- **`PixivHttpClient`**：配置标准 Pixiv 移动端请求头（`User-Agent`、`App-OS`、`App-Version`、`Accept-Language`）与直连支持。
- **`TokenRefreshPlugin`**：Ktor 自定义插件，在收到 400/401 认证失败时自动使用 Refresh Token 换取新 Access Token 并重放请求，消除上层鉴权感知。
- **`OAuthClient`**：实现 Pixiv 官方 OAuth2 PKCE 鉴权协议。

### 4.4 本地持久化与数据库层 (Persistence Layer)
- **SQLDelight 多数据库**：针对不同领域划分独立 SQLite 数据库文件（`account.db`、`history.db`、`ban.db`、`task.db`），保障模块解耦与数据隔离。
- **`SettingsRepository`**：基于 `multiplatform-settings` 统一管理免代理开关、夜间模式、AI 作品屏蔽开关与画质档位。

### 4.5 跨平台适配层 (Platform Layer)
- **`IllustSaver`**：Android 端写入 MediaStore 系统相册，Desktop 端写入用户 Downloads 或指定图片目录。
- **`IllustClipboard` & `IllustShare`**：封装跨平台的系统剪贴板与原生分享面板调用。

---

## 5. 关键类与接口说明

### `IllustRepository`
- `getRecommendedResponse(nextUrl: String?, forceRefresh: Boolean): Recommend`：获取主页推荐列表（带分页）。
- `getRankingResponse(mode: String, date: String?, nextUrl: String?): Ranking`：获取多维度排行榜（带分页）。
- `getFollowIllustsResponse(restrict: String, nextUrl: String?): FollowIllusts`：获取关注画师动态（带分页）。
- `getIllustCommentsResponse(illustId: Int, nextUrl: String?): CommentResponse`：获取评论列表（带分页）。
- `postComment(illustId: Int, comment: String, parentCommentId: Int?)`：提交或回复评论。

### `SearchRepository`
- `searchIllustResponse(word: String, sort: String, searchTarget: String, duration: String?, bookmarkNum: Int?, nextUrl: String?): Search`：高级组合检索插画（带分页）。
- `searchUserResponse(word: String, nextUrl: String?): UserPreviewsResponse`：按昵称检索画师（带分页）。

### `BanRepository`
- `isBannedByTags(banTags: List<String>, illustTags: List<String>): Boolean`：高性能标签黑名单匹配引擎。
- `addBannedIllust(id: Long, title: String)` / `removeBannedIllust(id: Long)`：作品黑名单管理。

---

## 6. 核心数据流与典型调用链

### 6.1 OAuth 认证与 Token 自动刷新流程

```
UI (LoginScreen) ──> AccountRepository.loginWithCode(code)
                         │
                         ▼
                     OAuthClient.exchangeCodeForToken()
                         │
                         ▼ (返回 AccessToken & RefreshToken)
                     AuthTokenStorage.saveAccount()
                         │
                         ▼
                     发出 loginEventFlow 通知全部页面重载
```
*在后续网络请求中，若 Access Token 过期导致 401，`TokenRefreshPlugin` 将在 Ktor Pipeline 中自动加锁拦截并刷新 Token，无需用户重新登录。*

### 6.2 列表触底流式加载（Infinite Scrolling）调用链

```
用户滑动列表 ──> LazyListState / LazyStaggeredGridState 触发 derivedStateOf 监听
                     │ (距离底部 <= 4~6 项 且 hasMore && !isLoadingMore)
                     ▼
                 Screen.loadMore() ──> Repository.getXXXResponse(nextUrl)
                                           │
                                           ▼ (Ktor 发起异步 GET 请求)
                                       Pixiv API 返回包含下一批数据与新 next_url
                                           │
                                           ▼
                                       BanRepository 过滤黑名单与 AI 作品
                                           │
                                           ▼
                                       追加到当前列表: list = list + filtered
```

---

## 7. 项目构建与运行方式

### 7.1 前置要求
- JDK 17 或 JDK 21
- Android SDK (API 34+)

### 7.2 核心构建命令

- **编译验证 Desktop 端共享模块**：
  ```bash
  ./gradlew :shared:compileKotlinDesktop
  ```
- **编译验证 Desktop 端可执行应用**：
  ```bash
  ./gradlew :composeApp:compileKotlinDesktop
  ```
- **运行 Desktop 端客户端**：
  ```bash
  ./gradlew :composeApp:run
  ```
- **编译验证 Android 端 Debug 构件**：
  ```bash
  ./gradlew :composeApp:compileDebugKotlinAndroid
  ```
- **构建 Android 端 APK 安装包**：
  ```bash
  ./gradlew :composeApp:assembleDebug
  ```
