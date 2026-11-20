# PixEz MIUIX — Agent 约束入口

PixEz（Pixiv 第三方客户端）Kotlin Multiplatform + Compose Multiplatform + MIUIX 重制版。

## 工具链与平台矩阵

- **基线**：JDK 17、Gradle 8.14.4、Kotlin 2.4.10、Compose Multiplatform 1.12.0、MIUIX 0.9.4、Coil 3.6.3
- **平台支持**：Android（API 31+，COUI AAR 清单固定 minSdkVersion=31）、Windows Desktop（x64）；macOS 未启用（缺少 zoomable 变体）
- **CI 状态**：无远程 CI，所有验证与打包均在本地执行

## 构建与验证命令（唯一清单）

> 执行工作目录：`compose-miuix/`。交付验收标准：目标平台命令无错误且单元测试全绿。

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

Android 单测任务只做 JVM 单元测试（源集 `shared/src/androidUnitTest`、`composeApp/src/androidUnitTest`），不启动模拟器；任务名可用 `./gradlew :shared:tasks --all` 复核，本轮实测为 `:shared:testDebugUnitTest` 与 `:composeApp:testDebugUnitTest`。

**发布前全量验证（单行）：**
```bash
./gradlew :shared:compileKotlinDesktop :shared:desktopTest :shared:testDebugUnitTest :composeApp:compileKotlinDesktop :composeApp:desktopTest :composeApp:testDebugUnitTest :composeApp:compileDebugKotlinAndroid :composeApp:packageWindowsSingleFileExe
```

**已知限制（C7 · SQLDelight 迁移校验在 Windows 上恒为 SKIPPED）：**

SQLDelight 生成的 `verify<SourceSet><Db>Migration` 任务（聚合名 `verifySqlDelightMigration`）在 `shared/build.gradle.kts` 中带 `onlyIf { !skip && !isWindows }`：Windows 上执行 `./gradlew :shared:verifyCommonMainTaskDatabaseMigration` 只输出 `> Task :shared:verifyCommonMainTaskDatabaseMigration SKIPPED` 与 `BUILD SUCCESSFUL`，等于零校验，不能作为 `.sqm` 与 schema 一致的证据。放开该条件强制运行必然失败，且与迁移内容无关：sqlite-jdbc 需把原生库释放到不可写目录，先报 `java.nio.file.AccessDeniedException: C:\WINDOWS\sqlite-3.45.2.0-<hash>-sqlitejdbc.dll.lck`，回退 `System.loadLibrary` 后再报 `java.lang.UnsatisfiedLinkError: no sqlitejdbc in java.library.path`。已排除（三种手段复现同一错误，故未改动构建脚本）：直接强制运行；`--no-daemon` 配显式 `TMP`/`TEMP`；`-Dorg.sqlite.tmpdir=<可写目录>`。结论：迁移校验只能在 Linux 或 WSL 环境补跑（本仓库无远程 CI），Windows 宿主以 `:shared:desktopTest`、`:shared:testDebugUnitTest` 覆盖 SQLDelight 相关逻辑。

## 核心开发红线

1. **组件约束**：严格采用 `top.yukonga.miuix.kmp`，严禁引入任何 Material 3（`androidx.compose.material3.*`）控件。
2. **多语言与常量**：UI 文本必须接入 `LocalStrings.current`（13 国语言，含波斯语 `fa` 与 RTL）。常量按层落位：网络/主机/引擎常量在 `network/`（`PixivHosts`、`HttpEngineDefaults`、`GitHubReleaseEndpoints`），凭据常量在 `platform/TokenCipherDefaults`，下载平台常量在 `platform/DownloadPlatformDefaults`；`ui/AppConstants.kt` 只保留 UI 常量与布局断点（其余为待清理的过渡委讬）。
3. **范围边界**：`archive/flutter-v1/` 为只读归档代码，禁止非明确要求的修改；测试先行，不可篡改既有测试断言。

## 文档索引

| 用途 | 目标文档 | 核心职责 |
| :--- | :--- | :--- |
| 构建验证 | `AGENTS.md` | 命令唯一清单与 Agent 核心约束入口 |
| UI 与交互 | `MIUIX_Spec.md` | 组件选型、Liquid Glass、响应式断点、多语言规范 |
| 架构与调用 | `Code_Wiki.md` | 分层架构、数据流向、核心 API、持久化与网络引擎 |
| 路线追踪 | `Global_TODO.md` | 优先级任务清单、架构决策结论、历史落地归档 |
| 跨会话状态 | `CONTEXT.md` | 本地会话记忆、系统安全不变量、环境踩坑与构建调优 |
| 发版规范 | `RELEASE_NOTES_GUIDE.md` | CHANGELOG 维护准则、发版流程与 Release 模板 |

> `MIUIX_Spec.md`、`Code_Wiki.md`、`Global_TODO.md`、`CONTEXT.md`、`RELEASE_NOTES_GUIDE.md`、`release-notes-*.md` 与 `release/` 目录均由 `.gitignore` 忽略，只存在于本地工作区、不随仓库分发；改动这些文档不会进入提交，修改后需本地留存。仓库根目录实际入库的文档只有本文件、`README.md`、`CHANGELOG.md`、`GEMINI.md`。
