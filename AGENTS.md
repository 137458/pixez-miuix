# PixEz MIUIX 项目开发规范与自动化规则

## 🤖 自主执行与自动化提交准则 (Autonomous Execution & Auto-Commit)

1. **自动更新文档 (Auto-Update Documentation)**:
   - 每次完成功能新增、代码重构、多语言扩展、依赖升级或 Bug 修复后，主动检查并更新相关文档（如 `README.md`、各模块说明与设计文档）。
   - 保证代码实现、常量定义、架构设计与文档描述保持 100% 实时同步。

2. **自动构建验证 (Auto-Verification)**:
   - 代码改动完成后，自动执行多平台编译验证：
     - `./gradlew :shared:compileKotlinDesktop`
     - `./gradlew :composeApp:compileKotlinDesktop`
     - `./gradlew :composeApp:compileDebugKotlinAndroid`
   - 确保 Desktop 与 Android 双端无编译错误和破坏性回归。

3. **自动 Git 提交（无需等待用户确认） (Auto-Commit without Confirmation)**:
   - 代码与文档验证无误后，**直接自动执行 Git 提交**，无需等待或询问用户。
   - 采用标准语义化提交格式（Conventional Commits）：
     - `feat(...)`: 新特性 / 新功能
     - `fix(...)`: 缺陷修复
     - `refactor(...)`: 架构重构 / 多语言扩展 / 常量收敛
     - `docs(...)`: 文档更新
     - `perf(...)`: 性能优化
     - `chore(...)`: 依赖与工程配置调整

4. **MIUIX / HyperOS 规范约束**:
   - 严格使用 Xiaomi HyperOS / MIUIX (`top.yukonga.miuix.kmp`) 官方组件（`Card`, `BasicComponent`, `OverlayDialog`, `OverlayBottomSheet`, `WindowDialog`, `Slider`, `LinearProgressIndicator`, `InfiniteProgressIndicator` 等），严禁泄漏引入 Material 3 控件。
   - UI 文本统一接入 `LocalStrings.current` 多语言体系（`AppStrings.kt`）。
   - 外部链接、占位符模板、预设档位与阈值统一收敛至 `AppConstants.kt`。
