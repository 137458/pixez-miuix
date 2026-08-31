# PixEz MIUIX 项目开发规范与自动化规则

## 🤖 项目专属构建验证 (Auto-Verification)

- 代码改动完成后，自动执行多平台编译验证：
  - `./gradlew :shared:compileKotlinDesktop`
  - `./gradlew :composeApp:compileKotlinDesktop`
  - `./gradlew :composeApp:compileDebugKotlinAndroid`
  - `./gradlew :shared:desktopTest :composeApp:desktopTest`
- Compose 工具链基线：Kotlin 2.4.10、Compose Multiplatform 1.12.0、Coil 3.6.0、Gradle 8.14.4。
- 确保 Desktop 与 Android 双端无编译错误和破坏性回归。

## 🎨 MIUIX / HyperOS 规范约束
- 严格使用 Xiaomi HyperOS / MIUIX (`top.yukonga.miuix.kmp`) 官方组件（`Card`, `BasicComponent`, `OverlayDialog`, `OverlayBottomSheet`, `WindowDialog`, `Slider`, `LinearProgressIndicator`, `InfiniteProgressIndicator` 等），严禁泄漏引入 Material 3 控件。
- UI 文本统一接入 `LocalStrings.current` 多语言体系（`AppStrings.kt`）。
- 外部链接、占位符模板、预设档位与阈值统一收敛至 `AppConstants.kt`。
