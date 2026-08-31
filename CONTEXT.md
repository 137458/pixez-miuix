# PixEz 项目上下文记忆

> 本文件是跨会话的版本化项目记忆入口。项目约束以 `AGENTS.md` / `GEMINI.md` 为准；架构背景见 `Code_Wiki.md`，路线与待办见 `Global_TODO.md`。

## 当前工作范围

- 根目录 Flutter 分支已归档，后续修复默认**不修改**归档 Flutter 源码及其原生 Runner。
- 当前维护目标是 `compose-miuix` Kotlin Multiplatform / Compose 分支。
- 目标平台：Android、JVM Desktop、iOS ARM64 / Simulator ARM64、macOS ARM64。
- 模块：`:shared` 与 `:composeApp`。

## 关键技术基线

- Kotlin `2.4.10`、Compose Multiplatform `1.12.0`、MIUIX `0.9.4-rc01`。
- Gradle wrapper `8.14.4`，JVM target 17。
- Ktor 3.1.2、SQLDelight 2.0.2、Coil 3.6.0、Kotlin Coroutines 1.10.2。
- UI 必须使用 `top.yukonga.miuix.kmp` 官方组件；文本使用 `LocalStrings.current`；平台差异使用 expect/actual。

## 本轮修复批次

### 已确认的高风险主题

1. commonMain 误用 JVM API，阻断 iOS/macOS 编译。
2. 认证 token 存储、账号选择、token 刷新锁边界与 OAuth PKCE 并发。
3. 分页/图片/Spotlight/更新 URL 缺少 HTTPS 与 host 限制。
4. 下载与更新文件名路径穿越、无大小上限、非原子写入及取消状态。
5. 搜索/榜单/推荐列表的旧分页状态混入新查询。
6. logout、主 tab 恢复和 Settings changeVersion 的状态同步。

### 本轮约定

- 优先先恢复所有 shared native target 的可编译性，再处理安全与数据一致性。
- 网络请求不得在 token storage 的 Mutex 内执行。
- 认证客户端只能向明确允许的 Pixiv host 发送 Authorization。
- 下载/更新必须在受控目录内使用安全 basename、临时文件和完整性校验后原子提交。
- 不直接覆盖已有未提交修改；每次改动保持最小范围并保留旧数据库兼容性。

## 本轮已完成

- 新增 `TrustedUrlPolicy`，为 API 分页、图片、Pixivision 与 GitHub 更新地址要求 HTTPS、可信主机且禁止 URL 凭据。
- 修复账户编辑在 token storage Mutex 内执行网络请求的死锁；提交前校验账号未被切换；logout 发出统一事件。
- 下载 retry 与各平台 `IllustSaver` 统一拒绝路径型文件名；iOS/macOS 拒绝空图片输入。
- 更新下载器改为可信重定向、大小上限、临时文件、输入流关闭、同步落盘后原子替换；更新元数据不再把任意 asset/HTML 页面当安装包。
- commonMain 的时间、调度和 Coil 缓存文件读取改为跨平台 API，新增各平台默认图片目录抽象。
- 搜索与排行榜在条件变更时清空旧分页状态，并使用 request generation 丢弃过期回调；SettingsRepository 补齐 UI 相关 setter 的 `notifyChanged()`。

## 验证记录

- 本会话开始时工作区已有未提交 Compose 改动，不能视为本轮变更。
- 使用 Android Studio JBR（JDK 17）验证通过：`shared:compileKotlinDesktop`、`composeApp:compileKotlinDesktop`、`composeApp:compileDebugKotlinAndroid`、`shared:desktopTest`。
- 本轮升级目标为消除 Coil/Skiko 版本不兼容告警与 Gradle 8.14.3 的升级提示；需以本轮依赖解析和编译结果确认。
- Windows 主机没有 Android AVD/设备，不能做设备级回归。
- Flutter SDK 不可用；Flutter 分支已归档，本轮不执行 Flutter 验证。
- iOS/macOS 目标应在 macOS CI 验证；本机无法运行 Apple toolchain。

## 后续验证命令

在 `compose-miuix` 目录执行：

```text
./gradlew :shared:compileKotlinDesktop
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :shared:compileKotlinMacosArm64
./gradlew :shared:desktopTest
```

其中 iOS/macOS 目标应在 macOS CI 验证；Windows 仅能验证 JVM Desktop 与 Android。

## 未解决风险索引

更新供应链、token 安全存储、下载流式大小限制、SAF 目录接入、列表 generation/cancellation、logout 事件流和 release signing 仍需继续处理。详细问题证据见本会话的审查报告与对应源码行号。
