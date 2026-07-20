# M38 里程碑：用户详情页作品 Tab 屏蔽作品过滤

## 状态
已完成（代码与本地提交）。

- PRD commit: `b90f5beb`
- 实现 commit: `70595659`
- code review: 已执行，未发现 P0/P1 问题
- 编译验证: Android Debug + Desktop 双端 BUILD SUCCESSFUL
- push 状态: 已推送至 origin master。初次推送因直连 github.com:443 失败；后通过 http/https proxy `127.0.0.1:7897` 成功推送。

## 目标
M35-M37 已完成首页、排行榜与搜索页的屏蔽作品过滤。M38 将用户详情页「作品」Tab 接入相同的过滤机制，使用户作品列表中也不会展示已被屏蔽的作品。

## 范围

### 必做（按最小任务量拆分）

1. **依赖传递**
   - `RootContent` 将 `banRepository` 传递到 `UserDetailScreen`。

2. **用户详情页作品 Tab 接入过滤**
   - `UserDetailScreen` 增加 `banRepository: BanRepository` 参数。
   - `UserDetailTabContent` 与 `UserWorksTab` 增加 `banRepository` 参数。
   - `UserWorksTab` 加载用户作品后，使用 `BanRepository.getBannedIllustIds()` 过滤掉被屏蔽作品，再将结果交给 `IllustTabBody`。
   - 过滤逻辑在 `produceState` 内部完成，与现有错误/空态/重试机制保持一致；屏蔽集合查询失败时降级为不过滤。

3. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M38 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用 M35 引入的 `BanRepository.getBannedIllustIds()`，不在用户详情逻辑中新增数据查询。
- 过滤动作放在 `getUserIllusts` 返回成功后、UI 展示前，不修改 `UserRepository` 的 API。
- 屏蔽集合查询失败时降级为不过滤，避免阻塞用户作品主功能。
- 本次仅处理「作品」Tab，「收藏」Tab 放入后续里程碑，控制改动面。

## 验收条件

- [x] 被屏蔽作品不再出现在 `UserDetailScreen` 作品 Tab 中。
- [x] 全部作品均被屏蔽时，作品 Tab 显示空态占位。
- [x] 切换用户或重试后，过滤结果同步更新。
- [x] Android + Desktop 双端编译通过。
- [x] M38 code review 完成，无 P0/P1 问题遗留。
