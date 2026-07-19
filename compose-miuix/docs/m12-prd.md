# M12 里程碑：用户好P友（粉丝）列表

## 状态
已完成。

## 目标
原 Flutter PixEz 用户详情页可通过「好P友」入口查看关注自己的用户列表（对应 `/v1/user/follower`）。当前 MIUIX 用户详情页仅有「关注」入口，M12 将补齐该功能。

## 范围

### 必做（按最小任务量拆分）

1. **好P友列表 API**
   - `UserRepository` 新增 `getUserFollowers(userId, restrict)`，调用 `/v1/user/follower`。
   - 复用现有 `UserPreviewsResponse` 模型解析响应。
   - `restrict` 支持 `public` / `private`，默认 `public`。

2. **好P友列表页**
   - 新增 `UserFollowerListScreen`，通过 `produceState` + `runCatchingNonCancel` 加载列表。
   - 列表项展示用户头像、名称、账号，以及最近几张作品预览。
   - 点击用户项导航到用户详情页。
   - 将 `UserFollowListScreen` 与 `UserFollowerListScreen` 共用的列表项提取为 `UserPreviewItem` 组件，避免重复代码。

3. **入口接入**
   - `UserDetailScreen` 头部资料显示「好P友 $totalMypixivUsers」，点击后导航到好P友列表页。
   - 仅在好P友数大于 0 时展示为可点击。
   - 接入 `RootComponent` / `RootContent` 路由。

## 技术决策

- 沿用 `produceState` + `runCatchingNonCancel` 加载模式。
- 复用 M11 的 `UserPreview` 模型与列表项 UI。
- 由于 Pixiv `/v1/user/detail` 的 `Profile` 模型未提供独立的 `total_follower` 字段，与原 Flutter 应用一致，使用 `total_mypixiv_users` 作为入口计数，UI 标签沿用原应用中文翻译「好P友」。

## 验收条件

- [x] `UserRepository.getUserFollowers` 成功返回用户预览列表。
- [x] `UserFollowerListScreen` 正确展示好P友用户与预览作品。
- [x] `UserDetailScreen` 好P友数可点击进入列表页。
- [x] Android + Desktop 双端编译通过。
- [x] M12 code review 完成，无 P0/P1 问题遗留。
