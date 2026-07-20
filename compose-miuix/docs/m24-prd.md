# M24 里程碑：Spotlight 页嵌入推荐用户入口

## 状态
已完成。

## 目标
原 Flutter PixEz 的 Spotlight 发现页顶部会展示一排「推荐用户」头像（`RecomUserRoad`），点击头像进入用户详情，点击「更多」进入完整推荐用户列表。M24 将为 MIUIX 版 `SpotlightScreen` 增加这一横向入口。

## 范围

### 必做（按最小任务量拆分）

1. **推荐用户 Road 组件**
   - 在 `SpotlightScreen.kt` 中新增 `RecomUserRoad` 组件。
   - 使用 `LazyRow` 横向展示推荐用户头像与名称。
   - 每个条目点击后调用 `onUserClick(userId)`。
   - 列表末尾提供「更多」按钮，点击后调用 `onRecomUserListClick()` 进入完整 `RecomUserScreen`。

2. **SpotlightScreen 接入数据与回调**
   - `SpotlightScreen` 新增 `onUserClick: (Int) -> Unit` 与 `onRecomUserListClick: () -> Unit` 参数。
   - 页面加载时同时请求 `/v1/user/recommended?filter=for_android`，取前 N 条（如 10 条）作为 Road 数据。
   - 推荐用户加载失败不影响 Spotlight 文章网格展示，仅隐藏 Road 或显示微小错误占位。

3. **RootContent 传入回调**
   - `RootContent` 中 `Spotlight -> SpotlightScreen` 调用处传入 `component::onUserClicked` 与 `component::onRecomUserListClicked`。

## 验收条件

- [x] Spotlight 页顶部展示横向推荐用户头像列表。
- [x] 点击头像进入用户详情页。
- [x] 点击「更多」进入完整推荐用户列表页。
- [x] 未登录时不展示推荐用户 Road，避免无效 401 请求。
- [x] Android + Desktop 双端编译通过。
- [x] M24 code review 完成，无 P0/P1 问题遗留。
