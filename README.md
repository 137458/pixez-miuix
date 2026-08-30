<div align="center">

<img src="./compose-miuix/shared/src/commonMain/composeResources/drawable/ic_pixez_logo.png" alt="PixEz Logo" width="128" height="128" />

# PixEz MIUIX

**基于 Compose Multiplatform 与 Xiaomi HyperOS (MIUIX) 设计规范打造的高颜值、现代跨平台 Pixiv 客户端**

[![License](https://img.shields.io/badge/License-GPL--3.0-orange.svg?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Desktop%20%7C%20iOS%20%7C%20macOS-blue.svg?style=flat-square)](#下载与安装)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7f52ff.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-4285F4.svg?style=flat-square&logo=jetpackcompose)](https://www.jetbrains.com/compose-multiplatform/)
[![MIUIX](https://img.shields.io/badge/Design-Xiaomi%20HyperOS%20%2F%20MIUIX-ff6900.svg?style=flat-square)](https://github.com/compose-miuix-ui/miuix)

[English](./.github/README_en.md) · [Bahasa Indonesia](./.github/README_id.md) · [使用指南 & FAQ](.github/FAQ.md) · [发布版本](https://github.com/137458/pixez-miuix/releases)

</div>

---

## 目录

- [项目简介](#项目简介)
- [核心特性](#核心特性)
- [文档中心](#文档中心)
- [下载与安装](#下载与安装)
- [使用说明](#使用说明)
- [工程架构](#工程架构)
- [源码构建](#源码构建)
- [贡献者与鸣谢](#贡献者与鸣谢)
- [社区与反馈](#社区与反馈)
- [免责声明](#免责声明)
- [开源协议](#开源协议)

---

## 项目简介

**PixEz MIUIX** 是原知名开源 Pixiv 第三方客户端 PixEz 的全新世代重构版本。本项目全面拥抱 **Kotlin Multiplatform (KMP)** 与 **Compose Multiplatform (CMP)** 技术栈，并深度结合 **Xiaomi HyperOS (MIUIX)** 设计语言与交互规范，为各平台用户提供原生流畅、通透轻盈、富有动感的美学体验。

无论是在移动设备还是桌面设备上，PixEz MIUIX 均支持中国大陆地区免代理直连，让优质艺术作品触手可及。

---

## 核心特性

- **Xiaomi HyperOS (MIUIX) 原生交互美学**：
  - 官方 Squircle 超椭圆几何圆角与 MIUIX 层级卡片容器 (`Card`, `BasicComponent`)。
  - Liquid Glass 悬浮液态玻璃底栏：高斯模糊基底、动态高光与透镜折射三重渲染。
  - 动态流光背景：基于 Android 13+ 原生 `RuntimeShader` 与 Desktop Skia `RuntimeEffect` 的实时 GLSL 着色器。
  - 大屏与平板响应式适配：自适应多列瀑布流与居中限宽悬浮底栏。
- **全链路流式分页与无限滚动**：
  - 首页推荐、关注动态、插画搜索、画师搜索、排行榜、相关推荐、系列作品与评论区均支持触底平滑预加载与下拉刷新。
- **强大的 Pixiv 核心功能**：
  - 推荐与多维度榜单（日榜、周榜、月榜、新人榜、原创榜、R-18 榜）。
  - 高清大图多页轮播、动图 (Ugoira) 原生帧播放。
  - Pixivision 官方特辑沉浸式图文阅读器。
  - 评论区多级回复与作品关联推荐。
- **严密的内容过滤与安全防护系统**：
  - 标签屏蔽、作品 ID 屏蔽、画师 ID 屏蔽、NSFW 敏感内容遮罩与 AI 生成作品过滤。
- **收藏与下载深度联动**：
  - 收藏自动下载、下载自动收藏、收藏自动打标与关注画师多项深度联动配置。
- **多任务下载与资源管理**：
  - 原图一键高速下载、后台任务队列调度、断点续传与自动同步至系统相册。
- **全自动鉴权与免代理直连**：
  - OAuth2 PKCE 认证流程与 Token 自动无感刷新。
  - 内置免代理 SNI 混淆直连与防盗链图片管线。

---

## 文档中心

本项目建立了完善的工程与设计规范文档库，欢迎查阅：

- [Code Wiki (架构全景手册)](Code_Wiki.md)：详细记录项目分层架构、目录结构、模块职责、数据流调用链与关键类说明。
- [MIUIX Spec (UI 架构与设计规范)](MIUIX_Spec.md)：详细规定 MIUIX 组件选型、Liquid Glass 渲染、响应式断点与大屏适配准则。
- [Global TODO (全局待办与路线图)](Global_TODO.md)：统一汇总各模块演进清单、优先级与交付状态。

---

## 下载与安装

请前往 [GitHub Releases](https://github.com/137458/pixez-miuix/releases) 页面下载对应平台的最新安装包或运行包：

### Android
- 下载 `PixEz-MIUIX-v*.apk` 文件并在设备上直接安装运行（支持 Android 7.0 / API 24 及以上系统，已适配手机、折叠屏与平板）。

### 桌面端 (Windows / macOS / Linux)
- **跨平台可执行包**: 下载 `PixEz-MIUIX-v*-windows-x64.jar`（或跨平台 JAR 包），确保本地配备 Java 17 或 21 运行环境，在终端执行 `java -jar <文件名>.jar` 或直接双击即可运行。
- **Windows 安装包**: 可在 Release 页面或 Actions 构建产物中下载 `.msi` 安装包与便携版。

---

## 使用说明

1. **登录账号**：启动应用后进入登录页，点击登录并完成 Pixiv 网页端授权，应用将自动完成 Token 兑换并持久化。
2. **浏览与发现**：
   - 底部悬浮底栏提供“推荐 (Hello)”、“动态 (New)”、“发现 (Spotlight)”、“我的 (User)”快速切换。
   - 列表向下滑动时将自动流式预加载后续作品，下拉即可触发即时刷新。
3. **作品操作**：
   - 点击作品卡片进入沉浸式详情页，可进行收藏、点赞、下载原图、查看画师主页、阅读评论及关联作品。
4. **屏蔽设置**：
   - 进入“设置 -> 屏蔽管理”，可配置屏蔽标签、画师黑名单、作品黑名单及是否过滤 AI 生成内容。

---

## 工程架构

```text
pixez-flutter-MIUIX/
├── compose-miuix/                     # Compose Multiplatform 跨平台源码
│   ├── composeApp/                    # 各平台入口模块 (Android / Desktop)
│   │   ├── src/androidMain/           # Android 原生入口与清单文件
│   │   └── src/desktopMain/           # Desktop JVM 平台入口与打包配置
│   └── shared/                        # 跨平台共享核心模块
│       └── src/commonMain/
│           ├── composeResources/      # 矢量图标与多语言资源
│           ├── sqldelight/            # SQLDelight 数据库架构定义
│           └── kotlin/com/perol/pixez/shared/
│               ├── data/              # 数据模型、数据库驱动与 14 个核心 Repository
│               ├── network/           # Ktor HTTP 客户端、OAuth 认证与 Token 刷新插件
│               ├── platform/          # 跨平台 Expect 抽象 (相册、剪贴板、分享)
│               └── ui/                # 46 个业务界面、MIUIX 组件与液态玻璃动效
├── Code_Wiki.md                       # 架构全景手册
├── MIUIX_Spec.md                      # UI 设计与组件规范
└── Global_TODO.md                     # 全局待办与路线图
```

---

## 源码构建

如需从源码编译开发，请确保本地具备以下开发环境：
- **JDK**: Java Development Kit 17 或 21
- **Android SDK**: `compileSdk = 36`, `minSdk = 24`

```bash
# 1. 切换至 Compose 工程目录
cd compose-miuix

# 2. 编译并验证 Desktop 端共享模块
./gradlew :shared:compileKotlinDesktop

# 3. 运行 Desktop (JVM) 桌面端客户端
./gradlew :composeApp:run

# 4. 编译并安装 Android Debug 版本至连接的设备
./gradlew :composeApp:installDebug

# 5. 打包 Android Release APK
./gradlew :composeApp:assembleRelease

# 6. 打包当前桌面平台安装包 (MSI / DMG / DEB)
./gradlew :composeApp:packageDistributionForCurrentOS
```

---

## 贡献者与鸣谢

本项目感谢所有为 PixEz 及其 MIUIX 跨平台重构做出贡献的开发者与社区成员：

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

如果你愿意为本项目贡献代码、优化设计或完善多语言翻译，欢迎随时提交 Pull Request！

特别鸣谢 [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) 社区提供的优秀 Compose MIUIX 跨平台组件库。

---

## 社区与反馈

- 使用指南 & 常见问题：[点此查看 FAQ 文档](.github/FAQ.md)
- 邮件反馈：PxezFeedback@outlook.com
- Telegram 官方频道：[@PixEzChannel](https://t.me/PixEzChannel)
- Discord 社区：[@PixEz](https://discord.gg/Em9AeJbg)
- QQ 交流群：815791942

---

## 免责声明

1. **非官方客户端声明**：PixEz MIUIX 为开源社区与个人开发者基于学习、研究和技术探索目的打造的第三方 Pixiv 客户端，与 pixiv Inc. 无任何商业关联或官方合作。
2. **知识产权与版权归属**：本应用内展示的所有插画、漫画、小说、动图、特辑及相关元数据版权均归原作者及 pixiv Inc. 所有。用户通过本应用获取或下载的内容仅供个人学习、鉴赏与交流，严禁将相关资源用于任何形式的商业用途或未经授权的二次分发。
3. **网络与安全合规**：应用内置的直连通道与技术方案仅用于网络通信研究与连通性优化，使用者应自觉遵守所在国家及地区的法律法规与平台服务条款。因用户个人不当使用造成的任何纠纷、损失或法律责任，由使用者自行承担，本项目开发者及贡献者不承担任何直接或连带责任。

---

## 开源协议

本项目遵循 [GPL-3.0 License](LICENSE) 开源协议。
