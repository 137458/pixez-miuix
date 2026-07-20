# M40 里程碑：相关作品页屏蔽作品过滤

## 状态
已完成。

## 目标
M39 已完成用户详情页作品与收藏 Tab 的屏蔽过滤。M40 将相关作品页（RelatedIllustsScreen）接入相同的过滤机制，使相关作品推荐中也不会展示已被屏蔽的作品。

## 范围

### 必做（按最小任务量拆分）

1. **依赖传递**
   - `RootContent` 将 `banRepository` 传递到 `RelatedIllustsScreen`。

2. **相关作品页接入过滤**
   - `RelatedIllustsScreen` 增加 `banRepository: BanRepository` 参数。
   - 加载相关作品列表后，使用 `BanRepository.getBannedIllustIds()` 过滤掉被屏蔽作品，再将结果交给 `IllustStaggeredGrid`。
   - 过滤逻辑在 `produceState` 内部完成，与现有错误/空态/重试机制保持一致；屏蔽集合查询失败时降级为不过滤。

3. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M40 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用 M35 引入的 `BanRepository.getBannedIllustIds()`，不在相关作品逻辑中新增数据查询。
- 过滤动作放在 `getIllustRelated` 返回成功后、UI 展示前，不修改 `IllustRepository` 的 API。
- 屏蔽集合查询失败时降级为不过滤，避免阻塞相关作品主功能。

## 验收条件

- [x] 被屏蔽作品不再出现在 `RelatedIllustsScreen` 瀑布流中。
- [x] 全部相关作品均被屏蔽时，页面显示空态占位。
- [x] 切换作品或重试后，过滤结果同步更新。
- [x] Android + Desktop 双端编译通过。
- [x] M40 code review 完成，无 P0/P1 问题遗留。
