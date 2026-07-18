# M4 垂直切片拆分

> 注：目标仓库 `137458/pixez-miuix` 已禁用 GitHub Issues，因此将切片以本地文档形式记录，作为实施顺序依据。

## 切片 1：网络基础设施与认证

**标题**: 建立 Ktor Client、OAuth2 与 Token 刷新基础设施

**What to build**: 实现跨平台的 Ktor Client 配置、Pixiv 固定请求头生成、授权码登录、Token 刷新拦截器，以及旧 `AccountDatabase` 的读取/写入能力。完成后应用至少能完成一次 OAuth2 登录并将账号信息持久化到旧数据库。

**Acceptance criteria**:
- [ ] Ktor Client 能正确生成 `X-Client-Time`/`X-Client-Hash` 并附加固定 headers。
- [ ] 实现 PKCE `code_verifier`/`code_challenge` 生成与 WebView 登录 URL 构建。
- [ ] 实现 `code` 换 `access_token`/`refresh_token` 的请求。
- [ ] 实现 401 自动刷新 Token 并重试原请求的插件/拦截器。
- [ ] `AccountDatabase` 可读写当前登录账号。
- [ ] Android + Desktop 编译通过。

**Blocked by**: None - can start immediately.

## 切片 2：首页推荐真实数据

**标题**: 首页推荐接入真实 Pixiv API

**What to build**: 实现 `IllustRepository.getRecommended()`，将 `HelloScreen` 从 `FakeData` 切换到 Repository。页面进入时展示加载状态，成功后渲染真实推荐插画列表。

**Acceptance criteria**:
- [ ] `HelloScreen` 展示真实 `/v1/illust/recommended` 数据。
- [ ] 下拉刷新可重新加载数据。
- [ ] 加载失败时展示错误提示与重试按钮。
- [ ] Android + Desktop 编译通过。

**Blocked by**: 切片 1

## 切片 3：排行榜真实数据

**标题**: 排行榜接入真实 Pixiv API

**What to build**: 实现 `IllustRepository.getRanking(mode, date)`，将 `RankingScreen` 从 `FakeData` 切换到 Repository。默认展示日榜，支持切换周榜/月榜等模式。

**Acceptance criteria**:
- [ ] `RankingScreen` 展示真实 `/v1/illust/ranking` 数据。
- [ ] 支持日/周/月等模式切换。
- [ ] 加载失败时展示错误提示与重试按钮。
- [ ] Android + Desktop 编译通过。

**Blocked by**: 切片 1

## 切片 4：搜索真实数据

**标题**: 搜索接入真实 Pixiv API

**What to build**: 实现 `SearchRepository.searchIllust(word)`，将搜索结果区域从 `FakeData` 切换到 Repository。保留热门标签和历史记录展示，输入关键词后展示真实搜索结果。

**Acceptance criteria**:
- [ ] 输入关键词后展示真实 `/v1/search/illust` 结果。
- [ ] 热门标签使用真实 `/v1/trending-tags/illust`。
- [ ] 搜索历史仍使用本地存储（M4 可继续用内存/Settings 占位）。
- [ ] Android + Desktop 编译通过。

**Blocked by**: 切片 1

## 切片 5：作品详情真实数据

**标题**: 作品详情接入真实 Pixiv API

**What to build**: 实现 `IllustRepository.getIllustDetail(id)`，将 `IllustDetailScreen` 从 `FakeData` 切换到 Repository。通过 `illustId` 查询真实作品详情。

**Acceptance criteria**:
- [ ] `IllustDetailScreen` 展示真实 `/v1/illust/detail` 数据。
- [ ] 大图、标题、作者、标签、统计数均来自 API。
- [ ] 加载失败时展示错误提示与重试按钮。
- [ ] Android + Desktop 编译通过。

**Blocked by**: 切片 1

## 切片 6：用户详情真实数据

**标题**: 用户详情接入真实 Pixiv API

**What to build**: 实现 `UserRepository.getUserDetail(id)` 与 `getUserIllusts(id, type)`，将 `UserDetailScreen` 从 `FakeData` 切换到 Repository。展示真实用户资料与作品列表。

**Acceptance criteria**:
- [ ] `UserDetailScreen` 展示真实 `/v1/user/detail` 数据。
- [ ] 用户作品列表展示真实 `/v1/user/illusts` 数据。
- [ ] 加载失败时展示错误提示与重试按钮。
- [ ] Android + Desktop 编译通过。

**Blocked by**: 切片 1
