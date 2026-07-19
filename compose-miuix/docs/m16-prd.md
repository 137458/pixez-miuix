# M16 里程碑：用户详情操作菜单

## 状态
已完成。commit 见 `99e62e73`。

## 目标
原 Flutter PixEz 用户详情页 AppBar 提供「分享」按钮，弹出菜单提供「复制信息」等操作。M16 将为 MIUIX 用户详情页补充对应的操作菜单能力，使用户可复制用户信息或主页链接。

## 范围

### 必做（按最小任务量拆分）

1. **用户详情操作菜单组件**
   - 新增 `UserActionMenu` 组件，基于 MIUIX `SuperBottomSheet`。
   - 提供「复制信息」与「复制链接」两项操作。
   - 复用现有 `ToastMessage` 显示操作结果。

2. **用户详情页接入**
   - `UserDetailScreen` TopAppBar 增加「更多」按钮。
   - 点击后打开 `UserActionMenu`。
   - 调用现有 `IllustClipboard` 复制，并通过 Toast 反馈成功/失败。
   - 「复制信息」格式与原应用保持一致：`painter:{user.name}\npid:{user.id}`。
   - 「复制链接」使用 `https://www.pixiv.net/users/{user.id}`。

3. **边界处理**
   - 「更多」按钮在用户资料未加载完成时禁用。

## 验收条件

- [x] `UserActionMenu` 组件已实现并提供复制信息 / 复制链接。
- [x] `UserDetailScreen` 可通过「更多」按钮打开菜单并执行复制。
- [x] 复制成功/失败均通过 Toast 反馈。
- [x] Android + Desktop 双端编译通过。
- [x] M16 code review 完成，无 P0/P1 问题遗留。
