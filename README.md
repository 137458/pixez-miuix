<div align="center">

<img src="./compose-miuix/shared/src/commonMain/composeResources/drawable/ic_pixez_logo.png" alt="PixEz Logo" width="128" height="128" />

# PixEz MIUIX

**基于 Compose Multiplatform 与 Xiaomi HyperOS (MIUIX) 设计规范打造的高颜值、现代跨平台 Pixiv 客户端**

[![License](https://img.shields.io/badge/License-GPL--3.0-orange.svg?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Desktop%20%7C%20iOS%20%7C%20macOS-blue.svg?style=flat-square)](#-支持平台与架构)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7f52ff.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-4285F4.svg?style=flat-square&logo=jetpackcompose)](https://www.jetbrains.com/compose-multiplatform/)
[![MIUIX](https://img.shields.io/badge/Design-Xiaomi%20HyperOS%20%2F%20MIUIX-ff6900.svg?style=flat-square)](https://github.com/compose-miuix-ui/miuix)

[English](./.github/README_en.md) · [Bahasa Indonesia](./.github/README_id.md) · [使用指南 & FAQ](.github/FAQ.md) · [发布版本](https://github.com/137458/pixez-miuix/releases)

</div>

---

## 📖 项目简介

**PixEz MIUIX** 是原知名开源 Pixiv 第三方客户端 PixEz 的全新世代重构版本。本项目全面拥抱 **Kotlin Multiplatform (KMP)** 与 **Compose Multiplatform (CMP)** 技术栈，并深度结合 **Xiaomi HyperOS (MIUIX)** 设计语言与交互规范，为各平台用户提供原生流畅、通透轻盈、富有动感的美学体验。

无论是在移动设备还是桌面设备上，PixEz MIUIX 均支持中国大陆地区免代理直连，让优质艺术作品触手可及。

---

## ✨ 核心特性与设计亮点

### 🎨 极致的 Xiaomi HyperOS (MIUIX) 交互美学
- **全套 HyperOS 视觉规范**：采用官方 Squircle（超椭圆）几何圆角、层级卡片容器（`Card`、`BasicComponent`）与系统级色彩体系。
- **IosLiquidGlass 悬浮液态玻璃底栏**：
  - 官方 3 层高级渲染架构：`CombinedBackdrop` 高斯模糊基底、动态交互高光拾取、色散折射透镜层。
  - 动态阻尼弹性手势拖拽动画（`DampedDragAnimation`），支持平滑跟随与回弹。
- **HyperOS 2 / HyperOS 3 动态流光背景**：
  - 官方 GLSL 运行时动态着色器（Android 13+ 原生 `RuntimeShader` / Desktop 端 Skia `RuntimeEffect`）。
  - 60 帧色彩阶段平滑过渡与流动噪点，带来前沿系统的光效体验。
- **平滑视差与微动效**：Hero 图标根据列表滚动实时缩放与渐隐，顶部标题动态淡入过渡。

### ⚡ 现代化多平台架构与极致性能
- **跨平台技术栈**：
  - **核心 UI**：Jetpack / JetBrains Compose Multiplatform，真正做到一套代码多端统一且兼具平台原生表现。
  - **页面路由**：Decompose 架构接管完整的页面栈生命周期与物理返回键调度。
  - **异步网络**：Ktor Client + Kotlinx.Serialization，支持全自动 Token 刷新机制与免代理 SNI 混淆直连。
  - **本地数据库**：SQLDelight 强类型多数据库架构，无缝兼容原版历史记录与屏蔽规则。
  - **高性能图片管线**：Coil 3 跨平台渲染引擎，内置 Referer 防盗链与多级内存/磁盘缓存。

### 🖼️ 全面的 Pixiv 核心功能覆盖
- **浏览与发现**：
  - **推荐与关注**：日榜、周榜、月榜、新人榜、原创榜、R-18 榜等多维度榜单。
  - **动态瀑布流**：自适应列数瀑布流布局，智能加载预览缩略图。
  - **Spotlight 特典解析**：Pixivision 官方特辑图文解析与沉浸式阅读器。
- **作品与多媒体详情**：
  - 高清多图轮播浏览与缩放查看。
  - 动图（Ugoira）原生帧播放支持。
  - 关联作品智能推荐、作品系列（Series）导航、评论区互动与二级回复查看。
- **强大的搜索与屏蔽系统**：
  - 搜索建议、热门标签、按收藏数筛选、作者搜索、搜索历史管理。
  - 多重屏蔽引擎：标签屏蔽、作品 ID 屏蔽、画师 ID 屏蔽、AI 生成内容一键过滤。
- **下载与资源管理**：
  - 原图一键下载、后台任务队列调度、下载历史管理、自动同步至系统相册。
  - 剪贴板链接智能识别与跨应用一键分享。

---

## 📱 界面与功能预览

<div align="center">
<table>
  <tr>
    <td align="center"><b>发现与排行榜</b></td>
    <td align="center"><b>插画详情与作品浏览</b></td>
  </tr>
  <tr>
    <td><img src="./.github/preview/2.jpg" alt="Preview 1" width="360" /></td>
    <td><img src="./.github/preview/1.jpg" alt="Preview 2" width="360" /></td>
  </tr>
</table>
</div>

---

## 🏗️ 项目工程结构

```text
pixez-flutter-MIUIX/
├── compose-miuix/                     # Compose Multiplatform 新架构源码
│   ├── composeApp/                    # 各平台入口模块 (Android / Desktop)
│   │   ├── src/androidMain/           # Android 原生入口、Manifest、Adaptive 图标资源
│   │   └── src/desktopMain/           # Desktop JVM 平台入口与打包配置
│   └── shared/                        # 核心多平台共享逻辑模块
│       ├── src/commonMain/            # 跨平台通用逻辑
│       │   ├── composeResources/      # 共享矢量图标与图像资源
│       │   ├── sqldelight/            # SQLDelight 数据库架构定义文件
│       │   └── kotlin/com/perol/pixez/shared/
│       │       ├── data/              # 数据模型、SQLDelight 数据库与 Repository 层
│       │       ├── network/           # Ktor HTTP 客户端、OAuth 认证与直连引擎
│       │       ├── platform/          # 跨平台 Expect 声明 (相册、剪贴板、分享、返回键)
│       │       └── ui/
│       │           ├── animation/     # 液态手势阻尼与弹性物理动画
│       │           ├── components/    # MIUIX 风格通用组件 (液态底栏、卡片、更新弹窗等)
│       │           ├── effect/        # HyperOS 2 / 3 运行时 GLSL 着色器动态流光引擎
│       │           ├── navigation/    # Decompose 路由导航与页面栈管理
│       │           └── screens/       # 40+ 个完整业务屏幕与系统级设置页
│       ├── src/androidMain/           # Android 平台特异实现 (RuntimeShader、存储等)
│       ├── src/desktopMain/           # Desktop 平台特异实现 (Skia RuntimeEffect 等)
│       ├── src/iosMain/               # iOS 平台特异实现
│       └── src/macosMain/             # macOS 平台特异实现
└── .github/                           # GitHub Actions CI/CD 流水线与多语言文档
```

---

## 🛠️ 构建与开发指南

### 环境要求
- **Java Development Kit (JDK)**: JDK 17 及以上（推荐 JDK 21）
- **开发工具**: Android Studio Ladybug / Koala 或 IntelliJ IDEA Ultimate / Community
- **Android SDK**: `compileSdk = 36`, `minSdk = 24`

### 编译与运行命令

在终端中进入 `compose-miuix` 根目录：

```bash
cd compose-miuix

# 1. 编译并安装运行 Android Debug 版本至连接的设备/模拟器
./gradlew :composeApp:installDebug

# 2. 运行 Desktop (JVM) 桌面端应用
./gradlew :composeApp:run

# 3. 构建发布版 Android Release APK
./gradlew :composeApp:assembleRelease

# 4. 打包当前桌面平台原生可执行安装包 (MSI / DMG / DEB)
./gradlew :composeApp:packageDistributionForCurrentOS
```

---

## 👥 贡献者与鸣谢

感谢所有为 PixEz 及其 MIUIX 重构做出贡献的开发者与社区成员：

<table>
  <tr>
    <td align="center"><a href="https://github.com/Notsfsssf"><img src="https://avatars.githubusercontent.com/u/16934707?v=4" width="80px;" alt=""/><br /><sub><b>Perol_Notsfsssf</b></sub></a><br /><a href="https://github.com/137458/pixez-miuix/commits?author=Notsfsssf" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/137458"><img src="https://avatars.githubusercontent.com/u/104149371?v=4" width="80px;" alt=""/><br /><sub><b>Right now</b></sub></a><br /><a href="https://github.com/137458/pixez-miuix/commits?author=137458" title="Code">💻</a></td>
    <td align="center"><a href="https://xyx.moe"><img src="https://avatars.githubusercontent.com/u/9017470?v=4" width="80px;" alt=""/><br /><sub><b>Skimige</b></sub></a><br /><a href="https://github.com/137458/pixez-miuix/commits?author=Skimige" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/TragicLifeHu"><img src="https://avatars.githubusercontent.com/u/16817202?v=4" width="80px;" alt=""/><br /><sub><b>Tragic Life</b></sub></a><br /><a href="#translation-TragicLifeHu" title="Translation">🌍 (zh_TW)</a></td>
    <td align="center"><a href="http://ivtune.net"><img src="https://avatars.githubusercontent.com/u/54385201?v=4" width="80px;" alt=""/><br /><sub><b>karin722</b></sub></a><br /><a href="#translation-karin722" title="Translation">🌍 (ja)</a></td>
    <td align="center"><a href="http://archman.fun"><img src="https://avatars.githubusercontent.com/u/68731023?v=4" width="80px;" alt=""/><br /><sub><b>Romani-Archman</b></sub></a><br /><a href="#translation-Romani-Archman" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/itzXian"><img src="https://avatars.githubusercontent.com/u/34748039?v=4" width="80px;" alt=""/><br /><sub><b>Xian</b></sub></a><br /><a href="#translation-itzXian" title="Translation">🌍 (en_US)</a></td>
    <td align="center"><a href="https://github.com/ReikiAigawara"><img src="https://avatars.githubusercontent.com/u/66962815?v=4" width="80px;" alt=""/><br /><sub><b>Reiki Aigawara</b></sub></a><br /><a href="#translation-ReikiAigawara" title="Translation">🌍 (id_ID)</a></td>
  </tr>
</table>

特别鸣谢 [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) 社区提供的优秀 Compose MIUIX 组件库与设计灵感。

---

## 💬 社区与交流反馈

- 📘 **使用指南 & 常见问题**：[点此查看 FAQ 文档](.github/FAQ.md)
- 📮 **邮件反馈**：PxezFeedback@outlook.com
- ✈️ **Telegram 官方频道**：[@PixEzChannel](https://t.me/PixEzChannel)
- 🎮 **Discord 社区**：[@PixEz](https://discord.gg/Em9AeJbg)
- 🐧 **QQ 交流群**：815791942

---

## 📄 开源许可证

本项目遵循 [GPL-3.0 License](LICENSE) 开源许可证。
