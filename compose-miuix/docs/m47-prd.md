# M47 里程碑：AI 作品本地过滤

## 状态
已完成。

## 目标
原 Flutter PixEz 在屏蔽设置页提供「使带有 AI 生成标记的作品不可见」开关（本地键 `ban_ai_illust`）。开启后，列表中 `illust_ai_type == 2` 的作品不展示。M47 在 MIUIX 版中实现该本地设置读写，并在所有插画列表页接入过滤。

## 范围

### 必做（按最小任务量拆分）

1. **设置读写**
   - 在 [SettingsKeys](compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/data/settings/SettingsKeys.kt) 新增 `BAN_AI_ILLUST = "ban_ai_illust"`。
   - 在 [SettingsRepository](compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/data/settings/SettingsRepository.kt) 新增 `banAIIllust: Boolean`，默认值 `false`，读取时兼容旧版 `flutter.ban_ai_illust`。

2. **依赖传递**
   - [RootContent](compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/navigation/RootContent.kt) 与 [MainContent](compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/navigation/RootContent.kt) 将 `settingsRepository` 传递到需要过滤的插画列表页。
   - 接收方：`HelloScreen`、`RankingScreen`、`NewScreen`、`RelatedIllustsScreen`、`IllustSeriesScreen`、`UserDetailScreen`；`SearchScreen` 已持有 `settingsRepository`。

3. **列表页接入过滤**
   - 在以下页面的 `produceState` 中读取 `settingsRepository.banAIIllust`：
     - `HelloScreen`
     - `RankingScreen`
     - `SearchScreen`
     - `NewScreen`
     - `RelatedIllustsScreen`
     - `IllustSeriesScreen`
     - `UserDetailScreen` 的 `UserWorksTab` / `UserBookmarksTab`
   - 过滤条件追加：`!banAIIllust || it.illustAIType != 2`。
   - 与现有作品/画师/标签屏蔽过滤组合使用。

4. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M47 code review，无 P0/P1 问题遗留。

## 技术决策

- 设置键严格对齐旧 Flutter `mute_store.dart` 中的 `"ban_ai_illust"`，保证原有设置可迁移。
- 采用本地过滤而非调用 Pixiv 用户 AI 设置 API；账户级 AI 展示设置留到屏蔽设置页 UI 里程碑处理。
- `illustAIType == 2` 对应 AI 生成作品，与原 Flutter `exts.dart` 逻辑一致。
- 各列表页在 `produceState` 中同步读取设置；设置变更后列表页会在下次加载时生效。

## 验收条件

- [x] `SettingsRepository.banAIIllust` 可正确读写旧 `ban_ai_illust` 设置。
- [x] 开启 AI 过滤后，所有插画列表页中 `illustAIType == 2` 的作品不再出现。
- [x] 关闭 AI 过滤后，AI 作品正常展示。
- [x] Android + Desktop 双端编译通过。
- [x] M47 code review 完成，无 P0/P1 问题遗留。
