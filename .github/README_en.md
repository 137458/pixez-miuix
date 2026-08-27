<div align="center">

<img src="../compose-miuix/shared/src/commonMain/composeResources/drawable/ic_pixez_logo.png" alt="PixEz Logo" width="128" height="128" />

# PixEz MIUIX

**A gorgeous, modern, and fluid cross-platform Pixiv client built with Compose Multiplatform and Xiaomi HyperOS (MIUIX) design language**

[![License](https://img.shields.io/badge/License-GPL--3.0-orange.svg?style=flat-square)](../LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Desktop%20%7C%20iOS%20%7C%20macOS-blue.svg?style=flat-square)](#-supported-platforms--architecture)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7f52ff.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-4285F4.svg?style=flat-square&logo=jetpackcompose)](https://www.jetbrains.com/compose-multiplatform/)
[![MIUIX](https://img.shields.io/badge/Design-Xiaomi%20HyperOS%20%2F%20MIUIX-ff6900.svg?style=flat-square)](https://github.com/compose-miuix-ui/miuix)

[中文 README](../README.md) · [Bahasa Indonesia](./README_id.md) · [FAQ & Guidelines](FAQ.md) · [Releases](https://github.com/137458/pixez-miuix/releases)

</div>

---

## 📖 Introduction

**PixEz MIUIX** is the next-generation rewrite of the well-known Pixiv third-party client PixEz. Built entirely on **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)** with deep integration of **Xiaomi HyperOS (MIUIX)** design paradigms, PixEz MIUIX delivers a native, lightweight, and fluid visual aesthetic across mobile and desktop platforms.

Direct connection from Mainland China is supported without requiring external proxy configurations.

---

## ✨ Features & Design Highlights

### 🎨 Xiaomi HyperOS (MIUIX) Aesthetics
- **HyperOS Visual Foundations**: Squircle geometry, hierarchical card containers (`Card`, `BasicComponent`), and full system dynamic color tokens.
- **IosLiquidGlass Floating Navigation Bar**:
  - Official 3-layer rendering pipeline: `CombinedBackdrop` frosted glass base, specular highlight reflection, and chromatic dispersion lens.
  - Physics-based damped drag animation (`DampedDragAnimation`) with inertial elastic snapping.
- **HyperOS 2 / HyperOS 3 Dynamic Shaders**:
  - Official GLSL runtime dynamic background shader (Android 13+ native `RuntimeShader` / Desktop Skia `RuntimeEffect`).
  - 60fps continuous color interpolation and Perlin noise flow.
- **Parallax Micro-Interactions**: Hero application logo dynamic scale-and-fade synced with list scroll offset.

### ⚡ Modern Multiplatform Architecture & Performance
- **Unified Cross-Platform Core**: Jetpack / JetBrains Compose Multiplatform shared across Android, Desktop (JVM), iOS, and macOS.
- **Decompose Navigation**: Complete lifecycle management, multi-stack routing, and hardware back-press dispatching.
- **Async Networking**: Ktor Client + Kotlinx.Serialization with auto OAuth token refresh and direct SNI proxy engine.
- **Robust Persistence**: SQLDelight type-safe databases maintaining backward compatibility with historical records.
- **Optimized Image Pipeline**: Coil 3 async multiplatform loader with multi-tier caching and anti-hotlinking headers.

### 🖼️ Pixiv Core Capabilities
- **Discovery & Rankings**: Daily, weekly, monthly, rookie, original, and R-18 rankings.
- **Adaptive Staggered Grid**: Fluid column layout with smart preview resolution matching.
- **Pixivision / Spotlight**: Full article parsing with rich inline reader mode.
- **Artwork Details**: High-resolution carousel viewer, animated GIF (Ugoira) frame rendering, comment tree, related works, and series navigation.
- **Advanced Filtering & Shielding**: Multi-rule muting (by Tag, Artwork ID, Artist ID, AI-generated content toggle).
- **Download & Task Manager**: One-click original saving, background download queue, automatic gallery indexing, and clipboard quick lookup.

---

## 📱 Screenshots & Preview

<div align="center">
<table>
  <tr>
    <td align="center"><b>Discovery & Rankings</b></td>
    <td align="center"><b>Artwork Detail View</b></td>
  </tr>
  <tr>
    <td><img src="../.github/preview/2.jpg" alt="Preview 1" width="360" /></td>
    <td><img src="../.github/preview/1.jpg" alt="Preview 2" width="360" /></td>
  </tr>
</table>
</div>

---

## 🛠️ Build & Development

### Prerequisites
- **JDK 17+** (JDK 21 recommended)
- **Android Studio** (Ladybug / Koala) or IntelliJ IDEA
- **Android SDK**: `compileSdk = 36`, `minSdk = 24`

### Build Commands

```bash
cd compose-miuix

# Run Android Debug build on connected device
./gradlew :composeApp:installDebug

# Launch Desktop (JVM) application
./gradlew :composeApp:run

# Build signed Android Release APK
./gradlew :composeApp:assembleRelease

# Package Desktop distribution (MSI / DMG / DEB)
./gradlew :composeApp:packageDistributionForCurrentOS
```

---

## 👥 Contributors

Heartfelt thanks to everyone who contributes to PixEz:

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

Special thanks to [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) for their amazing Compose UI library.

---

## 💬 Community & Feedback

- 📘 [FAQ & Guidelines (Chinese)](FAQ.md)
- 📮 Email: PxezFeedback@outlook.com
- ✈️ Telegram Channel: [@PixEzChannel](https://t.me/PixEzChannel)
- 🎮 Discord: [@PixEz](https://discord.gg/Em9AeJbg)
- 🐧 QQ Group: 815791942

---

## ⚠️ Disclaimer

1. **Unofficial Client**: PixEz MIUIX is an open-source third-party Pixiv client developed by independent developers and the open-source community for educational, research, and personal use. It is not affiliated with, endorsed by, or associated with pixiv Inc. in any way.
2. **Copyright & Intellectual Property**: All illustrations, comics, novels, animations, Spotlight articles, and related media accessible via this application belong to their respective creators and pixiv Inc. Any content accessed or downloaded through this app is strictly for personal study, appreciation, and non-commercial research. Commercial use or unauthorized redistribution is strictly prohibited.
3. **Network & Legal Compliance**: Direct connection features and technical capabilities within this application are provided solely for networking research and accessibility exploration. Users are responsible for complying with local laws, regulations, and Pixiv's Terms of Service. The developers and contributors assume no liability for any misuse or legal repercussions resulting from the use of this software.

---

## 📄 License

This project is licensed under the [GPL-3.0 License](../LICENSE).