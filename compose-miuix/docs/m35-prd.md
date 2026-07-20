# M35 里程碑：首页屏蔽作品过滤

## 状态
已完成。

- 实现 commit: `待填充`
- code review: 已执行，未发现 P0/P1 问题
- 编译验证: Android Debug + Desktop 双端 BUILD SUCCESSFUL

## 目标
M34 已实现作品详情页的屏蔽入口，但列表页仍会展示被屏蔽的作品。M35 将首先在首页（HelloScreen）过滤掉已被屏蔽的作品，使屏蔽功能真正生效。

## 范围

### 必做（按最小任务量拆分）

1. **数据层支持**
   - 在 `BanRepository` 中新增 `getBannedIllustIds(): Set<Int>`，一次性查询全部屏蔽作品 ID 并转为集合，便于列表过滤。

2. **首页接入过滤**
   - `HelloScreen` 增加 `banRepository` 参数。
   - 加载插画列表后，使用 `getBannedIllustIds()` 过滤掉被屏蔽作品，再将结果交给 `IllustStaggeredGrid`。
   - 过滤逻辑在 `produceState` 内部完成，与现有错误/空态/重试机制保持一致。

3. **依赖传递**
   - `RootContent` / `MainContent` 将 `banRepository` 传递到 `HelloScreen`。

4. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M35 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用现有 `BanRepository`，新增 `getBannedIllustIds()` 辅助方法，避免在列表循环中多次查询数据库。
- 过滤动作放在数据加载成功后、UI 展示前，不修改 `IllustRepository` 的 API。
- 屏蔽列表通常数据量较小，直接在主线程将 `List<BanIllust>` 转为 `Set<Int>`；数据库查询仍走 `Dispatchers.IO`。
- 本次仅处理 `HelloScreen`，其他列表页（Search、Ranking、UserDetail 作品 Tab 等）放入后续里程碑，控制改动面。

## 验收条件

- [x] 被屏蔽作品不再出现在 `HelloScreen` 瀑布流中。
- [x] 全部作品均被屏蔽时，首页显示空态占位。
- [x] 屏蔽/取消屏蔽后返回首页，首页列表能反映最新屏蔽状态（通过重新进入或刷新触发）。
- [x] Android + Desktop 双端编译通过。
- [x] M35 code review 完成，无 P0/P1 问题遗留。
