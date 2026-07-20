# M39 里程碑：用户详情页收藏 Tab 屏蔽作品过滤

## 状态
已完成。

- PRD commit: `5bd80387`
- 实现 commit: `1dc38e57`
- code review: 已执行，未发现 P0/P1 问题
- 编译验证: Android Debug + Desktop 双端 BUILD SUCCESSFUL
- push 状态: 已推送至 origin master（`382d9fcb..1dc38e57`，通过 http/https proxy `127.0.0.1:7897`）

## 目标
M38 已完成用户详情页「作品」Tab 的屏蔽过滤。M39 将「收藏」Tab 接入相同的过滤机制，使用户收藏列表中也不会展示已被屏蔽的作品。

## 范围

### 必做（按最小任务量拆分）

1. **收藏 Tab 接入过滤**
   - `UserDetailTabContent` 将 `banRepository` 传递到 `UserBookmarksTab`。
   - `UserBookmarksTab` 增加 `banRepository: BanRepository` 参数。
   - 加载用户收藏插画后，使用 `BanRepository.getBannedIllustIds()` 过滤掉被屏蔽作品，再将结果交给 `IllustTabBody`。
   - 过滤逻辑在 `produceState` 内部完成，与现有错误/空态/重试机制保持一致；屏蔽集合查询失败时降级为不过滤。

2. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M39 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用 M35 引入的 `BanRepository.getBannedIllustIds()`，不在收藏逻辑中新增数据查询。
- 过滤动作放在 `getUserBookmarks` 返回成功后、UI 展示前，不修改 `UserRepository` 的 API。
- 屏蔽集合查询失败时降级为不过滤，避免阻塞收藏主功能。
- 本次仅处理「收藏」Tab，依赖传递已在 M38 完成。

## 验收条件

- [x] 被屏蔽作品不再出现在 `UserDetailScreen` 收藏 Tab 中。
- [x] 全部收藏均被屏蔽时，收藏 Tab 显示空态占位。
- [x] 切换用户、公开/私密选项或重试后，过滤结果同步更新。
- [x] Android + Desktop 双端编译通过。
- [x] M39 code review 完成，无 P0/P1 问题遗留。
