# M2 修复 PRD：核心模型与本地存储迁移审查后修复

## Problem Statement

M2 里程碑已完成核心数据模型（Kotlin `@Serializable`）、SQLDelight 本地数据库、Multiplatform Settings 设置桥接的初步迁移。代码审查发现若干 P0/P1 问题，若不修复将导致：

1. Desktop / iOS 无法读取旧版 Flutter 应用遗留的 SQLite 数据库，用户数据丢失。
2. Android / iOS 无法读取旧版 `shared_preferences` 存储的设置（键前缀兼容问题）。
3. 数据库 schema 与旧版行为存在细微差异（KVPair 唯一约束、Task 查询语义）。
4. 构建与 lint 配置过于粗糙，掩盖真实问题。

本 PRD 用于指导 M2 修复阶段，确保迁移后的数据层与旧版行为一致，为后续 UI 移植奠定可靠基础。

## Solution

对 M2 涉及的 `DriverFactory`、SQLDelight schema、`SettingsRepository`/`SettingsFactory`、`build.gradle.kts` 及部分模型进行定向修复，优先解决数据兼容性（P0），再解决正确性与可维护性（P1），最后处理命名与配置冗余（P2）。

## User Stories

1. 作为现有 Desktop 用户，我希望新应用能直接打开旧版 `%APPDATA%/com.perol.pixez/databases/` 下的数据库，使我的收藏、任务、历史记录不丢失。
2. 作为现有 iOS 用户，我希望新应用能继续访问 App Group `group.pixez` 下的 glance 数据库，使桌面小组件/历史浏览数据不丢失。
3. 作为现有 Android/iOS 用户，我希望新应用能读取旧版 `shared_preferences` 中的设置（包括 `flutter.` 前缀键），使我的画质、网络、主题设置保持原样。
4. 作为开发者，我希望 SQLDelight schema 与旧版表结构一致，避免 `INSERT OR REPLACE` 产生重复记录或查询语义偏差。
5. 作为开发者，我希望 `saveMode` 的读写行为与旧版一致，并在显式设置后清除旧版 `is_helplessway` 键。
6. 作为开发者，我希望构建配置不过度放宽 lint，避免隐藏真正的静态分析问题。
7. 作为用户，我希望数据模型对 Pixiv API 的字段缺失具有容错能力，不会因某个字段为 null 而整页白屏。

## Implementation Decisions

- **Desktop 数据库路径**：沿用旧 Flutter 桌面端 `path_provider` 返回的应用支持目录，并按平台追加正确的应用子目录：
  - Windows：`%APPDATA%/com.perol/pixez/databases`（由 `Runner.rc` 的 `CompanyName=com.perol` / `ProductName=pixez` 决定）
  - macOS：`~/Library/Application Support/com.perol.pixezFlutter/databases`（旧 bundle identifier）
  - Linux：`~/.local/share/com.perol.pixez/databases`（旧 Flutter 项目未提供 Linux 支持，仅作兜底）
- **iOS 数据库路径**：普通数据库使用 `Documents/databases`；`glanceillustpersist.db` 优先使用 App Group `group.pixez/DB/`，与旧版 `GlanceIllustPersistProvider.getPath()` 行为一致。
- **SharedPreferences 键前缀兼容**：在 `SettingsRepository` 读取层增加双键回退：先读无 `flutter.` 前缀键，再读带 `flutter.` 前缀键；写入时统一写新键（无前缀），保持向前兼容。
- **KVPair 唯一约束**：**移除** `key` 上的 `UNIQUE` 约束，保持与旧 Flutter `kvpair.db` schema 一致（旧版无唯一约束）。
- **Task 分页查询**：将 `WHERE ? OR status = ?` 拆分为 `selectAllPagedAsc/Desc` 与 `selectByStatusPagedAsc/Desc`，在 Repository 层根据状态值选择查询。
- **saveMode 回退清理**：setter 先移除 `is_helplessway` 再写入 `save_mode`，避免异常路径下旧键残留导致读取歧义；用命名常量替换魔术数字 `0/1/2`。
- **tagHisotry 拼写**：确认旧版 `ExportData` 中确实使用 `tagHisotry` 错拼，**保留**该 `@SerialName` 以保持导入导出兼容。
- **Desktop 设置迁移**：`createSettings()` 仅返回 `Settings` 实例；一次性旧 `shared_preferences.json` 迁移通过 `SettingsFactory.migrateIfNeeded()` 在后台协程（`Dispatchers.IO`）中显式执行，避免阻塞主线程。
- **SQLDelight 迁移文件**：为 `TaskDatabase` 与 `GlanceIllustPersistDatabase` 添加 `1.sqm`，分别补齐 `medium` 列与 `original_url`/`large_url` 列，使 schema 版本与旧 Flutter 的 v2 对齐，避免 Android/iOS/macOS 打开旧库时触发降级错误。
- **verifyMigrations**：移除全局 `VerifyMigrationTask.enabled = false`；改为非 Windows 平台默认启用，Windows 可通过 `-PskipVerifyMigrations=true` 显式跳过。
- **数据库命名**：将 `IllustPersistDatabase` 重命名为 `GlanceIllustPersistDatabase`，与实际表名 `glanceillustpersist` 及旧 `.db` 文件名一致，避免与未来真正的 `illustpersist.db` 混淆。
- **compileSdk**：MIUIX 0.9.x 要求 `compileSdk = 37`（Android 17 预览版），稳定版 CI 无法自动安装；已降级至 MIUIX 0.8.8（要求 `compileSdk = 36`）。
- **lint 配置**：`abortOnError = false` 保留（AGP 8.13 内嵌 Kotlin 编译器不兼容项目 Kotlin 2.4.0 元数据，暂无具体 issue id 可禁用）。
- **模型可空性**：按旧版 Dart 模型逐个核对，将 API 中可能缺失的字段改为可空（优先覆盖 `Illust`、`Novel`、`UserDetail` 核心路径）。
- **novelAIType 命名统一**：将 `NovelSeriesNovel.novelAiType` 重命名为 `novelAIType`，保持与 `illustAIType` 风格一致。

## Testing Decisions

- **路径测试**：在 desktopTest 中验证 `DriverFactory` 返回的路径字符串包含正确的平台子目录：Windows 为 `com.perol/pixez/databases`，macOS 为 `com.perol.pixezFlutter/databases`。
- **设置桥接测试**：扩展 `SettingsBridgeTest`，验证读取 `flutter.zoom_quality` 等前缀键能正确回退，写入后无前缀键可被读取。
- **数据库兼容性测试**：`OldDatabaseReadabilityTest` 继续保留内存 schema 测试；`DriverFactoryMigrationTest` 补充旧 Flutter v1/v2 数据库场景，验证 `1.sqm` 迁移与版本对齐逻辑。
- **序列化测试**：`ModelSerializationTest` 补充 `TagExportData` 的错拼 JSON 用例，确保兼容旧导出文件。
- **回归测试**：每次修改后执行 `./gradlew :compose-miuix:shared:testDebugUnitTest`、`:shared:desktopTest`、`:shared:compileDebugKotlinAndroid`、`:composeApp:compileKotlinDesktop`，确保无编译/测试失败。

## Out of Scope

- 不实现 UI 页面（属于 M3 及以后里程碑）。
- 不实现网络请求层（Ktor client、OAuth、token 刷新属于 M3）。
- 不实现跨平台文件保存、DeepLink、IAP、小组件等平台能力（属于后续里程碑）。
- 不处理 HarmonyOS（ohos）旧工程；目标平台为 Android / iOS / Desktop / macOS。
- 不修改旧 Flutter 代码；旧代码保持过渡保留。
- `shared_preferences` 插件版本为 `^2.5.5`，该版本在 Android/iOS 上会对键名添加 `flutter.` 前缀，必须处理。
- iOS App Group `group.pixez` 已配置， glance 数据库迁移依赖该 Group 可用。
- Windows 桌面路径由 `path_provider_windows` 读取 `Runner.rc` 的 `CompanyName`/`ProductName` 生成，结果为 `%APPDATA%/com.perol/pixez`，与单纯的 Android 包名 `com.perol.pixez` 不同。
- macOS 桌面路径由 `path_provider_foundation` 使用旧 bundle identifier `com.perol.pixezFlutter` 生成，与 Android 包名不同。
- `tagHisotry` 是旧版代码中的真实错拼，不是新代码 bug；保留它是为了确保旧导出数据仍可导入。
