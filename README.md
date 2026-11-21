<div align="center">

<img src="./compose-miuix/shared/src/main/res/drawable-nodpi/ic_pixez_logo.png" alt="PixEz Logo" width="128" height="128" />

# PixEz MIUIX

**基于 Kotlin 与 Jetpack Compose 的 Pixiv 第三方客户端，采用 Xiaomi HyperOS（MIUIX）与 ColorOS（COUI）双界面设计**

[![License](https://img.shields.io/badge/License-GPL--3.0-orange.svg?style=flat-square)](LICENSE)
[![Active Releases](https://img.shields.io/badge/Active%20Releases-Android%2012%2B-blue.svg?style=flat-square)](#下载与安装)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7f52ff.svg?style=flat-square\&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.08.01-4285F4.svg?style=flat-square\&logo=jetpackcompose)](https://developer.android.com/compose)
[![MIUIX](https://img.shields.io/badge/Design-Xiaomi%20HyperOS%20%2F%20MIUIX-ff6900.svg?style=flat-square)](https://github.com/compose-miuix-ui/miuix)

[English](./.github/README_en.md) · [Bahasa Indonesia](./.github/README_id.md) · [使用指南与 FAQ](.github/FAQ.md) · [发布版本](https://github.com/137458/pixez-miuix/releases)

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

**PixEz MIUIX** 是 Pixiv 第三方客户端 PixEz 的重制版本。项目使用 Jetpack Compose 与 Kotlin 开发，专注 Android 手机与平板端，支持 Xiaomi HyperOS（MIUIX）与 ColorOS（COUI）双界面皮肤。

面向中国大陆网络环境改善连通性：域名经 DoH 解析，图源可选镜像。ECH 档与兼容档的差异由底层 TLS 协商承担，应用不修改 SNI。

***

## 核心特性

- **MIUIX 界面与 Liquid Glass 悬浮底栏**：
  - 界面基于 MIUIX 组件库构建。
  - 悬浮底栏支持液态玻璃折射与背景模糊两种渲染；设备不支持 `RuntimeShader` 时回退为模糊。
  - 着色器动效在 Android 13（API 33）及以上使用系统 `RuntimeShader`，COUI 在 Android 14（API 34）及以上支持实时背景采样玻璃材质，低版本平滑退回背景模糊。
  - 平板与宽屏启用多列瀑布流。
  - 悬浮底栏限宽居中，最大 540 dp；内容区最大宽度 760 dp。
- **列表浏览**：
  - 首页、动态、搜索、排行、画师主页、系列与评论区均支持下拉刷新。
  - 列表触底时自动加载更多。
- **Pixiv 核心功能**：
  - 排行榜含日榜、周榜、月榜、新人榜、原创榜、R-18 榜与 AI 榜。
  - 插画与漫画支持多页浏览，动图（Ugoira）支持逐帧播放。
  - 小说支持站内搜索、详情浏览与阅读器阅读。阅读器含内嵌插画、章节标记与分页链接。
  - 小说可收藏，并保留阅读历史与评论浏览。
  - 关注动态内置漫画系列追更列表，可在系列页一键追更或取消追更。
  - Pixivision 特辑支持图文浏览。
  - 评论区支持多级回复。
  - 以图搜图经 SauceNAO 检索本地图片的相似作品。
- **内容过滤**：
  - 支持标签屏蔽、画师与作品屏蔽、NSFW 遮罩与 AI 生成作品过滤。
- **收藏与下载联动**：
  - 设置中可开启收藏后自动保存与保存后自动收藏。
  - 亦可开启收藏时自动添加标签与收藏时自动关注画师。
- **下载管理**：
  - 支持原图与分页原图下载。
  - 下载与动图压缩包共用 256 MB 单任务上限，超限中止并清理临时文件。
  - 下载队列并发数可在 1～6 之间调节，默认同时运行 2 个任务；失败任务可重试。
  - Android 端可将图片保存到系统相册（`Pictures/PixEz` 与 `Downloads/PixEz`）。
- **账号与网络**：
  - OAuth2 PKCE 登录；令牌过期后自动刷新并重试原请求。
  - 支持多账号切换。
  - 网络模式分 `ech`、`compat`、`standard` 三档，账号接口与图片接口分别设置。
  - 域名解析走 DoH；DoH 失败后熔断 5 分钟，冷却期内回退系统 DNS。
  - 图源支持默认 Host、镜像 Host 与自定义 Host。
  - `standard` 档强制回落默认 Host，并隐藏图源选项。
  - `TrustedUrlPolicy` 将分页、图片与特辑请求收敛到受信任域名。

***

## 下载与安装

请前往 [GitHub Releases](https://github.com/137458/pixez-miuix/releases) 页面下载最新 Android 安装包：

### Android

下载 `PixEz-MIUIX-v*.apk` 并在设备上直接安装。支持 Android 12（API 31）及以上系统，适配手机与平板。

***

## 使用说明

1. **登录账号**：启动后进入登录页，完成 Pixiv 网页端授权（OAuth2 PKCE）。应用自动换取令牌并保存。
2. **浏览与发现**：
   - 底部悬浮底栏提供「推荐」「搜索」「排行」「动态」「特辑」五个页签。
   - 列表触底时自动加载更多，下拉即可刷新。
3. **作品操作**：在详情页可收藏作品、下载原图、查看画师主页、阅读评论与相关推荐。
4. **内容过滤**：在设置中配置标签屏蔽、画师屏蔽、用户屏蔽与 AI 生成作品过滤。

***

## 工程架构

```text
pixez-flutter-MIUIX/
├── compose-miuix/                      # Android 主工程（Kotlin + Compose）
│   ├── composeApp/                     # 应用入口模块（MainActivity、小组件、清单与打包配置）
│   ├── gradle/libs.versions.toml       # 依赖版本目录
│   └── shared/                         # 共享业务与 UI 模块
│       └── src/main/
│           ├── res/                    # Android 资源（多语言与图标）
│           ├── sqldelight/             # SQLDelight 数据库定义与迁移
│           └── kotlin/com/perol/pixez/shared/
│               ├── data/               # 数据模型、本地存储与仓库
│               ├── navigation/         # Decompose 路由与导航
│               ├── network/            # Ktor 客户端、OAuth 认证与令牌刷新
│               ├── platform/           # 平台能力封装
│               ├── ui/                 # 页面、MIUIX / COUI 组件与动效
│               └── utils/              # 通用工具
├── .github/                            # FAQ、多语言 README 与截图
├── AGENTS.md                           # Agent 约束与构建验证命令清单
├── CHANGELOG.md                        # 版本变更记录
├── GEMINI.md                           # 指向 AGENTS.md 的桩文件
├── LICENSE                             # GPL-3.0 许可全文
└── README.md                           # 项目说明
```

`shared/src/main/kotlin` 共 384 个 Kotlin 源文件，包含 87 个 `*Screen.kt`（涵盖 HyperOS 与 ColorOS 双皮肤页面）。

### 技术栈

| 组件 | 版本 |
| :--- | :--- |
| JDK | 17 |
| Gradle | 8.14.4 |
| Kotlin | 2.4.10 |
| Android Gradle Plugin | 8.13.0 |
| Jetpack Compose BOM | 2026.08.01 (`compose-bom-alpha`) |
| MIUIX | 0.9.4 |
| COUI | 0.0.1-alpha12 |
| Ktor | 3.1.2 |
| Coil | 3.6.3 |
| SQLDelight | 2.0.2 |
| Decompose | 3.3.0 |
| kotlinx-coroutines | 1.10.2 |
| kotlinx-serialization | 1.8.1 |

版本以 `compose-miuix/gradle/libs.versions.toml` 与 `compose-miuix/composeApp/build.gradle.kts` 为准。

***

## 源码构建

### 环境要求

- JDK 17。
- Android SDK：`compileSdk = 36`、`minSdk = 31`、`targetSdk = 35`。
- 构建通过 Gradle Wrapper（Gradle 8.14.4）执行。

### 常用命令

以下命令在 `compose-miuix/` 目录执行。完整验证清单以 `AGENTS.md` 为唯一来源：

```bash
# Android Debug 编译验证
./gradlew :shared:compileDebugKotlin :composeApp:compileDebugKotlin

# 运行单元测试
./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest

# 构建 Android Debug APK
./gradlew :composeApp:assembleDebug
```

Android Release APK 使用 `./gradlew :composeApp:assembleRelease` 构建。未配置 `compose-miuix/keystore.properties` 时回退 debug 签名。此时产物版本名带 `-debug-signed` 后缀。

***

## 贡献者与鸣谢

本项目感谢所有为 PixEz 与 MIUIX 重制版做出贡献的开发者与社区成员：

<table>
  <tr>
    <td align="center"><a href="https://github.com/137458"><img src="https://avatars.githubusercontent.com/u/138956834?v=4" width="100px;" alt=""/><br /><sub><b>137458 (Rosemary)</b></sub></a><br />代码与界面</td>
    <td align="center"><a href="https://github.com/Notsfsssf"><img src="https://avatars.githubusercontent.com/u/16934707?v=4" width="100px;" alt=""/><br /><sub><b>Perol_Notsfsssf</b></sub></a><br />代码</td>
    <td align="center"><a href="https://xyx.moe"><img src="https://avatars.githubusercontent.com/u/9017470?v=4" width="100px;" alt=""/><br /><sub><b>Skimige</b></sub></a><br />文档</td>
    <td align="center"><a href="https://github.com/TragicLifeHu"><img src="https://avatars.githubusercontent.com/u/16817202?v=4" width="100px;" alt=""/><br /><sub><b>Tragic Life</b></sub></a><br />翻译（zh_TW）</td>
    <td align="center"><a href="http://ivtune.net"><img src="https://avatars.githubusercontent.com/u/54385201?v=4" width="100px;" alt=""/><br /><sub><b>karin722</b></sub></a><br />翻译（ja）</td>
    <td align="center"><a href="http://archman.fun"><img src="https://avatars.githubusercontent.com/u/68731023?v=4" width="100px;" alt=""/><br /><sub><b>Romani-Archman</b></sub></a><br />文档</td>
    <td align="center"><a href="https://github.com/itzXian"><img src="https://avatars.githubusercontent.com/u/34748039?v=4" width="100px;" alt=""/><br /><sub><b>Xian</b></sub></a><br />翻译（en_US）</td>
    <td align="center"><a href="https://github.com/ReikiAigawara"><img src="https://avatars.githubusercontent.com/u/66962815?v=4" width="100px;" alt=""/><br /><sub><b>Reiki Aigawara</b></sub></a><br />翻译（id_ID）</td>
  </tr>
</table>

欢迎通过 Pull Request 参与代码、界面与多语言翻译贡献。

特别鸣谢 [pixez-flutter](https://github.com/Notsfsssf/pixez-flutter)：PixEz 原版 Flutter 客户端。
特别鸣谢 [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)：Xiaomi HyperOS 风格的 Compose 组件库。

***

## 社区与反馈

- 使用指南与常见问题：[FAQ](.github/FAQ.md)
- 邮件反馈：[PxezFeedBack@outlook.com](mailto:PxezFeedBack@outlook.com)

***

## 免责声明

1. **非官方客户端声明**：PixEz MIUIX 由开源社区与个人开发者独立开发，目的为学习、研究与技术探索。本项目与 pixiv Inc. 及其关联公司无商业关联、投资关系或官方合作。本项目的名称、标识及内容均不代表 pixiv Inc. 的官方立场。
2. **知识产权与版权归属**：本应用展示的插画、漫画、小说、动图（Ugoira）、Pixivision 特辑及相关元数据，版权均归原作者及 pixiv Inc. 所有。用户获取或下载的内容，仅供个人非商业性学习、鉴赏与交流使用。严禁用于商业用途、未经授权的二次分发或其他侵权行为。
3. **网络与安全合规**：应用内置 DoH 域名解析与可选图源镜像，仅用于网络通信研究与连通性改善。使用者应遵守所在国家及地区的法律法规，以及 pixiv 平台的服务条款（Terms of Service）。因用户不当使用本应用或其中内容引发的纠纷、损失或法律责任，由使用者自行承担，本项目开发者及贡献者不承担任何直接或连带责任。
4. **服务可用性与稳定性**：本项目以开源形式免费提供，不保证应用及其内置服务的持续可用性、稳定性或安全性。开发者保留在不另行通知的情况下调整、中断或终止部分或全部功能的权利。
5. **第三方链接与内容**：本应用可能包含指向 pixiv 官方平台或其他第三方网站的链接及引用。外部资源由其各自所有者独立运营与负责。本项目对第三方内容与服务的准确性、合法性或安全性不作保证。

***

## 开源协议

本项目遵循 [GPL-3.0 License](LICENSE) 开源协议。
