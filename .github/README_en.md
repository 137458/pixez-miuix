<div align="center">

<img src="../compose-miuix/shared/src/commonMain/composeResources/drawable/ic_pixez_logo.png" alt="PixEz Logo" width="128" height="128" />

# PixEz MIUIX

**A third-party Pixiv client built with Kotlin Multiplatform and Compose Multiplatform, following the Xiaomi HyperOS (MIUIX) design language**

[![License](https://img.shields.io/badge/License-GPL--3.0-orange.svg?style=flat-square)](../LICENSE)
[![Active Releases](https://img.shields.io/badge/Active%20Releases-Android%20%7C%20Windows%20Desktop-blue.svg?style=flat-square)](#download--installation)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7f52ff.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.12.0-4285F4.svg?style=flat-square&logo=jetpackcompose)](https://www.jetbrains.com/compose-multiplatform/)
[![MIUIX](https://img.shields.io/badge/Design-Xiaomi%20HyperOS%20%2F%20MIUIX-ff6900.svg?style=flat-square)](https://github.com/compose-miuix-ui/miuix)

[中文 README](../README.md) · [Bahasa Indonesia](./README_id.md) · [FAQ & Guidelines](FAQ.md) · [Releases](https://github.com/137458/pixez-miuix/releases)

</div>

---

## Introduction

**PixEz MIUIX** is a rewrite of PixEz, a third-party Pixiv client. It uses **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)**. The UI is built on the MIUIX component library (`top.yukonga.miuix.kmp`).

Active targets are Android and Windows Desktop. The macOS target is not enabled.

The app reaches Pixiv from Mainland China without a proxy. Host resolution uses DNS-over-HTTPS and falls back to system DNS; an image mirror is optional. The ECH and compatibility tiers are decided by the underlying TLS negotiation, and the app does not modify SNI.

---

## Features

### MIUIX Design

- The UI is built on the MIUIX component library; the main containers are `Card` and `BasicComponent`.
- The floating bottom bar renders Liquid Glass refraction with background blur, and drops to blur when `RuntimeShader` is unavailable.
- Shader effects use the system `RuntimeShader` on Android 13 (API 33) and later, and Skia `RuntimeEffect` on desktop.
- The bottom bar carries five tabs: Recommend, Search, Ranking, Feed, and Spotlight.
- Tablets and wide screens switch to a multi-column staggered grid. The floating bottom bar caps at 540 dp and stays centered; the content column caps at 760 dp.

### Multiplatform Architecture

- `shared/src/commonMain` holds 304 Kotlin files, including 44 `*Screen.kt` and 18 repository classes under `data/repository/`.
- Decompose drives routing and navigation.
- Ktor Client handles networking: OAuth2 PKCE login, automatic token refresh with a retry of the original request, and multi-account switching.
- SQLDelight stores local data; Coil 3 loads images.
- Network: DNS-over-HTTPS resolution with a system DNS fallback and a five-minute circuit breaker. The image source can be the default host, a mirror host, or a custom host.
- `TrustedUrlPolicy` validates API, image, Pixivision, and release-asset URLs against allowed hosts before any request.

### Pixiv Core Capabilities

- Pull-to-refresh and load-more on home recommendations, the following feed, search, rankings, artist pages, series pages, comments, and the friends list.
- Rankings cover 12 modes, including daily, weekly, monthly, male, female, original, rookie, AI, and R-18 variants.
- Multi-page browsing for illustrations and manga; Ugoira playback frame by frame.
- Novels: in-app search, detail pages, and a reader with rich-text rendering (embedded illustrations, chapter markers, and page links); bookmarks, reading history, and comments.
- The following feed includes a manga series watchlist; a series can be watched or unwatched from its page in one tap.
- Pixivision articles; multi-level comment replies.
- Search by image: pick a local file and find similar works through SauceNAO.
- Content filtering: tag, artist, and work muting; NSFW masking; AI-generated work filtering.
- Bookmark and download automation: auto-save on bookmark, auto-bookmark on save, auto-tag on bookmark, auto-follow the artist on bookmark.
- Downloads: original images and per-page originals. Downloads and Ugoira archives share a 256 MB per-task cap; a task that exceeds the cap aborts and removes its temporary files. Queue concurrency runs from 1 to 6, default 2, and failed tasks can be retried. On Android, images go to the system gallery under `Pictures/PixEz`, and Ugoira archives under `Downloads/PixEz`.

---

## Screenshots

<div align="center">
<table>
  <tr>
    <td align="center"><b>Discovery & Rankings</b></td>
    <td align="center"><b>Artwork Detail View</b></td>
  </tr>
  <tr>
    <td><img src="./preview/2.jpg" alt="Discovery and rankings" width="360" /></td>
    <td><img src="./preview/1.jpg" alt="Artwork detail view" width="360" /></td>
  </tr>
</table>
</div>

---

## Download & Installation

Download the package for your platform from [GitHub Releases](https://github.com/137458/pixez-miuix/releases). The current release is `0.9.109.7-miuix`.

### Android

Install `PixEz-MIUIX-v*.apk` on the device. Android 12 (API 31) or later is required; phones and tablets are supported.

### Windows Desktop (no Java runtime required)

- **Single-file standalone**: `PixEz-Standalone.exe` embeds the runtime and starts on a double-click, with no extraction step.
- **Portable**: unzip `PixEz-windows-x64-portable.zip`, then run `PixEz.exe`.
- **MSI installer**: run `PixEz-*.msi`; it creates desktop and Start-menu shortcuts.

### Other desktop targets

Linux builds from source on a Linux host and can produce a Deb package. The macOS target is not enabled because the `zoomable` dependency ships no matching variant. Neither platform has official release packages.

---

## Build & Development

### Prerequisites

| Item | Value |
| :--- | :--- |
| JDK | 17 |
| Gradle | 8.14.4, through the Wrapper |
| Android Gradle Plugin | 8.13.0 |
| Android SDK | `compileSdk = 36`, `targetSdk = 35`, `minSdk = 31` |
| Library versions | `compose-miuix/gradle/libs.versions.toml` (Kotlin 2.4.10, Compose Multiplatform 1.12.0, MIUIX 0.9.4) |

Windows single-file EXE packaging also needs `csc.exe` from the .NET Framework 4.0.

### Build Commands

Run the commands from `compose-miuix/`. `AGENTS.md` is the single source of truth for the full verification checklist.

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

The Android Release APK is built with `./gradlew :composeApp:assembleRelease`. Without `compose-miuix/keystore.properties`, a release build falls back to debug signing and the version name gains a `-debug-signed` suffix; such artifacts must not be published.

---

## Contributors

Thanks to everyone who contributed to PixEz and to this MIUIX rewrite:

<table>
  <tr>
    <td align="center"><a href="https://github.com/137458"><img src="https://avatars.githubusercontent.com/u/138956834?v=4" width="100px;" alt=""/><br /><sub><b>137458 (Rosemary)</b></sub></a><br /><a href="https://github.com/137458/pixez-miuix/commits?author=137458" title="Code & UI">Code &amp; UI</a></td>
    <td align="center"><a href="https://github.com/Notsfsssf"><img src="https://avatars.githubusercontent.com/u/16934707?v=4" width="100px;" alt=""/><br /><sub><b>Perol_Notsfsssf</b></sub></a><br /><a href="https://github.com/Notsfsssf/pixez-flutter/commits?author=Notsfsssf" title="Code">Code</a></td>
    <td align="center"><a href="https://xyx.moe"><img src="https://avatars.githubusercontent.com/u/9017470?v=4" width="100px;" alt=""/><br /><sub><b>Skimige</b></sub></a><br /><a href="https://github.com/Skimige" title="Documentation">Docs</a></td>
    <td align="center"><a href="https://github.com/TragicLifeHu"><img src="https://avatars.githubusercontent.com/u/16817202?v=4" width="100px;" alt=""/><br /><sub><b>Tragic Life</b></sub></a><br /><a href="#translation-TragicLifeHu" title="Translation">Translation (zh_TW)</a></td>
    <td align="center"><a href="http://ivtune.net"><img src="https://avatars.githubusercontent.com/u/54385201?v=4" width="100px;" alt=""/><br /><sub><b>karin722</b></sub></a><br /><a href="#translation-karin722" title="Translation">Translation (ja)</a></td>
    <td align="center"><a href="http://archman.fun"><img src="https://avatars.githubusercontent.com/u/68731023?v=4" width="100px;" alt=""/><br /><sub><b>Romani-Archman</b></sub></a><br /><a href="#documentation-Romani-Archman" title="Documentation">Docs</a></td>
    <td align="center"><a href="https://github.com/itzXian"><img src="https://avatars.githubusercontent.com/u/34748039?v=4" width="100px;" alt=""/><br /><sub><b>Xian</b></sub></a><br /><a href="#translation-itzXian" title="Translation">Translation (en_US)</a></td>
    <td align="center"><a href="https://github.com/ReikiAigawara"><img src="https://avatars.githubusercontent.com/u/66962815?v=4" width="100px;" alt=""/><br /><sub><b>Reiki Aigawara</b></sub></a><br /><a href="#translation-ReikiAigawara" title="Translation">Translation (id_ID)</a></td>
  </tr>
</table>

Contributions to code, UI, and translations are welcome through Pull Requests.

Special thanks to [pixez-flutter](https://github.com/Notsfsssf/pixez-flutter), the original Flutter PixEz client.
Special thanks to [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix), the Compose UI library in Xiaomi HyperOS style.

---

## Community & Feedback

- User guide and FAQ: [FAQ & Guidelines (Chinese)](FAQ.md)
- Email feedback: [PxezFeedBack@outlook.com](mailto:PxezFeedBack@outlook.com)

---

## Disclaimer

1. **Unofficial client**: PixEz MIUIX is an open-source third-party Pixiv client. Independent developers and the open-source community build it for study, research, and personal use. It is not affiliated with, endorsed by, or associated with pixiv Inc.
2. **Copyright and intellectual property**: All illustrations, manga, novels, Ugoira animations, Pixivision articles, and related media belong to their creators and pixiv Inc. Content accessed or downloaded through the app is for personal, non-commercial appreciation only. Commercial use and unauthorized redistribution are prohibited.
3. **Network and legal compliance**: Network features exist for connectivity research only. Users are responsible for complying with local law and with Pixiv's Terms of Service. The developers and contributors accept no liability for misuse or its consequences.

---

## License

This project is licensed under the [GPL-3.0 License](../LICENSE).

---
