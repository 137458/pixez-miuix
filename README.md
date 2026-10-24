<div align="center">

<img src="./compose-miuix/shared/src/commonMain/composeResources/drawable/ic_pixez_logo.png" alt="PixEz Logo" width="128" height="128" />

# PixEz MIUIX

**基于 Kotlin Multiplatform 与 Compose Multiplatform 的 Pixiv 第三方客户端，采用 Xiaomi HyperOS（MIUIX）设计规范**

[![License](https://img.shields.io/badge/License-GPL--3.0-orange.svg?style=flat-square)](LICENSE)
[![Active Releases](https://img.shields.io/badge/Active%20Releases-Android%20%7C%20Windows%20Desktop-blue.svg?style=flat-square)](#下载与安装)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7f52ff.svg?style=flat-square\&logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.12.0-4285F4.svg?style=flat-square\&logo=jetpackcompose)](https://www.jetbrains.com/compose-multiplatform/)
[![MIUIX](https://img.shields.io/badge/Design-Xiaomi%20HyperOS%20%2F%20MIUIX-ff6900.svg?style=flat-square)](https://github.com/compose-miuix-ui/miuix)

[English](./.github/README_en.md) · [Bahasa Indonesia](./.github/README_id.md) · [使用指南 & FAQ](.github/FAQ.md) · [发布版本](https://github.com/137458/pixez-miuix/releases)

</div>

***

## 目录

- [项目简介](#项目简介)
- [核心特性](#核心特性)
- [下载与安装](#下载与安装)
- [使用说明](#使用说明)
- [工程架构](#工程架构)
- [源码构建](#源码构建)
- [贡献者与鸣谢](#贡献者与鸣谢)
- [社区与反馈](#社区与反馈)
- [免责声明](#免责声明)
- [开源协议](#开源协议)

***

## 项目简介

**PixEz MIUIX** 是 Pixiv 第三方客户端 PixEz 的重制版本。项目基于 Kotlin Multiplatform（简称 KMP）与 Compose Multiplatform（简称 CMP）开发。界面使用 MIUIX 组件库（`top.yukonga.miuix.kmp`）。

当前发行平台为 Android 与 Windows Desktop。iOS 仅做编译验证，macOS target 暂未启用。

应用面向中国大陆网络环境提供免代理直连。内置 SNI 直连、DoH 安全 DNS 与可选图源镜像。

***

## 核心特性

- **MIUIX 界面与 Liquid Glass 悬浮底栏**：
  - 界面基于 MIUIX 组件库构建，主要容器为 `Card` 与 `BasicComponent`。
  - 悬浮底栏支持液态玻璃折射与背景模糊两种渲染。设备不支持 `RuntimeShader` 时自动回退为模糊效果。
  - 着色器动效在 Android 13（API 33）及以上使用系统 `RuntimeShader`，桌面端使用 Skia `RuntimeEffect`。
  - 平板与宽屏启用多列瀑布流。悬浮底栏限宽居中（最大 540 dp），内容区最大宽度 760 dp。
- **列表浏览**：
  - 首页推荐、关注动态、搜索、排行榜、画师主页、系列作品、评论区与好友列表均支持下拉刷新。
  - 列表触底时自动加载更多内容。
- **Pixiv 核心功能**：
  - 排行榜：日榜、周榜、月榜、新人榜、原创榜、R-18 榜与 AI 榜。
  - 插画与漫画多页浏览，动图（Ugoira）逐帧播放。
  - 小说：站内搜索、详情浏览与阅读器阅读（支持内嵌插画、章节标记与分页链接），可收藏、记录阅读历史与查看发表评论。
  - 关注动态内置漫画系列追更列表，可在系列页一键追更或取消追更。
  - Pixivision 特辑图文浏览。
  - 评论区支持多级回复。
  - 以图搜图：选取本地图片，通过 SauceNAO 检索相似作品。
- **内容过滤**：
  - 支持标签屏蔽、画师与作品屏蔽、NSFW 遮罩与 AI 生成作品过滤。
- **收藏与下载联动**：
  - 可在设置中开启收藏后自动保存、保存后自动收藏、收藏时自动添加标签与收藏时自动关注画师。
- **下载管理**：
  - 支持原图与分页原图下载。下载与动图压缩包共用 256 MB 单任务上限，超限中止并清理临时文件。
  - 下载队列并发数可在 1～6 之间调节，默认同时运行 2 个任务。失败任务可重试。
  - Android 端可将图片保存到系统相册（`Pictures/PixEz` 与 `Downloads/PixEz`）。
- **账号与网络**：
  - OAuth2 PKCE 登录。令牌过期后自动刷新并重试原请求。
  - 支持多账号切换。
  - 免代理直连：SNI 直连模式、DoH 安全 DNS 与可选图源镜像。DoH 解析失败时自动熔断，冷却期内回退系统 DNS。
  - `TrustedUrlPolicy` 限制外部链接仅访问受信任域名。

***

## 下载与安装

请前往 [GitHub Releases](https://github.com/137458/pixez-miuix/releases) 页面下载对应平台的安装包或运行包：

### Android

下载 `PixEz-MIUIX-v*.apk` 并在设备上直接安装。支持 Android 7.0（API 24）及以上系统，适配手机与平板。

### Windows 桌面端（无需预装 Java 环境）

- **单文件独立版**：下载 `PixEz-Standalone.exe`，内嵌运行时，双击即可直接运行，无需解压。
- **免安装便携版**：下载 `PixEz-windows-x64-portable.zip`，解压后双击 `PixEz.exe` 即可运行。
- **MSI 安装包**：下载 `PixEz-*.msi`，双击按提示安装，自动创建桌面与开始菜单快捷方式。

### 其他桌面目标

Linux 可从源码构建，打包格式包含 Deb。macOS target 因依赖库缺少对应变体暂未启用。两个平台当前均未提供正式发行安装包。

***

## 使用说明

1. **登录账号**：启动应用进入登录页，完成 Pixiv 网页端授权。应用自动换取令牌并保存。
2. **浏览与发现**：
   - 底部悬浮底栏提供“推荐”、“搜索”、“排行”、“动态”、“特辑”五个页签。
   - 列表触底时自动加载更多内容，下拉即可刷新。
3. **作品操作**：在详情页可收藏作品、下载原图、查看画师主页、阅读评论与相关推荐。
4. **内容过滤**：在设置中配置标签屏蔽、画师屏蔽、用户屏蔽与 AI 生成作品过滤。

***

## Flutter v1 归档

Flutter v1 已从主线归档至 [`archive/flutter-v1/`](archive/flutter-v1/)，不再参与日常开发与桌面发布。该目录保留完整历史源码、平台工程与私有插件，供历史追溯与安全修复使用。新功能开发在 [`compose-miuix/`](compose-miuix/) 进行。

Android 端 Flutter v1 与 Compose 版使用不同应用 ID，无法覆盖安装升级。详细迁移说明见 [archive README](archive/flutter-v1/README.md)。

## 工程架构

```text
pixez-flutter-MIUIX/
├── compose-miuix/                      # Kotlin Multiplatform 工程
│   ├── composeApp/                     # 应用入口模块
│   │   ├── src/androidMain/            # Android 入口、清单与资源
│   │   └── src/desktopMain/            # Desktop JVM 入口与打包配置
│   └── shared/                         # 跨平台共享模块
│       └── src/commonMain/
│           ├── composeResources/       # 图标与多语言资源
│           ├── sqldelight/             # SQLDelight 数据库定义
│           └── kotlin/com/perol/pixez/shared/
│               ├── data/               # 数据模型、本地存储与 Repository
│               ├── navigation/         # Decompose 路由与导航
│               ├── network/            # Ktor 客户端、OAuth 认证与令牌刷新
│               ├── platform/           # Expect/Actual 平台抽象
│               ├── ui/                 # 页面、MIUIX 组件与动效
│               └── utils/              # 通用工具
├── AGENTS.md                           # 构建验证命令清单
├── CHANGELOG.md                        # 版本变更记录
├── Code_Wiki.md                        # 架构与调用链说明
├── MIUIX_Spec.md                       # UI 组件规范
└── Global_TODO.md                      # 待办与路线图
```

`shared/src/commonMain` 下包含 20 个 `*Repository.kt` 与 42 个 `*Screen.kt`。

***

## 源码构建

### 环境要求

- JDK 17。
- Android SDK：`compileSdk = 36`、`minSdk = 24`。
- 构建通过 Gradle Wrapper（Gradle 8.14.4）执行。
- Windows 单文件 EXE 打包依赖 .NET Framework 的 `csc.exe`。

### 常用命令

以下命令在 `compose-miuix/` 目录执行。完整验证清单以 `AGENTS.md` 为唯一来源：

```bash
# Android Debug 编译验证
./gradlew :composeApp:compileDebugKotlinAndroid

# 构建 Android Debug APK
./gradlew :composeApp:assembleDebug

# 编译 Desktop 共享模块
./gradlew :shared:compileKotlinDesktop

# 运行桌面端
./gradlew :composeApp:run

# 运行双端单元测试
./gradlew :shared:desktopTest :composeApp:desktopTest

# 打包 Windows 单文件 EXE
./gradlew :composeApp:packageWindowsSingleFileExe
```

Android Release APK 使用 `./gradlew :composeApp:assembleRelease` 构建。发布流程见 `RELEASE_NOTES_GUIDE.md`。

***

## 贡献者与鸣谢

本项目感谢所有为 PixEz 与 MIUIX 重制版做出贡献的开发者与社区成员：

<table>
  <tr>
    <td align="center"><a href="https://github.com/137458"><img src="https://avatars.githubusercontent.com/u/138956834?v=4" width="100px;" alt=""/><br /><sub><b>137458 (Rosemary)</b></sub></a><br /><a href="https://github.com/137458/pixez-miuix/commits?author=137458" title="Code & UI">💻 🎨</a></td>
    <td align="center"><a href="https://github.com/Notsfsssf"><img src="https://avatars.githubusercontent.com/u/16934707?v=4" width="100px;" alt=""/><br /><sub><b>Perol_Notsfsssf</b></sub></a><br /><a href="https://github.com/Notsfsssf/pixez-flutter/commits?author=Notsfsssf" title="Code">💻</a></td>
    <td align="center"><a href="https://xyx.moe"><img src="https://avatars.githubusercontent.com/u/9017470?v=4" width="100px;" alt=""/><br /><sub><b>Skimige</b></sub></a><br /><a href="https://github.com/Skimige" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/TragicLifeHu"><img src="https://avatars.githubusercontent.com/u/16817202?v=4" width="100px;" alt=""/><br /><sub><b>Tragic Life</b></sub></a><br /><a href="#translation-TragicLifeHu" title="Translation">🌍 (zh_TW)</a></td>
    <td align="center"><a href="http://ivtune.net"><img src="https://avatars.githubusercontent.com/u/54385201?v=4" width="100px;" alt=""/><br /><sub><b>karin722</b></sub></a><br /><a href="#translation-karin722" title="Translation">🌍 (ja)</a></td>
    <td align="center"><a href="http://archman.fun"><img src="https://avatars.githubusercontent.com/u/68731023?v=4" width="100px;" alt=""/><br /><sub><b>Romani-Archman</b></sub></a><br /><a href="#documentation-Romani-Archman" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/itzXian"><img src="https://avatars.githubusercontent.com/u/34748039?v=4" width="100px;" alt=""/><br /><sub><b>Xian</b></sub></a><br /><a href="#translation-itzXian" title="Translation">🌍 (en_US)</a></td>
    <td align="center"><a href="https://github.com/ReikiAigawara"><img src="https://avatars.githubusercontent.com/u/66962815?v=4" width="100px;" alt=""/><br /><sub><b>Reiki Aigawara</b></sub></a><br /><a href="#translation-ReikiAigawara" title="Translation">🌍 (id_ID)</a></td>
  </tr>
</table>

欢迎通过 Pull Request 参与代码、界面与多语言翻译贡献。

特别鸣谢 [pixez-flutter](https://github.com/Notsfsssf/pixez-flutter)：PixEz 原版 Flutter 客户端。
特别鸣谢 [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)：Xiaomi HyperOS 风格的 Compose 组件库。

***

## 社区与反馈

- 使用指南与常见问题：[FAQ](.github/FAQ.md)
- 邮件反馈：[gaaf14738@gmail.com](mailto:gaaf14738@gmail.com)

***

## 免责声明

1. **非官方客户端声明**：PixEz MIUIX 是由开源社区与个人开发者基于学习、研究与技术探索目的独立开发的第三方 Pixiv 客户端，与 pixiv Inc. 及其关联公司无任何商业关联、投资关系或官方合作。本项目的名称、标识及内容均不代表 pixiv Inc. 的官方立场。
2. **知识产权与版权归属**：本应用内展示的所有插画、漫画、小说、动图（Ugoira）、Pixivision 特辑及相关元数据的版权均归原作者及 pixiv Inc. 所有。用户通过本应用获取或下载的内容，仅供个人非商业性学习、鉴赏与交流使用。严禁将任何相关资源用于商业用途、未经授权的二次分发或其他侵犯版权的行为。
3. **网络与安全合规**：应用内置直连通道等网络优化方案，仅用于网络通信研究与连通性改善目的。使用者应自觉遵守所在国家及地区的法律法规，以及 pixiv 平台的服务条款（Terms of Service）。因用户个人不当使用本应用或其中内容所造成的任何纠纷、损失或法律责任，由使用者自行承担。本项目开发者及贡献者不承担任何直接或连带责任。
4. **服务可用性与稳定性**：本项目以开源形式免费提供，不保证应用及其内置服务的持续可用性、稳定性或安全性。开发者保留在不另行通知的情况下调整、中断或终止部分或全部功能的权利。
5. **第三方链接与内容**：本应用可能包含指向 pixiv 官方平台或其他第三方网站的链接及引用，上述外部资源由其各自所有者独立运营与负责。本项目对任何第三方内容、服务的准确性、合法性或安全性不作任何保证。


***

## 开源协议

本项目遵循 [GPL-3.0 License](LICENSE) 开源协议。
