# M22 里程碑：推荐用户列表分页加载

## 状态
已完成。

## 目标
原 Flutter PixEz 的推荐用户页面支持上拉加载更多（`RecomUserStore.next()`）。M22 将为 MIUIX 版 `RecomUserScreen` 增加分页加载能力，滚动到底部时自动加载下一页推荐用户。

## 范围

### 必做（按最小任务量拆分）

1. **推荐用户 API 分页支持**
   - `UserRepository` 新增 `getRecommendedUsers(nextUrl: String)` 重载，直接请求 Pixiv 返回的 `next_url` 加载下一页。
   - 复用现有 `UserPreviewsResponse` / `UserPreview` 模型解析响应，保留响应中的 `next_url`。

2. **推荐用户列表页分页状态**
   - `RecomUserScreen` 维护累积的 `List<UserPreview>` 与当前 `nextUrl`。
   - 首次加载沿用 `produceState` + `runCatchingNonCancel`；加载更多使用独立状态，避免重置列表。
   - 列表底部根据 `nextUrl` 显示「加载更多」入口或「没有更多了」提示；加载中/失败时显示对应占位。
   - 点击「加载更多」或重试时再次调用 Repository 加载下一页，并将结果追加到列表。

3. **边界处理**
   - 首次加载失败时仍展示错误占位与重试按钮。
   - 加载更多失败时保留已有列表，仅底部进入错误态，可单独重试。
   - 切换页面或进程重建后，从第一页重新加载（不持久化分页状态）。

## 验收条件

- [x] `UserRepository` 支持通过 `next_url` 加载下一页推荐用户。
- [x] `RecomUserScreen` 滚动到底部可加载更多，列表追加展示。
- [x] 加载更多失败时保留已有数据，支持单独重试。
- [x] Android + Desktop 双端编译通过。
- [x] M22 code review 完成，无 P0/P1 问题遗留。
