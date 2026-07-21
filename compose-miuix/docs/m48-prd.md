# M48 里程碑：设置页接入「屏蔽设置」入口与 AI 过滤开关

## 状态
已完成。

## 目标
M47 已实现 AI 作品本地过滤，但用户无法在 MIUIX 版中开关该功能。M48 在设置页新增「屏蔽设置」入口，打开屏蔽设置页后提供「使带有 AI 生成标记的作品不可见」开关，读写 `SettingsRepository.banAIIllust`，确保用户可控制 M47 的过滤行为。

## 范围

### 必做（按最小任务量拆分）

1. **路由与导航**
   - 在 `RootComponent` 新增 `Config.Shield` 与 `Child.Shield`。
   - 新增 `onShieldClicked()` 导航方法。
   - 在 `RootContent` 处理 `Child.Shield` 并渲染 `ShieldScreen`。

2. **设置页入口**
   - `SettingsScreen` 新增 `onShieldClick` 参数。
   - 在设置页增加「屏蔽设置」分组与入口项，点击后跳转屏蔽页。

3. **屏蔽设置页**
   - 新建 `ShieldScreen.kt`。
   - 页面标题「屏蔽设置」，提供返回按钮。
   - 提供开关项：「使带有 AI 生成标记的作品不可见」。
   - 开关状态绑定 `settingsRepository.banAIIllust`，切换时持久化到设置存储。

4. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M48 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用 `SettingsRepository.banAIIllust`（M47 已新增），无需新增数据层。
- 屏蔽页直接接收 `settingsRepository` 进行读写，与 `SettingsScreen` 主题模式持久化方式一致。
- 仅实现 AI 过滤开关；标签/画师/作品屏蔽管理留到 M49 及以后，避免单次任务量过大。

## 验收条件

- [x] 设置页出现「屏蔽设置」入口，点击进入屏蔽设置页。
- [x] 屏蔽设置页显示「使带有 AI 生成标记的作品不可见」开关。
- [x] 开关状态与 `banAIIllust` 设置一致，切换后写入设置并在返回列表页时生效。
- [x] Android + Desktop 双端编译通过。
- [x] M48 code review 完成，无 P0/P1 问题遗留。
