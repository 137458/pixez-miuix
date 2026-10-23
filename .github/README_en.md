<div align="center">

<img src="../compose-miuix/shared/src/commonMain/composeResources/drawable/ic_pixez_logo.png" alt="PixEz Logo" width="128" height="128" />

# PixEz MIUIX

**A third-party Pixiv client built with Kotlin Multiplatform and Compose Multiplatform, using the Xiaomi HyperOS (MIUIX) design specification**

[![License](https://img.shields.io/badge/License-GPL--3.0-orange.svg?style=flat-square)](../LICENSE)
[![Active Releases](https://img.shields.io/badge/Active%20Releases-Android%20%7C%20Windows%20Desktop-blue.svg?style=flat-square)](#download--installation)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7f52ff.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.12.0-4285F4.svg?style=flat-square&logo=jetpackcompose)](https://www.jetbrains.com/compose-multiplatform/)
[![MIUIX](https://img.shields.io/badge/Design-Xiaomi%20HyperOS%20%2F%20MIUIX-ff6900.svg?style=flat-square)](https://github.com/compose-miuix-ui/miuix)

[中文 README](../README.md) · [Bahasa Indonesia](./README_id.md) · [FAQ & Guidelines](FAQ.md) · [Releases](https://github.com/137458/pixez-miuix/releases)

</div>

---

## Introduction

**PixEz MIUIX** is a rewrite of PixEz, a third-party Pixiv client. The project is built on **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)**, with the UI based on the MIUIX component library (`top.yukonga.miuix.kmp`).

The current release platforms are Android and Windows Desktop. iOS is compile-verified only, and the macOS target is not enabled.

The app connects to Pixiv directly from Mainland China without a proxy. It includes SNI direct connection, DoH secure DNS, and an optional image mirror.

---

## Features

### MIUIX Design
- The UI is built on the MIUIX component library; the primary containers are `Card` and `BasicComponent`.
- The floating bottom bar supports Liquid Glass refraction and background blur rendering, and falls back to blur automatically when `RuntimeShader` is unsupported.
- Shader effects use the system `RuntimeShader` on Android 13 (API 33) and later, and Skia `RuntimeEffect` on desktop.
- The bottom bar provides five tabs: Recommend, Search, Ranking, Feed, and Spotlight.
- Tablets and wide screens enable a multi-column staggered grid. The floating bottom bar is width-capped and centered (max 540 dp); the content area is capped at 760 dp.

### Multiplatform Architecture
- `shared/src/commonMain` contains 19 `*Repository.kt` and 42 `*Screen.kt`.
- Decompose for routing and navigation.
- Ktor Client for networking: OAuth2 PKCE login, automatic token refresh with a retry of the original request, and multi-account switching.
- SQLDelight for the local database; Coil 3 for image loading.
- Direct connection from Mainland China: SNI direct mode, DoH secure DNS with fallback to system DNS after a resolution failure, and an optional image mirror.
- `TrustedUrlPolicy` restricts external links to trusted domains.

### Pixiv Core Capabilities
- Pull-to-refresh and load-more on the home recommendations, following feed, search, rankings, artist pages, series works, comment sections, and the friends list.
- Rankings: daily, weekly, monthly, rookie, original, R-18, and AI.
- Multi-page browsing for illustrations and manga; Ugoira playback frame by frame.
- Pixivision article browsing; multi-level comment replies.
- Search by image: pick a local image and find similar works through SauceNAO.
- Content filtering: tag muting, artist and work muting, NSFW masking, and AI-generated content filtering.
- Bookmark and download automation: auto-save on bookmark, auto-bookmark on save, auto-tag on bookmark, and auto-follow the artist on bookmark.
- Downloads: original images and per-page originals. Downloads and Ugoira archives share a 256 MB per-task limit; tasks that exceed it are aborted and temporary files are cleaned. Queue concurrency is adjustable from 1 to 6 (default 2). Failed tasks can be retried. On Android, images can be saved to the system gallery (`Pictures/PixEz` and `Downloads/PixEz`).

---

## Screenshots

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

## Download & Installation

Download the package for your platform from [GitHub Releases](https://github.com/137458/pixez-miuix/releases).

### Android

Install `PixEz-MIUIX-v*.apk` directly on the device. Requires Android 7.0 (API 24) or later; works on phones and tablets.

### Windows Desktop (no Java runtime required)

- **Single-file standalone**: `PixEz-Standalone.exe` embeds the runtime and runs directly, no extraction needed.
- **Portable**: extract `PixEz-windows-x64-portable.zip` and run `PixEz.exe`.
- **MSI installer**: run `PixEz-*.msi`; it creates desktop and Start-menu shortcuts.

### Other desktop targets

Linux can be built from source; packaging includes Deb. The macOS target is not enabled because a dependency lacks the corresponding variant. Neither platform has official release packages.

---

## Build & Development

### Prerequisites
- JDK 17
- Android SDK: `compileSdk = 36`, `minSdk = 24`
- Build through the Gradle Wrapper (Gradle 8.14.4)
- Windows single-file EXE packaging depends on the .NET Framework `csc.exe`

### Build Commands

Commands run from `compose-miuix/`. `AGENTS.md` is the single source of truth for the full verification checklist.

```bash
# Android Debug compile check
./gradlew :composeApp:compileDebugKotlinAndroid

# Build the Android Debug APK
./gradlew :composeApp:assembleDebug

# Compile the Desktop shared module
./gradlew :shared:compileKotlinDesktop

# Run the desktop app
./gradlew :composeApp:run

# Run unit tests on both platforms
./gradlew :shared:desktopTest :composeApp:desktopTest

# Package the Windows single-file EXE
./gradlew :composeApp:packageWindowsSingleFileExe
```

The Android Release APK is built with `./gradlew :composeApp:assembleRelease`.

---

## Contributors

Thanks to all developers and community members who contributed to PixEz and this MIUIX rewrite:

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

Contributions to code, UI, and translations are welcome through Pull Requests.

Special thanks to [pixez-flutter](https://github.com/Notsfsssf/pixez-flutter), the original Flutter PixEz client.
Special thanks to [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix), the Compose UI library in Xiaomi HyperOS style.

---

## Community & Feedback

- User guide and FAQ: [FAQ & Guidelines (Chinese)](FAQ.md)
- Email feedback: [gaaf14738@gmail.com](mailto:gaaf14738@gmail.com)

---

## Disclaimer

1. **Unofficial Client**: PixEz MIUIX is an open-source third-party Pixiv client developed by independent developers and the open-source community for educational, research, and personal use. It is not affiliated with, endorsed by, or associated with pixiv Inc. in any way.
2. **Copyright & Intellectual Property**: All illustrations, comics, novels, animations, Spotlight articles, and related media accessible via this application belong to their respective creators and pixiv Inc. Any content accessed or downloaded through this app is strictly for personal study, appreciation, and non-commercial research. Commercial use or unauthorized redistribution is strictly prohibited.
3. **Network & Legal Compliance**: Direct connection features and technical capabilities within this application are provided solely for networking research and accessibility exploration. Users are responsible for complying with local laws, regulations, and Pixiv's Terms of Service. The developers and contributors assume no liability for any misuse or legal repercussions resulting from the use of this software.

---

## License

This project is licensed under the [GPL-3.0 License](../LICENSE).
