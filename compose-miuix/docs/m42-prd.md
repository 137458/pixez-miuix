# M42 里程碑：插画系列页（IllustSeriesScreen）屏蔽作品过滤

## 状态
已完成。

## 目标
M41 已完成关注页屏蔽作品过滤。M42 将插画系列页（IllustSeriesScreen）接入相同的过滤机制，使系列内作品列表中也不会展示已被屏蔽的作品。

## 范围

### 必做（按最小任务量拆分）

1. **依赖传递**
   - `RootContent` 将 `banRepository` 传递到 `IllustSeriesScreen`。

2. **插画系列页接入过滤**
   - `IllustSeriesScreen` 增加 `banRepository: BanRepository` 参数。
   - 加载系列内作品列表后，使用 `BanRepository.getBannedIllustIds()` 过滤掉被屏蔽作品，再将结果交给 `IllustStaggeredGrid`。
   - 过滤逻辑在 `produceState` 内部完成，与现有错误/空态/重试机制保持一致；屏蔽集合查询失败时降级为不过滤。

3. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M42 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用 M35 引入的 `BanRepository.getBannedIllustIds()`，不在系列页逻辑中新增数据查询。
- 过滤动作放在 `getIllustSeries` 返回成功后、UI 展示前，不修改 `IllustRepository` 的 API。
- 屏蔽集合查询失败时降级为不过滤，避免阻塞插画系列页主功能。

## 验收条件

- [x] 被屏蔽作品不再出现在 `IllustSeriesScreen` 瀑布流中。
- [x] 系列内全部作品均被屏蔽时，页面显示空态占位。
- [x] 切换系列或重试后，过滤结果同步更新。
- [x] Android + Desktop 双端编译通过。
- [x] M42 code review 完成，无 P0/P1 问题遗留。
