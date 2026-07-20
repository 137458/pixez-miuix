# M36 里程碑：排行榜屏蔽作品过滤

## 状态
已完成。

- PRD commit: `ac75bbd4`
- 实现 commit: `2814a18b`
- code review: 已执行，未发现 P0/P1 问题
- 编译验证: Android Debug + Desktop 双端 BUILD SUCCESSFUL
- push 状态: 本地 commit 已完成，因 GitHub 网络连接问题暂存本地，待网络恢复后重试

## 目标
M35 已完成首页的屏蔽作品过滤。M36 将排行榜（RankingScreen）接入相同的过滤机制，使用户在排行榜中也不会看到已被屏蔽的作品。

## 范围

### 必做（按最小任务量拆分）

1. **依赖传递**
   - `RootContent` / `MainContent` 将 `banRepository` 传递到 `RankingScreen`。

2. **排行榜接入过滤**
   - `RankingScreen` 增加 `banRepository` 参数。
   - 加载排行榜插画后，使用 `BanRepository.getBannedIllustIds()` 过滤掉被屏蔽作品，再将结果交给 `IllustStaggeredGrid`。
   - 过滤逻辑在 `produceState` 内部完成，与现有错误/空态/重试机制保持一致。

3. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M36 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用 M35 新增的 `BanRepository.getBannedIllustIds()`，避免重复实现。
- 过滤动作放在 `repository.getRanking()` 成功后、UI 展示前，不修改 `IllustRepository` 的 API。
- 屏蔽集合查询失败时降级为不过滤，保证排行榜主功能可用。
- 本次仅处理 `RankingScreen`，其他列表页（Search、UserDetail 作品 Tab 等）放入后续里程碑。

## 验收条件

- [x] 被屏蔽作品不再出现在 `RankingScreen` 瀑布流中。
- [x] 全部作品均被屏蔽时，排行榜显示空态占位。
- [x] 切换排行榜模式或日期后，过滤结果同步更新。
- [x] Android + Desktop 双端编译通过。
- [x] M36 code review 完成，无 P0/P1 问题遗留。
