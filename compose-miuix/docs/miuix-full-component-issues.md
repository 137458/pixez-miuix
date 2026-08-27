# 拆分 Issue：全面接入 MIUIX / HyperOS 原生组件体系与视觉架构

> 因仓库禁用 GitHub Issues，以下切片以本地文档形式跟踪。父文档见 `miuix-full-component-prd.md`。

---

## Issue #1：偏好设置进阶重构与 SliderPreference/CheckboxPreference 落地

### Parent

`miuix-full-component-prd.md`

### What to build

1. 重构 `CrossAdapterSettingScreen.kt` 与 `LayoutSettingScreen.kt` 中的列宽阈值调节组件，使用 MIUIX 原生 `SliderPreference` 替代手写的 `BoxWithConstraints + Column + Slider`。
2. 接入 `SliderPreference` 的吸附关键点（Magnetic snapping）与数值实时标签，支持触觉振动反馈与无障碍读数。
3. 在屏蔽设置、多选操作中梳理接入 `CheckboxPreference`，规范多选开关布局。

### Acceptance criteria

- [ ] `CrossAdapterSettingScreen.kt` 竖屏/横屏自适应列宽调节使用原生 `SliderPreference`；
- [ ] `LayoutSettingScreen.kt` 对应列宽调节与阈值展示使用原生 `SliderPreference`；
- [ ] 列宽拖拽时阈值数值实时响应，拖动结束写回 `SettingsRepository`；
- [ ] 偏好项处于禁用状态时视觉与触控正确禁用；
- [ ] Desktop 与 Android 双端编译通过。

### Blocked by

None - can start immediately.

---

## Issue #2：Overlay 与 Window 弹窗/浮层体系规范化与级联菜单接入

### Parent

`miuix-full-component-prd.md`

### What to build

1. 全面梳理应用内的弹窗与浮层：页面级弹窗（删除确认、格式编辑、单页操作）统一收敛至 `OverlayDialog` / `OverlayBottomSheet`；全局级弹窗（版本更新升级提示、全局未登录提示）规范为 `WindowDialog`。
2. 在 `IllustDetailScreen.kt`、`UserDetailScreen.kt` 等顶栏操作区接入 `OverlayIconDropdownMenu`，将原本平铺在底部抽屉或难以发现的操作（复制作品链接、浏览器打开、屏蔽画师/标签）以原生锚点气泡菜单形式挂载在更多按钮上。
3. 在搜索与分类筛选中引入 `OverlayListPopup` 与 `OverlayCascadingListPopup` 支持多级分组快速展开。

### Acceptance criteria

- [ ] 页面级确认弹窗均依附于 `Scaffold` 宿主层级，页面出栈时无残留悬空；
- [ ] `IllustDetailScreen` 右上角支持点击弹出 MIUIX 原生 `OverlayIconDropdownMenu`；
- [ ] 气泡菜单在深浅色主题下高光、投影与圆角渲染正常；
- [ ] 菜单点击外部或点击任意项后自动收起；
- [ ] Desktop 与 Android 双端编译通过。

### Blocked by

None - can start immediately.

---

## Issue #3：全局长列表与瀑布流接入 MIUIX 垂直滚动条 (VerticalScrollBar)

### Parent

`miuix-full-component-prd.md`

### What to build

1. 为所有核心双列/单列瀑布流与超长列表页面接入 `top.yukonga.miuix.kmp.basic.VerticalScrollBar`：
   - `HelloScreen.kt`（推荐流 / 关注动态流）
   - `RankingScreen.kt`（日榜/周榜/月榜网格）
   - `SearchScreen.kt`（搜索结果作品网格）
   - `HistoryScreen.kt`（历史记录瀑布流）
   - `CommentsScreen.kt`（评论区长列表）
   - `DownloadHistoryScreen.kt` 与 `DownloadTaskScreen.kt`（下载历史与任务队列）
2. 使用 `rememberScrollBarAdapter(listState)` 绑定，在最外层 `Box` 容器中贴边对齐；
3. 将列表的 `contentPadding` 传递为 `trackPadding`，确保滚动条与悬浮底栏或透明顶栏保持安全间距，不发生遮挡。

### Acceptance criteria

- [ ] 上述所有核心列表在滑行时均能平滑显示 MIUIX 原生贴边滚动条；
- [ ] 在 Desktop / 平板设备上支持鼠标拖拽滚动条快速跳至列表任意位置；
- [ ] 滚动条在停止滚动后平滑淡出，不阻挡列表项点击；
- [ ] 底部边缘与悬浮底栏（Floating Bottom Bar）安全留白，不发生视觉穿插；
- [ ] Desktop 与 Android 双端编译通过。

### Blocked by

None - can start immediately.

---

## Issue #4：全局微交互气泡 (TooltipBox) 与原生角标 (Badge) 体系

### Parent

`miuix-full-component-prd.md`

### What to build

1. 封装符合 MIUIX 规范的 `MiuixTooltipIconButton` 或直接使用 `TooltipBox + PlainTooltip`：
   - 为顶栏无文字图标按钮（返回、搜索、过滤、排序、设置）接入气泡提示；
   - 为插画/漫画详情页操作区（收藏、原图、下载、分享、评论）接入气泡提示。
2. 全局接入原生 `Badge` 与 `BadgedBox`：
   - 将 `SettingsScreen.kt` 更新标记 `"NEW"` 替换为原生 `Badge`；
   - 将作品卡片左上角 `"AI"` 生成标识与多图 `"P{N}"` 标识由手写 Box 重构为原生 `Badge`；
   - 为底部导航栏（动态项、下载任务项）接入 `BadgedBox` 红点徽标。

### Acceptance criteria

- [ ] 桌面端鼠标悬停在关键操作按钮上方 500ms 内弹出 MIUI 胶囊气泡，移开后淡出；
- [ ] 移动端长按关键操作按钮触发触觉反馈并展示气泡；
- [ ] 所有气泡文案统一收敛至 `LocalStrings.current` 多语言；
- [ ] 作品卡片与设置项中的 Badge 样式符合 HyperOS 高对比度胶囊规范；
- [ ] Desktop 与 Android 双端编译通过。

### Blocked by

None - can start immediately.

---

## Issue #5：原生拾色器 (ColorPicker) 与滚轮数字选择器 (NumberPicker)

### Parent

`miuix-full-component-prd.md`

### What to build

1. 重构 `ThemeSettingScreen.kt` 中的自定义种子色选择器，接入 `top.yukonga.miuix.kmp.basic.ColorPicker` 与 `ColorPalette` 原生组件，替代手写预设方块网格，支持连续色环取色与实时调色板预览。
2. 重构 `DownloadSettingScreen.kt` 并发任务数设置，以及缓存清理保留天数等场景，接入 `top.yukonga.miuix.kmp.basic.NumberPicker` 原生滚轮组件，提供平滑阻尼选数体验。

### Acceptance criteria

- [ ] 种子色选择弹窗内原生渲染 `ColorPicker`，拖动色环或调整明度实时响应；
- [ ] 选定颜色后全局主题即时刷新，并持久化到本地仓库；
- [ ] 并发任务数在对话框或偏好行中可通过 `NumberPicker` 上下滚动选择；
- [ ] Desktop 与 Android 双端编译通过。

### Blocked by

None - can start immediately.

---

## Issue #6：面包屑路径导航 (BreadcrumbBar) 与悬浮操作栏 (FloatingToolbar)

### Parent

`miuix-full-component-prd.md`

### What to build

1. 在存储与下载路径选择或文件浏览中，引入 `top.yukonga.miuix.kmp.basic.BreadcrumbBar + BreadcrumbItem`，实现分段层级路径展示并支持各级面包屑点击跳转。
2. 在全屏大图查看或漫画阅读模式下，在 `Scaffold.floatingToolbar` 槽位嵌入 MIUI 原生 `FloatingToolbar` 胶囊悬浮操作栏，支持折叠、半透明与平滑退场。

### Acceptance criteria

- [ ] 路径层级通过 `BreadcrumbBar` 优雅拆分，路径过长时自动支持水平平滑滑动；
- [ ] 全屏浏览模式下 `FloatingToolbar` 居中悬浮于底部，点击操作与转场流畅；
- [ ] Desktop 与 Android 双端编译通过。

### Blocked by

None - can start immediately.

---

## Issue #7：引入连续曲率超椭圆 (miuix-squircle) 与渐进式毛玻璃 (miuix-blur)

### Parent

`miuix-full-component-prd.md`

### What to build

1. 在 `libs.versions.toml` 与 `build.gradle.kts` 中确认并引入 `miuix-squircle` 与 `miuix-blur` 依赖；
2. 全局替换作品卡片、画师头像、对话框与常规容器的 `RoundedCornerShape`，采用 `Modifier.squircleSurface` / `Modifier.squircleClip` 实现 HyperOS 标志性 Squircle 连续曲率平滑圆角；
3. 为悬浮底栏、沉浸式顶栏接入 `Modifier.progressiveTextureBlur`，实现景深边缘平滑渐变毛玻璃效果。

### Acceptance criteria

- [ ] `miuix-squircle` 成功引入并在高版本 Android / Desktop 启用 Shader 级超椭圆渲染；
- [ ] 低版本系统自动平滑降级，无类加载异常或运行时崩溃；
- [ ] 悬浮底栏具备自然的毛玻璃通透感与景深过渡；
- [ ] Desktop 与 Android 双端编译通过。

### Blocked by

Issue #1 至 #6 完成基础组件规范化后执行。
