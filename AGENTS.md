# PixEz MIUIX — Agent 约束入口

PixEz（Pixiv 第三方客户端）的 Kotlin Multiplatform + Compose Multiplatform + Miuix 重制版，Android / Windows Desktop 双端（iOS 仅编译验证；macOS target 因 zoomable 无 macOS 变体暂未启用）。

> 本文件是本项目构建验证命令的**唯一清单**，其他文档只引用、不重复维护。所有命令在 `compose-miuix/` 目录执行。

## 构建与验证

改动完成的标准 = 涉及平台对应的命令跑通，无编译错误与回归：

| 目标 | 命令 |
|---|---|
| Desktop 共享模块编译 | `./gradlew :shared:compileKotlinDesktop` |
| Desktop 应用编译 | `./gradlew :composeApp:compileKotlinDesktop` |
| 双端单元测试 | `./gradlew :shared:desktopTest :composeApp:desktopTest` |
| Android Debug 编译 | `./gradlew :composeApp:compileDebugKotlinAndroid` |
| Android Debug APK | `./gradlew :composeApp:assembleDebug` |
| Windows 单文件 EXE | `./gradlew :composeApp:packageWindowsSingleFileExe` |
| 桌面端运行 | `./gradlew :composeApp:run` |
| iOS 编译验证（仅 macOS 机器 / CI） | `./gradlew :shared:compileKotlinIosSimulatorArm64` |

发布前全量验证（一行版）：

```bash
./gradlew :shared:compileKotlinDesktop :shared:desktopTest :composeApp:compileKotlinDesktop :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid :composeApp:packageWindowsSingleFileExe
```

工具链基线：JDK 17（与 CI 一致）、Kotlin 2.4.10、Compose Multiplatform 1.12.0、Coil 3.6.0、Gradle 8.14.4。

## UI 规范

- UI 规范全文见 `MIUIX_Spec.md`（组件选型、Liquid Glass、悬浮底栏、大屏适配、多语言），新增页面或通用组件前必读。
- 严格使用 `top.yukonga.miuix.kmp` 官方组件，严禁引入 Material 3 控件。
- UI 文本统一接入 `LocalStrings.current` 多语言体系；常量收敛至 `AppConstants.kt`。

## 项目约定

- `archive/flutter-v1/` 为旧 Flutter 版源码归档，修复默认不改动。
- iOS 目标仅能在 macOS 机器本地验证，Windows 无法运行 Apple toolchain；macOS target 当前未启用。项目当前无 CI，构建与测试均本地执行（命令以上表为唯一清单）。

## 文档索引

| 何时读 | 文档 |
|---|---|
| 构建验证命令 | 本文件（唯一清单） |
| UI 规范全文 | `MIUIX_Spec.md` |
| 架构与调用链 | `Code_Wiki.md` |
| 路线与待办 | `Global_TODO.md` |
| 跨会话记忆 / 本轮修复批次 | `CONTEXT.md` |
| 发布流程与用语 | `RELEASE_NOTES_GUIDE.md` |
