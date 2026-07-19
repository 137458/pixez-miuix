# M6 里程碑：二级页面与数据填充

## 目标
将 M4/M5 中仍为占位或缺失的页面数据与二级页面逐步补齐，保持与原 Flutter PixEz 功能一致，不新增原应用不支持的功能。

## 范围

### 必做（按最小任务量拆分）
1. **NewScreen 真实关注数据**
   - 新增 `FollowIllusts` 响应模型，对应 `/v2/illust/follow`。
   - `IllustRepository` 添加 `getFollowIllusts(restrict: String)`。
   - `NewScreen` 检测登录状态，未登录时显示登录入口；登录后加载关注画师最新插画。
   - 支持 `all`/`public`/`private` 三种可见性筛选。

2. **Spotlight 真实数据（已完成）**
   - 接入 Pixiv Spotlight API `/v1/spotlight/articles`，替换当前 `FakeData` 占位。
   - `SpotlightScreen` 以卡片网格展示文章缩略图与标题，点击打开文章 URL。

3. **评论列表（已完成）**
   - 新增 `Comment`/`CommentResponse` 模型，对应 `/v3/illust/comments`。
   - `IllustRepository` 添加 `getIllustComments(illustId: Int)`。
   - 新增 `CommentsScreen`，展示评论列表、空态与错误重试。
   - `IllustDetailScreen` 信息区添加可点击评论统计项，Decompose 导航到评论页。

4. **作品下载任务管理（TODO）**
   - 实现插画下载队列、进度反馈与本地保存。

5. **相关作品二级页面（已完成）**
   - `IllustRepository` 添加 `getIllustRelated(illustId: Int)`，调用 `/v2/illust/related`。
   - 新增 `RelatedIllustsScreen`，复用 `IllustStaggeredGrid` 展示相关插画。
   - `IllustDetailScreen` 信息区添加"相关作品"入口，Decompose 导航到相关作品页。

6. **画师系列等二级页面（TODO）**
   - 用户详情/作品详情进入画师系列等页面。

## 技术决策
- 登录状态通过 `AccountRepository.currentAccount()` 判断，与 `HelloScreen` 保持一致。
- 可见性筛选状态使用 `rememberSaveable` 保存进程重建。
- 网络请求统一使用 `runCatchingNonCancel` + `networkCall` 错误处理。

## 验收条件
- Android + Desktop 双端编译通过。
- 未登录时 NewScreen 显示登录入口。
- 登录后 NewScreen 可切换 all/public/private 并展示关注作品。
- 各新增功能需经过 code review 并修复 P0/P1 问题。
