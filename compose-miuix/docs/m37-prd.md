# M37 里程碑：搜索页屏蔽作品过滤

## 状态
进行中。

## 目标
M35、M36 已完成首页与排行榜的屏蔽作品过滤。M37 将搜索页（SearchScreen）的作品搜索结果接入相同的过滤机制，使用户在搜索作品时也不会看到已被屏蔽的作品。

## 范围

### 必做（按最小任务量拆分）

1. **依赖传递**
   - `RootContent` / `MainContent` 将 `banRepository` 传递到 `SearchScreen`。
   - 注意 `SearchScreen` 在 `RootContent` 中有两处调用：`Child.Search` 与 `MainTab.Search`，均需传递。

2. **搜索页接入过滤**
   - `SearchScreen` 增加 `banRepository: BanRepository` 参数。
   - `SearchIllustResultGrid` 增加 `banRepository: BanRepository` 参数。
   - 加载作品搜索结果后，使用 `BanRepository.getBannedIllustIds()` 过滤掉被屏蔽作品，再将结果交给 Ugoira 本地过滤与 `IllustStaggeredGrid`。
   - 过滤逻辑在 `produceState` 内部完成，与现有错误/空态/重试机制保持一致；屏蔽集合查询失败时降级为不过滤。

3. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M37 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用 M35 引入的 `BanRepository.getBannedIllustIds()`，不在搜索逻辑中新增数据查询。
- 过滤动作放在作品搜索 API 返回成功后、Ugoira 本地过滤与 UI 展示前，不修改 `SearchRepository` 的 API。
- 屏蔽集合查询失败时降级为不过滤，避免阻塞搜索主功能。
- 本次仅处理 `SearchScreen` 的「作品」Tab，「画师」Tab 不涉及作品过滤。

## 验收条件

- [ ] 被屏蔽作品不再出现在 `SearchScreen` 作品搜索结果中。
- [ ] 全部搜索结果均被屏蔽时，作品 Tab 显示空态占位。
- [ ] 切换搜索词或筛选条件后，过滤结果同步更新。
- [ ] Android + Desktop 双端编译通过。
- [ ] M37 code review 完成，无 P0/P1 问题遗留。
