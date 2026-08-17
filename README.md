<img src="./compose-miuix/shared/src/commonMain/composeResources/drawable/ic_pixez_logo.png" alt="logo" width="120" height="120" align="right" />

# PixEz MIUIX ![](https://img.shields.io/badge/license-GPL--3.0-orange.svg) ![](https://img.shields.io/badge/Platform-Compose%20Multiplatform-blue.svg) ![](https://img.shields.io/badge/Design-Xiaomi%20HyperOS%20%2F%20MIUIX-ff6900.svg)

[English README Here](./.github/README_en.md)<br />
[README Bahasa Indonesia klik disini](./.github/README_id.md)

> 🌟 **新世代重构**：本项目全面采用 **Compose Multiplatform + MIUIX**（Xiaomi HyperOS 设计风格与组件库）进行架构重构。新代码位于 [`compose-miuix/`](./compose-miuix)。

使用 Compose Multiplatform 编写的高颜值、现代化的 pixiv 第三方跨平台客户端，目标平台为 **Android / iOS / Desktop (JVM) / macOS**。

同样支持中国大陆地区免代理直连。

---

## ✨ 特性与亮点

- 🎨 **Xiaomi HyperOS (MIUIX) 设计规范**：
  - 全套 Squircle 超椭圆曲线几何与 MIUIX 主题系统，细腻还原 HyperOS 交互风格。
  - **IosLiquidGlass 悬浮液态玻璃底栏**：官方 3 层渲染架构（毛玻璃基底、动态高光、色散折射透镜、物理阻尼拖拽回弹手势）。
  - **HyperOS 2 / HyperOS 3 动态流光更新界面**：官方 GLSL 运行时着色器（`RuntimeShader` / Skia `RuntimeEffect`）流动光效与视差 Hero Logo。
- ⚡ **Compose Multiplatform 现代化架构**：
  - 采用 Kotlin Multiplatform + Compose Multiplatform 跨平台技术栈。
  - Ktor + Kotlinx.Serialization 打造高性能异步网络层，支持 Token 自动续期。
  - SQLDelight 本地持久化，完美兼容历史数据。
  - Coil3 多平台异步图片加载管线与智能防盗链处理。
- 🖼️ **Pixiv 核心功能完整覆盖**：
  - 插画 / 动图 (Ugoira) / 漫画 / 小说浏览与详情展示。
  - 日榜、周榜、月榜、新人榜、R18 等多榜单自由切换。
  - Pixivision / Spotlight 特典文章图文并茂解析。
  - 瀑布流自适应布局、智能标签筛选、屏蔽名单与高级搜索。
  - 一键原图下载、系统相册同步、一键分享与剪贴板快捷解析。

---

## 📱 界面预览

| 发现与排行榜 | 插画详情 |
|:---:|:---:|
| ![Preview](./.github/preview/2.jpg) | ![Preview](./.github/preview/1.jpg) |

---

## 🛠️ 构建与运行

### 环境要求
- **JDK 17+**
- **Android Studio** Ladybug / Koala 或 IntelliJ IDEA
- **Android SDK** (compileSdk 36, minSdk 24)

### 编译运行 (Compose Multiplatform)

进入 `compose-miuix` 项目目录：

```bash
cd compose-miuix

# 运行 Android 端 Debug
./gradlew :composeApp:installDebug

# 运行 Desktop (JVM) 端
./gradlew :composeApp:run

# 构建 Android Release APK
./gradlew :composeApp:assembleRelease
```

---

## 👥 贡献 / Contribute

感谢每一位参与本项目建设的开发者与贡献者：

<table>
  <tr>
    <td align="center"><a href="https://github.com/Notsfsssf"><img src="https://avatars.githubusercontent.com/u/16934707?v=4" width="80px;" alt=""/><br /><sub><b>Perol_Notsfsssf</b></sub></a><br /><a href="https://github.com/137458/pixez-miuix/commits?author=Notsfsssf" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/137458"><img src="https://avatars.githubusercontent.com/u/104149371?v=4" width="80px;" alt=""/><br /><sub><b>Right now</b></sub></a><br /><a href="https://github.com/137458/pixez-miuix/commits?author=137458" title="Code">💻</a></td>
    <td align="center"><a href="https://xyx.moe"><img src="https://avatars.githubusercontent.com/u/9017470?v=4" width="80px;" alt=""/><br /><sub><b>Skimige</b></sub></a><br /><a href="https://github.com/137458/pixez-miuix/commits?author=Skimige" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/TragicLifeHu"><img src="https://avatars.githubusercontent.com/u/16817202?v=4" width="80px;" alt=""/><br /><sub><b>Tragic Life</b></sub></a><br /><a href="#translation-TragicLifeHu" title="Translation">🌍 (zh_TW)</a></td>
    <td align="center"><a href="http://ivtune.net"><img src="https://avatars.githubusercontent.com/u/54385201?v=4" width="80px;" alt=""/><br /><sub><b>karin722</b></sub></a><br /><a href="#translation-karin722" title="Translation">🌍 (ja)</a></td>
    <td align="center"><a href="http://archman.fun"><img src="https://avatars.githubusercontent.com/u/68731023?v=4" width="80px;" alt=""/><br /><sub><b>Romani-Archman</b></sub></a><br /><a href="#translation-Romani-Archman" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/itzXian"><img src="https://avatars.githubusercontent.com/u/34748039?v=4" width="80px;" alt=""/><br /><sub><b>Xian</b></sub></a><br /><a href="#translation-itzXian" title="Translation">🌍 (en_US)</a></td>
  </tr>
</table>

如果你愿意为本项目贡献代码、优化设计或完善多语言翻译，欢迎随时提交 Pull Request！

---

## 💬 交流与反馈

- [使用指南 & 常见问题](.github/FAQ.md)
- 邮件反馈：PxezFeedback@outlook.com
- Telegram 交流频道：[@PixEzChannel](https://t.me/PixEzChannel)
- Discord：[@PixEz](https://discord.gg/Em9AeJbg)
- 企鹅交流群：815791942

---

## 📄 开源许可证

本项目基于 [GPL-3.0 License](LICENSE) 开源。
