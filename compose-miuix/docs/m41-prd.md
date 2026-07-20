# M41 里程碑：关注页（NewScreen）屏蔽作品过滤

## 状态
已完成。

## 目标
M40 已完成相关作品页屏蔽作品过滤。M41 将关注页（NewScreen）接入相同的过滤机制，使关注画师最新作品中也不会展示已被屏蔽的作品。

## 范围

### 必做（按最小任务量拆分）

1. **依赖传递**
   - `RootContent` / `MainContent` 将 `banRepository` 传递到 `NewScreen`。

2. **关注页接入过滤**
   - `NewScreen` 增加 `banRepository: BanRepository` 参数。
   - 加载关注作品列表后，使用 `BanRepository.getBannedIllustIds()` 过滤掉被屏蔽作品，再将结果交给 `IllustStaggeredGrid`。
   - 过滤逻辑在 `produceState` 内部完成，与现有错误/空态/重试机制保持一致；屏蔽集合查询失败时降级为不过滤。

3. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M41 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用 M35 引入的 `BanRepository.getBannedIllustIds()`，不在关注页逻辑中新增数据查询。
- 过滤动作放在 `getFollowIllusts` 返回成功后、UI 展示前，不修改 `IllustRepository` 的 API。
- 屏蔽集合查询失败时降级为不过滤，避免阻塞关注页主功能。

## 验收条件

- [x] 被屏蔽作品不再出现在 `NewScreen` 瀑布流中。
- [x] 全部关注作品均被屏蔽时，页面显示空态占位。
- [x] 切换可见性筛选或重试后，过滤结果同步更新。
- [x] Android + Desktop 双端编译通过。
- [x] M41 code review 完成，无 P0/P1 问题遗留。
