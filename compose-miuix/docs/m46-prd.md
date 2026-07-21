# M46 里程碑：列表页屏蔽标签过滤

## 状态
已完成。

## 目标
M45 已完成屏蔽标签数据层。M46 在所有已接入屏蔽作品/画师过滤的插画列表页中，同时过滤包含被屏蔽标签的作品，完成与原 Flutter 应用一致的标签屏蔽体验。

## 范围

### 必做（按最小任务量拆分）

1. **插画瀑布流列表页**
   - [HelloScreen](compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/screens/HelloScreen.kt)
   - [RankingScreen](compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/screens/RankingScreen.kt)
   - [SearchScreen](compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/screens/SearchScreen.kt)
   - [NewScreen](compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/screens/NewScreen.kt)
   - [RelatedIllustsScreen](compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/screens/RelatedIllustsScreen.kt)
   - [IllustSeriesScreen](compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/screens/IllustSeriesScreen.kt)
   - [UserDetailScreen](compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/ui/screens/UserDetailScreen.kt) 中的 `UserWorksTab` 与 `UserBookmarksTab`

2. **过滤逻辑**
   - 在现有 `produceState` 中，加载列表数据后同时调用：
     - `BanRepository.getBannedIllustIds()`
     - `BanRepository.getBannedUserIds()`
     - `BanRepository.getAllBanTags()`
   - 过滤条件：
     - `it.id !in bannedIllustIds`
     - `it.user.id !in bannedUserIds`
     - `!banRepository.isBannedByTags(banTags, it.tags.flatMap { listOfNotNull(it.name, it.translatedName) })`
   - 任一屏蔽查询失败时均降级为不过滤，确保主列表功能可用。

3. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M46 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用 M45 新增的 `BanRepository.getAllBanTags()` 与 `isBannedByTags(banTags, tags)`。
- 传入作品的 `IllustTag.name` 与 `IllustTag.translatedName` 共同参与匹配，提高屏蔽命中率。
- 所有屏蔽数据查询失败时降级为不过滤，保持主功能可用。

## 验收条件

- [x] 所有插画列表页加载结果中，包含被屏蔽标签的作品不再出现。
- [x] 屏蔽标签为正则表达式时，按 `containsMatchIn` 在标签文本中搜索匹配。
- [x] 屏蔽标签查询失败时，页面仍可正常展示不过滤的列表。
- [x] Android + Desktop 双端编译通过。
- [x] M46 code review 完成，无 P0/P1 问题遗留。
