# PixEz MIUIX — Agent 约束入口

PixEz（Pixiv 第三方客户端）Kotlin Multiplatform + Compose Multiplatform + MIUIX 重制版。

## 工具链与平台矩阵

- **基线**：JDK 17、Gradle 8.14.4、Kotlin 2.4.10、Compose Multiplatform 1.12.0、MIUIX 0.9.4、Coil 3.6.3
- **平台支持**：Android（API 24+）、Windows Desktop（x64）；iOS（仅 macOS 编译验证）；macOS 未启用（缺少 zoomable 变体）
- **CI 状态**：无远程 CI，所有验证与打包均在本地执行

## 构建与验证命令（唯一清单）

> 执行工作目录：`compose-miuix/`。交付验收标准：目标平台命令无错误且单元测试全绿。

| 目标 | 执行命令 |
| :--- | :--- |
| Desktop 共享模块编译 | `./gradlew :shared:compileKotlinDesktop` |
| Desktop 应用编译 | `./gradlew :composeApp:compileKotlinDesktop` |
| 双端单元测试 | `./gradlew :shared:desktopTest :composeApp:desktopTest` |
| Android Debug 编译 | `./gradlew :composeApp:compileDebugKotlinAndroid` |
| Android Debug APK | `./gradlew :composeApp:assembleDebug` |
| Windows 单文件 EXE | `./gradlew :composeApp:packageWindowsSingleFileExe` |
| 桌面端运行 | `./gradlew :composeApp:run` |
| iOS 编译验证（限 macOS） | `./gradlew :shared:compileKotlinIosSimulatorArm64` |

**发布前全量验证（单行）：**
```bash
./gradlew :shared:compileKotlinDesktop :shared:desktopTest :composeApp:compileKotlinDesktop :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid :composeApp:packageWindowsSingleFileExe
```

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
