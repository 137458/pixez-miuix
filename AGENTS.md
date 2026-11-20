# PixEz MIUIX — Agent 约束入口

PixEz（Pixiv 第三方客户端）Kotlin Multiplatform + Compose Multiplatform + MIUIX 重制版。

## 工具链与平台矩阵

- **基线**：JDK 17、Gradle 8.14.4、Kotlin 2.4.10、AGP 8.13.0
- **应用栈**：Compose Multiplatform 1.12.0、MIUIX 0.9.4、COUI 0.0.1-alpha12（仅 Android）、Coil 3.6.3、SQLDelight 2.0.2
- **平台支持**：Android（API 31+，COUI AAR 清单固定 `minSdkVersion=31`）、Windows Desktop（x64）；macOS 未启用（缺少 `zoomable` 变体）
- **SDK 与版本**：`compileSdk 36`、`targetSdk 35`；`versionCode` 为 `10010097`，`versionName` 为 `0.9.109.7-miuix`，与 `AppInfo.kt` 的 `VERSION_NAME` 由 `AppInfoVersionParityTest` 约束一致
- **CI 状态**：无远程 CI，所有验证与打包均在本地执行

## 构建与验证命令（唯一清单）

> 执行工作目录：`compose-miuix/`。交付验收标准：目标平台命令无错误，单元测试全绿。

| 目标 | 执行命令 |
| :--- | :--- |
| Desktop 共享模块编译 | `./gradlew :shared:compileKotlinDesktop` |
| Desktop 应用编译 | `./gradlew :composeApp:compileKotlinDesktop` |
| Desktop 双端单元测试 | `./gradlew :shared:desktopTest :composeApp:desktopTest` |
| Android 共享模块单元测试 | `./gradlew :shared:testDebugUnitTest` |
| Android 应用单元测试 | `./gradlew :composeApp:testDebugUnitTest` |
| Android Debug 编译 | `./gradlew :composeApp:compileDebugKotlinAndroid` |
| Android Debug APK | `./gradlew :composeApp:assembleDebug` |
| Windows 单文件 EXE | `./gradlew :composeApp:packageWindowsSingleFileExe` |
| 桌面端运行 | `./gradlew :composeApp:run` |

Android 单测只跑 JVM 单元测试，源集为 `shared/src/androidUnitTest` 与 `composeApp/src/androidUnitTest`，不启动模拟器。任务名为 `:shared:testDebugUnitTest` 与 `:composeApp:testDebugUnitTest`，可用 `./gradlew :shared:tasks --all` 复核。

**发布前全量验证（单行）：**
```bash
./gradlew :shared:compileKotlinDesktop :shared:desktopTest :shared:testDebugUnitTest :composeApp:compileKotlinDesktop :composeApp:desktopTest :composeApp:testDebugUnitTest :composeApp:compileDebugKotlinAndroid :composeApp:packageWindowsSingleFileExe
```

**已知限制（SQLDelight 迁移校验在 Windows 上恒为 SKIPPED）**

- **生效条件**：任务为 `verify<SourceSet><Db>Migration`，聚合名为 `verifySqlDelightMigration`。`shared/build.gradle.kts` 的 `onlyIf` 在 `-PskipVerifyMigrations` 为真、或 `os.name` 含 `win` 时跳过。
- **Windows 行为**：`./gradlew :shared:verifyCommonMainTaskDatabaseMigration` 只输出 `> Task :shared:verifyCommonMainTaskDatabaseMigration SKIPPED` 与 `BUILD SUCCESSFUL`。零校验，不能作为 `.sqm` 与 schema 一致的证据。
- **强制运行必失败**：sqlite-jdbc 需把原生库释放到不可写目录。先报 `java.nio.file.AccessDeniedException: C:\WINDOWS\sqlite-3.45.2.0-<hash>-sqlitejdbc.dll.lck`；回退 `System.loadLibrary` 后再报 `java.lang.UnsatisfiedLinkError: no sqlitejdbc in java.library.path`。直接强制、`--no-daemon` 配显式 `TMP`/`TEMP`、`-Dorg.sqlite.tmpdir=<可写目录>` 三种手段复现同一错误，与迁移内容无关，故未改构建脚本。
- **替代覆盖**：迁移校验只能在 Linux 或 WSL 补跑；Windows 宿主以 `:shared:desktopTest` 与 `:shared:testDebugUnitTest` 覆盖 SQLDelight 逻辑。

## 核心开发红线

1. **组件约束**：只用 `top.yukonga.miuix.kmp`，禁止引入 Material 3（`androidx.compose.material3.*`）控件。
2. **多语言与常量**：UI 文本必须接入 `LocalStrings.current`，共 13 种语言，含波斯语 `fa` 与 RTL 排版。常量按层落位：网络与主机常量在 `network/`（`PixivHosts`、`HttpEngineDefaults`、`GitHubReleaseEndpoints`）；凭据常量在 `platform/TokenCipherDefaults`；下载平台常量在 `platform/DownloadPlatformDefaults`。`ui/AppConstants.kt` 只放 UI 常量与布局断点，URL 前缀引自上述常量，不另写字面量。
3. **范围边界**：`archive/flutter-v1/` 为只读归档代码，非明确要求不得修改；测试先行，不可篡改既有测试断言。

## 文档索引

| 类别 | 文档 | 职责 |
| :--- | :--- | :--- |
| 入库 | `AGENTS.md` | 命令唯一清单与 Agent 核心约束入口 |
| 入库 | `README.md` | 项目简介、核心特性、下载安装与源码构建 |
| 入库 | `GEMINI.md` | 指向 `AGENTS.md` 的桩文件 |
| 入库 | `CHANGELOG.md` | 版本变更记录 |
| 入库 | `LICENSE` | GPL-3.0 许可全文 |
| 入库 | `.github/FAQ.md` | 使用指南与常见问题 |
| 入库 | `.github/README_en.md` | 英文 README |
| 入库 | `.github/README_id.md` | 印尼语 README |
| 本地留存 | `MIUIX_Spec.md` | 组件选型、Liquid Glass、响应式断点、多语言规范 |
| 本地留存 | `Code_Wiki.md` | 分层架构、数据流向、核心 API、持久化与网络引擎 |
| 本地留存 | `Global_TODO.md` | 优先级任务清单、架构决策结论、历史落地归档 |
| 本地留存 | `CONTEXT.md` | 本地会话记忆、系统安全不变量、环境踩坑与构建调优 |
| 本地留存 | `RELEASE_NOTES_GUIDE.md` | CHANGELOG 维护准则、发版流程与 Release 模板 |

> 本地留存项被 `.gitignore` 忽略：`Code_Wiki.md`、`CONTEXT.md`、`*Spec.md`、`*TODO.md`、`RELEASE_NOTES_GUIDE.md`、`release-notes-*.md`、`release/`、`docs/`、`compose-miuix/docs/`、`.agents/`。这些文档只在本机生效，改动不进提交，需自行备份。

