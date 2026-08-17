<img src="../compose-miuix/shared/src/commonMain/composeResources/drawable/ic_pixez_logo.png" alt="logo" width="120" height="120" align="right" />

# PixEz MIUIX ![](https://img.shields.io/badge/license-GPL--3.0-orange.svg) ![](https://img.shields.io/badge/Platform-Compose%20Multiplatform-blue.svg) ![](https://img.shields.io/badge/Design-Xiaomi%20HyperOS%20%2F%20MIUIX-ff6900.svg)

[中文 README 点这里](../README.md)<br />
[README Bahasa Indonesia klik disini](./README_id.md)

> 🌟 **Next-Gen Architecture**: This project has been rebuilt using **Compose Multiplatform + MIUIX** (Xiaomi HyperOS design system & component library). The new codebase is located in [`compose-miuix/`](../compose-miuix).

A modern, fluid, and beautifully designed 3rd-party Pixiv cross-platform client targeting **Android / iOS / Desktop (JVM) / macOS**.

Direct access from Mainland China is supported without additional proxies.

---

## ✨ Features & Highlights

- 🎨 **Xiaomi HyperOS (MIUIX) Design Language**:
  - Squircle super-ellipse geometry and full MIUIX theme tokens for an authentic HyperOS experience.
  - **IosLiquidGlass Floating Navigation Bar**: Official 3-layer architecture (frosted glass backdrop, interactive specular highlights, chromatic dispersion lens, and damped physics drag-and-snap gesture).
  - **HyperOS 2 / HyperOS 3 Dynamic Shaders**: Official GLSL runtime shaders (`RuntimeShader` on Android 13+ / Skia `RuntimeEffect` on Desktop) and parallax Hero Logo animation.
- ⚡ **Modern Multiplatform Architecture**:
  - Kotlin Multiplatform + Compose Multiplatform foundation.
  - Ktor + Kotlinx.Serialization asynchronous networking with auto token refresh.
  - SQLDelight multiplatform local persistence.
  - Coil3 async image loading pipeline with anti-hotlinking support.
- 🖼️ **Complete Pixiv Core Capabilities**:
  - Illustrations, Manga, Novels, and animated GIF (Ugoira) support.
  - Daily, weekly, monthly, rookie, and R-18 ranking exploration.
  - Pixivision / Spotlight rich article parser and reader.
  - Adaptive staggered grid layout, smart tag filtering, mute list, and advanced search.
  - One-click original image saving, system gallery sync, sharing, and clipboard parsing.

---

## 🛠️ Build & Run

### Prerequisites
- **JDK 17+**
- **Android Studio** (Ladybug / Koala) or IntelliJ IDEA
- **Android SDK** (compileSdk 36, minSdk 24)

### Build Commands

Navigate into the `compose-miuix` folder:

```bash
cd compose-miuix

# Run Android Debug build on connected device / emulator
./gradlew :composeApp:installDebug

# Run Desktop (JVM) application
./gradlew :composeApp:run

# Build Android Release APK
./gradlew :composeApp:assembleRelease
```

---

## 👥 Contributors

Thanks to everyone contributing to PixEz:

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

---

## 💬 Community & Feedback

- [FAQ & Guidelines (Chinese)](FAQ.md)
- Feedback Email: PxezFeedback@outlook.com
- Telegram: [@PixEzChannel](https://t.me/PixEzChannel)
- Discord: [@PixEz](https://discord.gg/Em9AeJbg)
- QQ Group: 815791942

---

## 📄 License

This project is licensed under the [GPL-3.0 License](../LICENSE).