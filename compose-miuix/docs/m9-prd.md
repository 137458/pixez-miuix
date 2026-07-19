# M9 里程碑：用户详情页收藏夹

## 状态
已完成。commit 见 `daad10d4`。

## 目标
在原 Flutter PixEz 用户详情页中，作品列表与收藏列表是两个独立的 Tab。当前 MIUIX 用户详情页仅展示作品，M9 将补齐「收藏」Tab，并保持与原应用行为一致。

## 范围

### 必做（按最小任务量拆分）

1. **用户收藏 API** ✅
   - `UserRepository` 新增 `getUserBookmarks(userId, restrict)`，调用 `/v1/user/bookmarks/illust`。
   - 复用现有 `UserIllusts` 模型解析响应（字段包含 `illusts` 与 `next_url`）。
   - `restrict` 支持 `public` / `private`，默认 `public`。

2. **用户详情页 Tab 切换** ✅
   - `UserDetailScreen` 头部下方增加 `TabRow`，包含「作品」「收藏」两个 Tab。
   - 作品 Tab 展示现有 `getUserIllusts` 数据。
   - 收藏 Tab 展示 `getUserBookmarks` 数据，并在顶部提供「公开 / 私密」切换（仅当前用户查看自己收藏时最有意义；对他人默认展示公开收藏）。

3. **状态与错误处理** ✅
   - 每个 Tab 独立维护加载/空态/错误占位。
   - 切换 Tab 或公开/私密选项时自动重新加载。
   - 收藏操作失败沿用现有 `followError` 风格提示。

### 不做

- 收藏标签筛选（原应用有，但超出最小任务量，迁出至后续里程碑）。
- 收藏分页加载（当前其他页面同样未实现分页，保持一致）。
- 小说 / 漫画类型切换（保持当前插画类型）。

## 技术决策

- Tab 组件使用 MIUIX 0.8.8 `top.yukonga.miuix.kmp.basic.TabRow`。
- 公开/私密切换复用 `TabRow`；为保持简洁，未引入额外组件。
- 继续沿用 `produceState` + `runCatchingNonCancel` 加载模式。
- Tab 内容区域使用 `Box(Modifier.weight(1f))` 占满剩余空间，避免 `Column` 内滥用 `fillMaxSize()`。
- 插画网格复用现有 `IllustStaggeredGrid` 组件。

## 验收条件

- [x] 进入用户详情页，可在「作品」与「收藏」之间切换。
- [x] 「收藏」Tab 能正确加载公开收藏插画。
- [x] 切换「公开 / 私密」后重新加载对应收藏列表。
- [x] Android + Desktop 双端编译通过。
- [x] M9 code review 完成，无 P0/P1 问题遗留。

## Code Review 记录

本次 review 发现 2 个 P1 问题，已全部修复：

1. `IllustTabBody` 未复用 `IllustStaggeredGrid`，直接手写 `LazyVerticalStaggeredGrid` + `IllustCard`。已替换为 `IllustStaggeredGrid`。
2. `Column` 内 Tab 内容直接使用 `fillMaxSize()` 占满空间，存在布局风险。已改为 `Box(Modifier.weight(1f).fillMaxWidth())` 包裹。
