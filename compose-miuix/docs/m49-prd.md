# M49 里程碑：屏蔽标签管理 UI

## 状态
已完成。

## 目标
M45 已完成屏蔽标签数据层，M46 已将其接入列表页过滤，但用户仍无法在 MIUIX 版中查看、添加或删除被屏蔽的标签。M49 在屏蔽设置页提供标签管理功能：展示现有屏蔽标签（按名称字典序排序）、添加新标签（支持普通标签与 `r'...'` 正则形式）、删除已有标签。

## 范围

### 必做（按最小任务量拆分）

1. **依赖传递**
   - `ShieldScreen` 新增 `banRepository: BanRepository` 参数。
   - `RootContent` 在渲染 `Child.Shield` 时同时传入 `settingsRepository` 与 `banRepository`。

2. **屏蔽标签展示**
   - 进入 `ShieldScreen` 时通过 `LaunchedEffect` 加载全部屏蔽标签。
   - 使用 `FlowRow` 展示标签 chips，按 `name.toLowerCase()` 字典序排序。
   - 标签 chip 使用 Miuix `Button` / `TextButton` 实现，点击后弹出删除确认对话框。

3. **添加标签**
   - 「标签」分组右侧提供添加按钮，点击弹出 `SuperDialog`（MIUIX 0.8.8 中对话框组件为 `SuperDialog`，与新版 `OverlayDialog` 等价）。
   - 对话框内包含一个 `TextField`，输入标签名或正则表达式（如 `r'R-18$'`）。
   - 确认后调用 `BanRepository.insertBanTag(name, translateName = "")`，成功后刷新标签列表；操作期间禁用确认按钮，避免重复提交。

4. **删除标签**
   - 点击标签 chip 弹出 `SuperDialog` 确认对话框，确认后调用 `BanRepository.deleteBanTag(id)`，成功后刷新列表；操作期间禁用删除按钮，避免重复提交。

5. **错误与加载提示**
   - 增删查操作失败时通过 `ToastMessage` 提示用户。
   - 操作期间禁用相关按钮，避免重复提交。

6. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M49 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用 `BanRepository` 中已实现的 `getAllBanTags()`、`insertBanTag()`、`deleteBanTag()`。
- `translateName` 在标签管理场景中暂不提供单独输入，新增时传空字符串；后续若需要可按同样模式扩展。
- 正则标签与普通标签共用同一输入框，用户通过 `r'...'` 前缀自行书写，与旧 Flutter 行为一致。
- 标签删除前必须二次确认，避免误触。

## 验收条件

- [x] 屏蔽设置页显示「标签」分组及所有已屏蔽标签 chips。
- [x] 点击添加按钮可输入并保存新标签（含正则形式）。
- [x] 保存后标签列表实时刷新。
- [x] 点击标签 chip 弹出确认对话框，确认后删除并刷新列表。
- [x] 增删失败时给出 Toast 提示。
- [x] Android + Desktop 双端编译通过。
- [x] M49 code review 完成，无 P0/P1 问题遗留。
