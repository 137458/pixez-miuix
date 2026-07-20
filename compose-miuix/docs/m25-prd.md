# M25 里程碑：设置页账号信息与退出登录

## 状态
已完成。

## 目标
原 Flutter PixEz 的设置页展示当前登录账号信息并提供「退出登录」入口。M25 将为 MIUIX 版 `SettingsScreen` 增加账号分组，已登录时显示头像、名称、账号与退出按钮，未登录时显示登录入口。

## 范围

### 必做（按最小任务量拆分）

1. **账号分组 UI**
   - `SettingsScreen` 新增 `accountRepository: AccountRepository` 与 `onLoginClick: () -> Unit` 参数。
   - 使用 `produceState` 异步加载 `currentAccount()`。
   - 在「主题」分组上方新增「账号」分组：
     - 已登录：展示账号头像、`name`/`account`/`userId` 文本，底部提供「退出登录」按钮。
     - 未登录：展示「未登录」文本与「去登录」按钮。

2. **退出登录逻辑**
   - 点击「退出登录」后调用 `accountRepository.logout()`。
   - 使用 `try/finally` 确保加载态重置；成功后将当前账号状态置为 null。
   - 取消时也能保证加载态重置。

3. **路由接入**
   - `RootContent` 中 `Settings -> SettingsScreen` 调用处传入 `accountRepository` 与 `component::onLoginClicked`。

## 验收条件

- [x] 设置页展示当前登录账号的头像、名称、账号信息。
- [x] 未登录时显示「去登录」按钮并跳转到登录页。
- [x] 点击「退出登录」后清空本地账号数据并刷新界面为未登录状态。
- [x] Android + Desktop 双端编译通过。
- [x] M25 code review 完成，无 P0/P1 问题遗留。
