# M21 里程碑：推荐用户列表

## 状态
已完成。

## 目标
原 Flutter PixEz 在发现页提供「推荐用户」列表（`/v1/user/recommended`）。M21 将为 MIUIX 版新增推荐用户页面，展示系统推荐的关注对象，点击可进入用户详情。

## 范围

### 必做（按最小任务量拆分）

1. **推荐用户 API**
   - `UserRepository` 新增 `getRecommendedUsers()`，调用 `/v1/user/recommended?filter=for_android`。
   - 复用现有 `UserPreviewsResponse` / `UserPreview` 模型解析响应。

2. **推荐用户列表页**
   - 新增 `RecomUserScreen`，使用 `LazyColumn` 展示推荐用户头像、名称、账号及最近作品预览。
   - 复用现有 `UserPreviewItem` 组件，保持与用户关注/好P友列表一致的视觉与交互。
   - 沿用 `produceState` + `runCatchingNonCancel` 加载模式，独立维护加载 / 空态 / 错误占位。

3. **路由与入口**
   - `RootComponent` / `RootContent` 增加 `RecomUserList` 路由与页面渲染。
   - `HelloScreen` TopAppBar 增加「推荐用户」入口按钮，点击后导航到推荐用户列表。

## 验收条件

- [x] `UserRepository` 新增 `getRecommendedUsers()` 并通过 `/v1/user/recommended` 获取数据。
- [x] 新增 `RecomUserScreen` 展示推荐用户列表，点击用户项进入用户详情。
- [x] `HelloScreen` 提供进入推荐用户列表的入口。
- [x] Android + Desktop 双端编译通过。
- [x] M21 code review 完成，无 P0/P1 问题遗留。
