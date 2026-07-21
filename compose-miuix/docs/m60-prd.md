# M60 PRD：收藏标签管理页

## 状态

已完成。

## 目标

原 Flutter 版设置页提供「收藏标签」入口，用户可以管理常用的搜索/收藏标签列表，并点击标签直接查看搜索结果。当前 MIUIX 版缺少该入口与页面。M60 补齐收藏标签管理页，仅做 UI 层替换，数据沿用旧版 `book_tag_list` 键，不新增业务功能。

## 范围

- `SettingsRepository` 新增 `bookTagList` 属性，通过 `SettingsKeys.BOOK_TAG_LIST` 以 JSON 字符串列表读写，兼容旧版 `flutter.book_tag_list`。
- `RootComponent` 新增 `Config.BookTag` 与 `Child.BookTag`，提供 `onBookTagClicked()` 导航。
- `RootContent` 映射 `Child.BookTag` 到新的 `BookTagScreen`。
- 创建 `BookTagScreen`：
  - 展示当前收藏标签列表。
  - 点击标签跳转搜索页（调用 `onTagSearch(tag)`）。
  - 支持新增标签（SuperDialog + TextField）。
  - 支持删除标签（确认对话框）。
  - 支持上移/下移调整顺序。
- `SettingsScreen` 新增「收藏」分组与「收藏标签」入口。

## 不在范围

- 应用内直接展示每个标签的 Tab 搜索结果（跳转 `SearchScreen` 实现等价功能）。
- 标签导入/导出（原应用通过 SAF/iOS Share 实现，MIUIX 版暂未提供文件选择能力）。

## 技术决策

- 标签列表使用 `remember { mutableStateOf(...) }` 缓存，变更后同步写回 `SettingsRepository.setStringList`。
- 新增/删除/排序操作期间禁用相关按钮，避免重复提交。
- 空列表时展示占位提示并显式提供添加入口。
- 与 `ShieldScreen` 的删除确认对话框风格保持一致。

## 实现步骤

1. `SettingsKeys.kt` 确认 `BOOK_TAG_LIST` 常量；`SettingsRepository.kt` 新增 `bookTagList` 属性。
2. `RootComponent.kt` 新增 `Config.BookTag`、`Child.BookTag` 与导航方法。
3. `RootContent.kt` 映射并传递 `onTagSearch`。
4. 创建 `BookTagScreen.kt`。
5. `SettingsScreen.kt` 新增入口。
6. 编译验证与 code review。

## 验收条件

- [x] `SettingsRepository` 可正确读写 `book_tag_list`。
- [x] `SettingsScreen` 显示「收藏 > 收藏标签」入口。
- [x] `BookTagScreen` 正确展示标签列表，空列表时有占位提示。
- [x] 新增标签后即时保存并刷新列表。
- [x] 删除标签需确认，删除后即时保存。
- [x] 上移/下移可调整标签顺序并即时保存。
- [x] 点击标签可跳转搜索页（调用外部导航）。
- [x] Android + Desktop 双端编译通过。
- [x] M60 code review 完成，无 P0/P1 问题遗留。
