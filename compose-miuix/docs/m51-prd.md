# M51 PRD：AI 作品显示设置页

## 状态
已完成。

## 目标
原 Flutter 版「屏蔽设置」页提供「AI 作品显示设置」入口，点击后从 Pixiv 服务器拉取当前账号的 AI 显示偏好，并在二级页面展示「显示」与「部分隐藏」两个互斥选项；用户切换选项后调用服务器接口更新设置，并以服务器返回结果刷新页面选中状态。M51 在 MIUIX 版中补齐该入口与二级设置页，保持与原应用功能一致。

## 必做（按最小任务量拆分）

1. **网络层 API**
   - 在 `UserRepository` 中新增 `getUserAISettings(): ShowAIResponse`，调用 `GET /v1/user/ai-show-settings`。
   - 在 `UserRepository` 中新增 `updateUserAISettings(showAI: Boolean): ShowAIResponse`，调用 `POST /v1/user/ai-show-settings/edit`，使用 `application/x-www-form-urlencoded` 表单提交参数 `show_ai`。
   - 复用现有 `ShowAIResponse` 数据模型（字段 `show_ai: Boolean`）。

2. **导航与路由**
   - 在 `RootComponent` 中新增 `onAISettingClicked()` 导航方法与 `Config.AISetting` 路由。
   - 在 `RootContent` 中处理 `Child.AISetting`，渲染新的 `UserShowAISettingScreen`。

3. **屏蔽设置页入口**
   - 在 `ShieldScreen` 的「AI 作品」分组下新增「AI 作品显示设置」条目（`BasicComponent`）。
   - 点击时显示加载状态，调用 `UserRepository.getUserAISettings()`，成功后跳转；失败时通过 `ToastMessage` 提示。

4. **AI 作品显示设置页**
   - 新建 `UserShowAISettingScreen`：顶部标题栏含返回按钮，主体为两个选项行「显示」与「部分隐藏」。
   - 页面进入时根据传入的初始 `showAI` 值高亮对应选项。
   - 点击未选中选项时调用 `UserRepository.updateUserAISettings()`，根据返回结果刷新高亮状态。
   - 更新期间禁用选项点击或显示加载反馈，避免重复提交；失败时通过 `ToastMessage` 提示。

5. **依赖注入**
   - `RootContent` 向 `ShieldScreen` 传递 `userRepository`。
   - `RootContent` 向 `UserShowAISettingScreen` 传递 `userRepository`。

## 技术决策

- 将 AI 设置 API 归入 `UserRepository`，因为该设置属于用户账号级偏好，与 `UserRepository` 职责一致。
- 二级页面采用独立 `Config` 路由，符合现有设置/关于/屏蔽等二级页面模式。
- 选项行复用 `BasicComponent`，右侧通过 `endActions` 展示选中标记（✓），与 `SettingsScreen` 的主题选择模式保持一致。
- 入口加载与页面更新均使用 `runCatchingNonCancel` 处理异常，并通过 `ToastMessage` 反馈。
- 屏蔽设置页的「AI 作品显示设置」入口放在现有的「AI 作品」分组内，位于本地 AI 过滤开关之后，对齐原应用布局。

## 验收条件

- [x] `UserRepository` 新增 `getUserAISettings` 与 `updateUserAISettings`，请求方式与参数与原 Flutter 版一致。
- [x] `RootComponent` 提供 `onAISettingClicked` 导航，`RootContent` 正确渲染新页面。
- [x] `ShieldScreen` 显示「AI 作品显示设置」入口，点击后先加载网络设置再跳转。
- [x] `UserShowAISettingScreen` 正确展示「显示」与「部分隐藏」选项，并高亮当前设置。
- [x] 切换选项后调用更新接口，并根据返回结果刷新高亮；失败时给出 Toast 提示。
- [x] Android + Desktop 双端编译通过。
- [x] M51 code review 完成，无 P0/P1 问题遗留。

## 不在范围

- AI 作品过滤本地开关（已在 M47 完成）。
- 屏蔽标签/画师/作品的添加删除（已在 M49/M50 完成）。
- 其他 Pixiv 账号级设置项。
