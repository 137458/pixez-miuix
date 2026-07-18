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

- **Desktop 数据库路径**：在 `path_provider` 返回的应用支持目录下追加 `com.perol.pixez` 包名，再追加 `databases`，与旧 Flutter 桌面端一致。
  - Windows：`%APPDATA%/com.perol.pixez/databases`
  - macOS：`~/Library/Application Support/com.perol.pixez/databases`
  - Linux：`~/.local/share/com.perol.pixez/databases`
- **iOS 数据库路径**：普通数据库使用 `Documents/databases`；`glanceillustpersist.db` 优先使用 App Group `group.pixez/DB/`，与旧版 `GlanceIllustPersistProvider.getPath()` 行为一致。
- **SharedPreferences 键前缀兼容**：在 `SettingsRepository` 读取层增加双键回退：先读无 `flutter.` 前缀键，再读带 `flutter.` 前缀键；写入时统一写新键（无前缀），保持向前兼容。
- **KVPair 唯一约束**：在 `.sq` 中为 `key` 增加 `UNIQUE` 约束，使 `INSERT OR REPLACE` 按预期工作。
- **Task 分页查询**：将 `WHERE ? OR status = ?` 拆分为 `selectAllPagedAsc/Desc` 与 `selectByStatusPagedAsc/Desc`，在 Repository 层根据状态值选择查询。
- **saveMode 回退清理**：setter 写入 `save_mode` 时同步移除 `is_helplessway`，避免过期回退。
- **tagHisotry 拼写**：确认旧版 `ExportData` 中确实使用 `tagHisotry` 错拼，**保留**该 `@SerialName` 以保持导入导出兼容。
- **Desktop 设置迁移**：实现一次性迁移：启动时检测旧 Flutter `shared_preferences.json`，将其内容导入新存储后标记迁移完成。
- **compileSdk**：需确认 MIUIX 0.9.2 真实要求。当前 `compileSdk = 37` 在标准 Android SDK 中不存在，若构建失败则降至可用的最新稳定版（如 35）。
- **lint 配置**：将 `abortOnError = false` 改为仅禁用具体的 metadata 不兼容 issue，保留其他 lint 检查。
- **模型可空性**：按旧版 Dart 模型逐个核对，将 API 中可能缺失的字段改为可空（优先覆盖 `Illust`、`Novel`、`UserDetail` 核心路径）。
- **verifyMigrations 配置去重**：移除每个数据库块内的 `verifyMigrations.set(false)`，仅保留全局 `VerifyMigrationTask.enabled = false`。
- **novelAIType 命名统一**：将 `NovelSeriesNovel.novelAiType` 重命名为 `novelAIType`，保持与 `illustAIType` 风格一致。

## Testing Decisions

- **路径测试**：在 desktopTest 中验证 `DriverFactory` 返回的路径字符串包含 `com.perol.pixez/databases`。
- **设置桥接测试**：扩展 `SettingsBridgeTest`，验证读取 `flutter.zoom_quality` 等前缀键能正确回退，写入后无前缀键可被读取。
- **数据库兼容性测试**：`OldDatabaseReadabilityTest` 继续保留内存 schema 测试；在条件允许时补充真实旧 `.db` 样本文件测试。
- **序列化测试**：`ModelSerializationTest` 补充 `TagExportData` 的错拼 JSON 用例，确保兼容旧导出文件。
- **回归测试**：每次修改后执行 `./gradlew :compose-miuix:shared:testDebugUnitTest` 与桌面测试任务，确保无编译/测试失败。

## Out of Scope

- 不实现 UI 页面（属于 M3 及以后里程碑）。
- 不实现网络请求层（Ktor client、OAuth、token 刷新属于 M3）。
- 不实现跨平台文件保存、DeepLink、IAP、小组件等平台能力（属于后续里程碑）。
- 不处理 HarmonyOS（ohos）旧工程；目标平台为 Android / iOS / Desktop / macOS。
- 不修改旧 Flutter 代码；旧代码保持过渡保留。

## Further Notes

- `shared_preferences` 插件版本为 `^2.5.5`，该版本在 Android/iOS 上会对键名添加 `flutter.` 前缀，必须处理。
- iOS App Group `group.pixez` 已配置， glance 数据库迁移依赖该 Group 可用。
- Windows `identity_name` 为 `com.perol.pixez`，与 Android 包名一致，桌面路径应使用同一包名。
- `tagHisotry` 是旧版代码中的真实错拼，不是新代码 bug；保留它是为了确保旧导出数据仍可导入。
