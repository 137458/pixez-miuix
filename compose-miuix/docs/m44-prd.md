# M44 里程碑：列表页屏蔽画师过滤

## 状态
已完成。

## 目标
M43 已完成屏蔽画师数据层。M44 在所有已接入屏蔽作品过滤的插画列表页中，同时过滤被屏蔽画师的作品，保持与原 Flutter 应用一致的屏蔽体验。

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
   - 在现有 `produceState` 中，加载列表数据后同时调用 `BanRepository.getBannedIllustIds()` 与 `BanRepository.getBannedUserIds()`。
   - 过滤条件：`it.id !in bannedIllustIds && it.user.id !in bannedUserIds`。
   - 任一查询失败时均降级为不过滤，确保主列表功能可用。

3. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M44 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用 M35-M42 建立的 `produceState + runCatchingNonCancel` 过滤模式，新增 `getBannedUserIds()` 调用。
- 保持顺序执行：先取作品列表，再并行并非必要；本地 DB 查询足够快，顺序执行可读性更高，且与现有代码风格一致。
- 不引入新的依赖参数：各页面已持有 `banRepository`，直接复用。

## 验收条件

- [x] 所有插画列表页加载结果中，被屏蔽画师的作品不再出现。
- [x] 屏蔽画师查询失败时，页面仍可正常展示不过滤的列表。
- [x] Android + Desktop 双端编译通过。
- [x] M44 code review 完成，无 P0/P1 问题遗留。
