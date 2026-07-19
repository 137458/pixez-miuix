# M11 里程碑：用户关注列表

## 状态
进行中。

## 目标
原 Flutter PixEz 用户详情页可查看用户关注列表。当前 MIUIX 用户详情页仅展示资料与作品/收藏，M11 将补齐「关注」入口与列表页。

## 范围

### 必做（按最小任务量拆分）

1. **关注列表 API**
   - `UserRepository` 新增 `getUserFollowing(userId, restrict)`，调用 `/v1/user/following`。
   - 复用现有 `UserPreviewsResponse` 模型解析响应。
   - `restrict` 支持 `public` / `private`，默认 `public`。

2. **关注列表页**
   - 新增 `UserFollowListScreen`，通过 `produceState` + `runCatchingNonCancel` 加载关注列表。
   - 列表项展示用户头像、名称、账号，以及最近几张作品预览。
   - 点击用户项导航到用户详情页。

3. **入口接入**
   - `UserDetailScreen` 头部资料显示关注数，点击后导航到关注列表页。
   - 仅在关注数大于 0 时展示为可点击。

### 不做

- 粉丝列表（`/v1/user/follower`）迁出至 M12。
- 关注列表的公开/私密切换（仅使用默认 public）。
- 在列表项上直接进行关注/取消关注操作（迁出至后续里程碑）。
- 分页加载（当前其他页面同样未实现分页，保持一致）。

## 技术决策

- 沿用 `produceState` + `runCatchingNonCancel` 加载模式。
- 列表项 UI 使用 MIUIX 基础组件（`Surface`、`Text`、`Image`）。
- 复用 `PixivAsyncImage` 加载头像与作品预览图。
- 导航沿用 Decompose `ChildStack` 与 `RootComponent`。

## 验收条件

- [ ] `UserRepository.getUserFollowing` 成功返回关注用户预览列表。
- [ ] `UserFollowListScreen` 正确展示关注用户与预览作品。
- [ ] `UserDetailScreen` 关注数可点击进入关注列表。
- [ ] Android + Desktop 双端编译通过。
- [ ] M11 code review 完成，无 P0/P1 问题遗留。
