# M5 里程碑：登录与核心交互功能

## 目标
将原 Flutter PixEz 的登录、收藏、关注、搜索历史等核心交互功能移植到 MIUIX 页面，保持功能与原应用一致，不新增原应用不支持的功能。

## 范围

### 必做
1. **登录流程**
   - 新增 `LoginScreen`，支持通过授权码（Authorization Code）完成 OAuth2 PKCE 登录。
   - 在 `HelloScreen` 检测登录状态，未登录时显示登录入口。
   - 登录成功后刷新首页推荐（未登录时首页使用 walkthrough 匿名接口）。

2. **收藏作品**
   - 在 `IllustDetailScreen` 顶部操作栏实现收藏/取消收藏按钮。
   - 新增 `BookmarkRepository` 封装 `/v2/illust/bookmark/add` 与 `/v1/illust/bookmark/delete`。

3. **关注用户**
   - 在 `UserDetailScreen` 头部实现关注/取消关注按钮。
   - 在 `BookmarkRepository` 或新增 `FollowRepository` 中封装 `/v1/user/follow/add` 与 `/v1/user/follow/delete`。

4. **搜索历史持久化**
   - `SearchScreen` 的搜索历史通过 `SettingsRepository.setStringList/getStringList` 持久化。

### 不做
- 下载任务管理（M6）。
- 评论列表、相关作品、画师系列等二级页面（M6）。
- WebView 内嵌登录（先使用浏览器 + 授权码输入方案，跨平台成本更低）。

## 技术决策
- 使用 `AccountRepository.currentAccount()` 判断登录状态。
- 登录成功后通过 `produceState` 的 key 变化触发页面重新加载。
- 收藏/关注操作通过 suspend 函数调用，按钮显示加载/成功反馈（Miuix 的 `BasicComponent` 或 `Button`）。
- 搜索历史使用 JSON 字符串列表保存，与旧版 `shared_preferences` 兼容。

## 验收条件
- Android + Desktop 双端编译通过。
- 未登录时首页能显示匿名推荐，登录后显示个人推荐。
- 作品详情页可收藏/取消收藏。
- 用户详情页可关注/取消关注。
- 搜索历史在进程重启后保留。
