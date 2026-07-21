# M50 PRD：屏蔽设置页补齐（画师与作品管理）

## 状态
已完成。

## 目标
M49 已完成屏蔽标签管理。原 Flutter 版「屏蔽设置」页除标签外，还展示被屏蔽的画师（painter）与作品（illust），并允许用户删除。M50 在 MIUIX 版 `ShieldScreen` 中补齐这两个分组，使其与原应用功能保持一致；不新增原应用未支持的添加入口（画师/作品仍由其他页面加入屏蔽列表）。

## 必做（按最小任务量拆分）

1. **数据加载**
   - 进入 `ShieldScreen` 时同时加载被屏蔽画师、被屏蔽作品与标签。
   - 分组按名称字典序排序：画师按 `name.toLowerCase()`，作品按 `name.toLowerCase()`。

2. **画师分组**
   - 使用 `SmallTitle` 与 `FlowRow` 展示「画师」分组。
   - 每个画师以 chip 形式展示名称，点击弹出 `SuperDialog` 确认删除。
   - 确认后调用 `BanRepository.deleteBanUser(id)`，成功后刷新列表。
   - 不显示添加按钮（对齐原应用）。

3. **作品分组**
   - 使用 `SmallTitle` 与 `FlowRow` 展示「作品」分组。
   - 每个作品以 chip 形式展示名称/标题，点击弹出 `SuperDialog` 确认删除。
   - 确认后调用 `BanRepository.deleteBanIllust(id)`，成功后刷新列表。
   - 不显示添加按钮（对齐原应用）。

4. **删除确认对话框复用**
   - 将现有标签删除确认对话框抽取为通用 `DeleteConfirmationDialog`，接收标题、摘要、加载态与确认/取消回调。
   - 标签、画师、作品删除均使用该对话框，减少重复代码。

5. **加载与错误状态**
   - 加载中显示 `LoadingPlaceholder` 或分组内禁用交互，避免操作冲突。
   - 删除操作期间禁用对应删除按钮，防止重复提交。
   - 加载或删除失败时通过 `ToastMessage` 提示用户。

## 技术决策

- 复用现有 `BanRepository`：通过 `getAllBanUsers`、`deleteBanUser`、`getAllBanIllusts`、`deleteBanIllust` 操作旧 `banuserid.db` 与 `banillustid.db`。
- 保持 `ShieldScreen` 单文件实现，不新增独立页面；分组直接追加在「标签」分组之后。
- 使用 `FlowRow` 展示 chips，与 M49 标签分组风格一致。
- 使用 `SuperDialog`（MIUIX 0.8.8）作为确认对话框，保持与 M49 一致。

## 验收条件

- [x] 屏蔽设置页显示「画师」分组及所有已屏蔽画师 chips。
- [x] 屏蔽设置页显示「作品」分组及所有已屏蔽作品 chips。
- [x] 点击画师/作品 chip 弹出确认对话框，确认后删除并刷新列表。
- [x] 删除操作期间禁用确认按钮，避免重复提交。
- [x] 加载或删除失败时给出 Toast 提示。
- [x] Android + Desktop 双端编译通过。
- [x] M50 code review 完成，无 P0/P1 问题遗留。

## 不在范围

- 从屏蔽设置页添加新的画师/作品（原应用未提供该入口）。
- AI 作品显示设置（Pixiv 账号级设置）页面，留待后续里程碑。
- 屏蔽列表导出/导入功能。
