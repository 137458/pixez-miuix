# M4 PRD：网络层接入与真实数据替换

## Problem Statement

M3 已完成核心页面 UI 与导航框架移植，但所有页面仍使用 `FakeData` 生成的静态 mock 数据。用户无法浏览真实的 Pixiv 内容，搜索、排行榜、作品详情、用户详情等核心功能均停留在布局验证阶段。为了恢复并保留原 Flutter 应用的核心体验，必须在 Compose Multiplatform 中重建网络层，将 UI 从假数据切换到真实的 Pixiv API 数据。

## Solution

在 `compose-miuix/shared` 中建立跨平台网络层，使用 Ktor Client 替代原 Flutter 的 Dio/Rhttp，复用原应用已验证的 API 端点、请求头生成逻辑与 OAuth2 认证流程。网络层按职责拆分为：

1. **Ktor Client 基础设施**：base URL、固定请求头、`X-Client-Time`/`X-Client-Hash` 生成、网络模式适配（ech/compat/standard）、日志拦截。
2. **认证与 Token 管理**：OAuth2 授权码登录、Token 刷新、Account 数据库存储与读取、请求头自动注入 Bearer Token。
3. **Pixiv API Repository**：按页面需求实现推荐、排行榜、搜索、作品详情、用户详情等接口，返回 M2 已定义的 Kotlin 数据模型。
4. **UI 接入**：将首页、排行榜、最新、搜索、作品详情、用户详情等页面从 `FakeData` 切换到 Repository，并补充加载/错误/空态占位。

M4 以"能浏览真实 Pixiv 内容"为验收目标，不追求一次性覆盖原应用全部 API。

## User Stories

1. 作为用户，我希望打开应用后看到真实的 Pixiv 推荐插画，而不是示例占位图。
2. 作为用户，我希望在排行榜页面按日/周/月等维度查看真实热门作品。
3. 作为用户，我希望在搜索页面输入关键词后看到真实的搜索结果。
4. 作为用户，我希望点击作品卡片进入真实的作品详情页，看到实际标题、作者、标签、浏览/收藏数。
5. 作为用户，我希望点击作者头像进入真实的用户详情页，看到实际头像、简介和作品列表。
6. 作为用户，我希望应用能记住我的登录状态，下次打开不需要重新登录。
7. 作为用户，我希望在 Token 过期时应用自动刷新，而不需要手动重新登录。
8. 作为开发者，我希望网络层跨平台复用，避免为 Android/iOS/Desktop 分别实现。
9. 作为开发者，我希望 Repository 返回统一的数据模型，便于 UI 层无差别消费。
10. 作为开发者，我希望网络错误能被统一捕获并展示友好提示，而不是崩溃或白屏。

## Implementation Decisions

- **HTTP 客户端**：使用 Ktor Client（`ktor-client-core` + `ktor-client-content-negotiation` + `ktor-serialization-kotlinx-json` + `ktor-client-logging`），已声明在 `libs.versions.toml` 与 `shared/build.gradle.kts` 中。Android/Desktop 使用 OkHttp 引擎，iOS/macOS 使用 Darwin 引擎。
- **请求头生成**：复用原 Flutter `ApiClient` 逻辑——生成 ISO8601 UTC 时间戳，拼接固定 `hashSalt` 后计算 MD5，作为 `X-Client-Time` 与 `X-Client-Hash`；固定 `User-Agent`、`App-OS`、`App-OS-Version`、`App-Version`、`Accept-Language`。
- **网络模式**：复用原 Flutter `NetworkMode` 概念（standard/ech/compat）。M4 先实现 `standard` 模式（直连），`ech` 与 `compat` 模式在 M4 完成后通过后续补丁补充，避免阻塞主路径。
- **认证流程**：
  - 登录使用 Pixiv OAuth2 授权码流程（PKCE），生成 `code_verifier` 与 `code_challenge`，打开 WebView 让用户授权，回调中提取 `code` 换 `access_token`/`refresh_token`。
  - 不实现密码登录（原 Flutter 已废弃）。
  - Token 刷新在 401 触发时由拦截器自动执行，刷新成功后重试原请求。
- **Account 存储**：复用 M2 已迁移的 `AccountDatabase`（原 `account.db`），字段与旧 Flutter `AccountPersist` 保持一致。当前登录账号从数据库读取，支持多账号切换的 UI 在 M5 实现。
- **Repository 接口**：按页面定义 Repository 接口，如 `IllustRepository`、`UserRepository`、`SearchRepository`，先返回 `Flow` 或挂起函数结果；UI 层在 `commonMain` 中通过简单 StateHolder 或 Compose `produceState` 消费。
- **错误处理**：Repository 将网络异常映射为 sealed class `NetworkResult<T>`（Success/Error/Loading）或抛出自定义异常；UI 层统一展示重试按钮与错误文案。
- **图片加载**：继续使用 Coil3，图片 URL 从 API 响应中获取；图片域名 `i.pximg.net` 受 Referer 限制，Coil 请求需添加 `Referer: https://app-api.pixiv.net/`。
- **数据模型复用**：M2 已定义的 `Illust`、`UserDetail` 等模型可直接用于 API 反序列化；如发现字段缺失，就地补充默认值。

## Testing Decisions

- **单元测试**：
  - 测试 `PixivHeaders` 生成的时间戳格式与 MD5 计算结果。
  - 测试 `OAuthClient.generateCodeChallenge`/`generateCodeVerifier` 符合 PKCE 规范。
  - 使用 Ktor `MockEngine` 测试 Repository 在 200/401/500 下的行为。
- **集成测试**：
  - 在 Desktop 端运行应用，验证推荐接口返回真实数据并渲染到 `HelloScreen`。
  - 验证 Token 刷新流程（可通过临时修改本地 token 为过期值触发）。
- **UI 测试**：
  - 验证加载、错误、空态占位在 `IllustStaggeredGrid` 中的展示。

## Out of Scope

- 图片下载、保存、分享任务管理（M5）。
- 小说、评论、收藏夹、关注列表、历史记录等二级页面（M5/M6）。
- `ech` 与 `compat` 网络模式完整实现（M4 仅保留接口，默认 `standard`）。
- 多账号切换 UI（M5）。
- IAP、小组件、DeepLink、推送等系统能力。

## Further Notes

- 保持与原 Flutter 应用一致的 API 调用行为，不添加原应用不支持的功能。
- 所有新增代码须通过 Android + Desktop 编译验证。
- M4 完成后进行一次 code review，审查重点为：异常处理、Token 安全、Repository 接口设计、跨平台兼容性。
