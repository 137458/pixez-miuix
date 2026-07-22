# M71 PRD：账号信息编辑页

## 状态

已完成。

## 目标

原 Flutter 版设置页提供「账号信息」入口（`AccountEditPage`），支持修改密码、邮箱、导出 Token、账号注销。当前 MIUIX 版 `SettingsScreen` 已展示账号头像与登出按钮，但缺少账号详情编辑入口。M71 移植该页面，仅做 UI 层替换与接口调用，不新增业务功能。

## 范围

- 复用现有 `AccountRepository` 读取当前账号信息。
- `RootComponent` 新增 `Config.AccountEdit` 与 `Child.AccountEdit`，提供 `onAccountEditClicked()` 导航。
- `RootContent` 映射 `Child.AccountEdit` 到新的 `AccountEditScreen`。
- 创建 `AccountEditScreen`：
  - 使用 `Scaffold`、`TopAppBar`、`LazyColumn`、`TextField`、`Button` 等 MIUIX 组件。
  - 展示只读的账号名（Pixiv ID）。
  - 提供当前密码、新密码、邮箱输入框；保存时调用账号编辑接口。
  - 若 `isMailAuthorized == 1`，提供「复制 Refresh Token」入口。
  - 提供「账号注销」入口，点击后二次确认并跳转 `AccountDeletionWebview`（若 WebView 能力已接入）或打开系统浏览器。
- `SettingsScreen` 在「账号」分组新增「账号信息」入口（已登录时显示）。

## 不在范围

- 不修改登录/注册流程。
- 不新增 Pixiv OAuth 接口；仅复用现有 `AccountRepository` 或网络层。
- 不实现账号切换多账号管理（已有 `AccountSelectPage` 逻辑不在本次范围）。
- 不在 Desktop 端实现 WebView 内嵌注销页（回退到系统浏览器）。

## 技术决策

- 与 `SettingsScreen` 账号分组风格保持一致：入口放在账号头像下方。
- 密码输入框支持显示/隐藏切换。
- 保存前做邮箱格式校验；接口失败时通过 `ToastMessage` 展示错误信息。
- Refresh Token 复制通过平台剪贴板能力实现（复用现有 `IllustClipboard` 或新增 expect/actual）。

## 实现步骤

1. `RootComponent.kt` 新增 `Config.AccountEdit`、`Child.AccountEdit` 与 `onAccountEditClicked()`。
2. `RootContent.kt` 导入 `AccountEditScreen` 并映射 `Child.AccountEdit`。
3. 创建 `AccountEditScreen.kt`：
   - 从 `AccountRepository` 获取当前账号。
   - 使用 `TextField` 收集当前密码、新密码、邮箱。
   - 实现保存按钮：校验后调用编辑接口，成功后更新本地账号缓存。
   - 实现 Token 复制与账号注销入口。
4. `SettingsScreen.kt` 在「账号」分组新增「账号信息」入口，参数增加 `onAccountEditClick`。
5. 在 `RootContent` 中 `SettingsScreen` 调用处传入 `component::onAccountEditClicked`。
6. 编译验证与 code review。

## 验收条件

- [ ] 已登录时 `SettingsScreen`「账号」分组显示「账号信息」入口。
- [ ] `AccountEditScreen` 正确展示账号名、邮箱等当前信息。
- [ ] 修改邮箱/密码后点击保存，调用接口并给出成功/失败提示。
- [ ] 已邮箱认证账号显示「复制 Refresh Token」入口且可复制。
- [ ] 账号注销入口点击后二次确认。
- [ ] Android + Desktop 双端编译通过。
- [ ] M71 code review 完成，无 P0/P1 问题遗留。
